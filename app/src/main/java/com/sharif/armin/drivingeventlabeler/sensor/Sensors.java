package com.sharif.armin.drivingeventlabeler.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import com.sharif.armin.drivingeventlabeler.util.Utils;

public class Sensors {
    private SensorManager sensorManager;
    private SensorListener sensorListener;

    private long time;
    private int sensor_frequency;

    private boolean has_gyr = false;
    private boolean has_mgm = false;

    public float[] mgm = new float[4],
                    rac = new float[4],
                    gyr = new float[4],
                    acc = new float[4],
                    rot = new float[5],
                    lac = new float[4];

    private ComplementryFilterOrientation fusedOrientation;
    private LinearAcceleration linerAcc;

    public Sensors(SensorManager sensorManager, int sensor_frequency) {
        this.sensorManager = sensorManager;
        this.sensorListener = new SensorListener();
        this.sensor_frequency = sensor_frequency;
        fusedOrientation = new ComplementryFilterOrientation();
        linerAcc = new LinearAcceleration();
    }

    public void start() {
        register();
    }

    public void stop() {
        unRegister();
    }

    private void register() {
        fusedOrientation.reset();

        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                Utils.freq2delay(this.sensor_frequency));

        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                Utils.freq2delay(this.sensor_frequency));

        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                Utils.freq2delay(this.sensor_frequency));

        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                Utils.freq2delay(this.sensor_frequency));
    }

    private void unRegister(){
        sensorManager.unregisterListener(this.sensorListener);
    }

    private void processGYR(long time, SensorEvent event){
        System.arraycopy(event.values, 0, gyr, 0, this.gyr.length-1);
        this.gyr[this.gyr.length-1] = time;
    }
    private void processRAC(long time, SensorEvent event){
        System.arraycopy(event.values, 0, rac, 0, this.rac.length-1);
        this.rac[this.rac.length-1] = time;
    }
    private void processMGM(long time, SensorEvent event){
        System.arraycopy(event.values, 0, mgm, 0, this.mgm.length-1);
        this.mgm[this.mgm.length-1] = time;
    }
    private void processACC(long time, float[] acc){
        System.arraycopy(acc, 0, this.acc, 0, this.acc.length-1);
        this.acc[this.acc.length-1] = time;
    }
    private void processROT(long time, float[] rot){
        System.arraycopy(rot, 0, this.rot, 0, this.rot.length-1);
        this.rot[this.rot.length-1] = time;
    }
    private void processLAC(long time, SensorEvent event){
        System.arraycopy(event.values, 0, lac, 0, this.lac.length-1);
        this.lac[this.lac.length-1] = time;
    }

    private class SensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            time = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
            switch(event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    processGYR(time, event);
                    has_gyr = true;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    processMGM(time, event);
                    has_mgm = true;
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    processRAC(time, event);
                    if (!fusedOrientation.isInitialized()){
                        if (has_gyr && has_mgm){
                            fusedOrientation.setInitialOrientation(
                                    ComplementryFilterOrientation.getAccMgmOrientationVector(rac, mgm));
                        }
                    }else{
                        float[] orientation = fusedOrientation.calculateOrientation(event.timestamp, rac, gyr, mgm);
                        processACC(time, linerAcc.getAcc(rac, orientation));
                        processROT(time, fusedOrientation.getRotationVector());
                    }
                    break;

                case Sensor.TYPE_LINEAR_ACCELERATION:
                    processLAC(time, event);
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    }
}
