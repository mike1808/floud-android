package com.example.floudcloud.app;

import android.app.Activity;

import com.example.floudcloud.app.network.FloudSpiceService;
import com.octo.android.robospice.SpiceManager;

public abstract class BaseActivity extends Activity {
    private SpiceManager spiceManager = new SpiceManager(FloudSpiceService.class);

    @Override
    protected void onStart() {
        spiceManager.start(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        spiceManager.shouldStop();
        super.onStop();
    }

    protected SpiceManager getSpiceManager() {
        return spiceManager;
    }
}
