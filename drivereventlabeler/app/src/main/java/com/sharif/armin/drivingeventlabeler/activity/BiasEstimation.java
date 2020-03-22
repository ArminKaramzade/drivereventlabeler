package com.sharif.armin.drivingeventlabeler.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;
import com.sharif.armin.drivingeventlabeler.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BiasEstimation extends AppCompatActivity {
    private int gyrN, racN;
    private float[] gyrMu, racMu;
    private SensorManager sensorManager;
    private SensorListener sensorListener;
    private String dir = "setting";
    private String fn = "biases.csv";
    private CSVWriter writer;
    final static int msecs = 20000;
    boolean finished = false;
    private TextView txttimer;
    File f;
    private Thread thread;
    private boolean pause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bias_estimation);
        pause = false;
        gyrMu = new float[] {0, 0, 0};
        racMu = new float[] {0, 0, 0};
        gyrN = 0;
        racN = 0;
    }

    @Override
    protected void onPause() {
        if(thread != null) {
            thread.interrupt();
            pause = true;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pause){
            sensorManager.unregisterListener(sensorListener);
            finish();
            Context context = getApplicationContext();
            CharSequence text = "Bias estimation has failed.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    @Override
    public void onBackPressed() {
        if(thread != null){
            thread.interrupt();
            sensorManager.unregisterListener(sensorListener);
            Context context = getApplicationContext();
            CharSequence text = "Bias estimation has failed.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
        finish();
    }

    public void start(View view){
        Button btn = (Button) findViewById(R.id.start_button);
        btn.setEnabled(false);
        ViewCompat.setBackgroundTintList(btn, getResources().getColorStateList(R.color.gray));
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorListener = new SensorListener();
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        Thread.sleep(msecs);
                        finished = true;
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        txttimer = (TextView) findViewById(R.id.textTimer);
        new CountDownTimer(msecs, 1000){
            public void onTick(long mseconds){
                String txt = String.format("remaining time:" + "%d", mseconds / 1000);
                txttimer.setText(txt);
            }
            public void onFinish() {
                String txt = String.format("remaining time:" + "%d", 0);
                txttimer.setText(txt);
            }
        }.start();
    }

    public void stop(){
        File directory = new File(MainActivity.directory.getPath() + File.separator + dir);
        if(!directory.exists()){
            directory.mkdirs();
        }
        f = new File(MainActivity.directory.getPath() + File.separator + dir + File.separator + fn);
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer = new CSVWriter(new FileWriter(f, false));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] line = new String [] {"sensor", "X", "Y", "Z"};
        writer.writeNext(line);
        sensorManager.unregisterListener(sensorListener);
        float gyrBX = 0, gyrBY = 0, gyrBZ = 0;
        if (gyrN != 0){
            gyrBX = gyrMu[0] / gyrN;
            gyrBY = gyrMu[1] / gyrN;
            gyrBZ = gyrMu[2] / gyrN;
        }
        float racBX = 0, racBY = 0, racBZ = 0;
        if (racN != 0){
            racBX = racMu[0] / racN;
            racBY = racMu[1] / racN;
            racBZ = (racMu[2] / racN) - SensorManager.GRAVITY_EARTH;
        }

        line = new String [] {"gyr", String.valueOf(gyrBX), String.valueOf(gyrBY), String.valueOf(gyrBZ)};
        writer.writeNext((line));
        line = new String [] {"rac", String.valueOf(racBX), String.valueOf(racBY), String.valueOf(racBZ)};
        writer.writeNext((line));
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
        Context context = getApplicationContext();
        CharSequence text = "Bias estimation has finished.";
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

    }

    private class SensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch(event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    gyrMu[0] += event.values[0];
                    gyrMu[1] += event.values[1];
                    gyrMu[2] += event.values[2];
                    gyrN += 1;
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    racMu[0] += event.values[0];
                    racMu[1] += event.values[1];
                    racMu[2] += event.values[2];
                    racN += 1;
                    break;
            }
            if (finished)
                stop();
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    }
}
