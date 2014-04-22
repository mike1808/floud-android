package com.example.floudcloud.app.network;

import com.example.floudcloud.app.model.User;
import com.example.floudcloud.app.model.UserSignin;
import com.example.floudcloud.app.model.UserSignup;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;

public interface FloudAuth {
    @PUT("/")
    User signup(@Body UserSignup user);

    @POST("/login")
    User singin(@Body UserSignin user);


    @GET("/current")
    User getCurrentUser(@Header("Authorization") String apiKey);
}
