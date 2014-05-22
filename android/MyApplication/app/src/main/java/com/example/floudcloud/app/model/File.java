package com.example.floudcloud.app.model;

import java.util.ArrayList;

public class File {
    private String path;
    private long size;
    private int version;
    private boolean deleted;
    private String hash;
    private String modified;
    private String created;

    public File(String path, long size, String hash) {
        this.path = path;
        this.size = size;
        this.hash = hash;
        this.deleted = false;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof File)) return false;

        File file = (File) other;

        return this.path.equals(file.getPath()) &&
                this.hash.equals(file.getHash()) &&
                this.deleted == file.isDeleted();
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

    public boolean isDeleted() {
        return deleted;
    }

    public String getModified() {
        return modified;
    }

    @SuppressWarnings("serial")
    public static class List extends ArrayList<File> {
    }
}
