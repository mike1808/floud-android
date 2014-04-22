package com.example.floudcloud.app.network.exception;

import retrofit.RetrofitError;


public class UnauthorizedError extends RuntimeException {
    public UnauthorizedError(RetrofitError cause) {
        super(cause);
    }
}
