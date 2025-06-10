package com.example.lab3;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_PROGRESS = "progress";

    private EditText urlInput;
    private TextView sizeText;
    private TextView typeText;
    private TextView progressText;
    private ProgressBar progressBar;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean serviceBound = false;
    private LiveData<ProgressEvent> progressLiveData;
    private ProgressEvent lastProgress;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBound = true;
            DownloadService.DownloadBinder binder = (DownloadService.DownloadBinder) service;
            progressLiveData = binder.getProgressEvent();
            progressLiveData.observe(MainActivity.this, MainActivity.this::updateProgress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (progressLiveData != null) progressLiveData.removeObservers(MainActivity.this);
            serviceBound = false;
        }
    };

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startDownload();
                else Toast.makeText(this, "Brak uprawnień", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        urlInput = findViewById(R.id.editUrl);
        sizeText = findViewById(R.id.textSize);
        typeText = findViewById(R.id.textType);
        progressText = findViewById(R.id.textProgress);
        progressBar = findViewById(R.id.progressBar);
        Button btnInfo = findViewById(R.id.btnInfo);
        Button btnDownload = findViewById(R.id.btnDownload);

        if (savedInstanceState != null) {
            lastProgress = savedInstanceState.getParcelable(EXTRA_PROGRESS);
            if (lastProgress != null) updateProgress(lastProgress);
            String sizeVal = savedInstanceState.getString("sizeText");
            String typeVal = savedInstanceState.getString("typeText");
            String urlVal = savedInstanceState.getString("urlText");
            if (sizeVal != null) sizeText.setText(sizeVal);
            if (typeVal != null) typeText.setText(typeVal);
            if (urlVal != null) urlInput.setText(urlVal);
        } else if (getIntent().hasExtra(EXTRA_PROGRESS)) {
            lastProgress = getIntent().getParcelableExtra(EXTRA_PROGRESS);
            if (lastProgress != null) updateProgress(lastProgress);
        }

        btnInfo.setOnClickListener(v -> fetchInfo());
        btnDownload.setOnClickListener(v -> checkPermissionAndDownload());

        Intent serviceIntent = new Intent(this, DownloadService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            progressLiveData.removeObservers(this);
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (lastProgress != null)
            outState.putParcelable(EXTRA_PROGRESS, lastProgress);
        outState.putString("sizeText", sizeText.getText().toString());
        outState.putString("typeText", typeText.getText().toString());
        outState.putString("urlText", urlInput.getText().toString());
    }

    private void fetchInfo() {
        final String url = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(url) || !url.startsWith("https://")) {
            Toast.makeText(this, R.string.invalid_url_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        sizeText.setText("");
        typeText.setText("");
        executor.execute(() -> {
            HttpsURLConnection connection = null;
            try {
                URL u = new URL(url);
                connection = (HttpsURLConnection) u.openConnection();
                connection.setRequestMethod("GET");
                final int size = connection.getContentLength();
                final String type = connection.getContentType();
                handler.post(() -> {
                    sizeText.setText(String.valueOf(size));
                    typeText.setText(type);
                });
            } catch (IOException e) {
                handler.post(() -> Toast.makeText(MainActivity.this, R.string.fetch_error_toast, Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void checkPermissionAndDownload() {
        String requiredPermission = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermission = Manifest.permission.POST_NOTIFICATIONS;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requiredPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        }
        if (requiredPermission != null && ActivityCompat.checkSelfPermission(this, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, requiredPermission)) {
                Toast.makeText(this, "Potrzebne uprawnienie", Toast.LENGTH_SHORT).show();
            }
            requestPermissionLauncher.launch(requiredPermission);
        } else {
            startDownload();
        }
    }

    private void startDownload() {
        final String url = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(url) || !url.startsWith("https://")) {
            Toast.makeText(this, R.string.invalid_url_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra(DownloadService.EXTRA_URL, url);
        intent.putExtra(DownloadService.EXTRA_FILE_NAME, fileName);
        startService(intent);
    }

    private void updateProgress(ProgressEvent event) {
        lastProgress = event;
        if (event == null) return;
        progressBar.setMax(event.total);
        progressBar.setProgress(event.progress);
        progressText.setText(event.progress + "/" + event.total);
    }
}
