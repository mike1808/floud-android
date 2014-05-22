package com.example.floudcloud.app.service;

import android.app.IntentService;
import android.content.Intent;

import com.example.floudcloud.app.receiver.GcmBroadcastReceiver;

public class GcmIntentService extends IntentService {
    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        sendBroadcast(new Intent(MainService.RUN_DIGEST));

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
}