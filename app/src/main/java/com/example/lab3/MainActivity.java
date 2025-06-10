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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;

import com.example.lab3.databinding.ActivityMainBinding;
import com.example.lab3.network.DownloadService;
import com.example.lab3.network.ProgressEvent;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private DownloadService.LocalBinder serviceBinder;
    private boolean bound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBinder = (DownloadService.LocalBinder) service;
            LiveData<ProgressEvent> live = serviceBinder.getProgress();
            live.observe(MainActivity.this, MainActivity.this::updateProgress);
            bound = true;
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Button fetch = binding.btnFetch;
        Button download = binding.btnDownload;

        fetch.setOnClickListener(v -> fetchInfo());
        download.setOnClickListener(v -> startDownload());
    }

    private void fetchInfo() {
        final String url = binding.editUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url) || !url.startsWith("https://")) {
            Toast.makeText(this, R.string.invalid_url_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        binding.textSize.setText("");
        binding.textType.setText("");
        executor.execute(() -> {
            HttpsURLConnection connection = null;
            try {
                URL u = new URL(url);
                connection = (HttpsURLConnection) u.openConnection();
                connection.setRequestMethod("GET");
                final int size = connection.getContentLength();
                final String type = connection.getContentType();
                runOnUiThread(() -> {
                    binding.textSize.setText(String.valueOf(size));
                    binding.textType.setText(type);
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.fetch_error_toast, Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void startDownload() {
        String url = binding.editUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url) || !url.startsWith("https://")) {
            Toast.makeText(this, R.string.invalid_url_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        String required = getRequiredPermission();
        if (!required.isEmpty() && ActivityCompat.checkSelfPermission(this, required) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{required}, 100);
            return;
        }
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra(DownloadService.EXTRA_URL, url);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private static String getRequiredPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.POST_NOTIFICATIONS;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Manifest.permission.WRITE_EXTERNAL_STORAGE;
        }
        return "";
    }

    private void updateProgress(ProgressEvent event) {
        ProgressBar bar = binding.progressBar;
        TextView txt = binding.textProgress;
        if (event.total > 0) {
            bar.setMax(event.total);
            bar.setProgress(event.progress);
            txt.setText(event.progress + " / " + event.total + " B");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (bound) unbindService(connection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startDownload();
        }
    }
}
