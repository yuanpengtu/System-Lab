package com.example.o0orick.camera;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class LoginActivity extends AppCompatActivity {

    private EditText mUsernameView;
    private EditText mPasswordView;
    private String mUsername;
    private Socket mSocket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SocketIOManager app = (SocketIOManager) getApplication();
        mSocket = app.getSocket();

        mUsernameView = (EditText) findViewById(R.id.username_input);
        mPasswordView = (EditText) findViewById(R.id.password_input);

        Button signinButton = (Button) findViewById(R.id.sign_in_button);
        signinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegActivity.class);
                startActivity(intent);
            }
        });

        Button loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                attemptLogin();
                /*Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);*/
            }
        });

        mSocket.on("login", onLogin);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Login Activity", "destroyed");
        mSocket.off("login", onLogin);
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mUsernameView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString().trim();
        String password = mPasswordView.getText().toString().trim();

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            mUsernameView.setError(getString(R.string.error_field_user_required));
            mUsernameView.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            mPasswordView.setError(getString(R.string.error_field_pwd_required));
            mPasswordView.requestFocus();
            return;
        }

        mUsername = username;

        String str = "{\"username\": " + username + ", \"pwd\": " + password + "}";
        JSONObject _user;

        try {
            _user = new JSONObject(str);
            mSocket.emit("login", _user);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            JSONArray userList;

            int numUsers;
            try {
                numUsers = data.getInt("numUsers");
                userList = data.getJSONArray("UserList");
                Log.v("LoginActivity", userList.toString());
                SocketIOManager app = (SocketIOManager) getApplication();

                app.setME(mUsername);

                for (int i = 0; i < userList.length(); i++) {
                    String t = userList.getString(i);
                    Log.v("LoginActivity", t);
                    if (!t.equals(mUsername) && !t.isEmpty() && t != null) {
                        app.putOUserList(t, t);
                    }
                }
            } catch (JSONException e) {
                return;
            }

            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            setResult(RESULT_OK, intent);
            startActivity(intent);
            finish();
        }
    };
}
