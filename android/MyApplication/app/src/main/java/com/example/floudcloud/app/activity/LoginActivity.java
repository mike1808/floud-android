package com.example.floudcloud.app.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.floudcloud.app.R;
import com.example.floudcloud.app.model.User;
import com.example.floudcloud.app.model.UserSignin;
import com.example.floudcloud.app.network.exception.BadRequestError;
import com.example.floudcloud.app.service.MainService;
import com.example.floudcloud.app.utility.SharedPrefs;

import retrofit.RetrofitError;


public class LoginActivity extends BaseActivity {
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

        usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        pwdEditText = (EditText) findViewById(R.id.pwdEditText);

        Button loginBtn = (Button) findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(onLoginBtnClick);

        Button signupBtn = (Button) findViewById(R.id.signupBtn);
        signupBtn.setOnClickListener(onSignupBtnClick);

        String apiKey = getApiKey();

        if (apiKey != null && !apiKey.isEmpty()) {
            Intent statusIntent = new Intent(LoginActivity.this, StatusActivity.class);

            startActivity(statusIntent);
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

        setApiKey(user.token);
        Intent statusIntent = new Intent(LoginActivity.this, StatusActivity.class);
        statusIntent.putExtra(MainService.EXTRA_API_KEY, user.token);

        startActivity(statusIntent);
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
        protected User doInBackground(UserSignin ...users) {
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
            progressDialog.dismiss();
            String error = user.getError();

            if (error != null) {
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
            } else {
                loginUser(user);
            }

        }
    }
}
