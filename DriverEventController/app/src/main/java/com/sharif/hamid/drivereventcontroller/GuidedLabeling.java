package com.sharif.hamid.drivereventcontroller;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class GuidedLabeling extends AppCompatActivity implements SocketObserver {

    private TextView txttimer,txtlabel;

    private MySocket socket;
    private String message;
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        socket = MainActivity.getInstance().getSocket();
        socket.registerObserver(this);
        handler = new Handler();
        setContentView(R.layout.activity_guided_labeling);
        txttimer = findViewById(R.id.textTimer);
        txtlabel = findViewById(R.id.monitor);
        CountUpTimer timer = new CountUpTimer(24 * 60 * 60 * 1000) {
            public void onTick(int second) {
                String txt = String.format("%d", second / 60) + ":" + String.format("%02d", second % 60);
                txttimer.setText(txt);
            }
        };
        timer.start();
    }

    public void aggresive(View view) {
        message = "aggresive";
        socket.sendMessage(message);

    }

    public void normal(View view) {
        message = "normal";
        socket.sendMessage(message);

    }

    public void remove(View view) {
        message = "remove";
        socket.sendMessage(message);

    }

    public void terminate(){
        socket.removeObserver(this);
        finish();

    }
    boolean isStopped = false;
    public void stop(View view){
        isStopped = true;
        message = "stop";
        socket.sendMessage(message);
        terminate();
    }
    @Override
    public void onBackPressed() {
        isBackPressed = true;
        message = "back";
        socket.sendMessage(message);
        terminate();
    }
    boolean isBackPressed = false;
    @Override
    protected void onPause(){
        if(!isBackPressed && !isStopped) {
            message = "stop";
            socket.sendMessage(message);

            terminate();

        }
        super.onPause();
    }




    @Override
    public void onMessageRecieved(final String message) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                txtlabel.setBackgroundResource(R.color.red);
                if("turn".contentEquals(message)){
                    txtlabel.setText("چرخش");
                }else if("brake".contentEquals(message)){
                    txtlabel.setText("ترمز");
                }else if("lane_change".contentEquals(message)){
                    txtlabel.setText("تغییر باند");
                }else if("clear".contentEquals(message)){
                    txtlabel.setBackgroundColor(0x000000);
                    txtlabel.setText("");
                }
            }
        };
        handler.post(run);

    }

    abstract class CountUpTimer extends CountDownTimer {
        private static final long INTERVAL_MS = 1000;
        private final long duration;

        protected CountUpTimer(long durationMs) {
            super(durationMs, INTERVAL_MS);
            this.duration = durationMs;
        }

        public abstract void onTick(int second);

        @Override
        public void onTick(long msUntilFinished) {
            int second = (int) ((duration - msUntilFinished) / 1000);
            onTick(second);
        }

        @Override
        public void onFinish() {
            onTick(duration / 1000);
        }
    }




}
