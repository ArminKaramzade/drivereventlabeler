package com.sharif.armin.drivingeventlabeler.activity;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sharif.armin.drivingeventlabeler.R;
import com.sharif.armin.drivingeventlabeler.detection.Detector;
import com.sharif.armin.drivingeventlabeler.detection.DetectorObserver;
import com.sharif.armin.drivingeventlabeler.detection.Event;
import com.sharif.armin.drivingeventlabeler.detection.SensorTest;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;
import com.sharif.armin.drivingeventlabeler.write.Writer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class GuidedLabeling extends AppCompatActivity implements DetectorObserver {
    private Thread thread = null;
    private TextView txttimer, txtlabel;
    private int sensor_f, gps_delay;
    private Sensors sensors;
    private Writer writer;
    boolean TestFlag = false;
    private String TestDir;
    private String filename;
    private Detector detector;
    private LinkedList<Event> upcomingEvents;
    private Event event;
    private SensorTest sensorTest;
    private boolean removeFlag = false, proccessingFlag = false;
    private boolean pause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_labeling);
        pause = false;
        txtlabel = findViewById(R.id.monitor);
        txttimer = findViewById(R.id.textTimer);
        CountUpTimer timer = new CountUpTimer(24 * 60 * 60 * 1000) {
            public void onTick(int second) {
                String txt = String.format("%d", second / 60) + ":" + String.format("%02d", second % 60);
                txttimer.setText(txt);
            }
        };
        timer.start();

        Intent intent = getIntent();
        sensor_f = Integer.parseInt(intent.getStringExtra(MainActivity.sensor_frequency));
        gps_delay = Integer.parseInt(intent.getStringExtra(MainActivity.gps_delay));
        TestFlag = intent.getBooleanExtra(MainActivity.TestFlag, false);
        TestDir = intent.getStringExtra(MainActivity.Direction);

        writer = new Writer(MainActivity.directory.getPath());
        filename = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".zip";
        if (TestFlag) {
            sensorTest = new SensorTest(TestDir);
            detector = new Detector(sensor_f, sensorTest);
            detector.registerObserver(this);
            detector.start();
            sensorTest.start();
            filename = TestDir + "test.zip";
        }
        else {
            sensors = Sensors.getInstance();
            sensors.setSensorManager((SensorManager) getSystemService(Context.SENSOR_SERVICE));
            sensors.setLocationManager((LocationManager) getSystemService((Context.LOCATION_SERVICE)));
            sensors.setGpsDelay(gps_delay);
            sensors.setSensorFrequency(sensor_f);
            detector = new Detector(sensor_f, sensors);
            detector.registerObserver(this);
            detector.start();
            sensors.start();
        }
        upcomingEvents = new LinkedList<>();
    }

    @Override
    public void onEventDetected(Event _event) {
        upcomingEvents.add(_event);
        if (TestFlag && _event.getEventLabel().compareTo("Finish") == 0){
            this.writer.saveAndRemove(filename);
            this.sensorTest.stop();
            this.detector.stop();
            Context context = getApplicationContext();
            CharSequence text = "Data Saved into " + MainActivity.directory.getPath() + filename + ".";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        proccessingFlag = true;
        event = upcomingEvents.getFirst();
        upcomingEvents.removeFirst();
        correctEvent(event);
    }

    private void correctEvent(final Event event) {
        if(thread != null) {
            thread.interrupt();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtlabel.setBackgroundResource(R.color.red);
                            if (event.getEventLabel().compareTo("turn") == 0)
                                txtlabel.setText(R.string.turn_text_activity_guided_labeling);
                            else if (event.getEventLabel().compareTo("brake") == 0)
                                txtlabel.setText(R.string.brake_text_activity_guided_labeling);
                            else if (event.getEventLabel().compareTo("lane_change") == 0)
                                txtlabel.setText(R.string.lane_change_text_activity_guided_labeling);
                        }
                    });
                    Thread.sleep(2000);
                    if (! removeFlag) {
                        writer.writeLabel(event.getEventLabel(), event.getStart(), event.getEnd());
                    }
                    removeFlag = false;
                    proccessingFlag = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtlabel.setBackgroundColor(0x00000000);
                            txtlabel.setText("");
                        }
                    });
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @Override
    protected void onPause(){
        if (!TestFlag) {
            this.sensors.stop();
        }
        else {
            sensorTest.stop();
        }
        this.detector.stop();
        this.detector.removeObserver(this);
        pause = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(pause){
            this.writer.saveAndRemove(filename);
            finish();
            showSavedToast();
        }
    }

    @Override
    public void onBackPressed() {
        this.writer.remove(filename);
        finish();
        showCancelledToast();
    }

    public void stop(View view){
        this.writer.saveAndRemove(filename);
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

    public void laneChange(View view) {
        if(proccessingFlag) {
            event.setEventLabel("lane_change/" + event.getEventLabel());
        }
    }

    public void turn(View view) {
        if(proccessingFlag) {
            event.setEventLabel("turn/" + event.getEventLabel());
        }
    }

    public void brake(View view) {
        if(proccessingFlag) {
            event.setEventLabel("brake/" + event.getEventLabel());
        }
    }

    public void remove(View view) {
        if(proccessingFlag){
            removeFlag = true;
        }
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


