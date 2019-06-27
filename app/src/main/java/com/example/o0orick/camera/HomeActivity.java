package com.example.o0orick.camera;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.example.o0orick.camera.howeassistant.MyLocation;
import com.example.o0orick.camera.howeassistant.activity.Helper;
import com.example.o0orick.camera.ChatActivity;
import com.loopeer.cardstack.CardStackView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * 大部分代码使用AS自动生成的
 * Socket部分自己处理
 */
public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, CardStackView.ItemExpendListener {

    private static final int USER_IN_INFO = 1;
    private static final int USER_OUT_INFO = 2;
    private Socket mSocket;

    private String SelectedUser;
    private boolean IsUsb;

    private MenuItem speakhelper;
    public static Integer[] TEST_DATAS = new Integer[]{
            R.color.color_1,
            R.color.color_2,
            R.color.color_3,
            R.color.color_4,
            R.color.color_5,
            R.color.color_6,
            R.color.color_7,
            R.color.color_8,
            R.color.color_9,
            R.color.color_10,
            R.color.color_11,
            R.color.color_12,
            R.color.color_13,
            R.color.color_14,
            R.color.color_15,
            R.color.color_16,
            R.color.color_17,
            R.color.color_18,
            R.color.color_19,
            R.color.color_20,
            R.color.color_21,
            R.color.color_22,
            R.color.color_23,
            R.color.color_24,
            R.color.color_25,
            R.color.color_26
    };
    private SocketIOManager app;
    private CardStackView mStackView;
    private HomeStackAdapter mTestStackAdapter;

    FloatingActionButton fab;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            final List<String> OnlineUserList = new ArrayList<String>(app.getOUserList().keySet());
            final List<Integer> ColorList = new ArrayList<Integer>(Arrays.asList(TEST_DATAS).subList(0, app.getOUserList().size()));
            switch (msg.what) {
                case USER_IN_INFO:
                case USER_OUT_INFO:
                    mTestStackAdapter.updateData(ColorList, OnlineUserList);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
//        speakhelper=(MenuItem)findViewById(R.id.ho.helper_test) ;
//        if(speakhelper!=null) {
//            speakhelper.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//                @Override
//                public boolean onMenuItemClick(MenuItem item) {
//
//                    return false;
//                }
//            });
//        }


        app = (SocketIOManager) getApplication();
        mSocket = app.getSocket();

        mSocket.on("user_in", onUserIn);
        mSocket.on("user_out", onUserOut);
        mSocket.on("chat_req", onChatReq);

        IsUsb = false;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //app.setChatUser(SelectedUser);
                String str = "{\"username\": " + app.getChatUser() + "}";
                JSONObject _user;

                try {
                    _user = new JSONObject(str);
                    mSocket.emit("chat_to", _user);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (IsUsb) {
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(HomeActivity.this, UsbMainActivity.class);
                    startActivity(intent);
                }

            }
        });

        fab.hide();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        TextView mUserTextView = (TextView) headerView.findViewById(R.id.nav_user_text);
        mUserTextView.setText(app.getME());

        //cardstackview相关
        mStackView = (CardStackView) findViewById(R.id.stackview);
        mStackView.setItemExpendListener(this);
        mTestStackAdapter = new HomeStackAdapter(this);
        mTestStackAdapter.setApp(app);

        mStackView.setAdapter(mTestStackAdapter);

        final List<String> OnlineUserList = new ArrayList<String>(app.getOUserList().keySet());
        final List<Integer> ColorList = new ArrayList<Integer>(Arrays.asList(TEST_DATAS).subList(0, app.getOUserList().size()));

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (ColorList.size() > 0 && OnlineUserList.size() > 0)
                            mTestStackAdapter.updateData(ColorList, OnlineUserList);
                    }
                }
                , 200
        );

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("HomeActivity", "Destroyed");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                if (IsUsb) {
                    item.setTitle(R.string.action_settings_usb);
                    IsUsb = false;
                } else {
                    item.setTitle(R.string.action_settings_default);
                    IsUsb = true;
                }
                break;
            case R.id.helper_test:
                Intent helper = new Intent(HomeActivity.this, Helper.class);
                startActivity(helper);
                break;
            case R.id.word_chat:
                Intent word_chat = new Intent(HomeActivity.this, ChatActivity.class);
                word_chat.putExtra("IsUsb",IsUsb);
                startActivity(word_chat);
                break;
            case R.id.location_test:
                Intent location_test = new Intent(HomeActivity.this, MyLocation.class);
                startActivity(location_test);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

       /* if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onItemExpend(boolean expend) {
        if (expend) {
            fab.show();
        } else {
            fab.hide();
            SelectedUser = "";
            app.setChatUser("");
        }

    }

    private Emitter.Listener onUserIn = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            String newUser;
            try {
                newUser = data.getString("username");
                Log.v("HomeActivity->user in:", newUser);

                app.putOUserList(newUser, newUser);

                Message message = new Message();
                message.what = USER_IN_INFO;
                handler.sendMessage(message);

            } catch (JSONException e) {
                return;
            }
        }

    };

    private Emitter.Listener onUserOut = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            String outUser;
            try {
                outUser = data.getString("username");
                Log.v("HomeActivity->user out:", outUser);

                app.popOUserList(outUser);

                Message message = new Message();
                message.what = USER_OUT_INFO;
                handler.sendMessage(message);

            } catch (JSONException e) {
                return;
            }
        }

    };


    private Emitter.Listener onChatReq = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            String _reqUser;
            try {
                _reqUser = data.getString("username");
                Log.v("HomeActivity->ChatReq", _reqUser);

                if (IsUsb) {
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(HomeActivity.this, UsbMainActivity.class);
                    startActivity(intent);
                }
            } catch (JSONException e) {
                return;
            }
        }
    };
}
