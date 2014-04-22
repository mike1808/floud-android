package com.example.floudcloud.app.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.floudcloud.app.R;
import com.example.floudcloud.app.utility.Constants;
import com.example.floudcloud.app.utility.SharedPrefs;


public class SettingsActivity extends BaseActivity {
    public static final int RESULT_LOGOUT = 99;
    private Button saveButton;
    private Button logoutButton;
    private EditText pathEditText;


    private View.OnClickListener onSaveButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SharedPrefs.addItem(Constants.PREF_PATH, pathEditText.getText().toString());

            setResult(RESULT_OK);
            finish();
        }

    };

    private View.OnClickListener onLogoutButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(RESULT_LOGOUT);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        saveButton = (Button) findViewById(R.id.saveButton);
        logoutButton = (Button) findViewById(R.id.logoutButton);
        pathEditText = (EditText) findViewById(R.id.pathEditText);

        String path = SharedPrefs.getItem("", Constants.PREF_PATH);
        pathEditText.setText(path, TextView.BufferType.EDITABLE);

        saveButton.setOnClickListener(onSaveButtonClickListener);
        logoutButton.setOnClickListener(onLogoutButtonClickListener);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
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

}
