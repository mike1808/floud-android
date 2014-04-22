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
import com.example.floudcloud.app.model.UserSignup;
import com.example.floudcloud.app.network.exception.BadRequestError;

import retrofit.RetrofitError;


public class SignupActivity extends BaseActivity {
    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText fullnameEditText;
    private EditText pwdEditText;

    private Button signupBtn;

    private ProgressDialog progressDialog;


    private View.OnClickListener onSignupBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String username = usernameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String fullname = fullnameEditText.getText().toString();
            String password = pwdEditText.getText().toString();

            if(username.isEmpty() || email.isEmpty() || fullname.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignupActivity.this, R.string.signup_warning, Toast.LENGTH_LONG).show();
                return;
            }

            final SignUpTask signUpTask = new SignUpTask();
            signUpTask.execute(new UserSignup(username, password, email, fullname));
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        emailEditText = (EditText) findViewById(R.id.emailEditText);
        fullnameEditText = (EditText) findViewById(R.id.fullnameEditText);
        pwdEditText = (EditText) findViewById(R.id.pwdEditText);


        signupBtn = (Button) findViewById(R.id.signupBtn);
        signupBtn.setOnClickListener(onSignupBtnClick);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.signup, menu);
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

    private void signupUser(final User user) {
        setApiKey(user.token);
        progressDialog.dismiss();
        usernameEditText.setText("");
        pwdEditText.setText("");
        emailEditText.setText("");
        fullnameEditText.setText("");

        Intent statusIntent = new Intent(SignupActivity.this, StatusActivity.class);

        startActivity(statusIntent);
    }

    private final class SignUpTask extends AsyncTask<UserSignup, Void, User> {
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(SignupActivity.this);
            progressDialog.setTitle("Signing up");
            progressDialog.setMessage("Wait a little...");
            progressDialog.show();
        }

        @Override
        protected User doInBackground(UserSignup ...users) {
            User user = null;
            try {
                user = getFloudService().signup(users[0]);
            } catch (BadRequestError cause) {
                user = new User("Invalid signup data");
            } catch (RetrofitError cause) {
                user = new User("Signup is failed");
            }

            return user;
        }

        @Override
        protected void onPostExecute(User user) {
            String error = user.getError();

            if (error != null) {
                Toast.makeText(SignupActivity.this, error, Toast.LENGTH_LONG).show();
            } else {
                signupUser(user);
            }

        }
    }

}
