package com.example.floudcloud.app.network.exception;

import retrofit.RetrofitError;

public class NotFoundError extends RuntimeException {
    public NotFoundError(RetrofitError cause) {
    }
}
