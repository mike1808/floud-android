package com.example.floudcloud.app.network;

import com.example.floudcloud.app.model.File;
import com.example.floudcloud.app.model.FileMove;
import com.example.floudcloud.app.model.SimpleReturn;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

public interface FloudFile {
    @GET("/")
    File.List getFilesList(@Query("from") long from);

    @Multipart
    @POST("/upload")
    File uploadFile(@Part("file") TypedFile file, @Field("path") String path,
                    @Field("size") long size, @Field("hash") String hash);

    @GET("/file")
    TypedFile getFile(@Query("path") String path);

    @DELETE("/file")
    SimpleReturn deleteFile(@Query("path") String path);

    @PUT("/move")
    SimpleReturn moveFile(@Body() FileMove fm);
}
