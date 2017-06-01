package com.shine.bcast;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

/**
 * Created by zoubingshun on 2017/5/11.
 */

public class MyService extends Service {

    private MyVideoMonitor monitor;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
        super.onCreate();
        monitor = MyVideoMonitor.getInstance(MainActivity.getProjection());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        monitor.init();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
