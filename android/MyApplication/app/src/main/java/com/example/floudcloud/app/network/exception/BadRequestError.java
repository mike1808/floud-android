package com.example.floudcloud.app.network.exception;

import retrofit.RetrofitError;

public class BadRequestError extends RuntimeException {
    public BadRequestError(RetrofitError cause) {
        super(cause);
    }
}
