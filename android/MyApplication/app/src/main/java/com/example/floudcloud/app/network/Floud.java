package com.example.floudcloud.app.network;

import com.example.floudcloud.app.model.File;
import com.example.floudcloud.app.model.User;
import com.example.floudcloud.app.model.UserSignin;
import com.example.floudcloud.app.model.UserSignup;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

public interface Floud {
    @PUT("/users")
    User signup(@Body UserSignup user);

    @POST("/users/login")
    User singin(@Body UserSignin user);

    @GET("/users/current")
    User getCurrentUser(@Header("Authorization") String apiKey);

    @Multipart
    @POST("/files/upload")
    File uploadFile(@Header("Authorization") String apiKey,
                    @Part("file")TypedFile file, @Field("path") String path,
                    @Field("size") int size, @Field("hash") String hash);

    @GET("/files/file")
    TypedFile getFile(@Header("Authorization") String apiKey,
                      @Query("path") String path);

    @DELETE("/files")
    void deleteFile(@Header("Authorization") String apiKey,
                    @Query("path") String path);
}
