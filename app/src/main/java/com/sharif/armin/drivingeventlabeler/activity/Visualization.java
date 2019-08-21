package com.sharif.armin.drivingeventlabeler.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sharif.armin.drivingeventlabeler.R;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;
import com.sharif.armin.drivingeventlabeler.util.Utils;

public class Visualization extends AppCompatActivity{
    private Sensors sensors;
    private LineChart mChart;
    private Thread thread = null;
    private int sensor_f;
    private boolean plotData = true;

    private SensorListener sensorListener;
    private SensorManager sensorManager;

    private String to_plot = "acc";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization);
        Intent intent = getIntent();

        sensor_f = Integer.parseInt(intent.getStringExtra(MainActivity.sensor_frequency));
        sensors = new Sensors((SensorManager) getSystemService(Context.SENSOR_SERVICE), sensor_f);
        sensors.start();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorListener = new SensorListener();
        sensorManager.registerListener(this.sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                Utils.freq2delay(this.sensor_f));

        mChart = findViewById(R.id.chart1);
        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setText("Sensor Data");
        mChart.setTouchEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(true);

        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        mChart.setData(data);

        Legend l = mChart.getLegend();
        l.setTextColor(Color.BLACK);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.GRAY);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        startPlot();
    }

    private void startPlot() {
        if(thread != null){
            thread.interrupt();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        plotData = true;
                        Thread.sleep(1000 / sensor_f);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private void addEntry(float[] sensor_data) {
        LineData data = mChart.getData();
        if(data != null){
            ILineDataSet set = data.getDataSetByIndex(0);
            ILineDataSet set1 = data.getDataSetByIndex(1);
            ILineDataSet set2 = data.getDataSetByIndex(2);
            if(set == null){
                set = createSet();
                set1 = createSet1();
                set2 = createSet2();
                data.addDataSet(set);
                data.addDataSet(set1);
                data.addDataSet(set2);
            }
            data.addEntry(new Entry(set.getEntryCount(),sensor_data[0]),0);
            data.addEntry(new Entry(set1.getEntryCount(),sensor_data[1]),1);
            data.addEntry(new Entry(set2.getEntryCount(),sensor_data[2]),2);
            data.notifyDataChanged();
            mChart.notifyDataSetChanged();
//            mChart.setMaxVisibleValueCount(200);
            mChart.moveViewToX(data.getEntryCount());
            mChart.setVisibleXRangeMaximum(200);
        }
    }

    private LineDataSet createSet(){
        LineDataSet set = new LineDataSet(null,"X Axis");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setColor(Color.MAGENTA);
        set.setHighLightColor(Color.MAGENTA);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return  set;
    }

    private LineDataSet createSet1(){
        LineDataSet set = new LineDataSet(null,"Y Axis");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setColor(Color.BLUE);
        set.setHighLightColor(Color.BLUE);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return  set;
    }

    private LineDataSet createSet2(){
        LineDataSet set = new LineDataSet(null,"Z Axis");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setColor(Color.RED);
        set.setHighLightColor(Color.RED);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return  set;
    }
    @Override
    public void onPause() {
        super.onPause();
        if (thread != null){
            thread.interrupt();
        }
        sensors.stop();
    }
    @Override
    public void onResume() {
        super.onResume();
        sensors.start();
    }
    @Override
    public void onDestroy() {
        sensors.stop();
        thread.interrupt();
        super.onDestroy();
    }

    public void linearAcc(View view){
        to_plot = "lac";
        mChart.clearValues();
    }
    public void ownLinearAcc(View view){
        to_plot = "acc";
        mChart.clearValues();
    }
    public void rawAcc(View view){
        to_plot = "rac";
        mChart.clearValues();
    }
    public void gyro(View view){
        to_plot = "gyr";
        mChart.clearValues();
    }
    public void magno(View view){
        to_plot = "mgm";
        mChart.clearValues();
    }

    private class SensorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (plotData) {
                if (to_plot == "rac") {
                    addEntry(sensors.rac);
                } else if (to_plot == "acc") {
                    addEntry(sensors.acc);
                } else if (to_plot == "lac") {
                    addEntry(sensors.lac);
                } else if (to_plot == "gyr") {
                    addEntry(sensors.gyr);
                } else if (to_plot == "mgm") {
                    addEntry(sensors.mgm);
                }
            }
            plotData = false;
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    }
}
