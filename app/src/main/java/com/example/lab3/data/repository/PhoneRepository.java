package com.example.lab3.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.lab3.data.dao.PhoneDao;
import com.example.lab3.data.db.PhoneRoomDatabase;
import com.example.lab3.data.entity.Phone;

import java.util.List;

public class PhoneRepository {

    private final PhoneDao phoneDao;
    private final LiveData<List<Phone>> allPhones;

    public PhoneRepository(Application app) {
        PhoneRoomDatabase db = PhoneRoomDatabase.getDatabase(app);
        phoneDao = db.phoneDao();
        allPhones = phoneDao.getAlphabetizedPhones();
    }

    public LiveData<List<Phone>> getAllPhones() {
        return allPhones;
    }

    public void insert(Phone p) {
        PhoneRoomDatabase.databaseWriteExecutor.execute(() -> phoneDao.insert(p));
    }

    public void update(Phone p) {
        PhoneRoomDatabase.databaseWriteExecutor.execute(() -> phoneDao.update(p));
    }

    public void delete(Phone p) {
        PhoneRoomDatabase.databaseWriteExecutor.execute(() -> phoneDao.delete(p));
    }

    public void deleteAll() {
        PhoneRoomDatabase.databaseWriteExecutor.execute(phoneDao::deleteAll);
    }
}
