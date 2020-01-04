package com.sharif.armin.drivingeventlabeler.sensor.magnetometer;

public class Magnetometer {
    private static final float MS2S = 1.0f / 1000.0f;
    private static float timeConstant;

    public static void setTimeConstant(float timeConstant){
        Magnetometer.timeConstant = timeConstant;
    }

    private long prevTime;
    private float[] magnetic;

    public float[] getMagnetic(){ return this.magnetic;}

    public Magnetometer(){
        magnetic = new float[3];
        magnetic[0] = 0;
        magnetic[1] = 0;
        magnetic[2] = 0;
        prevTime = 0;
    }

    public void reset() {
        magnetic[0] = 0;
        magnetic[1] = 0;
        magnetic[2] = 0;
        prevTime = 0;
    }

    public void filter(float[] mgm, long time) {
        final float dT = (time - this.prevTime) * MS2S;
        this.prevTime = time;
        float alpha = timeConstant / (timeConstant + dT);
        magnetic[0] = magnetic[0] * alpha + (1 - alpha) * mgm[0];
        magnetic[1] = magnetic[1] * alpha + (1 - alpha) * mgm[1];
        magnetic[2] = magnetic[2] * alpha + (1 - alpha) * mgm[2];
    }
}
