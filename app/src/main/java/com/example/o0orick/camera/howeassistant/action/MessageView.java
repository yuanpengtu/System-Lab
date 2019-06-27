package com.example.o0orick.camera.howeassistant.action;

import android.content.Intent;

import com.example.o0orick.camera.howeassistant.activity.Helper;


public class MessageView {
	private Helper mActivity;
	
	public MessageView(Helper activity){
		mActivity=activity;
	}
	
	public void start(){
		Intent intent=new Intent();
		intent.setClassName("com.android.mms","com.android.mms.ui.ConversationList");
		mActivity.startActivity(intent);
	}
}
