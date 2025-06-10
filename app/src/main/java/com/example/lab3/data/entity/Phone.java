package com.example.lab3.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "phones")
public class Phone {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull @ColumnInfo(name = "maker")           private String maker;
    @NonNull @ColumnInfo(name = "model")           private String model;
    @NonNull @ColumnInfo(name = "android_version") private String androidVersion;
    @NonNull @ColumnInfo(name = "web_site")        private String webSite;

    // Konstruktor
    public Phone(@NonNull String maker,
                 @NonNull String model,
                 @NonNull String androidVersion,
                 @NonNull String webSite) {
        this.maker = maker;
        this.model = model;
        this.androidVersion = androidVersion;
        this.webSite = webSite;
    }

    // Konstruktor do aktualizacji
    @Ignore
    public Phone(long id,
                 @NonNull String maker,
                 @NonNull String model,
                 @NonNull String androidVersion,
                 @NonNull String webSite) {
        this.id = id;
        this.maker = maker;
        this.model = model;
        this.androidVersion = androidVersion;
        this.webSite = webSite;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getMaker() {
        return maker;
    }

    public void setMaker(@NonNull String maker) {
        this.maker = maker;
    }

    @NonNull
    public String getModel() {
        return model;
    }

    public void setModel(@NonNull String model) {
        this.model = model;
    }

    @NonNull
    public String getAndroidVersion() {
        return androidVersion;
    }

    public void setAndroidVersion(@NonNull String androidVersion) {
        this.androidVersion = androidVersion;
    }

    @NonNull
    public String getWebSite() {
        return webSite;
    }

    public void setWebSite(@NonNull String webSite) {
        this.webSite = webSite;
    }
}
