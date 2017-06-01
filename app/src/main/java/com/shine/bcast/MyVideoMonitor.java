package com.shine.bcast;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import com.example.local.NetworkNative;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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

    private MyVideoMonitor(MediaProjection projection) {
        this.projection = projection;
    }

    public static MyVideoMonitor getInstance(MediaProjection projection) {
        if (mMonitor == null) {
            mMonitor = new MyVideoMonitor(projection);
        }
        return mMonitor;
    }

    //初始化编码器
    public void init() {
        networkNative = new NetworkNative();
        networkNative.OpenSocket();
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
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Surface surface = mVideoEncoder.createInputSurface();

        mVideoEncoder.start();

        projection.createVirtualDisplay("MyMYMY", Utils.SCREEN_WIDTH, Utils.SCREEN_HEIGHT, 1, 0, surface, null, null);

        Log.e("Binson", "视频流编码器初始化完成!");
        sender = new Sender();
        new Thread(sender).start();

    }

    NetworkNative networkNative;

    public void work1(OutputStream outputStream) {
        Log.e("Binson", "work1");
        ByteBuffer[] byteBuffers = mVideoEncoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean encoderDone = false;

        while (!encoderDone) {
            Log.e("Binson", "encoderDone");
            int encoderStatus = mVideoEncoder.dequeueOutputBuffer(info, 0);
//            Log.d("encoderStatus", encoderStatus + "");
            ByteBuffer encodedData = byteBuffers[encoderStatus];
            byte[] bs = new byte[info.size];
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                networkNative.SendFrame(bs, bs.length, 0);
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                byteBuffers = mVideoEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // not expected for an encoder
                MediaFormat newFormat = mVideoEncoder.getOutputFormat();
            } else {
                if (encodedData == null) {
                    //something's wrong with the encoder
                    break;
                }
                // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                encodedData.position(info.offset);
                encodedData.limit(info.offset + info.size);
//                encodedData.array();
                if (outputStream != null) {
                    try {
                        encodedData.get(bs, info.offset, info.offset + info.size);
//                        outputStream.write(bs);
//                        outputStream.flush();
                        networkNative.SendFrame(bs, bs.length, 1);
                        Log.e("Binson", "flush..." + bs.length);
                    } catch (Exception e) {
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
                Log.e("Binson", "start Server");
                ServerSocket serverSocket = new ServerSocket(60000);
                Log.e("Binson", "Server is On");
                Socket socket = serverSocket.accept();
                Log.e("Binson", "got Client!");
                outputStream = socket.getOutputStream();
                work1(outputStream);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Binson", "start server error!" + e.toString());
            }
        }

        public void shutDown() {
            isStart = false;
        }

    }

}
