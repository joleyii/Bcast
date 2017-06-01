package com.shine.bcast;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MediaProjectionManager mediaProjectionManager;
    private static MediaProjection projection;

    private CustomMediaCallBack mediaCallback;

    private SurfaceView surfaceView;

    private Surface mSurface;

    private MyService service;

    private final int CAPATURE_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        findViewById(R.id.start_capture).setOnClickListener(this);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaCallback = new CustomMediaCallBack();
//        surfaceView= (SurfaceView) findViewById(R.id.surface);
//        mSurface=surfaceView.getHolder().getSurface();
        service = new MyService();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_capture:

                if (mediaProjectionManager != null) {
                    Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(intent, CAPATURE_REQUEST_CODE);
                }

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAPATURE_REQUEST_CODE:
                    projection = mediaProjectionManager.getMediaProjection(resultCode, data);

                    // 注册回调:
                    projection.registerCallback(mediaCallback, null);


                    startService(new Intent(MainActivity.this, MyService.class));

                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (projection != null) {
            projection.unregisterCallback(mediaCallback);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
//        if(projection!=null){
//            projection.stop();
//        }
    }

    public static MediaProjection getProjection() {
        return projection;
    }
}
