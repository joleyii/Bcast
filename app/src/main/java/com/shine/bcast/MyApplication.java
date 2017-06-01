package com.shine.bcast;

import android.app.Application;

/**
 * Created by zoubingshun on 2017/5/8.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
    }
}
