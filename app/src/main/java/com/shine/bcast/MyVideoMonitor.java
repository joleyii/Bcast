package com.shine.bcast;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import com.example.local.NetworkNative;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by zoubingshun on 2017/5/8.
 */

public final class MyVideoMonitor {


    private MediaCodec.BufferInfo mVideoBufferInfo;
    private MediaCodec mVideoEncoder;
    private MediaProjection projection;
    private static MyVideoMonitor mMonitor;

    private Sender sender;
    private NetworkNative networkNative;

    private MyVideoMonitor(MediaProjection projection) {
        this.projection = projection;
        this.networkNative = new NetworkNative();
        networkNative.OpenSocket();
        createfile();
    }

    public static MyVideoMonitor getInstance(MediaProjection projection) {
        if (mMonitor == null) {
            mMonitor = new MyVideoMonitor(projection);
        }
        return mMonitor;
    }

    //初始化编码器
    public void init() {
        try {
            mVideoEncoder = MediaCodec.createEncoderByType("video/avc");
//           mVideoEncoder=MediaCodec.createByCodecName(C)
        } catch (IOException e) {
            e.printStackTrace();
        }


        mVideoBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", Utils.SCREEN_WIDTH, Utils.SCREEN_HEIGHT);
        int frameRate = 25;

        // Set some required properties. The media codec may fail if these aren't defined.
//        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        format.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
//        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
//        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
//        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames


        format.setInteger(MediaFormat.KEY_BIT_RATE, Utils.SCREEN_WIDTH * Utils.SCREEN_HEIGHT * 5);
//        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setFloat(MediaFormat.KEY_FRAME_RATE, 10.0f);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Surface surface = mVideoEncoder.createInputSurface();

        mVideoEncoder.start();

        projection.createVirtualDisplay("MyMYMY", Utils.SCREEN_WIDTH, Utils.SCREEN_HEIGHT, 1, 0, surface, null, null);

        Log.e("Binson", "视频流编码器初始化完成!");

        sender = new Sender();
        new Thread(sender).start();

    }

    byte[] good;

    public void work1(OutputStream outputStream) {
        ByteBuffer[] byteBuffers = mVideoEncoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean encoderDone = false;
        while (!encoderDone) {
            int encoderStatus = mVideoEncoder.dequeueOutputBuffer(info, 0);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (good != null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d("SendFrameSendFrame", good.length + ";;;;;;");
                    networkNative.SendFrame(good, good.length, 1);
                    try {
                        outputStream1.write(good, 0, good.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // no output available yet
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                byteBuffers = mVideoEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // not expected for an encoder
                MediaFormat newFormat = mVideoEncoder.getOutputFormat();
            } else {
                ByteBuffer encodedData = byteBuffers[encoderStatus];
                if (encodedData == null) {
                    //something's wrong with the encoder
                    break;
                }
                // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                encodedData.position(info.offset);
                encodedData.limit(info.offset + info.size);

//                encodedData.array();
//                if (outputStream != null) {
//                    try {
                byte[] bs = new byte[info.size];
                encodedData.get(bs, info.offset, info.offset + info.size);
//                        outputStream.write(bs);
//                        outputStream.flush();
//                        Log.e("Binson", "flush..." + bs.length);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
                if (info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                    good = bs;
                    Log.d("SendFrameSendFrame", bs.length + "" + ">>>F");
                    networkNative.SendFrame(bs, bs.length, 1);
                    try {
                        outputStream1.write(bs, 0, bs.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d("SendFrameSendFrame", bs.length + "" + ">>>N");
                    networkNative.SendFrame(bs, bs.length, 0);
                    try {
                        outputStream1.write(bs, 0, bs.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                encoderDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                mVideoEncoder.releaseOutputBuffer(encoderStatus, false);
            }
        }

    }


    public void work(OutputStream outputStream) {
        while (true) {
            int bufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, 0);
            Log.e("Binson", "index is :" + bufferIndex);

            if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // nothing available yet
                break;
            } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                //if (mTrackIndex >= 0) {
                //    throw new RuntimeException("format changed twice");
                //}
                //mTrackIndex = mMuxer.addTrack(mVideoEncoder.getOutputFormat());
                //if (!mMuxerStarted && mTrackIndex >= 0) {
                //    mMuxer.start();
                //    mMuxerStarted = true;
                //}
            } else if (bufferIndex < 0) {
                // not sure what's going on, ignore it
            } else {
                ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(bufferIndex);
                if (encodedData == null) {
                    throw new RuntimeException("couldn't fetch buffer at index " + bufferIndex);
                }
                // Fixes playability issues on certain h264 decoders including omxh264dec on raspberry pi
                // See http://stackoverflow.com/a/26684736/4683709 for explanation
                //if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                //    mVideoBufferInfo.size = 0;
                //}

                //Log.d(TAG, "Video buffer offset: " + mVideoBufferInfo.offset + ", size: " + mVideoBufferInfo.size);
                if (mVideoBufferInfo.size != 0) {
                    Log.e("Binson", "start write");
                    encodedData.position(mVideoBufferInfo.offset);
                    encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
                    if (outputStream != null) {
                        try {
                            byte[] b = new byte[encodedData.remaining()];
                            encodedData.get(b);
                            outputStream.write(b);
                            outputStream.flush();
                            Log.e("Binson", "flush!!" + b.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    /*
                    if (mMuxerStarted) {
                        encodedData.position(mVideoBufferInfo.offset);
                        encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
                        try {
                            if (mSocketOutputStream != null) {
                                byte[] b = new byte[encodedData.remaining()];
                                encodedData.get(b);
                                mSocketOutputStream.write(b);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mMuxer.writeSampleData(mTrackIndex, encodedData, mVideoBufferInfo);
                    } else {
                        // muxer not started
                    }
                    */
                }

                mVideoEncoder.releaseOutputBuffer(bufferIndex, false);

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }

    }


    class Sender implements Runnable {
        private boolean isStart = true;
        private OutputStream outputStream;

        @Override
        public void run() {
            try {
//                Log.e("Binson", "start Server");
//                ServerSocket serverSocket = new ServerSocket(60000);
//                Log.e("Binson", "Server is On");
//                Socket socket = serverSocket.accept();
//                Log.e("Binson", "got Client!");
//                outputStream = socket.getOutputStream();
                work1(outputStream);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Binson", "start server error!" + e.toString());
            }
        }

        public void shutDown() {
            isStart = false;
        }

    }
    private BufferedOutputStream outputStream1;

    private void createfile() {
        String path = "/sdcard/aa.h264";
        File file = new File(path);
        try {
            outputStream1 = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
