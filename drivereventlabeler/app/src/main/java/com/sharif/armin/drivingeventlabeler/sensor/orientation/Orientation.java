package com.sharif.armin.drivingeventlabeler.sensor.orientation;

import com.sharif.armin.drivingeventlabeler.sensor.SensorSample;
import com.sharif.armin.drivingeventlabeler.util.Utils;
import org.apache.commons.math3.complex.Quaternion;

public class Orientation {
    private static final float MS2S = 1.0f / 1000.0f;
    private static float timeConstant;

    public static void setTimeConstant(float timeConstant){
        Orientation.timeConstant = timeConstant;
    }

    private long prevTime;
    private Quaternion rotationVector;
    private float[] angularVelocity,
                    gravity,
                    magnetic;
    private Madgwick madgwick;

    public Quaternion getRotationVector(){
        return this.rotationVector;
    }

    public Orientation(){
        prevTime = 0;
        rotationVector = null;
        angularVelocity = new float[3];
        gravity = new float[3];
        magnetic = new float[3];
        madgwick = new Madgwick();
    }

    public void reset(){
        prevTime = 0;
        rotationVector = null;
    }

    public void filter(SensorSample angularVelocitySS, SensorSample gravitySS, SensorSample magneticSS, long time) {
        System.arraycopy(angularVelocitySS.values, 0, angularVelocity, 0, angularVelocity.length);
        System.arraycopy(gravitySS.values, 0, gravity, 0, gravity.length);
        System.arraycopy(magneticSS.values, 0, magnetic, 0, magnetic.length);
        if (rotationVector == null){
            Quaternion q = Utils.getAccMgmOrientationVector(gravity, magnetic);
            rotationVector = q;
            madgwick.setQ(q);
        }
        if (prevTime != 0) {
            final float dT = (time - prevTime) * MS2S;
            madgwick.MadgwickAHRSupdate(angularVelocity, gravity, magnetic, dT);
            final float alpha = timeConstant / (timeConstant + dT);
            rotationVector = rotationVector.multiply(alpha).add(madgwick.getQuaternion().multiply(1f-alpha));
        }
        prevTime = time;
    }
}
