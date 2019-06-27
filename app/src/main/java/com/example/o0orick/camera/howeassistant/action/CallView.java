package com.example.o0orick.camera.howeassistant.action;

import android.content.Intent;

import com.example.o0orick.camera.howeassistant.activity.Helper;

public class CallView {

    private Helper mActivity;

    public CallView(Helper activity) {
        mActivity = activity;
    }

    public void start() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CALL_BUTTON);
        mActivity.startActivity(intent);
    }
}
