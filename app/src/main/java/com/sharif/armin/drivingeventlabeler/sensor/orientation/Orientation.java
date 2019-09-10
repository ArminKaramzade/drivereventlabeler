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
    private static float timeConstant = 0.1f;
    private Madgwick madgwick = new Madgwick();

    public static void setTimeConstant(float timeConstant){
        Orientation.timeConstant = timeConstant;
    }
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
        if (rotationVector == null){
            Quaternion q = Utils.getAccMgmOrientationVector(acc, mgm);
            rotationVector = q;
            madgwick.setQ(q);
        }
        if (prevTime != 0) {
            final float dT = (time - prevTime) * MS2S;
            madgwick.MadgwickAHRSupdate(gyr, acc, mgm, dT);
            final float alpha = timeConstant / (timeConstant + dT);
            rotationVector = rotationVector.multiply(alpha).add(madgwick.getQuaternion().multiply(1f-alpha));
        }
        prevTime = time;
    }
}
