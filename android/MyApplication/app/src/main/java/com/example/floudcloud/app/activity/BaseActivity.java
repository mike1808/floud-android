package com.example.floudcloud.app.activity;

import android.app.Activity;
import android.os.Bundle;

import com.example.floudcloud.app.network.FloudAuth;
import com.example.floudcloud.app.network.FloudService;
import com.example.floudcloud.app.utility.SharedPrefs;

public abstract class BaseActivity extends Activity {
    private FloudService floudService = new FloudService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPrefs.init(getApplicationContext());
    }


    protected FloudAuth getFloudService() {
        return floudService.getAuthService();
    }

    protected void setApiKey(String apiKey) {
        floudService.setApiKey(apiKey);
    }

    protected String getApiKey() {
        return floudService.getApiKey();
    }
}
