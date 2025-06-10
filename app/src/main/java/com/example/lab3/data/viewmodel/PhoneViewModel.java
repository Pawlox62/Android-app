package com.example.lab3.data.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.lab3.data.entity.Phone;
import com.example.lab3.data.repository.PhoneRepository;

import java.util.List;

public class PhoneViewModel extends AndroidViewModel {

    private final PhoneRepository repo;
    private final LiveData<List<Phone>> allPhones;

    public PhoneViewModel(@NonNull Application application) {
        super(application);
        repo = new PhoneRepository(application);
        allPhones = repo.getAllPhones();
    }

    public LiveData<List<Phone>> getAllPhones() {
        return allPhones;
    }

    public void insert(Phone p) {
        repo.insert(p);
    }

    public void update(Phone p) {
        repo.update(p);
    }

    public void delete(Phone p) {
        repo.delete(p);
    }

    public void deleteAll() {
        repo.deleteAll();
    }
}
