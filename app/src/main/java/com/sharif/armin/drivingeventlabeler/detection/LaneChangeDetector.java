package com.sharif.armin.drivingeventlabeler.detection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class LaneChangeDetector {
    private static int MinDuration = 2 * 1000;
    public static int MaxDuration = 10 * 1000;
    private static float lacEnergyThreshold = 0.01f;
    private static float gyrEnergyThreshold = 0.0008f;
    private static int subSampleParameter = 10;
    private static int winLength = 300;
    private static boolean laneChangeEvent = false, laneChangeEventStopCheck = false;
    private static int laneChangeDist = 1 * 1000;
    private static int laneChangeStopIndex = 0;
    private static long laneChangeStart, laneChangeStop;
    private static LinkedList<Float> laneChangeWindow = new LinkedList<>();
    private static float dtwThreshold = 0.09f;

    public static void setMinDuration(int MinDuration){ LaneChangeDetector.MinDuration = MinDuration;}
    public static void setMaxDuration(int MaxDuration){ LaneChangeDetector.MaxDuration = MaxDuration;}
    public static void setLacXEnergyThreshold(float lacXEnergyThreshold){ LaneChangeDetector.lacEnergyThreshold = lacXEnergyThreshold;}
    public static void setGyrZEnergyThreshold(float gyrZEnergyThreshold){ LaneChangeDetector.gyrEnergyThreshold = gyrZEnergyThreshold;}
    public static void setSubSampleParameter(int subSampleParameter){ LaneChangeDetector.subSampleParameter = subSampleParameter;}
    public static void setDtwThreshold(float dtwThreshold){ LaneChangeDetector.dtwThreshold = dtwThreshold;}




    private static boolean dtwDetection(LinkedList<Float> laneChangeWindow) {
        ArrayList<Float> subSample = new ArrayList<>();
        int i = 10;
        for (Float sample: laneChangeWindow) {
            if (i == subSampleParameter){
                i = 0;
                subSample.add(sample);
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

    public static Event lanechangeDetect(LinkedList<Float> lac, float gyrZEnergy, float lacXEnergy, LinkedList<Long> time) {
        if (gyrZEnergy / Detector.windowSize >= LaneChangeDetector.gyrEnergyThreshold || lacXEnergy / Detector.windowSize >= LaneChangeDetector.lacEnergyThreshold) {
            if (!laneChangeEvent && !laneChangeEventStopCheck) {
                laneChangeWindow = (LinkedList<Float>) lac.clone();
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
                LinkedList<Float> temp = new LinkedList<>();
                ListIterator<Float> iter = laneChangeWindow.listIterator(laneChangeWindow.size() - winLength);
                for (int i = 0; i < winLength; i++) {
                    if(iter.hasNext())
                        temp.add(iter.next());
                }
                if (LaneChangeDetector.dtwDetection(temp)) {
                    long end = time.getLast();
                    return new Event(end - winLength*10, end, "lane_change");
                }
            }
        }
        else if (laneChangeEvent) {
            laneChangeStop = time.get(Detector.overLap);
            laneChangeEventStopCheck = true;
            laneChangeEvent = false;
            laneChangeStopIndex = laneChangeWindow.size();
        }
        if (laneChangeEventStopCheck) {
            long t = time.getFirst();
            if (t - laneChangeStop < laneChangeDist){
                laneChangeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }
            else {
                laneChangeEventStopCheck = false;
                LinkedList<Float> temp = new LinkedList<>();
                ListIterator<Float> iter = laneChangeWindow.listIterator(laneChangeStopIndex - Math.min(winLength, laneChangeStopIndex));
                for (int i = 0; iter.hasNext(); i++) {
                    temp.add(iter.next());
                }
                if (LaneChangeDetector.dtwDetection(temp)) {
//                    long end = time.get(Detector.overLap);
                    return new Event(Math.max(laneChangeStop - winLength*10, laneChangeStart), laneChangeStop, "lane_change");
                }
            }

        }
        return null;
    }
}



//public class LaneChangeDetector {
//    private static int MinDuration;
//    public static int MaxDuration;
//    private static float lacXEnergyThreshold;
//    private static float gyrZEnergyThreshold;
//    private static int subSampleParameter;
//    private static float dtwThreshold;
//    private static boolean laneChangeEvent = false;
//    private static long laneChangeStart, laneChangeStop;
//    private static LinkedList<Float> laneChangeWindow = new LinkedList<>();
//
//    public static void setMinDuration(int MinDuration){ LaneChangeDetector.MinDuration = MinDuration;}
//    public static void setMaxDuration(int MaxDuration){ LaneChangeDetector.MaxDuration = MaxDuration;}
//    public static void setLacXEnergyThreshold(float lacXEnergyThreshold){ LaneChangeDetector.lacXEnergyThreshold = lacXEnergyThreshold;}
//    public static void setGyrZEnergyThreshold(float gyrZEnergyThreshold){ LaneChangeDetector.gyrZEnergyThreshold = gyrZEnergyThreshold;}
//    public static void setSubSampleParameter(int subSampleParameter){ LaneChangeDetector.subSampleParameter = subSampleParameter;}
//    public static void setDtwThreshold(float dtwThreshold){ LaneChangeDetector.dtwThreshold = dtwThreshold;}
//
//
//    private static boolean dtwDetection(LinkedList<Float> laneChangeWindow) {
//        ArrayList<Float> subSample = new ArrayList<>();
//        int i = 10;
//        for (Float sample: laneChangeWindow) {
//            if (i == subSampleParameter){
//                i = 0;
//                subSample.add(sample);
//            }
//            i += 1;
//        }
//        float[] template1 = new float[25],
//                template2 = new float[25];
//        for (int j = 0; j < 25; j++) {
//            template1[j] = (float) Math.sin(((float)j/25.0f) * 2 * Math.PI);
//            template2[j] = -template1[j];
//        }
//
//        float[] win = new float[subSample.size()];
//        Iterator<Float> subitr = subSample.listIterator(0);
//        for (int k = 0; k < subSample.size(); k++) {
//            win[k] = subitr.next();
//        }
//
//        DTW dtw = new DTW(win, template1);
//        DTW dtw1 = new DTW(win, template2);
//        float dis = Math.min(dtw.getDistance(), dtw1.getDistance());
//        if (dis < dtwThreshold){
//            return true;
//        }
//        return false;
//    }
//
//    public static Event lanechangeDetect(LinkedList<Float> lac, float gyrZEnergy, float lacXEnergy, LinkedList<Long> time) {
//        if (gyrZEnergy / Detector.windowSize >= LaneChangeDetector.gyrZEnergyThreshold || lacXEnergy / Detector.windowSize >= LaneChangeDetector.lacXEnergyThreshold) {
//            if (!laneChangeEvent) {
//                laneChangeWindow = (LinkedList<Float>) lac.clone();
//                laneChangeStart = time.getFirst();
//            }
//            else {
//                laneChangeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
//            }
//            laneChangeEvent = true;
//        }
//        else if (laneChangeEvent) {
//            laneChangeStop = time.get(Detector.overLap);
//            laneChangeEvent = false;
//            if (LaneChangeDetector.dtwDetection(laneChangeWindow) &&
//                    laneChangeStop - laneChangeStart < LaneChangeDetector.MaxDuration &&
//                    laneChangeStop - laneChangeStart > LaneChangeDetector.MinDuration ) {
//                return new Event(laneChangeStart, laneChangeStop, "lane_change");
//            }
//        }
//        return null;
//    }
//}
