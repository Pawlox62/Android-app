package com.example.lab3.network;

import android.os.Parcel;
import android.os.Parcelable;

public class ProgressEvent implements Parcelable {
    public static final int OK = 0;
    public static final int IN_PROGRESS = 1;
    public static final int ERROR = 2;

    public int progress;
    public int total;
    public int result;

    public ProgressEvent() {}

    protected ProgressEvent(Parcel in) {
        progress = in.readInt();
        total = in.readInt();
        result = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(progress);
        dest.writeInt(total);
        dest.writeInt(result);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ProgressEvent> CREATOR = new Creator<ProgressEvent>() {
        @Override
        public ProgressEvent createFromParcel(Parcel in) {
            return new ProgressEvent(in);
        }

        @Override
        public ProgressEvent[] newArray(int size) {
            return new ProgressEvent[size];
        }
    };
}
