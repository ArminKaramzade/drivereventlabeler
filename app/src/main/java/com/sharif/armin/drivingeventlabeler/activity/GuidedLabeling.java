package com.sharif.armin.drivingeventlabeler.activity;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sharif.armin.drivingeventlabeler.R;
import com.sharif.armin.drivingeventlabeler.detection.Detector;
import com.sharif.armin.drivingeventlabeler.detection.Event;
import com.sharif.armin.drivingeventlabeler.gps.GPS;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;
import com.sharif.armin.drivingeventlabeler.write.Writer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class GuidedLabeling extends AppCompatActivity implements PropertyChangeListener {
    private Thread thread = null;

    private TextView txttimer, txtlabel;
    private int sensor_f, gps_delay;
    private Sensors sensors;
    private GPS gps;
    private Writer writer;
    private String filename;
    private Detector Detector;
    private LinkedList<Event> upcomingEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_labeling);
        txtlabel = (TextView) findViewById(R.id.textlabel);
        txttimer = (TextView) findViewById(R.id.textTimer);
        ManualLabeling.CountUpTimer timer = new ManualLabeling.CountUpTimer(24 * 60 * 60 * 1000) {
            public void onTick(int second) {
                String txt = String.format("%d", second / 60) + ":" + String.format("%02d", second % 60);
                txttimer.setText(txt);
            }
        };
        Intent intent = getIntent();
        sensor_f = Integer.parseInt(intent.getStringExtra(MainActivity.sensor_frequency));
        gps_delay = Integer.parseInt(intent.getStringExtra(MainActivity.gps_delay));

        writer = new Writer(MainActivity.directory.getPath());

        sensors = new Sensors((SensorManager) getSystemService(Context.SENSOR_SERVICE), sensor_f);
        gps = new GPS((LocationManager) getSystemService((Context.LOCATION_SERVICE)), writer, gps_delay);

        sensors.start();
        gps.start();
        filename = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".zip";

        Detector = new Detector(sensor_f, this, sensors);
        upcomingEvents = new LinkedList<>();
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        String name = propertyChangeEvent.getPropertyName();
        Event newvalue = (Event) propertyChangeEvent.getNewValue();
        upcomingEvents.add(newvalue);
        showEvent();
    }

    private void showEvent() {
        if(thread == null ||thread.isInterrupted() || upcomingEvents.size() == 0){
            return;
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        txtlabel.setBackgroundResource(R.color.green);
                        txtlabel.setText(upcomingEvents.getFirst().getEventLable());
                        Thread.sleep(2000);
                        writeLable(upcomingEvents.getFirst());
                        upcomingEvents.removeFirst();
                        showEvent();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private void writeLable(Event event) {
        writer.writeLBL(event.getEventLable(), event.getStart(), event.getEnd());
        txtlabel.setBackgroundResource(R.color.red);
    }

    protected void onStop(){
        super.onStop();
        sensors.stop();
        gps.stop();
    }
    protected void onDestroy(){
        super.onDestroy();
        sensors.stop();
        gps.stop();
    }

    public void stop(View view){
        this.sensors.stop();
        this.gps.stop();
        this.writer.saveAndRemove(filename);
        Context context = getApplicationContext();
        CharSequence text = "Data Saved into " + MainActivity.directory.getPath() + filename + ".";
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void laneChange(View view) {
        if(thread == null || thread.isInterrupted()){
            return;
        }
        writeLable(upcomingEvents.getFirst());
        upcomingEvents.removeFirst();
        showEvent();
    }

    public void turn(View view) {
        if(thread == null || thread.isInterrupted()){
            return;
        }
        writeLable(upcomingEvents.getFirst());
        upcomingEvents.removeFirst();
        showEvent();
    }

    public void brake(View view) {
        if(thread == null || thread.isInterrupted()){
            return;
        }
        writeLable(upcomingEvents.getFirst());
        upcomingEvents.removeFirst();
        showEvent();
    }

}
