package com.example.lab3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.app.PendingIntent;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class DownloadService extends Service {
    public static final String TAG = DownloadService.class.getSimpleName();
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_FILE_NAME = "file";

    private static final String CHANNEL_ID = "download";
    private static final int NOTIFICATION_ID = 1;

    private final MutableLiveData<ProgressEvent> progressLiveData = new MutableLiveData<>(null);
    private final IBinder binder = new DownloadBinder();
    private HandlerThread handlerThread;
    private Handler handler;
    private NotificationManager notificationManager;

    public class DownloadBinder extends android.os.Binder {
        public LiveData<ProgressEvent> getProgressEvent() {
            return progressLiveData;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handlerThread = new HandlerThread("DownloadService");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String url = intent.getStringExtra(EXTRA_URL);
        final String name = intent.getStringExtra(EXTRA_FILE_NAME);
        handler.post(() -> downloadFile(url, name));
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Download", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(ProgressEvent event, boolean ongoing) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("progress", event);
        PendingIntent pendingIntent = TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(notificationIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(event.total, event.progress, event.total == 0)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing);
        if (event.progress >= event.total && event.total > 0) {
            builder.setContentTitle(getString(R.string.notification_text_finished))
                    .setOngoing(false)
                    .setAutoCancel(true);
        } else {
            builder.setContentTitle(getString(R.string.notification_title_downloading));
        }
        return builder.build();
    }

    private void downloadFile(String urlStr, String fileName) {
        Log.d(TAG, "downloadFile: start");
        HttpsURLConnection connection = null;
        OutputStream outputStream = null;
        Uri fileUri = null;
        int downloaded = 0;
        int total = 0;
        ProgressEvent lastEvent = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            total = connection.getContentLength();
            ProgressEvent startEvent = new ProgressEvent(0, total, ProgressEvent.IN_PROGRESS);
            lastEvent = startEvent;
            progressLiveData.postValue(startEvent);
            startForeground(NOTIFICATION_ID, buildNotification(startEvent, true));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, connection.getContentType());
                values.put(MediaStore.Downloads.IS_PENDING, 1);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                outputStream = resolver.openOutputStream(fileUri);
            } else {
                File outFile = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), fileName);
                if (outFile.exists()) outFile.delete();
                outputStream = new FileOutputStream(outFile);
            }
            DataInputStream reader = new DataInputStream(connection.getInputStream());
            byte[] buffer = new byte[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                downloaded += read;
                ProgressEvent progressEvent = new ProgressEvent(downloaded, total, ProgressEvent.IN_PROGRESS);
                lastEvent = progressEvent;
                progressLiveData.postValue(progressEvent);
                Log.d(TAG, "downloaded " + downloaded + "/" + total);
                notificationManager.notify(NOTIFICATION_ID, buildNotification(progressEvent, true));
            }
            ProgressEvent finishedEvent = new ProgressEvent(downloaded, total, ProgressEvent.OK);
            lastEvent = finishedEvent;
            progressLiveData.postValue(finishedEvent);
            Log.d(TAG, "download complete");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fileUri != null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(fileUri, values, null, null);
            }
        } catch (Exception e) {
            ProgressEvent errorEvent = new ProgressEvent(downloaded, total, ProgressEvent.ERROR);
            lastEvent = errorEvent;
            progressLiveData.postValue(errorEvent);
            Log.d(TAG, "download error", e);
            Log.e(TAG, "downloadFile", e);
        } finally {
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException ignore) {}
            if (connection != null) connection.disconnect();
            stopForeground(STOP_FOREGROUND_DETACH);
            if (lastEvent == null) {
                lastEvent = new ProgressEvent(downloaded, total, ProgressEvent.ERROR);
            }
            notificationManager.notify(NOTIFICATION_ID, buildNotification(lastEvent, false));
            stopSelf();
        }
    }
}
