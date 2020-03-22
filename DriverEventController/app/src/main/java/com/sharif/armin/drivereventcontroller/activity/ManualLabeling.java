package com.sharif.armin.drivereventcontroller.activity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.sharif.armin.drivereventcontroller.R;
import com.sharif.armin.drivereventcontroller.socket.MySocket;
import com.sharif.armin.drivereventcontroller.socket.SocketObserver;

public class ManualLabeling extends AppCompatActivity implements SocketObserver {

    private TextView txtCounter;
    private MySocket socket;
    private String message;
    private boolean isStopped = false;
    private boolean isBackPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_manual_labeling);
        socket = MainActivity.getInstance().getSocket();
        socket.registerObserver(this);
        txtCounter = (TextView) findViewById(R.id.textTimer);
        CountUpTimer timer = new CountUpTimer(24 * 60 * 60 * 1000) {
            public void onTick(int second) {
                String txt = String.format("%d", second / 60) + ":" + String.format("%02d", second % 60);
                txtCounter.setText(txt);
            }
        };
        timer.start();
        initButton((Button) findViewById(R.id.lane_change_button));
        initButton((Button) findViewById(R.id.accelerate_button));
        initButton((Button) findViewById(R.id.brake_button));
        initButton((Button) findViewById(R.id.u_turn_button));
        initButton((Button) findViewById(R.id.turn_right_button));
        initButton((Button) findViewById(R.id.turn_left_button));
    }

    public void changeButtonColor(Button btn, String flag) {
        if (flag == "0") {
            btn.setTag("1");
            ViewCompat.setBackgroundTintList(btn, getResources().getColorStateList(R.color.orange));
        } else {
            btn.setTag("0");
            ViewCompat.setBackgroundTintList(btn, getResources().getColorStateList(R.color.green));
        }
    }

    public void initButton(Button btn) {
        btn.setTag("0");
        ViewCompat.setBackgroundTintList(btn, getResources().getColorStateList(R.color.green));
    }

    public void terminate(){
        socket.removeObserver(this);
        finish();
    }

    public void stop(View view){
        isStopped = true;
        message = "stop";
        socket.sendMessage(message);
        terminate();
    }

    public void laneChange(View view) {
        Button btn = (Button) findViewById(R.id.lane_change_button);
        String flag = (String) btn.getTag();
        message = "laneChange";
        socket.sendMessage(message);
        changeButtonColor(btn, flag);
    }

    public void turnRight(View view) {
        Button btn = (Button) findViewById(R.id.turn_right_button);
        String flag = (String) btn.getTag();
        message = "turnRight";
        socket.sendMessage(message);
        changeButtonColor(btn, flag);
    }

    public void turnLeft(View view) {
        Button btn = (Button) findViewById(R.id.turn_left_button);
        String flag = (String) btn.getTag();
        message = "turnLeft";
        socket.sendMessage(message);
        changeButtonColor(btn, flag);
    }

    public void uTurn(View view) {
        Button btn = (Button) findViewById(R.id.u_turn_button);
        String flag = (String) btn.getTag();
        message = "uTurn";
        socket.sendMessage(message);
        changeButtonColor(btn, flag);
    }

    public void accelerate(View view) {
        Button btn = (Button) findViewById(R.id.accelerate_button);
        String flag = (String) btn.getTag();
        message = "accelerate";
        socket.sendMessage(message);
        changeButtonColor(btn, flag);
    }

    public void brake(View view) {
        Button btn = (Button) findViewById(R.id.brake_button);
        String flag = (String) btn.getTag();
        message = "brake";
        socket.sendMessage(message);
        changeButtonColor(btn, flag);
    }

    @Override
    public void onBackPressed() {
        isBackPressed = true;
        message = "back";
        socket.sendMessage(message);
        terminate();
    }

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
    public void onMessageRecieved(String message) {
    }

    public abstract class CountUpTimer extends CountDownTimer {
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







