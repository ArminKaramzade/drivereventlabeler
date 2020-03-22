package com.sharif.armin.drivingeventlabeler.socket;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.sharif.armin.drivingeventlabeler.sensor.SensorsObserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MySocket{
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

    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    Thread serverThread = null;
    public static final int SERVER_PORT = 3003;
    private Handler handler;
    private Context context;

    public MySocket(Context context, Handler handler){
        this.context = context;
        this.handler = handler;
        mObservers = new ArrayList<>();
    }

    public void start(){
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    public void disconnect(){
        if (null != serverThread) {
            sendMessage("Disconnect");
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverThread.interrupt();
            serverThread = null;
        }
    }

    public void showToast(String message){
        final String _message = message;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, _message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void sendMessage(String message) {
        final String _message = message;
        try {
            if (null != tempClientSocket) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrintWriter out = null;
                        try {
                            out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(tempClientSocket.getOutputStream())),
                                    true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        out.println(_message);
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Error starting server");
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showToast("Error Communicating to Client");
                    }
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            tempClientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Error Connecting to Client");
            }
            showToast("Connected to Client");
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    if (null == read || "Disconnect".contentEquals(read)) {
                        Thread.interrupted();
                        break;
                    }
                    notifyObserversMessageRecieved(read);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
