package com.sharif.armin.drivingeventlabeler.sensor;

public class SensorSample {
    public long time;
    public float[] values = new float[0];
    public String type;
    public SensorSample(int valueSize, String type){
        this.values = new float[valueSize];
        this.type = type;
    }
}
