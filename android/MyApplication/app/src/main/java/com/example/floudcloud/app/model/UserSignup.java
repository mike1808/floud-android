package com.example.floudcloud.app.model;

public class UserSignup {
    public String username;
    public String password;
    public String email;
    public String fullname;

    public UserSignup(String username, String password, String email, String fullname) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullname = fullname;
    }
}
