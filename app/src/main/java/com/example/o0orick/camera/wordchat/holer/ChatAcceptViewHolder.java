package com.example.o0orick.camera.wordchat.holer;

import android.view.View;
import android.widget.TextView;

import com.example.o0orick.camera.R;
import com.example.o0orick.camera.wordchat.base.BaseHolderRV;
import com.example.o0orick.camera.wordchat.bean.MessageRecord;

import butterknife.BindView;

public class ChatAcceptViewHolder extends BaseHolderRV<MessageRecord> {

    @BindView(R.id.name_tv)
    TextView mNameTv;
    @BindView(R.id.content_tv)
    TextView mContentTv;

    public ChatAcceptViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    protected void bindData() {
        mNameTv.setText(mDataBean.getUserName() + ":");
        mContentTv.setText(mDataBean.getContent());
    }
}
