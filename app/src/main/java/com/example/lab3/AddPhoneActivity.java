package com.example.lab3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lab3.databinding.ActivityAddPhoneBinding;

public class AddPhoneActivity extends AppCompatActivity {
    private ActivityAddPhoneBinding binding;
    private long phoneId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Tryb edycji
        Intent intent = getIntent();
        if (intent.hasExtra("EXTRA_ID")) {
            phoneId = intent.getLongExtra("EXTRA_ID", -1);
            binding.editManufacturer.setText(intent.getStringExtra("EXTRA_MANUFACTURER"));
            binding.editModel.setText(intent.getStringExtra("EXTRA_MODEL"));
            binding.editAndroidVersion.setText(intent.getStringExtra("EXTRA_VERSION"));
            binding.editWebsite.setText(intent.getStringExtra("EXTRA_WEBSITE"));
            binding.topAppBar.setTitle(R.string.edit_phone_title);
        }

        // Wstecz i Anuluj
        binding.topAppBar.setNavigationOnClickListener(v -> finish());
        binding.btnCancel.setOnClickListener(v -> finish());

        // Zapisz
        binding.btnSave.setOnClickListener(v -> {
            String manuf = binding.editManufacturer.getText().toString().trim();
            String model = binding.editModel.getText().toString().trim();
            String ver   = binding.editAndroidVersion.getText().toString().trim();
            String www   = binding.editWebsite.getText().toString().trim();

            if (manuf.isEmpty()) {
                binding.editManufacturer.setError("Wymagane");
                return;
            }
            if (model.isEmpty()) {
                binding.editModel.setError("Wymagane");
                return;
            }
            if (ver.isEmpty()) {
                binding.editAndroidVersion.setError("Wymagane");
                return;
            }
            if (www.isEmpty()) {
                binding.editWebsite.setError("Wymagane");
                return;
            }

            Intent result = new Intent();
            result.putExtra("EXTRA_MANUFACTURER", manuf);
            result.putExtra("EXTRA_MODEL", model);
            result.putExtra("EXTRA_VERSION", ver);
            result.putExtra("EXTRA_WEBSITE", www);
            if (phoneId != -1) {
                result.putExtra("EXTRA_ID", phoneId);
            }
            setResult(RESULT_OK, result);
            finish();
        });

        // strona WWW
        binding.btnWebsite.setOnClickListener(v -> {
            String url = binding.editWebsite.getText().toString().trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });
    }
}
