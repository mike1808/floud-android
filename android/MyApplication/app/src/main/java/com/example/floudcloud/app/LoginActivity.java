package com.example.floudcloud.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.floudcloud.app.model.User;
import com.example.floudcloud.app.network.request.SigninRequest;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import org.apache.http.HttpException;


public class LoginActivity extends BaseActivity {
    private static final String REQUEST_CACHE_KEY = "floud";
    private static final String LOG_TAG = "floud";
    private EditText usernameEditText;
    private EditText pwdEditText;
    private Button loginBtn;

    private SigninRequest signinRequest;

    private View.OnClickListener onLoginBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String username = usernameEditText.getText().toString();
            String password = pwdEditText.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, R.string.username_password_empty, Toast.LENGTH_SHORT).show();
            } else {
                signinRequest = new SigninRequest(username, password);

                getSpiceManager().execute(signinRequest, REQUEST_CACHE_KEY, DurationInMillis.ONE_MINUTE, new LoginRequestListener());
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        pwdEditText = (EditText) findViewById(R.id.pwdEditText);
        loginBtn = (Button) findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(onLoginBtnClick);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
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

    private void loginUser(final User user) {
        Toast.makeText(LoginActivity.this, user.token, Toast.LENGTH_LONG).show();
    }

    public final class LoginRequestListener implements RequestListener<User> {
        @Override
        public void onRequestFailure(SpiceException e) {
            Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onRequestSuccess(final User user) {
            loginUser(user);
        }
    }
}
