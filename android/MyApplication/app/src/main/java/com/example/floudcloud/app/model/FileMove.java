package com.example.floudcloud.app.model;


public class FileMove {
    private String oldPath;
    private String newPath;

    public FileMove(String newPath, String oldPath) {
        this.oldPath = oldPath;
        this.newPath = newPath;
    }
}
