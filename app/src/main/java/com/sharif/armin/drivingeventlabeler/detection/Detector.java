package com.sharif.armin.drivingeventlabeler.detection;

import com.sharif.armin.drivingeventlabeler.activity.GuidedLabeling;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import mr.go.sgfilter.SGFilter;

public class Detector {

    private PropertyChangeListener Listener;

    private Thread threadSensor = null;
    private Thread threadDetection = null;

    private boolean TestFlag = false;

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
    private SensorTest sensorTest;

    private LinkedList<float[]> lacFiltered;
    private LinkedList<float[]> gyrFiltered;
    private LinkedList<Long> time;

    private LinkedList<float[]> lacSavgolFilter;
    private LinkedList<float[]> gyrSavgolFilter;

    private float[] lacEnergy = new float[3];
    private float[] lacMean = new float[3];
    private float[] gyrEnergy = new float[3];
    private float[] gyrMean = new float[3];

    public LinkedList<Event> eventList;
    private boolean threadDetectionFlag;

    public Detector(int sensorFreq, PropertyChangeListener listener, Sensors sensors){
        Listener = listener;
        sensor_f = sensorFreq;

        eventList = new LinkedList<>();

        lacFiltered = new LinkedList<float[]>();
        gyrFiltered = new LinkedList<float[]>();
        time = new LinkedList<>();
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
                    while (true) {
                        if (threadDetectionFlag) {
                            threadDetectionFlag = false;
                            LinkedList lacCopy = (LinkedList) lacFiltered.clone();
                            LinkedList gyrCopy = (LinkedList) gyrFiltered.clone();
                            LinkedList t = (LinkedList) time.clone();
                            float[] lacEnergyCopy = new float[3],
                                    lacMeanCopy = new float[3],
                                    gyrEnergyCopy = new float[3],
                                    gyrMeanCopy = new float[3];
                            System.arraycopy(lacEnergy, 0, lacEnergyCopy, 0, lacEnergy.length );
                            System.arraycopy(lacMean, 0, lacMeanCopy, 0, lacMean.length );
                            System.arraycopy(gyrEnergy, 0, gyrEnergyCopy, 0, gyrEnergy.length );
                            System.arraycopy(gyrMean, 0, gyrMeanCopy, 0, gyrMean.length );

                            EventDetector(lacCopy, lacEnergyCopy, lacMeanCopy, gyrCopy, gyrEnergyCopy, gyrMeanCopy, t);

                            Thread.sleep(1000 / sensor_f);

                        }
                    }
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
        threadDetection.start();
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
                            step = 0;
                            threadDetectionFlag = true;
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

    public Detector(int sensorFreq, GuidedLabeling listener, SensorTest sensorTest) {
        Listener = listener;
        sensor_f = sensorFreq;

        TestFlag = true;

        eventList = new LinkedList<>();

        lacFiltered = new LinkedList<float[]>();
        gyrFiltered = new LinkedList<float[]>();
        time = new LinkedList<>();
        lacSavgolFilter = new LinkedList<float[]>();
        gyrSavgolFilter = new LinkedList<float[]>();
        this.sensorTest = sensorTest;

        sgFilter = new SGFilter(savgolNl, savgolNr);
        savgolcoeffs = SGFilter.computeSGCoefficients(savgolNl, savgolNr, savgolDegree);

        if(threadDetection != null){
            threadDetection.interrupt();
        }
        threadDetection = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    while (true) {
                        if (threadDetectionFlag) {
                            threadDetectionFlag = true;
                            LinkedList lacCopy = (LinkedList) lacFiltered.clone();
                            LinkedList gyrCopy = (LinkedList) gyrFiltered.clone();
                            LinkedList t = (LinkedList) time.clone();
                            float[] lacEnergyCopy = new float[3],
                                    lacMeanCopy = new float[3],
                                    gyrEnergyCopy = new float[3],
                                    gyrMeanCopy = new float[3];
                            System.arraycopy(lacEnergy, 0, lacEnergyCopy, 0, lacEnergy.length );
                            System.arraycopy(lacMean, 0, lacMeanCopy, 0, lacMean.length );
                            System.arraycopy(gyrEnergy, 0, gyrEnergyCopy, 0, gyrEnergy.length );
                            System.arraycopy(gyrMean, 0, gyrMeanCopy, 0, gyrMean.length );

                            EventDetector(lacCopy, lacEnergyCopy, lacMeanCopy, gyrCopy, gyrEnergyCopy, gyrMeanCopy, t);

                            Thread.sleep(1000 / sensor_f);

                        }
                    }
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
        threadDetection.start();
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
                            step = 0;
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

    private void EventDetector(LinkedList<float[]> lac, float[] lacEnergy, float[] lacMean, LinkedList<float[]> gyr, float[] gyrEnergy, float[] gyrMean, LinkedList<Long> time) {
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

        Event event1, event2, event3;
        Event last = null;
        // Brake
        event1 = BrakeEventDetector.brakeDetect(lac, lacEnergy, lacMean, time);

        // Turn
        event2 = TurnEventDetector.turnDetect(gyrEnergy, gyrMean, time);

        // lanechangeDetect
        event3 = LaneChangeDetector.lanechangeDetect(lac, gyrEnergy, lacEnergy, time);
        if (event1 != null){
//            Event last = eventList.get(eventList.size() - 1);
            eventList.add(event1);
            NotifyListener(this, event1.getEventLable(), last, event1);
        }else if (event2 != null){
//            Event last = eventList.get(eventList.size() - 1);
            eventList.add(event2);
            NotifyListener(this, event2.getEventLable(), last, event2);
        }else if (event3 != null){
//            Event last = eventList.get(eventList.size() - 1);
            eventList.add(event3);
            NotifyListener(this, event3.getEventLable(), last, event3);
        }
    }

    private boolean SensorAdd() {
        if (!TestFlag) {
            if (lacFiltered.size() < windowSize) {
                lacSavgolFilter.add(new float[]{sensors.getAcc().values[0], sensors.getAcc().values[1], sensors.getAcc().values[2]});
                gyrSavgolFilter.add(new float[]{sensors.getGyr().values[0], sensors.getGyr().values[1], sensors.getGyr().values[2]});
                if (lacSavgolFilter.size() > savgolNr + savgolNl + 1) {
                    lacSavgolFilter.removeFirst();
                    gyrSavgolFilter.removeFirst();
                }
                if (lacSavgolFilter.size() == savgolNl + savgolNr + 1){

                    time.add(sensors.getAcc().time - 150);
                    SensorSmooth();
                    if (time.size() > windowSize) {
                        time.removeFirst();
                    }
                }
                return false;
            }
            else {
                lacSavgolFilter.add(new float[]{sensors.getAcc().values[0], sensors.getAcc().values[1], sensors.getAcc().values[2]});
                gyrSavgolFilter.add(new float[]{sensors.getGyr().values[0], sensors.getGyr().values[1], sensors.getGyr().values[2]});

                lacSavgolFilter.removeFirst();
                gyrSavgolFilter.removeFirst();

                time.add(sensors.getAcc().time - 150);
                SensorSmooth();
                if (time.size() > windowSize) {
                    time.removeFirst();
                }

                return true;
            }
        }
        else {
            if (lacFiltered.size() < windowSize) {
                lacSavgolFilter.add(new float[]{sensorTest.getAcc().values[0], sensorTest.getAcc().values[1], sensorTest.getAcc().values[2]});
                gyrSavgolFilter.add(new float[]{sensorTest.getGyr().values[0], sensorTest.getGyr().values[1], sensorTest.getGyr().values[2]});
                if (lacSavgolFilter.size() > savgolNr + savgolNl + 1) {
                    lacSavgolFilter.removeFirst();
                    gyrSavgolFilter.removeFirst();
                }
                if (lacSavgolFilter.size() == savgolNl + savgolNr + 1){

                    time.add(sensorTest.getAcc().time - 150);
                    SensorSmooth();
                    if (time.size() > windowSize) {
                        time.removeFirst();
                    }
                }
                return false;
            }
            else {
                lacSavgolFilter.add(new float[]{sensorTest.getAcc().values[0], sensorTest.getAcc().values[1], sensorTest.getAcc().values[2]});
                gyrSavgolFilter.add(new float[]{sensorTest.getGyr().values[0], sensorTest.getGyr().values[1], sensorTest.getGyr().values[2]});

                lacSavgolFilter.removeFirst();
                gyrSavgolFilter.removeFirst();

                time.add(sensorTest.getAcc().time - 150);
                SensorSmooth();
                if (time.size() > windowSize) {
                    time.removeFirst();
                }

                return true;
            }
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

    public static boolean AcceptEventFunction(LinkedList<float[]> event, float mean){
        float var = 0;
        Iterator<float[]> iter = event.iterator();

        while (iter.hasNext()) {
            var += Math.pow(mean - iter.next()[1], 2);
        }
        var /= event.size();
        return var > VarThreshold;
    }

    public static Event brakeDetect(LinkedList<float[]> lac, float[] lacEnergy, float[] lacMean, LinkedList<Long> time) {
        if (lacEnergy[Detector.Y] / Detector.windowSize >= BrakeEventDetector.lacYEnergyThreshold && BrakeEventDetector.AcceptWindowFunction(lacMean[Detector.Y])) {
            if (!brakeEvent) {
                brakeWindow = (LinkedList<float[]>) lac.clone();
                brakeStart = time.getFirst();
            }
            else {
                brakeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }
            brakeEvent = true;
        }
        else if (brakeEvent) {
            brakeEvent = false;
            brakeStop = time.getLast();
            if (BrakeEventDetector.AcceptEventFunction(brakeWindow, lacMean[1]) && brakeStop - brakeStart < BrakeEventDetector.MaxDuration * 1000 && brakeStop - brakeStart > BrakeEventDetector.MinDuration * 1000){
                return new Event(brakeStart, brakeStop, new String("brake"));
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

    public static Event turnDetect(float[] gyrEnergy, float[] gyrMean, LinkedList<Long> time) {
        if (gyrEnergy[Detector.Z] / Detector.windowSize >= TurnEventDetector.gyrZEnergyThreshold && TurnEventDetector.AcceptWindowFunction(gyrMean[Detector.Z])) {
            if (!turnEvent) {
                turnStart = time.getFirst();
            }
            turnEvent = true;
        }
        else if (turnEvent) {
            turnStop = time.getLast();
            turnEvent = false;
            if (turnStop - turnStart < TurnEventDetector.MaxDuration * 1000 && turnStop - turnStart > TurnEventDetector.MinDuration * 1000){
                return new Event(turnStart, turnStop, new String("turn"));
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
    private static int winLength = 300;
    private static boolean laneChangeEvent = false, laneChangeEventStopCheck = false;
    private static int laneChangeDist = 1;
    private static int laneChangeStopIndex = 0;
    private static long laneChangeStart, laneChangeStop;
    private static LinkedList<float[]> laneChangeWindow = new LinkedList<>();
    private static float dtwThreshold = 0.09f;


    private static boolean dtwDetection(LinkedList<float[]> laneChangeWindow) {
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
            template1[j] = (float) Math.sin(((float)j/25.0f) * 2 * Math.PI);
            template2[j] = -template1[j];
        }

        float[] win = new float[subSample.size()];
        Iterator<Float> subitr = subSample.listIterator(0);
        for (int k = 0; k < subSample.size(); k++) {
            win[k] = subitr.next();
        }

        DTW dtw = new DTW(win, template1);
        DTW dtw1 = new DTW(win, template2);
        float dis = Math.min(dtw.getDistance(), dtw1.getDistance());
        if (dis < dtwThreshold){
            return true;
        }

        return false;
    }

    public static Event lanechangeDetect(LinkedList<float[]> lac, float[] gyrEnergy, float[] lacEnergy, LinkedList<Long> time) {
        if (gyrEnergy[Detector.Z] / Detector.windowSize >= LaneChangeDetector.gyrEnergyThreshold || lacEnergy[Detector.X] / Detector.windowSize >= LaneChangeDetector.lacEnergyThreshold) {
            if (!laneChangeEvent && !laneChangeEventStopCheck) {
                laneChangeWindow = (LinkedList<float[]>) lac.clone();
                laneChangeStart = time.getFirst();
            }
            else if (laneChangeEventStopCheck){
                laneChangeEventStopCheck = false;
                laneChangeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }
            else {
                laneChangeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }

            laneChangeEvent = true;
            if (laneChangeWindow.size() >= winLength) {
                LinkedList<float[]> temp = new LinkedList<>();
                ListIterator<float[]> iter = laneChangeWindow.listIterator(laneChangeWindow.size() - winLength);
                for (int i = 0; i < winLength; i++) {
                    temp.add(iter.next());
                }
                if (LaneChangeDetector.dtwDetection(temp)) {
                    long end = time.getLast();
                    return new Event(end - winLength*10, end, new String("lane_change"));
                }
            }
        }
        else if (laneChangeEvent) {
            laneChangeStop = time.getLast();
            laneChangeEventStopCheck = true;
            laneChangeEvent = false;
            laneChangeStopIndex = laneChangeWindow.size();
        }
        if (laneChangeEventStopCheck) {
            long t = time.getFirst();
            if (t - laneChangeStop < laneChangeDist * 1000){
                laneChangeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }
            else {
                laneChangeEventStopCheck = false;
                LinkedList<float[]> temp = new LinkedList<>();
                ListIterator<float[]> iter = laneChangeWindow.listIterator(laneChangeWindow.size() - Math.min(winLength, laneChangeWindow.size()));
                for (int i = 0; i < Math.min(winLength, laneChangeWindow.size()); i++) {
                    temp.add(iter.next());
                }
                if (LaneChangeDetector.dtwDetection(temp)) {
                    long end = time.getLast();
                    return new Event(Math.max(end - winLength*10, laneChangeStart), end, new String("lane_change"));
                }
            }

        }
        return null;
    }
}
