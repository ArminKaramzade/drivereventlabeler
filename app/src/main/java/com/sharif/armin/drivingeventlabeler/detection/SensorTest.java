package com.sharif.armin.drivingeventlabeler.detection;

import android.os.Environment;

import com.sharif.armin.drivingeventlabeler.sensor.SensorSample;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

public class SensorTest {
    String path;
    Thread thread;
    public SensorSample acc, gyr;
    BufferedReader accReader = null, gyrReader = null;
    String accRow, gyrRow;
    public SensorTest(String testDir) {
        try {
            path = new String(Environment.getExternalStorageDirectory().toString() + "/drivingeventlbl/" + testDir + "/");
            accReader = new BufferedReader(new FileReader(path + "aranged_Accelerometer.csv"));
            gyrReader = new BufferedReader(new FileReader(path + "aranged_Gyroscope.csv"));
            if(thread != null){
                thread.interrupt();
            }
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        accReader.readLine();
                        gyrReader.readLine();
                        while (!((accRow = accReader.readLine()) == null | (gyrRow = gyrReader.readLine()) == null)) {
                            String[] accs = accRow.split(",");
                            String[] gyrs = gyrRow.split(",");
                            acc = new SensorSample(3, Sensors.TYPE_LINEAR_ACCELERATION_PHONE);
                            gyr = new SensorSample(3, Sensors.TYPE_ANGULAR_VELOCITY_PHONE);
                            acc.time = (long) Double.parseDouble(accs[0]);
                            gyr.time = (long) Double.parseDouble(gyrs[0]);
                            acc.values[0] = Float.parseFloat(accs[1]);
                            acc.values[1] = Float.parseFloat(accs[2]);
                            acc.values[2] = Float.parseFloat(accs[3]);
                            gyr.values[0] = Float.parseFloat(gyrs[1]);
                            gyr.values[1] = Float.parseFloat(gyrs[2]);
                            gyr.values[2] = Float.parseFloat(gyrs[3]);
                            Thread.sleep(10);
                        }
                        accReader.close();
                        gyrReader.close();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        thread.start();
    }

    public SensorSample getAcc() {
        return acc;
    }

    public SensorSample getGyr() {
        return gyr;
    }
}
