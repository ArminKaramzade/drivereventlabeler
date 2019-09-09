package com.sharif.armin.drivingeventlabeler.sensor;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;

import com.sharif.armin.drivingeventlabeler.sensor.headingAngle.VehicleHeadingAngle;
import com.sharif.armin.drivingeventlabeler.sensor.linearAcceleration.LinearAcceleration;
import com.sharif.armin.drivingeventlabeler.sensor.orientation.Orientation;
import com.sharif.armin.drivingeventlabeler.util.Utils;
import org.apache.commons.math3.complex.Quaternion;
import java.util.ArrayList;

public class Sensors {
    private ArrayList<SensorsObserver> mObservers;
    public void registerObserver(SensorsObserver sensorsObserver){
        if(!mObservers.contains(sensorsObserver)) {
            mObservers.add(sensorsObserver);
        }
    }
    public void removeObserver(SensorsObserver sensorsObserver){
        if(mObservers.contains(sensorsObserver)) {
            mObservers.remove(sensorsObserver);
        }
    }
    public void notifyObserversSensorChanged(SensorSample sample){
        for (SensorsObserver observer: mObservers) {
            observer.onSensorChanged(sample);
        }
    }
    public void notifyObserversLocationChanged(Location location){
        for (SensorsObserver observer: mObservers) {
            observer.onLocationChanged(location);
        }
    }

    private SensorManager sensorManager;
    private SensorListener sensorListener;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private VehicleHeadingAngle bearing;
    private Orientation fusedOrientation;
    private LinearAcceleration linerAcc;
    private long time;
    private int sensorFrequency, gpsDelay;
    private boolean hasRac, hasMgm, initBng;
    private SensorSample mgm, rac, grv, gyr, acc, rot, rotV, bng;
    private SensorSample rot2, lac;
    private Location loc;

    public SensorSample getMgm() {
        return this.mgm;
    }
    public SensorSample getGrv() {
        return this.grv;
    }
    public SensorSample getRac() {
        return this.rac;
    }
    public SensorSample getAcc() {
        return this.acc;
    }
    public SensorSample getGyr() {
        return this.gyr;
    }
    public SensorSample getRot() {
        return this.rot;
    }
    public SensorSample getRotV(){
        return this.rotV;
    }
    public SensorSample getBng() {
        return this.bng;
    }
    public Location     getLoc() {
        return this.loc;
    }


    private Sensors(){
        this.sensorListener = new SensorListener();
        this.locationListener = new GPSListener();
        bearing = new VehicleHeadingAngle();
        fusedOrientation = new Orientation();
        linerAcc = new LinearAcceleration();
        mgm = new SensorSample(3, "MGM");
        rac = new SensorSample(3, "RAC");
        gyr = new SensorSample(3, "GYR");
        grv = new SensorSample(3, "GRV");
        acc = new SensorSample(3, "ACC");
        rot = new SensorSample(4, "ROT");
        rotV = new SensorSample(4, "ROTV");
        bng = new SensorSample(1, "BNG");
        rot2 = new SensorSample(4, "ROT2");
        lac = new SensorSample(3, "LAC");
        mObservers = new ArrayList<>();
    }
    private static class BillPughSingleton{
        private static final Sensors INSTANCE = new Sensors();
    }
    public static Sensors getInstance() {
        return BillPughSingleton.INSTANCE;
    }
    public void setSensorManager(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }
    public void setLocationManager(LocationManager locationManager) {
        this.locationManager = locationManager;
    }
    public void setSensorFrequency(int sensorFrequency) {
        this.sensorFrequency = sensorFrequency;
    }
    public void setGpsDelay(int gpsDelay) {
        this.gpsDelay = gpsDelay;
    }

    public void start() {
        hasRac = false;
        hasMgm = false;
        initBng = false;
        register();

    }
    public void stop() {
        unRegister();
        fusedOrientation.reset();
        linerAcc.reset();
        bearing.reset();
    }

    @SuppressLint("MissingPermission")
    private void register() {
        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                Utils.freq2delay(this.sensorFrequency));
        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                Utils.freq2delay(this.sensorFrequency));
        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                Utils.freq2delay(this.sensorFrequency));
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                this.gpsDelay,
                0,
                this.locationListener);

        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                Utils.freq2delay(this.sensorFrequency));
        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                Utils.freq2delay(this.sensorFrequency));
    }
    private void unRegister(){
        sensorManager.unregisterListener(this.sensorListener);
        locationManager.removeUpdates(this.locationListener);
    }

    private void processGYR(long time, SensorEvent event){
        System.arraycopy(event.values, 0, gyr.values, 0, this.gyr.values.length);
        this.gyr.time = time;
        notifyObserversSensorChanged(this.gyr);
        if(this.hasRac && this.hasMgm){
            fusedOrientation.filter(getGyr(), getRac(), getMgm(), time);
            Quaternion rot = fusedOrientation.getRotationVector();
            processROT(time, new float[] {(float) rot.getQ0(), (float) rot.getQ1(),
                    (float) rot.getQ2(), (float) rot.getQ3()});
            linerAcc.filter(getRac(), getRot());
            processACC(time, linerAcc.getAcc());
            processGRV(time, linerAcc.getGrv());
        }
        if(this.initBng) {
            float[] gyrEarth = Utils.rotate(getRot().values, getGyr().values);
            bearing.predict(gyrEarth[2], time);
            float angle = bearing.getAngle();
            processBNG(time, new float [] {angle});
            Quaternion rotE = fusedOrientation.getRotationVector();
            angle = 30f * (float) Math.PI / 180f;
            Quaternion rotV = rotE.multiply(new Quaternion(Math.cos(angle/2f), 0, 0, Math.sin(-angle/2f)));
            processROTVehicle(time, new float[] {(float) rotV.getQ0(), (float) rotV.getQ1(),
                    (float) rotV.getQ2(), (float) rotV.getQ3()});
        }
    }
    private void processRAC(long time, SensorEvent event){
        System.arraycopy(event.values, 0, rac.values, 0, this.rac.values.length);
        this.rac.time = time;
        notifyObserversSensorChanged(this.rac);
        hasRac = true;
//        linerAcc.filter(getRac());
//        processGRV(time, linerAcc.getGrv());
    }
    private void processMGM(long time, SensorEvent event){
        System.arraycopy(event.values, 0, mgm.values, 0, this.mgm.values.length);
        this.mgm.time = time;
        notifyObserversSensorChanged(this.mgm);
        hasMgm = true;
    }
    private void processACC(long time, float[] event){
        System.arraycopy(event, 0, this.acc.values, 0, this.acc.values.length);
        this.acc.time = time;
        notifyObserversSensorChanged(this.acc);
    }
    private void processGRV(long time, float[] event){
        System.arraycopy(event, 0, grv.values, 0, this.grv.values.length);
        this.grv.time = time;
        notifyObserversSensorChanged(this.grv);
    }
    private void processROT(long time, float[] event){
        System.arraycopy(event, 0, this.rot.values, 0, this.rot.values.length);
        this.rot.time = time;
        notifyObserversSensorChanged(this.rot);
    }
    private void processROT2(long time, SensorEvent event){
        System.arraycopy(event.values, 0, this.rot2.values, 0, this.rot2.values.length);
        this.rot2.time = time;
        notifyObserversSensorChanged(this.rot2);
    }
    private void processLAC(long time, SensorEvent event){
        System.arraycopy(event.values, 0, this.lac.values, 0, this.lac.values.length);
        this.lac.time = time;
        notifyObserversSensorChanged(this.lac);
    }
    private void processROTVehicle(long time, float[] event){
        System.arraycopy(event, 0, this.rotV.values, 0, this.rotV.values.length);
        this.rotV.time = time;
        notifyObserversSensorChanged(this.rotV);
    }
    private void processBNG(long time, float[] event){
        this.bng.values[0] = event[0];
        this.bng.time = time;
        notifyObserversSensorChanged(this.bng);
    }
    private void processLOC(Location location){
        loc = location;
        notifyObserversLocationChanged(this.loc);
        if(loc.hasBearing()) {
            float angle = loc.getBearing();
            if(angle <= 180){
                angle = -angle;
            }else{
                angle = 360 - angle;
            }
            angle = angle * 0.017453292519943295f;
            if(initBng == false){
                this.bearing.setCovX(new float[][] {{0.01f, 0f}, {0f, 0.01f}});
                this.bearing.setMuX(new float[] {angle, 0});
                this.bearing.setSigmaA(0.01f);
                this.bearing.setSigmaW(0.001f);
                this.bearing.setPrevTime(loc.getTime());
                initBng = true;
            }
            bearing.update(angle);
        }
    }

    private class SensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            time = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
            switch(event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    processGYR(time, event);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    processMGM(time, event);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    processRAC(time, event);
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    processROT2(time, event);
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    processLAC(time, event);
                    break;
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    }
    private class GPSListener implements LocationListener{
        @Override
        public void onLocationChanged(Location _location) {
            processLOC(_location);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onProviderDisabled(String provider) { }
    }
}
