package com.example.lab3.network;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lab3.R;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class FileInfoActivity extends AppCompatActivity {
    private EditText urlInput;
    private TextView sizeText;
    private TextView typeText;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_info);
        urlInput = findViewById(R.id.editUrl);
        sizeText = findViewById(R.id.textSize);
        typeText = findViewById(R.id.textType);
        Button btn = findViewById(R.id.btnFetch);
        btn.setOnClickListener(v -> fetchInfo());
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
                handler.post(() -> Toast.makeText(FileInfoActivity.this, R.string.fetch_error_toast, Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}
