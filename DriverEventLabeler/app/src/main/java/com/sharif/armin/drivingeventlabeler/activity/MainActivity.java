package com.sharif.armin.drivingeventlabeler.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.sharif.armin.drivingeventlabeler.R;
import com.sharif.armin.drivingeventlabeler.socket.MySocket;
import com.sharif.armin.drivingeventlabeler.socket.SocketObserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements SocketObserver {
    public static final String sensor_frequency = "com.drivingeventlabeler.mainActivity.sensor_frequency";
    public static final String gps_delay = "com.drivingeventlabeler.mainActivity.gps_delay";
    public static File directory;
    private static boolean controller;
    private static MainActivity Instance;
    private MySocket socket;
    private Handler handler;

    public static MainActivity getInstance(){
        return Instance;
    }
    public static void setController(boolean controller){
        MainActivity.controller = controller;
    }
    public static boolean getController(){
        return MainActivity.controller;
    }
    public MySocket getSocket(){return socket;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivity.Instance = this;
        requestForPermissions();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        socket.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0 : {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();

                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    public void init(){
        directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DrivingEventLabeler");
        if (!directory.exists()) {
            directory.mkdirs();
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, (Environment.getExternalStorageDirectory().toString() + File.separator + "DrivingEventLabeler"), duration);
            toast.show();
        }
        Setting.setParameters();
        if(MainActivity.controller){
            handler = new Handler();
            socket = new MySocket(getApplicationContext(), new Handler());
            socket.registerObserver(this);
            socket.start();
        }
    }

    public void requestForPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO}, 0);
        }
        else{
            init();
        }
    }

    public void manualLabeling(View view){
        manualLabeling();
    }

    public void manualLabeling() {
        Intent intent = new Intent(this, ManualLabeling.class);
        String sns_f = ((EditText)findViewById(R.id.sns_te)).getText().toString();
        String gps_f = ((EditText)findViewById(R.id.gps_te)).getText().toString();

        intent.putExtra(sensor_frequency, sns_f);
        intent.putExtra(gps_delay, gps_f);
        startActivity(intent);
    }

    public void visualization(View view){
        Intent intent = new Intent(this, Visualization.class);
        String sns_f = ((EditText)findViewById(R.id.sns_te)).getText().toString();
        String gps_f = ((EditText)findViewById(R.id.gps_te)).getText().toString();

        intent.putExtra(sensor_frequency, sns_f);
        intent.putExtra(gps_delay, gps_f);
        startActivity(intent);
    }

    public void guidedLabeling(View view){
        guidedLabeling();
    }

    public void guidedLabeling(){
        Intent intent = new Intent(this, GuidedLabeling.class);
        String sns_f = ((EditText)findViewById(R.id.sns_te)).getText().toString();
        String gps_f = ((EditText)findViewById(R.id.gps_te)).getText().toString();

        intent.putExtra(sensor_frequency, sns_f);
        intent.putExtra(gps_delay, gps_f);
        startActivity(intent);
    }

    public void biasEstimation(View view){
        Intent intent = new Intent(this, BiasEstimation.class);
        startActivity(intent);
    }

    public void setting(View view){
        Intent intent = new Intent(this, Setting.class);
        startActivity(intent);
    }

    @Override
    public void onMessageRecieved(String message) {
        if ("Guided".contentEquals(message)){
            Runnable run= new Runnable() {
                @Override
                public void run() {
                    guidedLabeling();
                }
            };
            handler.post(run);
        }
        else if ("Manual".contentEquals(message)){
            Runnable run= new Runnable() {
                @Override
                public void run() {
                    manualLabeling();
                }
            };
            handler.post(run);
        }
    }
}
