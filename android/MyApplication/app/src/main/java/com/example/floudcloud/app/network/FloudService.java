package com.example.floudcloud.app.network;

import com.example.floudcloud.app.network.exception.BadRequestError;
import com.example.floudcloud.app.network.exception.InternalServerError;
import com.example.floudcloud.app.network.exception.NotFoundError;
import com.example.floudcloud.app.network.exception.UnauthorizedError;
import com.example.floudcloud.app.utility.Constants;
import com.example.floudcloud.app.utility.SharedPrefs;
import com.google.gson.Gson;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;


public class FloudService {
    private final static String API_URL = "http://floud-cloud.no-ip.org/api/v1.0";
    private final static String AUTH_URI = "/users";
    private final static String FILE_URI = "/files";

    public final static String AUTH_URL = API_URL + AUTH_URI;
    public final static String FILE_URL = API_URL + FILE_URI;

    private FloudAuth floudAuthService;
    private FloudFile floudFileService;

    private String apiKey;
    private long timestamp;

    public FloudService() {
        RestAdapter authRestAdapter = new RestAdapter.Builder()
                .setEndpoint(AUTH_URL)
                .setConverter(new GsonConverter(new Gson()))
                .setErrorHandler(new MyErrorHandler())
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        floudAuthService = authRestAdapter.create(FloudAuth.class);
    }

    public FloudService(String apiKey, final String regId) {
        setApiKey(apiKey);

        RequestInterceptor authInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestInterceptor.RequestFacade request) {
                request.addHeader("Authorization", getApiKey());
                request.addQueryParam("regId", regId);
            }
        };

        RestAdapter fileRestAdapter = new RestAdapter.Builder()
                .setEndpoint(FILE_URL)
                .setConverter(new GsonConverter(new Gson()))
                .setErrorHandler(new MyErrorHandler())
                .setRequestInterceptor(authInterceptor)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        floudFileService = fileRestAdapter.create(FloudFile.class);
    }

    public FloudAuth getAuthService() {
        return floudAuthService;
    }

    public FloudFile getFileService() {
        return floudFileService;
    }

    public String getApiKey() {
        if (apiKey == null)
            apiKey = SharedPrefs.getItem(null, Constants.PREF_API_KEY);

        return apiKey;
    }


    public void setApiKey(String apiKey) {
        SharedPrefs.addItem(Constants.PREF_API_KEY, apiKey);

        this.apiKey = apiKey;
    }

    public long getTimestamp() {
        if (timestamp == 0)
            timestamp = SharedPrefs.getTimestamp();

        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        SharedPrefs.addTimestamp(timestamp);

        this.timestamp = timestamp;
    }


    private static class MyErrorHandler implements ErrorHandler {
        @Override
        public Throwable handleError(RetrofitError cause) {
            Response r = cause.getResponse();
            if (r != null) {
                if (r.getStatus() == 401) {
                    return new UnauthorizedError(cause);
                } else if (r.getStatus() == 400) {
                    return new BadRequestError(cause);
                } else if (r.getStatus() == 500) {
                    return new InternalServerError(cause);
                } else if (r.getStatus() == 404) {
                    return new NotFoundError(cause);
                }
            }
            return cause;
        }
    }

}
