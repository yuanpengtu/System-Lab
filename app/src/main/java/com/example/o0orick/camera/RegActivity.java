package com.example.o0orick.camera;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class RegActivity extends AppCompatActivity {

    private static final int UPDATE_INFO = 1;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mRepasswordView;

    private Socket mSocket;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_INFO:
                    mUsernameView.setError(getString(R.string.error_field_username_bad));
                    mUsernameView.requestFocus();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        SocketIOManager app = (SocketIOManager) getApplication();
        mSocket = app.getSocket();

        mUsernameView = (EditText) findViewById(R.id.username_reg_name);
        mPasswordView = (EditText) findViewById(R.id.password_reg_pwd);
        mRepasswordView = (EditText) findViewById(R.id.password_reg_reqpwd);

        Button regButton = (Button) findViewById(R.id.sign_in_button);
        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptReg();
            }
        });

        mSocket.on("reg", onReg);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void attemptReg() {
        // Reset errors.
        mUsernameView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString().trim();
        String password = mPasswordView.getText().toString().trim();
        String repassword = mRepasswordView.getText().toString().trim();

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

        if (TextUtils.isEmpty(repassword)) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            mRepasswordView.setError(getString(R.string.error_field_pwd_required));
            mRepasswordView.requestFocus();
            return;
        }

        if (!password.equals(repassword)) {
            mRepasswordView.setError(getString(R.string.error_field_repwd_equaled));
            mRepasswordView.requestFocus();
            return;
        }

        String str = "{\"username\": " + username + ", \"pwd\": " + password + "}";
        JSONObject _user;

        try {
            _user = new JSONObject(str);
            mSocket.emit("reg", _user);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private Emitter.Listener onReg = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            JSONArray userList;

            int res = 0;
            try {
                res = data.getInt("result");

            } catch (JSONException e) {
                return;
            }

            if (res > 0) {
                Intent intent = new Intent(RegActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Message message = new Message();
                message.what = UPDATE_INFO;
                handler.sendMessage(message);
            }

        }
    };
}
