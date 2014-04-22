package com.example.floudcloud.app.model;

public class User {
    public String token;
    public String userId;
    public String name;
    public String gravatar;
    public String email;
    public int expires;

    private String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public User(String error) {
        this.setError(error);
    }
}