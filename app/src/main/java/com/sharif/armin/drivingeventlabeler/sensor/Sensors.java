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
import com.sharif.armin.drivingeventlabeler.sensor.magnetometer.Magnetometer;
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

    private static boolean useAndroidDefaultSensors;
    public static final int TYPE_ANGULAR_VELOCITY_PHONE = 0,
            TYPE_ANGULAR_VELOCITY_EARTH = 1,
            TYPE_MAGNETIC_PHONE = 2,
            TYPE_GRAVITY_PHONE = 3,
            TYPE_RAW_ACCELERATION_PHONE = 4,
            TYPE_LINEAR_ACCELERATION_PHONE = 5,
            TYPE_LINEAR_ACCELERATION_VEHICLE = 6,
            TYPE_ROTATION_VECTOR_EARTH = 7,
            TYPE_ROTATION_VECTOR_VEHICLE = 8,
            TYPE_HEADING_ANGLE_VEHICLE = 9;

    public static void setUseAndroidDefaultSensors(boolean useAndroidDefaultSensors){
        Sensors.useAndroidDefaultSensors = useAndroidDefaultSensors;
    }

    private SensorManager sensorManager;
    private SensorListener sensorListener;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private VehicleHeadingAngle headingAngleFilter;
    private Orientation fusedOrientationFilter;
    private LinearAcceleration linearAccelerationFilter;
    private Magnetometer magnetometer;
    private long time;
    private int sensorFrequency, gpsDelay;
    private boolean rawAccelerationArrived, magneticArrived, headingAngleArrived;
    private float [] angularVelocityBias, rawAccelerationBias;
    private SensorSample angularVelocityPhone,
                         angularVelocityEarth,
                         magneticPhone,
                         gravityPhone,
                         rawAccelerationPhone,
                         linearAccelerationPhone,
                         linearAccelerationVehicle,
                         rotationVectorEarth,
                         rotationVectorVehicle,
                         headingAnglePhone;
    private Location location;

    public SensorSample getMagneticPhone() {
        return this.magneticPhone;
    }
    public SensorSample getGravityPhone() {
        return this.gravityPhone;
    }
    public SensorSample getRawAccelerationPhone() {
        return this.rawAccelerationPhone;
    }
    public SensorSample getLinearAccelerationPhone() {
        return this.linearAccelerationPhone;
    }
    public SensorSample getAngularVelocityPhone() {
        return this.angularVelocityPhone;
    }
    public SensorSample getRotationVectorEarth() {
        return this.rotationVectorEarth;
    }
    public SensorSample getRotationVectorVehicle(){
        return this.rotationVectorVehicle;
    }
    public SensorSample getHeadingAnglePhone() {
        return this.headingAnglePhone;
    }
    public SensorSample getLinearAccelerationVehicle(){
        return this.linearAccelerationVehicle;
    }
    public SensorSample getAngularVelocityEarth(){
        return this.angularVelocityEarth;
    }
    public Location     getLocation() {
        return this.location;
    }


    public Sensors(){
        this.sensorListener = new SensorListener();
        this.locationListener = new GPSListener();
        headingAngleFilter = new VehicleHeadingAngle();
        magnetometer = new Magnetometer();
        if(!useAndroidDefaultSensors) {
            fusedOrientationFilter = new Orientation();
            linearAccelerationFilter = new LinearAcceleration();
        }
        angularVelocityPhone = new SensorSample(3, TYPE_ANGULAR_VELOCITY_PHONE);
        angularVelocityEarth = new SensorSample(3, TYPE_ANGULAR_VELOCITY_EARTH);
        magneticPhone = new SensorSample(3, TYPE_MAGNETIC_PHONE);
        gravityPhone = new SensorSample(3, TYPE_GRAVITY_PHONE);
        rawAccelerationPhone = new SensorSample(3, TYPE_RAW_ACCELERATION_PHONE);
        linearAccelerationPhone = new SensorSample(3, TYPE_LINEAR_ACCELERATION_PHONE);
        linearAccelerationVehicle = new SensorSample(3, TYPE_LINEAR_ACCELERATION_VEHICLE);
        rotationVectorEarth = new SensorSample(4, TYPE_ROTATION_VECTOR_EARTH);
        rotationVectorVehicle = new SensorSample(4, TYPE_ROTATION_VECTOR_VEHICLE);
        headingAnglePhone = new SensorSample(1, TYPE_HEADING_ANGLE_VEHICLE);
        mObservers = new ArrayList<>();
        angularVelocityBias = Utils.getGyrBias();
        rawAccelerationBias = Utils.getRacBias();
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
        rawAccelerationArrived = false;
        magneticArrived = false;
        headingAngleArrived = false;
        register();
    }
    public void stop() {
        unRegister();
        headingAngleFilter.reset();
        magnetometer.reset();
        if(! useAndroidDefaultSensors) {
            fusedOrientationFilter.reset();
            linearAccelerationFilter.reset();
        }
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
        if(Sensors.useAndroidDefaultSensors) {
            sensorManager.registerListener(this.sensorListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                    Utils.freq2delay(this.sensorFrequency));
            sensorManager.registerListener(this.sensorListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                    Utils.freq2delay(this.sensorFrequency));
            sensorManager.registerListener(this.sensorListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                    Utils.freq2delay(this.sensorFrequency));
        }
    }

    private void unRegister(){
        sensorManager.unregisterListener(this.sensorListener);
        locationManager.removeUpdates(this.locationListener);
    }

    private void processAngularVelocityEarth(long time, float[] event){
        System.arraycopy(event, 0, this.angularVelocityEarth.values, 0, this.angularVelocityEarth.values.length);
        this.angularVelocityEarth.time = time;
        notifyObserversSensorChanged(this.angularVelocityEarth);
    }

    private void processLinearAccelerationVehicle(long time, float[] event){
        System.arraycopy(event, 0, this.linearAccelerationVehicle.values, 0, this.linearAccelerationVehicle.values.length);
        this.linearAccelerationVehicle.time = time;
        notifyObserversSensorChanged(this.linearAccelerationVehicle);
    }

    private void processAngularVelocityPhone(long time, SensorEvent event){
        System.arraycopy(event.values, 0, angularVelocityPhone.values, 0, this.angularVelocityPhone.values.length);
        this.angularVelocityPhone.values[0] -= angularVelocityBias[0];
        this.angularVelocityPhone.values[1] -= angularVelocityBias[1];
        this.angularVelocityPhone.values[2] -= angularVelocityBias[2];
        this.angularVelocityPhone.time = time;
        notifyObserversSensorChanged(this.angularVelocityPhone);
        if(this.rawAccelerationArrived && this.magneticArrived){
            if(!useAndroidDefaultSensors) {
                fusedOrientationFilter.filter(getAngularVelocityPhone(), getGravityPhone(),
                        getMagneticPhone(), time);
                Quaternion rotE = fusedOrientationFilter.getRotationVector();
                processRotationVectorEarth(time, new float[]{(float) rotE.getQ0(), (float) rotE.getQ1(),
                        (float) rotE.getQ2(), (float) rotE.getQ3()});
                linearAccelerationFilter.filter(getRawAccelerationPhone(), getRotationVectorEarth());
                processLinearAccelerationPhone(time, linearAccelerationFilter.getAcceleration());
                processGravityPhone(time, linearAccelerationFilter.getGravity());
            }
            float[] gyrEarth = Utils.rotate(getRotationVectorEarth().values, getAngularVelocityPhone().values);
            processAngularVelocityEarth(time, gyrEarth);
            if(this.headingAngleArrived) {
                headingAngleFilter.predict(gyrEarth[2], time);
                float angle = headingAngleFilter.getAngle();
                processHeadingAngleVehicle(time, new float [] {angle});
                Quaternion rotE = new Quaternion(rotationVectorEarth.values[0], rotationVectorEarth.values[1],
                                                 rotationVectorEarth.values[2], rotationVectorEarth.values[3]);
                Quaternion rotV = (new Quaternion(Math.cos(angle/2f), 0, 0, Math.sin(-angle/2f))).multiply(rotE);
                processRotationVectorVehicle(time, new float[] {(float) rotV.getQ0(), (float) rotV.getQ1(),
                        (float) rotV.getQ2(), (float) rotV.getQ3()});
                float[] accV = Utils.rotate(getRotationVectorVehicle().values, getLinearAccelerationPhone().values);
                processLinearAccelerationVehicle(time, accV);
            }
        }
    }

    private void processRawAccelerationPhone(long time, SensorEvent event){
        System.arraycopy(event.values, 0, rawAccelerationPhone.values, 0, this.rawAccelerationPhone.values.length);
        this.rawAccelerationPhone.values[0] -= rawAccelerationBias[0];
        this.rawAccelerationPhone.values[1] -= rawAccelerationBias[1];
        this.rawAccelerationPhone.values[2] -= rawAccelerationBias[2];
        this.rawAccelerationPhone.time = time;
        notifyObserversSensorChanged(this.rawAccelerationPhone);
        rawAccelerationArrived = true;
        if(!useAndroidDefaultSensors) {
            linearAccelerationFilter.filter(getRawAccelerationPhone());
            processGravityPhone(time, linearAccelerationFilter.getGravity());
        }
    }

    private void processMagneticPhone(long time, SensorEvent event){
//        System.arraycopy(event.values, 0, magneticPhone.values, 0, this.magneticPhone.values.length);
        this.magneticPhone.time = time;
        magnetometer.filter(event.values, time);
        this.magneticPhone.values[0] = magnetometer.getMagnetic()[0];
        this.magneticPhone.values[1] = magnetometer.getMagnetic()[1];
        this.magneticPhone.values[2] = magnetometer.getMagnetic()[2];
        notifyObserversSensorChanged(this.magneticPhone);
        magneticArrived = true;
    }

    private void processLinearAccelerationPhone(long time, float[] event){
        System.arraycopy(event, 0, this.linearAccelerationPhone.values, 0, this.linearAccelerationPhone.values.length);
        this.linearAccelerationPhone.time = time;
        notifyObserversSensorChanged(this.linearAccelerationPhone);
    }

    private void processGravityPhone(long time, float[] event){
        System.arraycopy(event, 0, gravityPhone.values, 0, this.gravityPhone.values.length);
        this.gravityPhone.time = time;
        notifyObserversSensorChanged(this.gravityPhone);
    }

    private void processRotationVectorEarth(long time, float[] event){
        System.arraycopy(event, 0, this.rotationVectorEarth.values, 0, this.rotationVectorEarth.values.length);
        this.rotationVectorEarth.time = time;
        notifyObserversSensorChanged(this.rotationVectorEarth);
    }

    private void processRotationVectorVehicle(long time, float[] event){
        System.arraycopy(event, 0, this.rotationVectorVehicle.values, 0, this.rotationVectorVehicle.values.length);
        this.rotationVectorVehicle.time = time;
        notifyObserversSensorChanged(this.rotationVectorVehicle);
    }

    private void processHeadingAngleVehicle(long time, float[] event){
        this.headingAnglePhone.values[0] = event[0];
        this.headingAnglePhone.time = time;
        notifyObserversSensorChanged(this.headingAnglePhone);
    }

    private void processLocation(Location location){
        this.location = location;
        notifyObserversLocationChanged(this.location);
        if(this.location.hasBearing()) {
            float angle = this.location.getBearing();
            if(angle <= 180){
                angle = -angle;
            }else{
                angle = 360 - angle;
            }
            angle = angle * 0.017453292519943295f;
            if(headingAngleArrived == false){
                this.headingAngleFilter.setCovX(new float[][] {{0.01f, 0f}, {0f, 0.01f}});
                this.headingAngleFilter.setMuX(new float[] {angle, 0});
                this.headingAngleFilter.setSigmaA(0.5f);
                this.headingAngleFilter.setSigmaW(0.001f);
                this.headingAngleFilter.setPrevTime(this.location.getTime());
                headingAngleArrived = true;
            }
            headingAngleFilter.update(angle);
        }
    }

    private class SensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            time = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
            switch(event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    processAngularVelocityPhone(time, event);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    processMagneticPhone(time, event);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    processRawAccelerationPhone(time, event);
                    break;

                case Sensor.TYPE_ROTATION_VECTOR:
                    if(Sensors.useAndroidDefaultSensors) {
                        float vals[] = new float[]{event.values[3], event.values[0], event.values[1], event.values[2]};
                        processRotationVectorEarth(time, vals);
                    }
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    if(Sensors.useAndroidDefaultSensors)
                        processLinearAccelerationPhone(time, event.values);
                    break;
                case Sensor.TYPE_GRAVITY:
                    if(Sensors.useAndroidDefaultSensors)
                        processGravityPhone(time, event.values);
                    break;
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    }

    private class GPSListener implements LocationListener{
        @Override
        public void onLocationChanged(Location _location) {
            processLocation(_location);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onProviderDisabled(String provider) { }
    }
}
