package com.example.lab3.data.db;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.lab3.data.dao.PhoneDao;
import com.example.lab3.data.entity.Phone;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Phone.class}, version = 1, exportSchema = false)
public abstract class PhoneRoomDatabase extends RoomDatabase {

    public abstract PhoneDao phoneDao();

    private static volatile PhoneRoomDatabase INSTANCE;

    public static PhoneRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PhoneRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    PhoneRoomDatabase.class,
                                    "phone_database")
                            .addCallback(roomCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final Callback roomCallback = new Callback() {
        @Override public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                PhoneDao dao = INSTANCE.phoneDao();
                dao.deleteAll();
                dao.insert(new Phone("Google",   "Pixel 5",   "Android 14", "https://store.google.com"));
                dao.insert(new Phone("Samsung",  "Galaxy S22","Android 14", "https://www.samsung.com"));
                dao.insert(new Phone("Nothing",  "Phone (2)", "Android 15 beta", "https://nothing.tech"));
            });
        }
    };

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);
}
