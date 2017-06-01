package com.shine.bcast;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

/**
 * Created by zoubingshun on 2017/5/8.
 */

public final class Utils {
    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;

    public static void init(Context context){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metric = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metric);
        SCREEN_WIDTH= metric.widthPixels;     // 屏幕宽度（像素）
        SCREEN_HEIGHT= metric.heightPixels;
        Log.e("Binson","width:"+SCREEN_WIDTH+",Height:"+SCREEN_HEIGHT);
    }
}
