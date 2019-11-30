package com.sharif.armin.drivingeventlabeler.detection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class LaneChangeDetector {
    private static int MinDuration;
    public static int MaxDuration;
    private static float lacXEnergyThreshold;
    private static float gyrZEnergyThreshold;
    private static int subSampleParameter;
    private static float dtwThreshold;
    private static boolean laneChangeEvent = false;
    private static long laneChangeStart, laneChangeStop;
    private static LinkedList<Float> laneChangeWindow = new LinkedList<>();

    public static void setMinDuration(int MinDuration){ LaneChangeDetector.MinDuration = MinDuration;}
    public static void setMaxDuration(int MaxDuration){ LaneChangeDetector.MaxDuration = MaxDuration;}
    public static void setLacXEnergyThreshold(float lacXEnergyThreshold){ LaneChangeDetector.lacXEnergyThreshold = lacXEnergyThreshold;}
    public static void setGyrZEnergyThreshold(float gyrZEnergyThreshold){ LaneChangeDetector.gyrZEnergyThreshold = gyrZEnergyThreshold;}
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
        if (gyrZEnergy / Detector.windowSize >= LaneChangeDetector.gyrZEnergyThreshold || lacXEnergy / Detector.windowSize >= LaneChangeDetector.lacXEnergyThreshold) {
            if (!laneChangeEvent) {
                laneChangeWindow = (LinkedList<Float>) lac.clone();
                laneChangeStart = time.getFirst();
            }
            else {
                laneChangeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }
            laneChangeEvent = true;
        }
        else if (laneChangeEvent) {
            laneChangeStop = time.get(Detector.overLap);
            laneChangeEvent = false;
            if (LaneChangeDetector.dtwDetection(laneChangeWindow) &&
                    laneChangeStop - laneChangeStart < LaneChangeDetector.MaxDuration &&
                    laneChangeStop - laneChangeStart > LaneChangeDetector.MinDuration ) {
                return new Event(laneChangeStart, laneChangeStop, "lane_change");
            }
        }
        return null;
    }
}
