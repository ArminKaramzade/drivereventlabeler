package com.sharif.armin.drivingeventlabeler.sensor.orientation;

import com.sharif.armin.drivingeventlabeler.sensor.SensorSample;
import com.sharif.armin.drivingeventlabeler.util.Utils;

import org.apache.commons.math3.complex.Quaternion;

public class Orientation {
    private static final float MS2S = 1.0f / 1000.0f;
    private long prevTime = 0;
    private Quaternion rotationVector = null;
    private float[] gyr = new float[3],
            acc = new float[3],
            mgm = new float[3];
    private Madgwick madgwick = new Madgwick();

    public Quaternion getRotationVector(){
        return this.rotationVector;
    }
    public void reset(){
        prevTime = 0;
        rotationVector = null;
    }

    public void filter(SensorSample gyrSensorSample, SensorSample accSensorSample, SensorSample mgmSensorSample, long time) {
        System.arraycopy(gyrSensorSample.values, 0, gyr, 0, gyr.length);
        System.arraycopy(accSensorSample.values, 0, acc, 0, acc.length);
        System.arraycopy(mgmSensorSample.values, 0, mgm, 0, mgm.length);
        // -----------------
        // set freq properly
        float freq = 1.f / ((time - prevTime) * MS2S);
        prevTime = time;
        // -----------------
        if (rotationVector == null){
            Quaternion q = Utils.getAccMgmOrientationVector(acc, mgm);
            rotationVector = q;
            madgwick.setQ(q);
        }
        madgwick.MadgwickAHRSupdate(gyr, acc, mgm, freq);
        // -----------------
        // LPF ?
        float alpha = 0f;
        rotationVector = madgwick.getQuaternion().multiply(1-alpha);
        // -----------------
    }
}
