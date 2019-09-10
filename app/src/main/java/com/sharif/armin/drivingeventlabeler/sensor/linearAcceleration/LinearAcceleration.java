package com.sharif.armin.drivingeventlabeler.sensor.linearAcceleration;

import android.hardware.SensorManager;
import com.sharif.armin.drivingeventlabeler.sensor.SensorSample;
import com.sharif.armin.drivingeventlabeler.util.Utils;
import org.apache.commons.math3.complex.Quaternion;

public class LinearAcceleration {
    private static final float MS2S = 1.0f / 1000.0f;
    private float[] acc = {0, 0, 0};
    private float[] grv = {0, 0, 0};
    private long prevTime = 0;
    private static float timeConstant = 1f;

    public float[] getAcc(){ return this.acc;}
    public float[] getGrv(){ return this.grv;}

    public static void setTimeConstant(float timeConstant) {
        LinearAcceleration.timeConstant = timeConstant;
    }
    public void reset(){
        grv[0] = 0;
        grv[1] = 0;
        grv[2] = 0;
    }

    public void filter(SensorSample rac, SensorSample rot){
        Quaternion rotationVector = new Quaternion(rot.values[0], rot.values[1], rot.values[2], rot.values[3]);
        float[] rotatedRac = Utils.rotate(rotationVector, rac.values);
        rotatedRac[2] -= SensorManager.GRAVITY_EARTH;
        this.acc = Utils.rotate(rotationVector.getConjugate(), rotatedRac);
    }


    public void filter(SensorSample rac){
        final float dT = (rac.time - this.prevTime) * MS2S;
        this.prevTime = rac.time;
        float alpha = timeConstant / (timeConstant + dT);
        grv[0] = grv[0] * alpha + (1 - alpha) * rac.values[0];
        grv[1] = grv[1] * alpha + (1 - alpha) * rac.values[1];
        grv[2] = grv[2] * alpha + (1 - alpha) * rac.values[2];
    }

}
