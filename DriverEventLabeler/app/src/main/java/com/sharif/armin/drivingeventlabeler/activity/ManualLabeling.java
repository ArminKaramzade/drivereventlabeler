package com.sharif.armin.drivingeventlabeler.activity;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sharif.armin.drivingeventlabeler.R;
import com.sharif.armin.drivingeventlabeler.sensor.SensorSample;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;
import com.sharif.armin.drivingeventlabeler.sensor.SensorsObserver;
import com.sharif.armin.drivingeventlabeler.socket.MySocket;
import com.sharif.armin.drivingeventlabeler.socket.SocketObserver;
import com.sharif.armin.drivingeventlabeler.write.Writer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ManualLabeling extends AppCompatActivity implements SensorsObserver, SocketObserver {
    private TextView txtCounter;
    private int sensor_f, gps_delay;
    private Sensors sensors;
    private Writer writer;
    private long accelerateStart, brakeStart, turnRightStart,
            turnLeftStart, uTurnStart, laneChangeStart;
    private String filename;
    private boolean pause;
    private static boolean voiceRecording;
    private MediaRecorder recorder = null;
    private String voiceName = "voice.3gp";
    private String voiceDir = MainActivity.directory.getPath() + File.separator + this.voiceName;
    private MySocket socket;
    private Handler handler;

    public static void setVoiceRecording(boolean voiceRecording){
        ManualLabeling.voiceRecording = voiceRecording;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_manual_labeling);
        if(MainActivity.getController()){
            socket = MainActivity.getInstance().getSocket();
            socket.registerObserver(this);
            handler = new Handler();
        }
        pause = false;
        txtCounter = (TextView) findViewById(R.id.textTimer);
        CountUpTimer timer = new CountUpTimer(24 * 60 * 60 * 1000) {
            public void onTick(int second) {
                String txt = String.format("%d", second / 60) + ":" + String.format("%02d", second % 60);
                txtCounter.setText(txt);
            }
        };
        timer.start();
        Intent intent = getIntent();
        if (! voiceRecording) {
            initButton((Button) findViewById(R.id.lane_change_button));
            initButton((Button) findViewById(R.id.accelerate_button));
            initButton((Button) findViewById(R.id.brake_button));
            initButton((Button) findViewById(R.id.u_turn_button));
            initButton((Button) findViewById(R.id.turn_right_button));
            initButton((Button) findViewById(R.id.turn_left_button));
        }
        else{
            ViewGroup layout = (ViewGroup) findViewById(R.id.lane_change_button).getParent();
            layout.removeView(findViewById(R.id.lane_change_button));
            layout.removeView(findViewById(R.id.accelerate_button));
            layout.removeView(findViewById(R.id.brake_button));
            layout.removeView(findViewById(R.id.lane_change_button));
            layout.removeView(findViewById(R.id.u_turn_button));
            layout.removeView(findViewById(R.id.turn_right_button));
            layout.removeView(findViewById(R.id.turn_left_button));
            this.recorder = new MediaRecorder();
            this.recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            this.recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            this.recorder.setOutputFile(MainActivity.directory.getPath() + File.separator + this.voiceName);
            this.recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            try {
                this.recorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        sensor_f = Integer.parseInt(intent.getStringExtra(MainActivity.sensor_frequency));
        gps_delay = Integer.parseInt(intent.getStringExtra(MainActivity.gps_delay));
        writer = new Writer(MainActivity.directory.getPath());
        this.sensors = new Sensors();
        this.sensors.setSensorManager((SensorManager) getSystemService(Context.SENSOR_SERVICE));
        this.sensors.setLocationManager((LocationManager) getSystemService((Context.LOCATION_SERVICE)));
        this.sensors.setGpsDelay(gps_delay);
        this.sensors.setSensorFrequency(sensor_f);
        this.sensors.registerObserver(this);
        this.sensors.start();
        if (voiceRecording) {
            this.recorder.start();
        }
        filename = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".zip";
    }

    @Override
    protected void onPause() {
        this.sensors.removeObserver(this);
        this.sensors.stop();
        if(this.recorder != null) {
            this.recorder.stop();
            this.recorder.release();
        }
        if(MainActivity.getController()){
            socket.removeObserver(this);
        }
        pause = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(pause){
            if(voiceRecording){
                this.writer.saveAndRemove(filename, new String[]{this.voiceDir});
            } else {
                this.writer.saveAndRemove(filename);
            }
            finish();
            showSavedToast();
        }
    }

    @Override
    public void onBackPressed() {
        back();
    }
    public void back() {
        if(voiceRecording){
            this.writer.remove(new String[]{this.voiceDir});
        } else {
            this.writer.remove();
        }
        finish();
        showCancelledToast();
    }

    public void stop(View view){
        stop();
    }
    public void stop(){
        if(voiceRecording){
            this.recorder.stop();
            this.recorder.release();
            this.writer.saveAndRemove(filename, new String[]{this.voiceDir});
            this.recorder = null;
        } else {
            this.writer.saveAndRemove(filename);
        }
        finish();
        showSavedToast();
    }

    private void showSavedToast(){
        Context context = getApplicationContext();
        CharSequence text = "Data saved into " + MainActivity.directory.getPath() + filename + ".";
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
    private void showCancelledToast(){
        Context context = getApplicationContext();
        CharSequence text = "Data didn't saved.";
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @Override
    public void onSensorChanged(SensorSample sample){
        switch (sample.type){
            case Sensors.TYPE_LINEAR_ACCELERATION_PHONE:
                writer.writeLinearAccelerationPhone(sample);
                break;
            case Sensors.TYPE_LINEAR_ACCELERATION_VEHICLE:
                writer.writeLinearAccelerationVehicle(sample);
                break;
            case Sensors.TYPE_RAW_ACCELERATION_PHONE:
                writer.writeRawAccelerationPhone(sample);
                break;
            case Sensors.TYPE_ANGULAR_VELOCITY_PHONE:
                writer.writeAngularVelocityPhone(sample);
                break;
            case Sensors.TYPE_ANGULAR_VELOCITY_EARTH:
                writer.writeAngularVelocityEarth(sample);
                break;
            case Sensors.TYPE_MAGNETIC_PHONE:
                writer.writeMagneticPhone(sample);
                break;
            case Sensors.TYPE_GRAVITY_PHONE:
                writer.writeGravityPhone(sample);
                break;
            case Sensors.TYPE_ROTATION_VECTOR_EARTH:
                writer.writeRotationVectorEarth(sample);
                break;
            case Sensors.TYPE_ROTATION_VECTOR_VEHICLE:
                writer.writeRotationVectorVehicle(sample);
                break;
            case Sensors.TYPE_HEADING_ANGLE_VEHICLE:
                writer.writeHeadingAngleVehicle(sample);
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location){
        writer.writeGPS(location);
    }

    public void initButton(Button btn) {
        btn.setTag("0");
        ViewCompat.setBackgroundTintList(btn, getResources().getColorStateList(R.color.green));
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

    public void laneChange(View view) {
        laneChange();
    }
    public void laneChange() {
        Button btn = (Button) findViewById(R.id.lane_change_button);
        String flag = (String) btn.getTag();
        if (flag == "0") {
            laneChangeStart = System.currentTimeMillis();
        }
        else {
            writer.writeLabel("lane_change", laneChangeStart, System.currentTimeMillis());
        }
        changeButtonColor(btn, flag);
    }

    public void turnRight(View view) {
        turnRight();
    }
    public void turnRight() {
        Button btn = (Button) findViewById(R.id.turn_right_button);
        String flag = (String) btn.getTag();
        if (flag == "0") {
            turnRightStart = System.currentTimeMillis();
        }
        else {
            writer.writeLabel("turn_rigth", turnRightStart, System.currentTimeMillis());
        }
        changeButtonColor(btn, flag);
    }

    public void turnLeft(View view) {
        turnLeft();
    }
    public void turnLeft() {
        Button btn = (Button) findViewById(R.id.turn_left_button);
        String flag = (String) btn.getTag();
        if (flag == "0") {
            turnLeftStart = System.currentTimeMillis();
        }
        else {
            writer.writeLabel("turn_left", turnLeftStart, System.currentTimeMillis());
        }
        changeButtonColor(btn, flag);
    }

    public void uTurn(View view) {
        uTurn();
    }
    public void uTurn() {
        Button btn = (Button) findViewById(R.id.u_turn_button);
        String flag = (String) btn.getTag();
        if (flag == "0") {
            uTurnStart = System.currentTimeMillis();
        }
        else {
            writer.writeLabel("u_turn", uTurnStart, System.currentTimeMillis());
        }
        changeButtonColor(btn, flag);
    }

    public void accelerate(View view) {
        accelerate();
    }
    public void accelerate() {
        Button btn = (Button) findViewById(R.id.accelerate_button);
        String flag = (String) btn.getTag();
        if (flag == "0") {
            accelerateStart = System.currentTimeMillis();
        }
        else {
            writer.writeLabel("acceleration", accelerateStart, System.currentTimeMillis());
        }
        changeButtonColor(btn, flag);
    }

    public void brake(View view) {
        brake();
    }
    public void brake() {
        Button btn = (Button) findViewById(R.id.brake_button);
        String flag = (String) btn.getTag();
        if (flag == "0") {
            brakeStart = System.currentTimeMillis();
        }
        else {
            writer.writeLabel("brake", brakeStart, System.currentTimeMillis());
        }
        changeButtonColor(btn, flag);
    }

    @Override
    public void onMessageRecieved(String message) {
        if ("stop".contentEquals(message)){
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    stop();
                }
            };
            handler.post(run);
        }
        else if ("laneChange".contentEquals(message)){
            laneChange();
        }
        else if ("turnRight".contentEquals(message)){
            turnRight();
        }
        else if ("turnLeft".contentEquals(message)){
            turnLeft();
        }
        else if ("uTurn".contentEquals(message)){
            uTurn();
        }
        else if ("accelerate".contentEquals(message)){
            accelerate();
        }
        else if ("brake".contentEquals(message)){
            brake();
        }
        else if ("back".contentEquals(message)){
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    back();
                }
            };
            handler.post(run);
        }
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
