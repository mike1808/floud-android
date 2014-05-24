package com.example.floudcloud.app.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.floudcloud.app.R;
import com.example.floudcloud.app.model.RegId;
import com.example.floudcloud.app.model.User;
import com.example.floudcloud.app.model.UserSignin;
import com.example.floudcloud.app.network.exception.BadRequestError;
import com.example.floudcloud.app.service.MainService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit.RetrofitError;


public class LoginActivity extends BaseActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;
    String regid;

    String SENDER_ID = "104421091711";
    private User user;
    private String apiKey;

    private EditText usernameEditText;
    private EditText pwdEditText;

    private ProgressDialog progressDialog;

    private View.OnClickListener onLoginBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String username = usernameEditText.getText().toString();
            String password = pwdEditText.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, R.string.username_password_empty, Toast.LENGTH_SHORT).show();
            } else {
                final LoginTask loginTask = new LoginTask();
                loginTask.execute(new UserSignin(username, password));
            }

        }
    };

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(LOG_TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private View.OnClickListener onSignupBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent signupIntent = new Intent(LoginActivity.this, SignupActivity.class);

            startActivity(signupIntent);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        context = getApplicationContext();

        usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        pwdEditText = (EditText) findViewById(R.id.pwdEditText);

        Button loginBtn = (Button) findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(onLoginBtnClick);

        Button signupBtn = (Button) findViewById(R.id.signupBtn);
        signupBtn.setOnClickListener(onSignupBtnClick);


        String apiKey = getApiKey();
        String regid = getRegistrationId(context);

        if (checkPlayServices()) {
            if (apiKey != null && !apiKey.isEmpty() && !regid.isEmpty()) {
                Intent statusIntent = new Intent(LoginActivity.this, StatusActivity.class);
                statusIntent.putExtra(MainService.EXTRA_API_KEY, apiKey);
                statusIntent.putExtra(MainService.EXTRA_REG_ID, regid);
                startActivity(statusIntent);
            }
        }
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

        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    private void loginUser(final User user) {
        usernameEditText.setText("");
        pwdEditText.setText("");

        apiKey = user.token;

        setApiKey(user.token);

        this.user = user;

        gcm = GoogleCloudMessaging.getInstance(this);
        regid = getRegistrationId(context);

        if (regid.isEmpty()) {
            registerInBackground();
        } else {
            progressDialog.dismiss();

            Intent statusIntent = new Intent(LoginActivity.this, StatusActivity.class);
            statusIntent.putExtra(MainService.EXTRA_API_KEY, user.token);
            statusIntent.putExtra(MainService.EXTRA_REG_ID, regid);
            startActivity(statusIntent);
        }


    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(LOG_TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(LOG_TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(LoginActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);

                    sendRegistrationIdToBackend();

                    storeRegistrationId(context, regid);
                } catch (IOException ex) {

                }

                return regid;
            }

            @Override
            protected void onPostExecute(String regId) {
                progressDialog.dismiss();

                Intent statusIntent = new Intent(LoginActivity.this, StatusActivity.class);
                statusIntent.putExtra(MainService.EXTRA_API_KEY, user.token);
                statusIntent.putExtra(MainService.EXTRA_REG_ID, regId);
                startActivity(statusIntent);
            }
        }.execute(null, null, null);
    }


    private boolean sendRegistrationIdToBackend() {
        try {
            getFloudService().regGcm(apiKey, regid);
        } catch (RetrofitError cause) {
            Log.e(LOG_TAG, "Could not send reg it to server");

            return false;
        }

        return true;
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(LOG_TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }


    private final class LoginTask extends AsyncTask<UserSignin, Void, User> {
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setTitle("Singing in");
            progressDialog.setMessage("Wait a little...");
            progressDialog.show();
        }

        @Override
        protected User doInBackground(UserSignin... users) {
            User user = null;
            try {
                user = getFloudService().singin(users[0]);
            } catch (BadRequestError cause) {
                user = new User("Invalid password or email");
            } catch (RetrofitError cause) {
                user = new User("Login is failed");
            }

            return user;
        }

        @Override
        protected void onPostExecute(User user) {
            String error = user.getError();

            if (error != null) {
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            } else {
                loginUser(user);
            }

        }
    }
}
