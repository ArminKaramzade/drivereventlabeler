package com.sharif.armin.drivingeventlabeler.detection;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class LaneChangeDetector {
    private static int MinDuration;
    private static int MaxDuration;
    private static float lacEnergyThreshold;
    private static float gyrEnergyThreshold;
    private static int subSampleParameter;
    private static int winLength;
    private static int laneChangeDist;
    private static float dtwThreshold;
    private static int templateSize;

    private long start, stop;
    private int dtwStartPtr;
    private boolean currentIsCandidate, lastWasCandidate;
    private boolean hasExpired;
    private LinkedList<Float> lacWindow = new LinkedList<>();
    private LinkedList<Long> timeWindow = new LinkedList<>();
    private float[] template1, template2;


    public static void setMinDuration(int MinDuration){ LaneChangeDetector.MinDuration = MinDuration;}
    public static void setMaxDuration(int MaxDuration){ LaneChangeDetector.MaxDuration = MaxDuration;}
    public static void setLacXEnergyThreshold(float lacXEnergyThreshold){ LaneChangeDetector.lacEnergyThreshold = lacXEnergyThreshold;}
    public static void setGyrZEnergyThreshold(float gyrZEnergyThreshold){ LaneChangeDetector.gyrEnergyThreshold = gyrZEnergyThreshold;}
    public static void setSubSampleParameter(int subSampleParameter){ LaneChangeDetector.subSampleParameter = subSampleParameter;}
    public static void setDtwThreshold(float dtwThreshold){ LaneChangeDetector.dtwThreshold = dtwThreshold;}
    public static void setWinLength(int winLength){ LaneChangeDetector.winLength = winLength;}
    public static void setLaneChangeDist(int laneChangeDist){ LaneChangeDetector.laneChangeDist = laneChangeDist;}
    public static void setTemplateSize(int templateSize){ LaneChangeDetector.templateSize = templateSize;}

    public LaneChangeDetector(){
        template1 = new float[templateSize];
        template2 = new float[templateSize];
        for (int j = 0; j < templateSize; j++) {
            template1[j] = (float) Math.sin(((float)j/(float)templateSize) * 2f * Math.PI);
            template2[j] = -template1[j];
        }
        currentIsCandidate = false;
        lastWasCandidate = false;
        hasExpired = true;
        dtwStartPtr = 0;
        start = -1;
        stop = -1;
    }

    private boolean dtwDetection(LinkedList<Float> laneChangeWindow) {
        ArrayList<Float> subSample = new ArrayList<>();
        int i = subSampleParameter;
        for (Float sample: laneChangeWindow) {
            if (i == subSampleParameter){
                i = 0;
                subSample.add(sample);
            }
            i += 1;
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

    public Event lanechangeDetect(LinkedList<Float> lac, float gyrZEnergy, float lacXEnergy, LinkedList<Long> time) {
        currentIsCandidate = (gyrZEnergy / Detector.windowSize >= LaneChangeDetector.gyrEnergyThreshold || lacXEnergy / Detector.windowSize >= LaneChangeDetector.lacEnergyThreshold);
        if(!currentIsCandidate && !lastWasCandidate && hasExpired) {
            // goto next window
        }
        else if(lastWasCandidate && hasExpired){
            throw new AssertionError("Should not enter here!");
        }
        else if(currentIsCandidate && !lastWasCandidate && hasExpired){
            timeWindow = (LinkedList<Long>) time.clone();
            lacWindow = (LinkedList<Float>) lac.clone();
            start = -1;
            stop = -1;
            dtwStartPtr = 0;
            lastWasCandidate = true;
            hasExpired = false;
        }
        else if((currentIsCandidate && !hasExpired) || (!currentIsCandidate && lastWasCandidate && !hasExpired)){
            timeWindow.addAll(time.subList(Detector.overLap, Detector.windowSize));
            lacWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
        }
        else if(!currentIsCandidate && !lastWasCandidate && !hasExpired){
            if(time.getLast() - timeWindow.get(dtwStartPtr) > LaneChangeDetector.laneChangeDist){
                hasExpired = true;
                LinkedList<Float> temp = new LinkedList<>();
                ListIterator<Float> iter = lacWindow.listIterator(dtwStartPtr);
                int j = 0;
                while(j < winLength && iter.hasNext()){
                    temp.add(iter.next());
                    j += 1;
                }
                if(dtwDetection(temp)){
                    if(start == -1){
                        start = timeWindow.get(dtwStartPtr);
                    }
                    stop = timeWindow.get(Math.min(dtwStartPtr+winLength, timeWindow.size()-1));
                    lacWindow.clear();
                    timeWindow.clear();
                    if(LaneChangeDetector.MinDuration <= stop - start && stop - start <= LaneChangeDetector.MaxDuration)
                        return new Event(start, stop, "lane_change");
                }
            }
        }
        else{
            throw new AssertionError("It's not possible!");
        }

        if(lacWindow.size() >= winLength && dtwStartPtr < lacWindow.size()){
            LinkedList<Float> temp = new LinkedList<>();
            ListIterator<Float> iter = lacWindow.listIterator(dtwStartPtr);
            int j = 0;
            while(j < winLength && iter.hasNext()){
                temp.add(iter.next());
                j += 1;
            }
            if(dtwDetection(temp)){
                if(start == -1){
                    start = timeWindow.get(dtwStartPtr);
                }
                stop = timeWindow.get(Math.min(dtwStartPtr+winLength, timeWindow.size()-1));
            }
            else {
                if(hasExpired){
                    if (start != -1) {
                        lacWindow.clear();
                        timeWindow.clear();
                        if(LaneChangeDetector.MinDuration <= stop - start && stop - start <= LaneChangeDetector.MaxDuration)
                            return new Event(start, stop, "lane_change");
                    }
                }
            }
            dtwStartPtr += (Detector.windowSize - Detector.overLap);
        }
        lastWasCandidate = currentIsCandidate;
        return null;
    }
}
