package com.sharif.armin.drivingeventlabeler.sensor.linearAcceleration;

import android.hardware.SensorManager;
import com.sharif.armin.drivingeventlabeler.sensor.SensorSample;
import com.sharif.armin.drivingeventlabeler.util.Utils;
import org.apache.commons.math3.complex.Quaternion;

public class LinearAcceleration {
    private static final float MS2S = 1.0f / 1000.0f;
    private static float timeConstant;

    public static void setTimeConstant(float timeConstant) {
        LinearAcceleration.timeConstant = timeConstant;
    }

    private float[] acceleration;
    private float[] gravity;
    private long prevTime;

    public float[] getAcceleration(){ return this.acceleration;}
    public float[] getGravity(){ return this.gravity;}

    public LinearAcceleration(){
        acceleration = new float[3];
        acceleration[0] = 0;
        acceleration[1] = 0;
        acceleration[2] = 0;
        gravity = new float[3];
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        prevTime = 0;
    }

    public void reset(){
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        acceleration[0] = 0;
        acceleration[1] = 0;
        acceleration[2] = 0;
        prevTime = 0;
    }

    public void filter(SensorSample rac, SensorSample rot){
        Quaternion rotationVector = new Quaternion(rot.values[0], rot.values[1], rot.values[2], rot.values[3]);
        float[] rotatedRac = Utils.rotate(rotationVector, rac.values);
        rotatedRac[2] -= SensorManager.GRAVITY_EARTH;
        this.acceleration = Utils.rotate(rotationVector.getConjugate(), rotatedRac);
    }

    public void filter(SensorSample rac){
        final float dT = (rac.time - this.prevTime) * MS2S;
        this.prevTime = rac.time;
        float alpha = timeConstant / (timeConstant + dT);
        gravity[0] = gravity[0] * alpha + (1 - alpha) * rac.values[0];
        gravity[1] = gravity[1] * alpha + (1 - alpha) * rac.values[1];
        gravity[2] = gravity[2] * alpha + (1 - alpha) * rac.values[2];
    }
}
