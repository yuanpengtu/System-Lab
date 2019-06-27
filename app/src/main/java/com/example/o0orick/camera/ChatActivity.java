package com.example.o0orick.camera;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import com.example.o0orick.camera.howeassistant.baidu.Const;
import com.example.o0orick.camera.wordchat.adapter.ChatRoomAdapter;
import com.example.o0orick.camera.wordchat.bean.MessageRecord;
import com.friendlyarm.AndroidSDK.HardwareControler;

public class ChatActivity extends AppCompatActivity {
    private static final int SYSTEM_MSG = 1;
    private static final int USER_MSG = 2;
    @BindView(R.id.recycler)
    RecyclerView mRecycler;
    @BindView(R.id.et_input_msg)
    EditText mEtInputMsg;
    @BindView(R.id.btn_send)
    ImageButton mBtnSend;
    ImageButton mBtnSendPos;

    private SocketIOManager app;
    private Socket mSocket;

    private String mUsername;
    private int mNumUsers;

    private ChatRoomAdapter mChatRoomAdapter;
    private List<MessageRecord> mMsgRecordList = new ArrayList<>();

    private String inputMsg;
    private String username;

    private boolean IsUsb;

    private MediaPlayer mp;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SYSTEM_MSG:
                    addBeanToRecycler(username, inputMsg);
                    break;
                case USER_MSG:
                    addBeanToRecycler(username, inputMsg);
                    try{
                        if(IsUsb)
                            HardwareControler.PWMPlay(6000);
                        else
                            mp.start();
                    }catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(1000);
                                if(IsUsb)
                                    HardwareControler.PWMStop();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        ButterKnife.bind(this);

        app = (SocketIOManager) getApplication();
        mSocket = app.getSocket();

        mUsername = app.getME();

        mChatRoomAdapter = new ChatRoomAdapter();
        mBtnSendPos=findViewById(R.id.btn_send_pos);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecycler.setLayoutManager(layoutManager);
        mRecycler.setAdapter(mChatRoomAdapter);
        mChatRoomAdapter.setDatas(mMsgRecordList);
        mChatRoomAdapter.setRecycler(mRecycler);

        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSend(false);
            }
        });
        mBtnSendPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSend(true);
            }
        });

        mSocket.on("word_chat", onWordChat);

        Intent intent=getIntent();
        IsUsb=intent.getBooleanExtra("IsUsb",false);

        String str = "{\"message\": " + "\"欢迎  " + app.getME() + "  的到来!\"" + ", \"username\": " + "\"聊天室\"" + "}";
        JSONObject _msg;

        try {
            _msg = new JSONObject(str);
            mSocket.emit("word_chat", _msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mp=new MediaPlayer();
        MediaPlayer mp =MediaPlayer.create(this, R.raw.ring1);


        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void attemptSend(boolean ispos) {
        if (null == mUsername) return;
        Log.v("Chat Activity", "send message");
        // Reset errors.
        mEtInputMsg.setError(null);

        // Store values at the time of the chat attempt.
        if(ispos)
            inputMsg=Const.MY_LOCATION;
        else
            inputMsg = mEtInputMsg.getText().toString().trim();

        // Check for a valid inputMsg.
        if (TextUtils.isEmpty(inputMsg)) {
            mEtInputMsg.setError(getString(R.string.error_field_message_required));
            mEtInputMsg.requestFocus();
            return;
        }

        mEtInputMsg.setText("");
        addBeanToRecycler(mUsername, inputMsg);

        String str = "{\"message\": " + "\"" + inputMsg + "\"" + ", \"username\": " + "\"" + mUsername + "\"" + "}";
        JSONObject _msg;

        try {
            _msg = new JSONObject(str);
            mSocket.emit("word_chat", _msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Emitter.Listener onWordChat = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            try {
                inputMsg = data.getString("msg");
                username = data.getString("username");
                if (username.equals(mUsername)) {
                    return;
                }

                Message message = new Message();
                if (username.equals("聊天室"))
                    message.what = SYSTEM_MSG;
                else
                    message.what = USER_MSG;
                handler.sendMessage(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void addBeanToRecycler(String username, String content) {
        MessageRecord messageRecord = new MessageRecord();
        messageRecord.setUserName(username);
        messageRecord.setContent(content);
        mChatRoomAdapter.addBeanToEnd(messageRecord);
    }

    @Override
    protected void onDestroy() {
        String str = "{\"message\": " + "\"欢送  " + app.getME() + "  的离开!\"" + ", \"username\": " + "\"聊天室\"" + "}";
        JSONObject _msg;

        try {
            _msg = new JSONObject(str);
            mSocket.emit("word_chat", _msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        super.onDestroy();
        Log.d("Chat Activity", "destroyed");
        mSocket.off("word_chat", onWordChat);
    }


    private void getData() {
        mUsername = app.getME();
    }

    private void initUI() {
        mChatRoomAdapter = new ChatRoomAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecycler.setLayoutManager(layoutManager);
        mRecycler.setAdapter(mChatRoomAdapter);
        mChatRoomAdapter.setDatas(mMsgRecordList);
        mChatRoomAdapter.setRecycler(mRecycler);

//        MessageRecord record = new MessageRecord();
//        record.setUserName("聊天室");
//        record.setContent("欢迎  " + app.getME() + "  的到来!");
//        mChatRoomAdapter.addBeanToEnd(record);

        String str = "{\"message\": " + "\"欢迎  " + app.getME() + "  的到来!\"" + ", \"username\": " + "\"聊天室\"" + "}";
        JSONObject _msg;

        try {
            _msg = new JSONObject(str);
            mSocket.emit("word_chat", _msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mEtInputMsg.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == 100 || id == EditorInfo.IME_NULL) {
                    Log.v("ChatActivity", "111");
                    attemptSend(false);
                    return true;
                }
                return false;
            }
        });

        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("ChatActivity", "222");
                attemptSend(false);
            }
        });
    }

}
