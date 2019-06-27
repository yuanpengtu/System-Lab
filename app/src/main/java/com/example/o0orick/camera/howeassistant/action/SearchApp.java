package com.example.o0orick.camera.howeassistant.action;

import android.content.Intent;
import android.net.Uri;

import com.example.o0orick.camera.howeassistant.activity.Helper;


public class SearchApp {
	private String mName;
	Helper mActivity;

	public SearchApp(String name, Helper activity){
		mName=name;
		mActivity=activity;
	}

	public void start(){
		mActivity.speakAnswer("正在搜索...");
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://search?q="+mName));
		mActivity.startActivity(intent);
	}
}
