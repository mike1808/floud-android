package com.example.floudcloud.app.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class File {
    private String path;
    private long size;
    private int version;
    private String hash;
    private String created;

    public File(String path, long size, String hash) {
        this.path = path;
        this.size = size;
        this.hash = hash;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof File)) return false;

        File file = (File) other;

        return this.path.equals(file.getPath()) && this.hash.equals(file.getHash());
    }

    public String getHash() {
        return hash;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public int getVersion() {
        return version;
    }

    public String getCreated() {
        return created;
    }

    @SuppressWarnings("serial")
    public static class List extends ArrayList<File> {
    }
}
