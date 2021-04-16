package com.sharif.armin.drivingeventlabeler.activity;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.sharif.armin.drivingeventlabeler.R;
import com.sharif.armin.drivingeventlabeler.detection.Detector;
import com.sharif.armin.drivingeventlabeler.detection.DetectorObserver;
import com.sharif.armin.drivingeventlabeler.detection.Event;
import com.sharif.armin.drivingeventlabeler.detection.SensorTest;
import com.sharif.armin.drivingeventlabeler.sensor.SensorSample;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;
import com.sharif.armin.drivingeventlabeler.sensor.SensorsObserver;
import com.sharif.armin.drivingeventlabeler.socket.MySocket;
import com.sharif.armin.drivingeventlabeler.socket.SocketObserver;
import com.sharif.armin.drivingeventlabeler.write.Writer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class GuidedLabeling extends AppCompatActivity implements DetectorObserver, SocketObserver, SensorsObserver {
    private Thread thread = null;
    private TextView txttimer, txtlabel;
    private int sensor_f, gps_delay;
    private Sensors sensors;
    private Writer writer;
    static boolean TestFlag;
    static String TestDir;
    private String filename;
    private Detector detector;
    private LinkedList<Event> upcomingEvents;
    private Event event;
    private SensorTest sensorTest;
    private boolean pause;
    private Handler handler;
    private MySocket socket;

    public static void setTestFlag(boolean testFlag){
        GuidedLabeling.TestFlag = testFlag;
    }
    public static void setTestDir(String testDir){
        GuidedLabeling.TestDir = testDir;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_guided_labeling);
        if(MainActivity.getController()){
            socket = MainActivity.getInstance().getSocket();
            socket.registerObserver(this);
            handler = new Handler();
        }
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

        writer = new Writer(MainActivity.directory.getPath());
        filename = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".zip";
        if (TestFlag) {
            sensorTest = new SensorTest(TestDir);
            detector = new Detector(sensor_f, sensorTest);
            detector.registerObserver(this);
            detector.start();
            sensorTest.start();
            filename = TestDir + File.separator + "test.zip";
        }
        else {
            sensors = new Sensors();
            sensors.setSensorManager((SensorManager) getSystemService(Context.SENSOR_SERVICE));
            sensors.setLocationManager((LocationManager) getSystemService((Context.LOCATION_SERVICE)));
            sensors.setGpsDelay(gps_delay);
            sensors.setSensorFrequency(sensor_f);
            sensors.registerObserver(this);
            detector = new Detector(sensor_f, sensors);
            detector.registerObserver(this);
            detector.start();
            sensors.start();
        }
        upcomingEvents = new LinkedList<>();
    }

    @Override
    public void onEventDetected(Event _event) {
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
            finish();
            startActivity(intent);
        }
        event = _event;
        correctEvent(event);
    }

    private void correctEvent(final Event event) {
        upcomingEvents.addFirst(event);
        boolean show = true;
        if(thread != null) {
            if(upcomingEvents.size() > 1) {
                Event lastEvent = upcomingEvents.get(1);
                upcomingEvents.removeLast();
                lastEvent.setEventLabel("intrupted/" + lastEvent.getEventLabel());
                writer.writeLabel(lastEvent.getEventLabel(), lastEvent.getStart(), lastEvent.getEnd());
            }
            thread.interrupt();
        }
        final boolean _show = show;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    if (_show) {
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
                                if(MainActivity.getController()){
                                    socket.sendMessage(event.getEventLabel());
                                }
                            }
                        });
                    }
                    Thread.sleep(2000);
                    if(event.getEventLabel().indexOf('/') == -1) {
                        event.setEventLabel("notConfirmed/" + event.getEventLabel());
                    }
                    writer.writeLabel(event.getEventLabel(), event.getStart(), event.getEnd());
                    clearMonitor();
                    upcomingEvents.removeFirst();
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
        if(MainActivity.getController()){
            socket.removeObserver(this);
        }
        pause = true;
        super.onPause();
    }

    private void clearMonitor(){
        if(MainActivity.getController()){
            socket.sendMessage("clear");
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtlabel.setBackgroundColor(0x00000000);
                txtlabel.setText("");
            }
        });
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
        back();
    }
    public void back(){
        this.writer.remove();
        finish();
        showCancelledToast();
        if(MainActivity.getController()){
            socket.removeObserver(this);
        }
    }

    public void stop(View view){
        stop();
    }
    public void stop(){
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

    public void aggresive(View view) {
        aggresive();
    }
    public void aggresive(){
        if(thread != null && event != null) {
            event.setEventLabel("aggresive/" + event.getEventLabel());
            clearMonitor();
        }
    }

    public void normal(View view) {
        normal();
    }
    public void normal(){
        if(thread != null && event != null) {
            event.setEventLabel("normal/" + event.getEventLabel());
            clearMonitor();
        }
    }

    public void remove(View view) {
        remove();
    }
    public void remove(){
        if(thread != null && event != null) {
            event.setEventLabel("remove/" + event.getEventLabel());
            clearMonitor();
        }
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
        else if ("back".contentEquals(message)) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    back();
                }
            };
            handler.post(run);
        }
        else if ("aggresive".contentEquals(message)) {
            aggresive();
        }
        else if ("remove".contentEquals(message)) {
            remove();
        }
        else if ("normal".contentEquals(message)) {
            normal();
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
}


