package com.example.lab3.network;

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
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class DownloadService extends Service {

    public static final String EXTRA_URL = "url";
    private static final String CHANNEL_ID = "download";
    private static final int NOTIFICATION_ID = 1;

    private final MutableLiveData<ProgressEvent> progress = new MutableLiveData<>();
    private final IBinder binder = new LocalBinder();
    private NotificationManager notificationManager;
    private ExecutorService executor;

    public class LocalBinder extends android.os.Binder {
        public LiveData<ProgressEvent> getProgress() { return progress; }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "download", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String url = intent.getStringExtra(EXTRA_URL);
        startForeground(NOTIFICATION_ID, buildNotification(0,0,false));
        progress.postValue(new ProgressEvent());
        executor.execute(() -> downloadFile(url));
        return START_NOT_STICKY;
    }

    private void downloadFile(String fileUrl) {
        HttpsURLConnection connection = null;
        ProgressEvent pe = new ProgressEvent();
        pe.result = ProgressEvent.IN_PROGRESS;
        try {
            URL url = new URL(fileUrl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int total = connection.getContentLength();
            pe.total = total;
            String mime = connection.getContentType();
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            OutputStream out = openOutput(fileName, mime);
            DataInputStream in = new DataInputStream(connection.getInputStream());
            byte[] buf = new byte[4096];
            int read;
            int downloaded = 0;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                downloaded += read;
                pe.progress = downloaded;
                progress.postValue(pe);
                notificationManager.notify(NOTIFICATION_ID,
                        buildNotification(downloaded, total, false));
                Log.d("DownloadService", "bytes=" + downloaded);
            }
            out.close();
            pe.result = ProgressEvent.OK;
            progress.postValue(pe);
            notificationManager.notify(NOTIFICATION_ID,
                    buildNotification(downloaded, total, true));
        } catch (Exception e) {
            pe.result = ProgressEvent.ERROR;
            progress.postValue(pe);
            Log.e("DownloadService", "download error", e);
        } finally {
            if (connection != null) connection.disconnect();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private OutputStream openOutput(String fileName, String mimeType) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            return resolver.openOutputStream(uri);
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File outFile = new File(dir, fileName);
            if (outFile.exists()) outFile.delete();
            return new FileOutputStream(outFile);
        }
    }

    private Notification buildNotification(int progressValue, int total, boolean finished) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(finished ? "Pobieranie zakończone" : "Pobieranie")
                .setProgress(total, progressValue, total==0)
                .setOngoing(!finished);
        return builder.build();
    }
}
