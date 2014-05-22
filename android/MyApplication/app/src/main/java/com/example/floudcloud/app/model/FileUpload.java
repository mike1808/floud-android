package com.example.floudcloud.app.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FileUpload implements Parcelable {
    public String path;
    public long size;
    public String hash;
    public String regId;

    public FileUpload(String path, long size, String hash, String regId) {
        this.path = path;
        this.size = size;
        this.hash = hash;
        this.regId = regId;
    }

    public FileUpload(Parcel in) {
        this.path = in.readString();
        this.size = in.readLong();
        this.hash = in.readString();
        this.regId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeLong(size);
        dest.writeString(hash);
        dest.writeString(regId);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public FileUpload createFromParcel(Parcel in) {
            return new FileUpload(in);
        }

        @Override
        public FileUpload[] newArray(int size) {
            return new FileUpload[size];
        }
    };
}
