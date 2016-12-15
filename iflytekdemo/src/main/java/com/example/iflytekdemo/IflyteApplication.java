package com.example.iflytekdemo;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

/**
 * Created by david on 2016/12/5.
 */

public class IflyteApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SpeechUtility.createUtility(this, "appid=5845396a");
    }
}
