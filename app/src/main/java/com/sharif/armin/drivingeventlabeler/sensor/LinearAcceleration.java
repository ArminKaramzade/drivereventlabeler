package com.sharif.armin.drivingeventlabeler.sensor;

import android.hardware.SensorManager;

public class LinearAcceleration {

    private float[] acc = new float[3];
    private static float[] gravity = new float[]{SensorManager.GRAVITY_EARTH,
            SensorManager.GRAVITY_EARTH, SensorManager.GRAVITY_EARTH};


    public float[] getAcc(float[] rac, float[] orientation){
        float[] grv = new float[3];
        grv[0] = (float) -(gravity[0] * -Math.cos(orientation[0]) * Math.sin(orientation[1]));
        grv[1] = (float) (gravity[1] * -Math.sin(orientation[0]));
        grv[2] = (float) (gravity[2] * Math.cos(orientation[0]) * Math.cos(orientation[1]));
        acc[0] = rac[0] - grv[0];
        acc[1] = rac[1] - grv[1];
        acc[2] = rac[2] - grv[2];
        return acc;
    }

}
