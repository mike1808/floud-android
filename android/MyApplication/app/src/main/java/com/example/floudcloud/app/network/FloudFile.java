package com.example.floudcloud.app.network;

import com.example.floudcloud.app.model.File;

import java.util.Date;

import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

public interface FloudFile {
    @GET("/list")
    File.List getFilesList(@Query("from") long from);

    @Multipart
    @POST("/upload")
    File uploadFile(@Part("file")TypedFile file, @Field("path") String path,
                    @Field("size") long size, @Field("hash") String hash);

    @GET("/file")
    TypedFile getFile(@Query("path") String path);

    @DELETE("/")
    void deleteFile(@Query("path") String path);
}
