package com.sharif.hamid.drivereventcontroller;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MySocket {
    private ArrayList<SocketObserver> mObservers;
    public void registerObserver(SocketObserver socketObserver){
        if(!mObservers.contains(socketObserver)) {
            mObservers.add(socketObserver);
        }
    }
    public void removeObserver(SocketObserver socketObserver){
        if(mObservers.contains(socketObserver)) {
            mObservers.remove(socketObserver);
        }
    }
    public void notifyObserversMessageRecieved(String message){
        for (SocketObserver observer: mObservers) {
            observer.onMessageRecieved(message);
        }
    }

    public static final int SERVERPORT = 3003;
    public String SERVER_IP = "YOUR_SERVER_IP";
    private ClientThread clientThread;
    private Thread thread;
    private Handler handler;
    private Context context;

    public MySocket(Context context, Handler handler, String ip){
        this.context = context;
        this.handler = handler;
        this.SERVER_IP = ip;
        mObservers = new ArrayList<>();
    }

    public void start(){
        clientThread = new ClientThread();
        thread = new Thread(clientThread);
        thread.start();
    }

    public void disconnect(){
        if (null != clientThread) {
            clientThread.sendMessage("Disconnect");
            clientThread = null;
        }
    }
    public boolean isConnected(){
        return (null != clientThread);
    }

    public void showToast(String message){

        final String _message = message;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, _message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void sendMessage(String message){
        final String _message = message;
        clientThread.sendMessage(_message);

    }

    class ClientThread implements Runnable {

        private Socket socket;
        private BufferedReader input;

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                showToast("connected to the server");
                while (!Thread.currentThread().isInterrupted()) {

                    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = input.readLine();
                    if (null == message || "Disconnect".contentEquals(message)) {
                        Thread.interrupted();
                        break;
                    }
                    notifyObserversMessageRecieved(message);
                }
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
                showToast("Not connected");
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

        void sendMessage(final String message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);
                            out.println(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
