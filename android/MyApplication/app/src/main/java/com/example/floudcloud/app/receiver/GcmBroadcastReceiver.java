package com.example.floudcloud.app.receiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.example.floudcloud.app.service.GcmIntentService;


public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, GcmIntentService.class);
        startWakefulService(context, service);
        setResultCode(Activity.RESULT_OK);
    }
}