package com.example.lab3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab3.data.entity.Phone;
import com.example.lab3.data.viewmodel.PhoneViewModel;
import com.example.lab3.databinding.ActivityMainBinding;
import com.example.lab3.ui.PhoneListAdapter;

public class MainActivity extends AppCompatActivity {

    private static final int ADD_PHONE_REQUEST  = 1;
    private static final int EDIT_PHONE_REQUEST = 2;

    private ActivityMainBinding binding;
    private PhoneViewModel phoneVM;
    private PhoneListAdapter adapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete_all) {
            phoneVM.deleteAll();
            Toast.makeText(this, R.string.all_deleted_toast, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_file_info) {
            startActivity(new Intent(this, com.example.lab3.network.FileInfoActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.topAppBar);

        // adapter z listenerem kliknięcia w wiersz
        adapter = new PhoneListAdapter(this, phone -> {
            Intent intent = new Intent(MainActivity.this, AddPhoneActivity.class);
            intent.putExtra("EXTRA_ID", phone.getId());
            intent.putExtra("EXTRA_MANUFACTURER", phone.getMaker());
            intent.putExtra("EXTRA_MODEL", phone.getModel());
            intent.putExtra("EXTRA_VERSION", phone.getAndroidVersion());
            intent.putExtra("EXTRA_WEBSITE", phone.getWebSite());
            startActivityForResult(intent, EDIT_PHONE_REQUEST);
        });

        binding.phoneListRecyclerView.setAdapter(adapter);
        binding.phoneListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        phoneVM = new ViewModelProvider(this).get(PhoneViewModel.class);
        phoneVM.getAllPhones().observe(this, adapter::setPhones);

        // Swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv,
                                            @NonNull RecyclerView.ViewHolder vh,
                                            @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                Phone p = adapter.getPhoneAtPosition(pos);
                phoneVM.delete(p);
                Toast.makeText(MainActivity.this,
                        "Usunięto telefon", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(binding.phoneListRecyclerView);

        // dodawanie
        binding.fabMain.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(MainActivity.this, AddPhoneActivity.class),
                        ADD_PHONE_REQUEST));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        String manuf = data.getStringExtra("EXTRA_MANUFACTURER");
        String model = data.getStringExtra("EXTRA_MODEL");
        String ver   = data.getStringExtra("EXTRA_VERSION");
        String www   = data.getStringExtra("EXTRA_WEBSITE");

        if (requestCode == ADD_PHONE_REQUEST) {
            Phone newPhone = new Phone(manuf, model, ver, www);
            phoneVM.insert(newPhone);
            Toast.makeText(this, "Dodano telefon", Toast.LENGTH_SHORT).show();
        }
        else if (requestCode == EDIT_PHONE_REQUEST) {
            long id = data.getLongExtra("EXTRA_ID", -1);
            if (id == -1) {
                Toast.makeText(this,
                        "Błąd aktualizacji: brak ID", Toast.LENGTH_SHORT).show();
                return;
            }
            Phone updated = new Phone(manuf, model, ver, www);
            updated.setId(id);
            phoneVM.update(updated);
            Toast.makeText(this,
                    "Zaktualizowano telefon", Toast.LENGTH_SHORT).show();
        }
    }
}
