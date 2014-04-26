package com.example.floudcloud.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.floudcloud.app.R;
import com.example.floudcloud.app.service.CloudOperationsService;
import com.example.floudcloud.app.service.FileObserverService;
import com.example.floudcloud.app.service.MainService;
import com.example.floudcloud.app.utility.Constants;
import com.example.floudcloud.app.utility.FileUtils;
import com.example.floudcloud.app.utility.SharedPrefs;


public class StatusActivity extends BaseActivity {
    private final int SETTINGS = 1;

    private View.OnClickListener onSettingsButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent settingsIntent = new Intent(StatusActivity.this, SettingsActivity.class);

            startActivityForResult(settingsIntent, SETTINGS);

        }
    };

    private View.OnClickListener onServiceToggleButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String path = SharedPrefs.getItem("", Constants.PREF_PATH);

            if (path.isEmpty()) {
                Toast.makeText(StatusActivity.this, "Specify path of the directory in settings", Toast.LENGTH_LONG).show();
            } else {
                startMonitoring(SharedPrefs.getItem("", Constants.PREF_API_KEY), path);
            }

        }
    };

    private View.OnClickListener onObsServiceToggleButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String path = SharedPrefs.getItem("", Constants.PREF_PATH);

            if (path.isEmpty()) {
                Toast.makeText(StatusActivity.this, "Specify path of the directory in settings", Toast.LENGTH_LONG).show();
            } else {
                startObs(SharedPrefs.getItem("", Constants.PREF_API_KEY), path);
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        Button settingsButton = (Button) findViewById(R.id.settingsButton);
        Button serviceToggleButton = (Button) findViewById(R.id.serviceToggleButton);
        Button obsServiceToggleButton = (Button) findViewById(R.id.obsServiceToggleButton);

        settingsButton.setOnClickListener(onSettingsButtonClickListener);
        serviceToggleButton.setOnClickListener(onServiceToggleButtonClickListener);
        obsServiceToggleButton.setOnClickListener(onObsServiceToggleButtonClickListener);

        String path = SharedPrefs.getItem(null, SharedPrefs.PREF_PATH);
        FileUtils.setFilePathBase(Environment.getExternalStorageDirectory().getAbsolutePath() + path);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case (SETTINGS) :
                if (resultCode == SettingsActivity.RESULT_LOGOUT) {
                    //TODO: Stop service
                    SharedPrefs.addItem(Constants.PREF_API_KEY, null);

                    finish();
                }
        }
    }


    private void startObs(String apiKey, String path) {
        Intent intent = new Intent(this, MainService.class);
        intent.putExtra(MainService.EXTRA_PATH, path);
        intent.putExtra(MainService.EXTRA_API_KEY, apiKey);
        // FIXME :(
        intent.putExtra(MainService.EXTRA_MEGA_KASTIL, 1);
        startService(intent);
    }


    private void startMonitoring(String apiKey, String path) {
        Intent intent = new Intent(this, MainService.class);
        intent.putExtra(MainService.EXTRA_PATH, path);
        intent.putExtra(CloudOperationsService.EXTRA_API_KEY, apiKey);
        startService(intent);
    }

}
