package com.example.o0orick.camera;

import android.app.Application;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;

//使用Application类，这样socket的生命周期可以贯穿整个应用
//并且可以适用多个场景，但是性能应该低点，相较于开一个线程
//在使用的时候，获取app实例，调用getSocket方法
public class SocketIOManager extends Application {
    private final String LOCAL_SOCKET_URL = "http://192.168.3.12:8081";
    private Socket mSocket;
    private Map<String, String> OUserList;
    private  String ME;
    private  String ChatUser;
    {
        try {
            OUserList = new HashMap<String, String>();
            mSocket = IO.socket(LOCAL_SOCKET_URL);
            mSocket.connect();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public Map<String, String> getOUserList() {return OUserList;}
    public void putOUserList(String key, String Value){
        if(key != null && Value != null && !OUserList.containsKey(key))
            OUserList.put(key, Value);
    }
    public void popOUserList(String key){
        if(key != null && OUserList.containsKey(key)){
            OUserList.remove(key);
        }
    }

    public void setME(String name){
        this.ME=name;
    }

    public String getME(){
        return ME;
    }

    public String getChatUser() {
        return ChatUser;
    }

    public void setChatUser(String chatUser) {
        ChatUser = chatUser;
    }
}
