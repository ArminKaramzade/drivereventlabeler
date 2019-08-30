package com.sharif.armin.drivingeventlabeler.detection;

import android.content.Context;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;

import com.sharif.armin.drivingeventlabeler.gps.GPS;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;
import com.sharif.armin.drivingeventlabeler.write.Writer;
import com.sharif.armin.drivingeventlabeler.activity.MainActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import mr.go.sgfilter.SGFilter;

public class Detector extends AppCompatActivity {
    private Thread threadSensor = null;
    private Thread threadDetection = null;

    private int X = 0, Y = 1, Z = 2;

    private int windowSize = 40;
    private int overLap = 30;
    private int stepSize = windowSize - overLap;
    private int step = 9;

    private int savgolNl = 15;
    private int savgolNr = 15;
    private int savgolDegree = 1;
    private double[] savgolcoeffs;
    private SGFilter sgFilter;
    private int savgolWindow = windowSize + savgolNl + savgolNr;

    private int sensor_f, gps_delay;
    private Sensors sensors;
    private GPS gps;
    private Writer writer;

    private String filename;

    private LinkedList<float[]> lacFiltered;
    private LinkedList<float[]> gyrFiltered;

    private LinkedList<float[]> lacSavgolFilter;
    private LinkedList<float[]> gyrSavgolFilter;

    private LinkedList<float[]> brakeWindow;
    private LinkedList<float[]> laneChangeWindow;

    private float[] lacEnergy = new float[3];
    private float[] lacMean = new float[3];
    private float[] gyrEnergy = new float[3];
    private float[] gyrMean = new float[3];

    private boolean brakeEvent = false,
                    turnEvent = false,
                    laneChangeEvent = false, laneChangeEventStopCheck = false;

    private int laneChangeDist = 1;
    private int laneChangeStopIndex = 0;

    private long brakeStart, brakeStop,
            turnStart, turnStop,
            laneChangeStart, laneChangeStop;



    public Detector(int sensorFreq, int gpsDelay){
        sensor_f = sensorFreq;
        gps_delay = gpsDelay;

        lacFiltered = new LinkedList<float[]>();
        gyrFiltered = new LinkedList<float[]>();
        lacSavgolFilter = new LinkedList<float[]>();
        gyrSavgolFilter = new LinkedList<float[]>();

        writer = new Writer(MainActivity.directory.getPath());

        sensors = new Sensors((SensorManager) getSystemService(Context.SENSOR_SERVICE), sensor_f);
        gps = new GPS((LocationManager) getSystemService((Context.LOCATION_SERVICE)), writer, gps_delay);

        sensors.start();
        gps.start();
        filename = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".zip";

        sgFilter = new SGFilter(savgolNl, savgolNr);
        savgolcoeffs = SGFilter.computeSGCoefficients(savgolNl, savgolNr, savgolDegree);

        if(threadDetection != null){
            threadDetection.interrupt();
        }
        threadDetection = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    LinkedList lacCopy = (LinkedList) lacFiltered.clone();
                    LinkedList gyrCopy = (LinkedList) gyrFiltered.clone();
                    float[] lacEnergyCopy = new float[3],
                            lacMeanCopy = new float[3],
                            gyrEnergyCopy = new float[3],
                            gyrMeanCopy = new float[3];
                    System.arraycopy(lacEnergy, 0, lacEnergyCopy, 0, lacEnergy.length );
                    System.arraycopy(lacMean, 0, lacMeanCopy, 0, lacMean.length );
                    System.arraycopy(gyrEnergy, 0, gyrEnergyCopy, 0, gyrEnergy.length );
                    System.arraycopy(gyrMean, 0, gyrMeanCopy, 0, gyrMean.length );

                    EventDetector(lacCopy, lacEnergyCopy, lacMeanCopy, gyrCopy, gyrEnergyCopy, gyrMeanCopy);

                    Thread.sleep(1000 / sensor_f);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });

        if(threadSensor != null){
            threadSensor.interrupt();
        }
        threadSensor = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        if (SensorAdd())
                            step += 1;
                        if (step == stepSize) {
                           threadDetection.start();
                        }
                        Thread.sleep(1000 / sensor_f);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        threadSensor.start();

    }

    private void EventDetector(LinkedList<float[]> lac, float[] lacEnergy, float[] lacMean, LinkedList<float[]> gyr, float[] gyrEnergy, float[] gyrMean) {
        float lacXVar = 0, lacYVar = 0, gyrZVar = 0;

        Iterator<float[]> laciterator = lacSavgolFilter.iterator();
        Iterator<float[]> gyriterator = gyrSavgolFilter.iterator();
        for (int i = 0; i < windowSize; i++) {
            float[] t = laciterator.next();
            lacXVar += Math.pow(lacMean[X] - t[X], 2);
            lacYVar += Math.pow(lacMean[Y] - t[Y], 2);
            t = gyriterator.next();
            gyrZVar += Math.pow(gyrMean[Z] - gyr.get(i)[Z], 2);
        }
        lacXVar /= windowSize;
        lacYVar /= windowSize;
        gyrZVar /= windowSize;

        // Brake
        brakeDetect(lac, lacEnergy, lacMean);

        // Turn
        turnDetect(gyrEnergy, gyrMean);

        // lanechange
        lanechange(lac, gyrEnergy, lacEnergy);

    }

    private void lanechange(LinkedList<float[]> lac, float[] gyrEnergy, float[] lacEnergy) {
        if (gyrEnergy[Z] / windowSize >= LaneChangeDetector.gyrEnergyThreshold || lacEnergy[X] / windowSize >= LaneChangeDetector.lacEnergyThreshold) {
            if (!laneChangeEvent && !laneChangeEventStopCheck) {
                laneChangeWindow = (LinkedList<float[]>) lac.clone();
                laneChangeStart = System.currentTimeMillis();
            }
            else if (laneChangeEventStopCheck){
                laneChangeEventStopCheck = false;
                laneChangeWindow.addAll(lac.subList(overLap, windowSize));
            }
            else {
                laneChangeWindow.addAll(lac.subList(overLap, windowSize));
            }
            laneChangeEvent = true;
        }
        else if (laneChangeEvent) {
            laneChangeStop = System.currentTimeMillis();
            laneChangeEventStopCheck = true;
            laneChangeEvent = false;
            laneChangeStopIndex = laneChangeWindow.size();
        }
        if (laneChangeEventStopCheck) {
            long t = System.currentTimeMillis();
            if (t - laneChangeStop < laneChangeDist * 1000){
                laneChangeWindow.addAll(lac.subList(overLap, windowSize));
            }
            else {
                laneChangeEventStopCheck = false;
                laneChangeWindow = (LinkedList<float[]>) laneChangeWindow.subList(0, laneChangeStopIndex);
                if (laneChangeStop - laneChangeStart > LaneChangeDetector.MinDuration) {
                    int[] starts = LaneChangeDetector.dtwDetection(laneChangeWindow);
                    //TODO event observer
                }
            }

        }
    }

    private void turnDetect(float[] gyrEnergy, float[] gyrMean) {
        if (gyrEnergy[Z] / windowSize >= TurnEventDetector.gyrZEnergyThreshold && TurnEventDetector.AcceptWindowFunction(gyrMean[Z])) {
            if (!turnEvent) {
                turnStart = System.currentTimeMillis();
            }
            turnEvent = true;
        }
        else if (turnEvent) {
            turnStop = System.currentTimeMillis();
            turnEvent = false;
            if (turnStop - turnStart < TurnEventDetector.MaxDuration * 1000 && turnStop - turnStart > TurnEventDetector.MinDuration * 1000){
                // TODO event observer
            }
        }
    }

    private void brakeDetect(LinkedList<float[]> lac, float[] lacEnergy, float[] lacMean) {
        if (lacEnergy[Y] / windowSize >= BrakeEventDetector.lacYEnergyThreshold && BrakeEventDetector.AcceptWindowFunction(lacMean[Y])) {
            if (!brakeEvent) {
                brakeWindow = (LinkedList<float[]>) lac.clone();
                brakeStart = System.currentTimeMillis();
            }
            else {
                brakeWindow.addAll(lac.subList(overLap, windowSize));
            }
            brakeEvent = true;
        }
        else if (brakeEvent) {
            brakeEvent = false;
            brakeStop = System.currentTimeMillis();
            if (BrakeEventDetector.AcceptEventFunction(brakeWindow) && brakeStop - brakeStart < BrakeEventDetector.MaxDuration * 1000 && brakeStop - brakeStart > BrakeEventDetector.MinDuration * 1000){
                //TODO event observer
            }
        }
    }

    private boolean SensorAdd() {

        if (lacFiltered.size() < windowSize) {
            lacSavgolFilter.add(new float[]{sensors.lac[X], sensors.lac[Y], sensors.lac[Z]});
            gyrSavgolFilter.add(new float[]{sensors.gyr[X], sensors.gyr[Y], sensors.gyr[Z]});
            if (lacSavgolFilter.size() > savgolNr + savgolNl + 1) {
                lacSavgolFilter.removeFirst();
                gyrSavgolFilter.removeFirst();
            }
            if (lacSavgolFilter.size() == savgolNl + savgolNr + 1){

                SensorSmooth();
            }
            return false;
        }
        else {
            lacSavgolFilter.add(new float[]{sensors.lac[X], sensors.lac[Y], sensors.lac[Z]});
            gyrSavgolFilter.add(new float[]{sensors.gyr[X], sensors.gyr[Y], sensors.gyr[Z]});

            lacSavgolFilter.removeFirst();
            gyrSavgolFilter.removeFirst();

            SensorSmooth();

            return true;
        }
    }

    private void SensorSmooth() {
        float[] lacX = new float[savgolWindow],
                lacY = new float[savgolWindow],
                lacZ = new float[savgolWindow],
                gyrX = new float[savgolWindow],
                gyrY = new float[savgolWindow],
                gyrZ = new float[savgolWindow];
        Iterator<float[]> laciterator = lacSavgolFilter.iterator();
        Iterator<float[]> gyriterator = gyrSavgolFilter.iterator();

        for (int i = X; i < savgolWindow; i++) {
            float[] t = laciterator.next();
            lacX[i] = t[X];
            lacY[i] = t[Y];
            lacZ[i] = t[Z];
            t = gyriterator.next();
            gyrX[i] = t[X];
            gyrY[i] = t[Y];
            gyrZ[i] = t[Z];
        }
        lacFiltered.add(new float[]{
                sgFilter.smooth(lacX, savgolNl, savgolNl + 1, savgolcoeffs)[savgolNl],
                sgFilter.smooth(lacY, savgolNl, savgolNl + 1, savgolcoeffs)[savgolNl],
                sgFilter.smooth(lacZ, savgolNl, savgolNl + 1, savgolcoeffs)[savgolNl]
        });
        gyrFiltered.add(new float[]{
                sgFilter.smooth(gyrX, savgolNl, savgolNl + 1, savgolcoeffs)[savgolNl],
                sgFilter.smooth(gyrY, savgolNl, savgolNl + 1, savgolcoeffs)[savgolNl],
                sgFilter.smooth(gyrZ, savgolNl, savgolNl + 1, savgolcoeffs)[savgolNl]
        });

        lacEnergy[X] += (float) Math.pow(lacFiltered.getLast()[X], 2);
        lacEnergy[Y] += (float) Math.pow(lacFiltered.getLast()[Y], 2);
        lacEnergy[Z] += (float) Math.pow(lacFiltered.getLast()[Z], 2);

        lacMean[X] += lacFiltered.getLast()[X] / windowSize;
        lacMean[Y] += lacFiltered.getLast()[Y] / windowSize;
        lacMean[Z] += lacFiltered.getLast()[Z] / windowSize;

        gyrEnergy[X] += (float) Math.pow(gyrFiltered.getLast()[X], 2);
        gyrEnergy[Y] += (float) Math.pow(gyrFiltered.getLast()[Y], 2);
        gyrEnergy[Z] += (float) Math.pow(gyrFiltered.getLast()[Z], 2);

        gyrMean[X] += gyrFiltered.getLast()[X] / windowSize;
        gyrMean[Y] += gyrFiltered.getLast()[Y] / windowSize;
        gyrMean[Z] += gyrFiltered.getLast()[Z] / windowSize;
        
        if (lacFiltered.size() > windowSize){
            lacEnergy[X] -= (float) Math.pow(lacFiltered.getFirst()[X], 2);
            lacEnergy[Y] -= (float) Math.pow(lacFiltered.getFirst()[Y], 2);
            lacEnergy[Z] -= (float) Math.pow(lacFiltered.getFirst()[Z], 2);

            lacMean[X] -= lacFiltered.getFirst()[X] / windowSize;
            lacMean[Y] -= lacFiltered.getFirst()[Y] / windowSize;
            lacMean[Z] -= lacFiltered.getFirst()[Z] / windowSize;

            gyrEnergy[X] -= (float) Math.pow(gyrFiltered.getFirst()[X], 2);
            gyrEnergy[Y] -= (float) Math.pow(gyrFiltered.getFirst()[Y], 2);
            gyrEnergy[Z] -= (float) Math.pow(gyrFiltered.getFirst()[Z], 2);

            gyrMean[X] -= gyrFiltered.getFirst()[X] / windowSize;
            gyrMean[Y] -= gyrFiltered.getFirst()[Y] / windowSize;
            gyrMean[Z] -= gyrFiltered.getFirst()[Z] / windowSize;
            
            lacFiltered.removeFirst();
            gyrFiltered.removeFirst();
        }
    }
}

class BrakeEventDetector {
    public static int MinDuration = 1;
    public static int MaxDuration = 10;
    public static float lacYEnergyThreshold = 0.1f;
    public static float VarThreshold = 0.02f;
    private static float AcceptFunctionThreshold = -0.1f;

    public static boolean AcceptWindowFunction(float Mean){
        return Mean < AcceptFunctionThreshold;
    }
    
    public static boolean AcceptEventFunction(LinkedList<float[]> event){
        float mean = 0, var = 0;
        for (float[] sample : event) {
            mean += sample[1];
        }
        mean /= event.size();
        for (float[] sample : event) {
            mean += Math.pow(mean - sample[1], 2);
        }
        var /= event.size();
        return var > VarThreshold;
    }
}

class TurnEventDetector {
    public static int MinDuration = 2;
    public static int MaxDuration = 10;
    public static float gyrZEnergyThreshold = 0.01f;
    private static float AcceptFunctionThreshold = 0.1f;

    public static boolean AcceptWindowFunction(float Mean){
        return Math.abs(Mean) >= AcceptFunctionThreshold;
    }
}

class LaneChangeDetector {
    public static int MinDuration = 2;
    public static int MaxDuration = 10;
    public static float lacEnergyThreshold = 0.01f;
    public static float gyrEnergyThreshold = 0.0008f;
    public static int subSampleParameter = 10;
    private static int winLength = 30;

    public static int[] dtwDetection(LinkedList<float[]> laneChangeWindow) {
        ArrayList<Integer> starts = new ArrayList<>();
        ArrayList<Float> subSample = new ArrayList<>();
        int i = 10;
        for (float[] sample: laneChangeWindow) {
            if (i == subSampleParameter){
                i = 0;
                subSample.add(sample[0]);
            }
            i += 1;
        }
        float[] template1 = new float[25],
                template2 = new float[25];
        for (int j = 0; j < 25; j++) {
            template1[i] = (float) Math.sin(((float)j/25) * 2 * Math.PI);
            template2[i] = -template1[i];
        }
        for (int j = 0; j < subSample.size() - winLength; j+= 5) {
            float[] win = new float[winLength];
            Iterator<Float> subitr = subSample.listIterator(j);
            for (int k = 0; k < winLength; k++) {
                win[j + k] = subitr.next();
            }

            DTW dtw = new DTW(win, template1);
            if (dtw.getDistance() < 0.09){
                starts.add(j * subSampleParameter);
                j += winLength;
            }
        }
        int[] s = new int[starts.size()];
        for (int j = 0; j < starts.size(); j++) {
            s[j] = starts.get(j);
        }
        return s;
    }
}
