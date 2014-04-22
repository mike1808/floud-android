package com.example.floudcloud.app.network.exception;

import retrofit.RetrofitError;

public class InternalServerError extends RuntimeException {
    public InternalServerError(RetrofitError cause) {
        super(cause);
    }
}
