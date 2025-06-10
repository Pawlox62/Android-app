package com.example.lab3.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.lab3.data.entity.Phone;

import java.util.List;

@Dao
public interface PhoneDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(Phone phone);

    @Update
    void update(Phone phone);

    @Delete
    void delete(Phone phone);

    @Query("DELETE FROM phones")
    void deleteAll();

    @Query("SELECT * FROM phones ORDER BY maker ASC")
    LiveData<List<Phone>> getAlphabetizedPhones();
}
