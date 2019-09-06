package com.sharif.armin.drivingeventlabeler.detection;

import android.content.Context;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;

import com.sharif.armin.drivingeventlabeler.sensor.Sensors;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import mr.go.sgfilter.SGFilter;

public class Detector {

    private PropertyChangeListener Listener;

    private Thread threadSensor = null;
    private Thread threadDetection = null;

    public static int X = 0, Y = 1, Z = 2;

    public static int windowSize = 40;
    public static int overLap = 30;
    public static int stepSize = windowSize - overLap;
    public static int step = 9;

    private int savgolNl = 15;
    private int savgolNr = 15;
    private int savgolDegree = 1;
    private double[] savgolcoeffs;
    private SGFilter sgFilter;
    private int savgolWindow = 1 + savgolNl + savgolNr;

    private int sensor_f;
    private Sensors sensors;

    private LinkedList<float[]> lacFiltered;
    private LinkedList<float[]> gyrFiltered;

    private LinkedList<float[]> lacSavgolFilter;
    private LinkedList<float[]> gyrSavgolFilter;

    private float[] lacEnergy = new float[3];
    private float[] lacMean = new float[3];
    private float[] gyrEnergy = new float[3];
    private float[] gyrMean = new float[3];

    public LinkedList<Event> eventList;

    public Detector(int sensorFreq, PropertyChangeListener listener, Sensors sensors){
        Listener = listener;
        sensor_f = sensorFreq;

        eventList = new LinkedList<>();

        lacFiltered = new LinkedList<float[]>();
        gyrFiltered = new LinkedList<float[]>();
        lacSavgolFilter = new LinkedList<float[]>();
        gyrSavgolFilter = new LinkedList<float[]>();
        this.sensors = sensors;

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

        Iterator<float[]> laciterator = lac.iterator();
        Iterator<float[]> gyriterator = gyr.iterator();
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

        Event event;

        // Brake
        event = BrakeEventDetector.brakeDetect(lac, lacEnergy, lacMean);

        // Turn
        event = TurnEventDetector.turnDetect(gyrEnergy, gyrMean);

        // lanechangeDetect
        event = LaneChangeDetector.lanechangeDetect(lac, gyrEnergy, lacEnergy);
        if (event != null){
            Event last = eventList.get(eventList.size() - 1);
            eventList.add(event);
            NotifyListener(this, event.getEventLable(), last, event);
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

        for (int i = 0; i < savgolWindow; i++) {
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
                sgFilter.smooth(lacX, savgolNl, savgolNl + 1, savgolcoeffs)[0],
                sgFilter.smooth(lacY, savgolNl, savgolNl + 1, savgolcoeffs)[0],
                sgFilter.smooth(lacZ, savgolNl, savgolNl + 1, savgolcoeffs)[0]
        });
        gyrFiltered.add(new float[]{
                sgFilter.smooth(gyrX, savgolNl, savgolNl + 1, savgolcoeffs)[0],
                sgFilter.smooth(gyrY, savgolNl, savgolNl + 1, savgolcoeffs)[0],
                sgFilter.smooth(gyrZ, savgolNl, savgolNl + 1, savgolcoeffs)[0]
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

    private void NotifyListener(Object object, String event, Event oldValue, Event newValue) {
        Listener.propertyChange(new PropertyChangeEvent(object, event, oldValue, newValue));
    }
}

class BrakeEventDetector {
    private static int MinDuration = 1;
    private static int MaxDuration = 10;
    private static float lacYEnergyThreshold = 0.1f;
    private static float VarThreshold = 0.02f;
    private static float AcceptFunctionThreshold = -0.1f;
    private static boolean brakeEvent = false;
    private static long brakeStart, brakeStop;
    private static LinkedList<float[]> brakeWindow = new LinkedList<>();

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

    public static Event brakeDetect(LinkedList<float[]> lac, float[] lacEnergy, float[] lacMean) {
        if (lacEnergy[Detector.Y] / Detector.windowSize >= BrakeEventDetector.lacYEnergyThreshold && BrakeEventDetector.AcceptWindowFunction(lacMean[Detector.Y])) {
            if (!brakeEvent) {
                brakeWindow = (LinkedList<float[]>) lac.clone();
                brakeStart = System.currentTimeMillis();
            }
            else {
                brakeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }
            brakeEvent = true;
        }
        else if (brakeEvent) {
            brakeEvent = false;
            brakeStop = System.currentTimeMillis();
            if (BrakeEventDetector.AcceptEventFunction(brakeWindow) && brakeStop - brakeStart < BrakeEventDetector.MaxDuration * 1000 && brakeStop - brakeStart > BrakeEventDetector.MinDuration * 1000){
                return new Event(brakeStart, brakeStop, new String("Brake"));
            }
        }
        return null;
    }

}

class TurnEventDetector {
    private static int MinDuration = 2;
    private static int MaxDuration = 10;
    private static float gyrZEnergyThreshold = 0.01f;
    private static float AcceptFunctionThreshold = 0.1f;
    private static long turnStart, turnStop;
    private static boolean turnEvent = false;


    private static boolean AcceptWindowFunction(float Mean){
        return Math.abs(Mean) >= AcceptFunctionThreshold;
    }

    public static Event turnDetect(float[] gyrEnergy, float[] gyrMean) {
        if (gyrEnergy[Detector.Z] / Detector.windowSize >= TurnEventDetector.gyrZEnergyThreshold && TurnEventDetector.AcceptWindowFunction(gyrMean[Detector.Z])) {
            if (!turnEvent) {
                turnStart = System.currentTimeMillis();
            }
            turnEvent = true;
        }
        else if (turnEvent) {
            turnStop = System.currentTimeMillis();
            turnEvent = false;
            if (turnStop - turnStart < TurnEventDetector.MaxDuration * 1000 && turnStop - turnStart > TurnEventDetector.MinDuration * 1000){
                return new Event(turnStart, turnStop, new String("Turn"));
            }
        }
        return null;
    }
}

class LaneChangeDetector {
    private static int MinDuration = 2;
    public static int MaxDuration = 10;
    private static float lacEnergyThreshold = 0.01f;
    private static float gyrEnergyThreshold = 0.0008f;
    private static int subSampleParameter = 10;
    private static int winLength = 30;
    private static boolean laneChangeEvent = false, laneChangeEventStopCheck = false;
    private static int laneChangeDist = 1;
    private static int laneChangeStopIndex = 0;
    private static long laneChangeStart, laneChangeStop;
    private static LinkedList<float[]> laneChangeWindow = new LinkedList<>();


    private static long dtwDetection(LinkedList<float[]> laneChangeWindow) {
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
                return (long) (j*subSampleParameter + laneChangeStart);
            }
        }
//        int[] s = new int[starts.size()];
//        for (int j = 0; j < starts.size(); j++) {
//            s[j] = starts.get(j);
//        }
//        return s;
        return 0;
    }

    public static Event lanechangeDetect(LinkedList<float[]> lac, float[] gyrEnergy, float[] lacEnergy) {
        if (gyrEnergy[Detector.Z] / Detector.windowSize >= LaneChangeDetector.gyrEnergyThreshold || lacEnergy[Detector.X] / Detector.windowSize >= LaneChangeDetector.lacEnergyThreshold) {
            if (!laneChangeEvent && !laneChangeEventStopCheck) {
                laneChangeWindow = (LinkedList<float[]>) lac.clone();
                laneChangeStart = System.currentTimeMillis();
            }
            else if (laneChangeEventStopCheck){
                laneChangeEventStopCheck = false;
                laneChangeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }
            else {
                laneChangeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
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
                laneChangeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }
            else {
                laneChangeEventStopCheck = false;
                laneChangeWindow = (LinkedList<float[]>) laneChangeWindow.subList(0, laneChangeStopIndex);
                if (laneChangeStop - laneChangeStart > LaneChangeDetector.MinDuration) {
                    long starts = LaneChangeDetector.dtwDetection(laneChangeWindow);
                    return new Event(starts, starts + 3000, new String("LaneChange"));
                }
            }

        }
        return null;
    }
}
