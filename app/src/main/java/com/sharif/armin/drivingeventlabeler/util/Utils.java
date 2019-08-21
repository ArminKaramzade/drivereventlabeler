package com.sharif.armin.drivingeventlabeler.util;

public class Utils {
    public static int freq2delay(int f) {
        return (int) (1000 * 1000 / f);
    }

    public static float[] toFloatArray(double[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        float[] ret = new float[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (float)arr[i];
        }
        return ret;
    }
}
