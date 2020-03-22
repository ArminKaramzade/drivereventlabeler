package com.sharif.hamid.drivereventcontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.widget.Toast;


@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    private static MainActivity Instance;

    @Override
    protected void onDestroy(){
        super.onDestroy();
        socket.disconnect();
    }

    EditText ipAddressTxt;
    Button btnManual,btnGuided, btnConnect;
    String SERVER_IP;
    private MySocket socket;

    public MySocket getSocket(){
        return this.socket;
    }

    public static MainActivity getInstance() {
        return Instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Instance = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ipAddressTxt = findViewById(R.id.ipAddressTxt);
        btnManual = findViewById(R.id.btnManual);
        btnGuided = findViewById(R.id.btnGuided);
        btnConnect = findViewById(R.id.btnConnect);


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ip = ipAddressTxt.getText().toString();
                socket = new MySocket(getApplicationContext(), new Handler(), ip);
                socket.start();
            }
        });

        btnManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (socket != null && socket.isConnected()) {
                    socket.sendMessage("Manual");
                    Intent intent = new Intent(getApplicationContext(), ManualLabeling.class);
                    startActivity(intent);
                }
            }
        });

        btnGuided.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (socket != null && socket.isConnected()){
                    socket.sendMessage("Guided");
                    Intent intent = new Intent(getApplicationContext(), GuidedLabeling.class);
                    startActivity(intent);
                }
            }
        });
    }
}
