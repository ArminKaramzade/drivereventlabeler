package com.sharif.armin.drivingeventlabeler.detection;

import com.sharif.armin.drivingeventlabeler.sensor.SensorSample;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import mr.go.sgfilter.SGFilter;

public class Detector {
    private ArrayList<DetectorObserver> mObservers;
    public void registerObserver(DetectorObserver detectorObserver){
        if(!mObservers.contains(detectorObserver)) {
            mObservers.add(detectorObserver);
        }
    }
    public void removeObserver(DetectorObserver detectorObserver){
        if(mObservers.contains(detectorObserver)) {
            mObservers.remove(detectorObserver);
        }
    }
    public void notifyObserversEventDetected(Event event){
        for (DetectorObserver observer: mObservers) {
            observer.onEventDetected(event);
        }
    }

    private Thread threadSensor = null;

    private boolean TestFlag = false;

    public static int X = 0, Y = 1, Z = 2;

    public static int windowSize;
    public static int overLap;
    public static int step;

    private static int savgolNl;
    private static int savgolNr;
    private static int savgolDegree;
    private double[] savgolcoeffs;
    private SGFilter sgFilter;

    private int sensorFreq;
    private Sensors sensors;
    private SensorTest sensorsTest;

    private LinkedList<float[]> lacFilteredWindow;
    private LinkedList<float[]> gyrFilteredWindow;
    private LinkedList<Long> time;

    private LinkedList<float[]> lacSavgolWindow;
    private LinkedList<float[]> gyrSavgolWindow;

    private float[] lacEnergy = new float[2];
    private float[] lacMean = new float[2];
    private float[] gyrEnergy = new float[1];
    private float[] gyrMean = new float[1];

    public static void setWindowSize(int windowSize){ Detector.windowSize = windowSize;}
    public static void setOverLap(int overLap){ Detector.overLap = overLap;}
    public static void setSavgolNl(int savgolNl){ Detector.savgolNl = savgolNl;}
    public static void setSavgolNr(int savgolNr){ Detector.savgolNr = savgolNr;}
    public static void setSavgolDegree(int savgolDegree){ Detector.savgolDegree = savgolDegree;}

    public LinkedList<Event> eventList;

    public Detector(int sensorFreq, Sensors sensors){
        step = windowSize - overLap - 1;
        mObservers = new ArrayList<>();
        this.sensorFreq = sensorFreq;

        eventList = new LinkedList<>();

        lacFilteredWindow = new LinkedList<>();
        gyrFilteredWindow = new LinkedList<>();
        time = new LinkedList<>();
        lacSavgolWindow = new LinkedList<>();
        gyrSavgolWindow = new LinkedList<>();
        this.sensors = sensors;

        sgFilter = new SGFilter(savgolNl, savgolNr);
        savgolcoeffs = SGFilter.computeSGCoefficients(savgolNl, savgolNr, savgolDegree);
    }

    public Detector(int sensorFreq, SensorTest sensorTest) {
        step = windowSize - overLap - 1;
        mObservers = new ArrayList<>();
        this.sensorFreq = sensorFreq;

        TestFlag = true;

        eventList = new LinkedList<>();

        lacFilteredWindow = new LinkedList<float[]>();
        gyrFilteredWindow = new LinkedList<float[]>();
        time = new LinkedList<>();
        lacSavgolWindow = new LinkedList<float[]>();
        gyrSavgolWindow = new LinkedList<float[]>();
        this.sensorsTest = sensorTest;

        sgFilter = new SGFilter(savgolNl, savgolNr);
        savgolcoeffs = SGFilter.computeSGCoefficients(savgolNl, savgolNr, savgolDegree);
    }

    public void stop() {
        threadSensor.interrupt();
    }

    public void start() {
        if(threadSensor != null){
            threadSensor.interrupt();
        }
        threadSensor = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try{
                        synchronized (this) {
                            if (TestFlag && sensorsTest.fin) {
                                notifyObserversEventDetected(new Event(0,0,"Finish"));
                                break;
                            }
                            if (TestFlag && !sensorsTest.start)
                                continue;
                            if (SensorAdd())
                                step += 1;
                            if (step == windowSize - overLap) {
                                step = 0;
                                EventDetector(lacFilteredWindow, lacEnergy, lacMean, gyrFilteredWindow, gyrEnergy, gyrMean, time);
                            }
                        }
                        Thread.sleep(1000 / Detector.this.sensorFreq);
                    }catch (InterruptedException e){
                        System.err.println("threadSensor");
                        e.printStackTrace();
                    }
                }
            }
        });
        threadSensor.start();
    }

    private void EventDetector(LinkedList<float[]> lac, float[] lacEnergy, float[] lacMean, LinkedList<float[]> gyr, float[] gyrEnergy, float[] gyrMean, LinkedList<Long> time) {
        Event event1, event2, event3;
        LinkedList <Float> lacX = new LinkedList<>(),
                lacY = new LinkedList<>();

        for (float[] t: lac) {
            lacX.add(t[0]);
            lacY.add(t[1]);
        }

        event1 = BrakeEventDetector.brakeDetect(lacY, lacEnergy[Y], lacMean[Y], time);
        event2 = TurnEventDetector.turnDetect(gyrEnergy[0], gyrMean[0], time);
        event3 = LaneChangeDetector.lanechangeDetect(lacX, gyrEnergy[0], lacEnergy[X], time);
        if (event1 != null){
            eventList.add(event1);
            notifyObserversEventDetected(event1);
        }else if (event2 != null){
            eventList.add(event2);
            notifyObserversEventDetected(event2);
        }else if (event3 != null){
            eventList.add(event3);
            notifyObserversEventDetected(event3);
        }
    }

    private boolean SensorAdd() {
        SensorSample accSS, gyrSS;
        if (TestFlag) {
            synchronized (sensorsTest) {
                accSS =  sensorsTest.getAcc();
                gyrSS =  sensorsTest.getGyr();
            }
        }
        else {
            accSS =  sensors.getLinearAccelerationVehicle();
            gyrSS =  sensors.getAngularVelocityEarth();
        }
        float[] acc = accSS.values,
                gyr = gyrSS.values;
        long accTime = accSS.time;
        if (lacFilteredWindow.size() < windowSize) {
            lacSavgolWindow.add(new float[]{acc[0], acc[1]});
            gyrSavgolWindow.add(new float[]{gyr[2]});
            if (lacSavgolWindow.size() > savgolNr + savgolNl + 1) {
                lacSavgolWindow.removeFirst();
                gyrSavgolWindow.removeFirst();
            }
            if (lacSavgolWindow.size() == savgolNl + savgolNr + 1){
                SensorSmooth(accTime);
            }
            return false;
        }
        else if (lacFilteredWindow.size() == windowSize && gyrFilteredWindow.size() == windowSize){
            lacSavgolWindow.add(new float[]{acc[0], acc[1]});
            gyrSavgolWindow.add(new float[]{gyr[2]});

            lacSavgolWindow.removeFirst();
            gyrSavgolWindow.removeFirst();

            SensorSmooth(accTime);
            return true;
        }
        return false;
    }

    private void SensorSmooth(long time) {
        this.time.add(time - savgolNl * 1000 / sensorFreq);
        if (this.time.size() > windowSize) {
            this.time.removeFirst();
        }
        float[] lacX = new float[1 + savgolNl + savgolNr],
                lacY = new float[1 + savgolNl + savgolNr],
                gyrZ = new float[1 + savgolNl + savgolNr];
        Iterator<float[]> laciterator = lacSavgolWindow.iterator();
        Iterator<float[]> gyriterator = gyrSavgolWindow.iterator();

        for (int i = 0; i < 1 + savgolNl + savgolNr; i++) {
            float[] t = laciterator.next();
            lacX[i] = t[X];
            lacY[i] = t[Y];
            t = gyriterator.next();
            gyrZ[i] = t[0];
        }
        lacFilteredWindow.add(new float[]{
                sgFilter.smooth(lacX, savgolNl, savgolNl + 1, savgolcoeffs)[0],
                sgFilter.smooth(lacY, savgolNl, savgolNl + 1, savgolcoeffs)[0]
        });
        gyrFilteredWindow.add(new float[]{
                sgFilter.smooth(gyrZ, savgolNl, savgolNl + 1, savgolcoeffs)[0]
        });

        lacEnergy[X] += (float) Math.pow(lacFilteredWindow.getLast()[X], 2);
        lacEnergy[Y] += (float) Math.pow(lacFilteredWindow.getLast()[Y], 2);

        lacMean[X] += lacFilteredWindow.getLast()[X] / windowSize;
        lacMean[Y] += lacFilteredWindow.getLast()[Y] / windowSize;

        gyrEnergy[0] += (float) Math.pow(gyrFilteredWindow.getLast()[0], 2);

        gyrMean[0] += gyrFilteredWindow.getLast()[0] / windowSize;

        if (lacFilteredWindow.size() > windowSize){
            lacEnergy[X] -= (float) Math.pow(lacFilteredWindow.getFirst()[X], 2);
            lacEnergy[Y] -= (float) Math.pow(lacFilteredWindow.getFirst()[Y], 2);

            lacMean[X] -= lacFilteredWindow.getFirst()[X] / windowSize;
            lacMean[Y] -= lacFilteredWindow.getFirst()[Y] / windowSize;

            lacFilteredWindow.removeFirst();
        }
        if (gyrFilteredWindow.size() > windowSize){
            gyrEnergy[0] -= (float) Math.pow(gyrFilteredWindow.getFirst()[0], 2);
            gyrMean[0] -= gyrFilteredWindow.getFirst()[0] / windowSize;

            gyrFilteredWindow.removeFirst();
        }
    }
}

