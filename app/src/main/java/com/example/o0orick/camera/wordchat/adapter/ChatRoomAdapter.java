package com.example.o0orick.camera.wordchat.adapter;

import android.view.View;

import com.example.o0orick.camera.R;
import com.example.o0orick.camera.wordchat.base.BaseAdapterRV;
import com.example.o0orick.camera.wordchat.base.BaseHolderRV;
import com.example.o0orick.camera.wordchat.holer.ChatAcceptViewHolder;


public class ChatRoomAdapter extends BaseAdapterRV {

    @Override
    protected int getLayoutResID(int viewType) {
        return R.layout.item_chat;
    }

    @Override
    protected BaseHolderRV createViewHolder(View view, int viewType) {
        return new ChatAcceptViewHolder(view);
    }
}
