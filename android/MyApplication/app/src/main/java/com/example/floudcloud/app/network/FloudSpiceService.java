package com.example.floudcloud.app.network;

import com.octo.android.robospice.retrofit.RetrofitGsonSpiceService;

public class FloudSpiceService extends RetrofitGsonSpiceService {

    private final static String BASE_URL = "http://141.136.82.97:3000/api/v1.0";

    @Override
    public void onCreate() {
        super.onCreate();
        addRetrofitInterface(Floud.class);
    }

    @Override
    protected String getServerUrl() {
        return BASE_URL;
    }

}
