package com.sharif.armin.drivingeventlabeler.detection;

import java.util.LinkedList;

public class TurnEventDetector {
    private static int MinDuration;
    private static int MaxDuration;
    private static float gyrZEnergyThreshold;
    private static float AcceptFunctionThreshold;
    private static long turnStart, turnStop;
    private static boolean turnEvent = false;

    public static void setMinDuration(int MinDuration){ TurnEventDetector.MinDuration = MinDuration;}
    public static void setMaxDuration(int MaxDuration){ TurnEventDetector.MaxDuration = MaxDuration;}
    public static void setGyrZEnergyThreshold(float gyrZEnergyThreshold){ TurnEventDetector.gyrZEnergyThreshold = gyrZEnergyThreshold;}
    public static void setAcceptFunctionThreshold(float AcceptFunctionThreshold){ TurnEventDetector.AcceptFunctionThreshold = AcceptFunctionThreshold;}

    private static boolean AcceptWindowFunction(float Mean){
        return Math.abs(Mean) >= AcceptFunctionThreshold;
    }

    public static Event turnDetect(float gyrZEnergy, float gyrZMean, LinkedList<Long> time) {
        if (gyrZEnergy / Detector.windowSize >= TurnEventDetector.gyrZEnergyThreshold && TurnEventDetector.AcceptWindowFunction(gyrZMean)) {
            if (!turnEvent) {
                turnStart = time.getFirst();
            }
            turnEvent = true;
        }
        else if (turnEvent) {
            turnStop = time.get(Detector.overLap);
            turnEvent = false;
            if (turnStop - turnStart < TurnEventDetector.MaxDuration && turnStop - turnStart > TurnEventDetector.MinDuration){
                return new Event(turnStart, turnStop, "turn");
            }
        }
        return null;
    }
}
