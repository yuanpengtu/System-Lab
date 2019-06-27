package com.example.o0orick.camera;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class UsbMainActivity extends AppCompatActivity {

    private SocketIOManager app;
    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_main);

        app = (SocketIOManager) getApplication();
        mSocket = app.getSocket();

        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance(app, mSocket))
                    .commit();
        }

        mSocket.on("chat_out",onChatOut);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onDestroy(){
        mSocket.emit("chat_out");
        mSocket.off("image");
        super.onDestroy();
    }

    private Emitter.Listener onChatOut = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            finish();
        }

    };

}
