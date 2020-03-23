package com.sharif.armin.drivereventcontroller.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.sharif.armin.drivereventcontroller.R;
import com.sharif.armin.drivereventcontroller.socket.MySocket;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    private static MainActivity Instance;

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (socket != null) {
            socket.disconnect();
        }
    }

    EditText ipAddressTxt;
    Button btnManual,btnGuided, btnConnect;
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
