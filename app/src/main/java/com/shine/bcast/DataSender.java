package com.shine.bcast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by zoubingshun on 2017/5/8.
 */

public final class DataSender implements Runnable{

    private Thread myThread;
    private boolean isStart=true;
    private OutputStream outputStream;

    @Override
    public void run() {
        try {
            ServerSocket serverSocket=new ServerSocket(4321);
            while(isStart){
                Socket socket=serverSocket.accept();
                outputStream=socket.getOutputStream();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(){
        //启动服务
        myThread=new Thread(this);
        myThread.start();
    }


    public void shutDown(){
        isStart=false;
    }

    public OutputStream getOutputStream(){
        return outputStream;
    }


}
