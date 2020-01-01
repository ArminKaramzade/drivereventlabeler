package com.sharif.armin.drivingeventlabeler.detection;

import java.util.Iterator;
import java.util.LinkedList;

public class BrakeEventDetector {
    private static int MinDuration;
    private static int MaxDuration;
    private static float lacYEnergyThreshold;
    private static float VarThreshold;
    private static float AcceptFunctionThreshold;

    private boolean brakeEvent = false;
    private long brakeStart, brakeStop;
    private LinkedList<Float> brakeWindow = new LinkedList<>();

    public static void setMinDuration(int MinDuration){ BrakeEventDetector.MinDuration = MinDuration;}
    public static void setMaxDuration(int MaxDuration){ BrakeEventDetector.MaxDuration = MaxDuration;}
    public static void setLacYEnergyThreshold(float lacYEnergyThreshold){ BrakeEventDetector.lacYEnergyThreshold = lacYEnergyThreshold;}
    public static void setVarThreshold(float VarThreshold){ BrakeEventDetector.VarThreshold = VarThreshold;}
    public static void setAcceptFunctionThreshold(float AcceptFunctionThreshold){ BrakeEventDetector.AcceptFunctionThreshold = AcceptFunctionThreshold;}


    private boolean AcceptWindowFunction(float Mean){
        return Mean < BrakeEventDetector.AcceptFunctionThreshold;
    }

    private boolean AcceptEventFunction(LinkedList<Float> event){
        double sqSum = 0, sum = 0;
        Iterator<Float> iter = event.iterator();

        while (iter.hasNext()) {
            double t = iter.next();
            sum += t;
            sqSum += t * t;
        }
        int n = event.size();
        double mean = sum / n;
        double var = sqSum / n - mean * mean;
        return var > BrakeEventDetector.VarThreshold;
    }

    public Event brakeDetect(LinkedList<Float> lac, float lacYEnergy, float lacYMean, LinkedList<Long> time) {
        if (lacYEnergy / Detector.windowSize >= BrakeEventDetector.lacYEnergyThreshold && AcceptWindowFunction(lacYMean)) {
            if (!brakeEvent) {
                brakeWindow = (LinkedList<Float>) lac.clone();
                brakeStart = time.getFirst();
            }
            else {
                brakeWindow.addAll(lac.subList(Detector.overLap, Detector.windowSize));
            }
            brakeEvent = true;
        }
        else if (brakeEvent) {
            brakeEvent = false;
            brakeStop = time.get(Detector.overLap);
            if (AcceptEventFunction(brakeWindow) &&
                    brakeStop - brakeStart < BrakeEventDetector.MaxDuration &&
                    brakeStop - brakeStart > BrakeEventDetector.MinDuration ){
                return new Event(brakeStart, brakeStop, "brake");
            }
        }
        return null;
    }

}
