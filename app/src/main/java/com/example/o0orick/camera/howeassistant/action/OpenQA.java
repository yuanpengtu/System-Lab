package com.example.o0orick.camera.howeassistant.action;


import com.example.o0orick.camera.howeassistant.activity.Helper;

public class OpenQA {

	private String mText;
	Helper mActivity;
	
	public OpenQA(String text, Helper activity){
		mText=text;
		mActivity=activity;
	}
	
	public void start(){
		mActivity.speakAnswer(mText);
	}
	
}
