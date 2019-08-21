package com.sharif.armin.drivingeventlabeler.write;

import android.hardware.SensorEvent;
import android.location.Location;

import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Writer {
    private static final int BUFFER = 2048;

    private enum name{
        RAC, GYR, MGM, ACC, GPS, ROT, LBL;
    }
    private static String[]  filenames = new String[name.values().length];
    private CSVWriter[] writers = new CSVWriter[name.values().length];
    private String headers[][] = new String[name.values().length][];
    private String path;

    public Writer(String path){
        this.path = path;
        filenames[name.RAC.ordinal()] = "RawAccelerometer.csv";
        filenames[name.GYR.ordinal()] = "Gyroscope.csv";
        filenames[name.MGM.ordinal()] = "Magnetometer.csv";
        filenames[name.ACC.ordinal()] = "Accelerometer.csv";
        filenames[name.GPS.ordinal()] = "GPS.csv";
        filenames[name.ROT.ordinal()] = "RotationVector.csv";
        filenames[name.LBL.ordinal()] = "Label.csv";
        headers[name.RAC.ordinal()] = new String[]{"timestamp", "RAC_X", "RAC_Y", "RAC_Z"};
        headers[name.GYR.ordinal()] = new String[]{"timestamp", "GYR_X", "GYR_Y", "GYR_Z"};
        headers[name.MGM.ordinal()] = new String[]{"timestamp", "MGM_X", "MGM_Y", "MGM_Z"};
        headers[name.ACC.ordinal()] = new String[]{"timestamp", "ACC_X", "ACC_Y", "ACC_Z"};
        headers[name.GPS.ordinal()] = new String[]{"timestamp", "LONG", "LAT", "SPEED", "HAS_SPEED",
                                                   "BEARING", "HAS_BEARING", "LOCATION_ACCURACY",
                                                   "HAS_LOCATION_ACCURACY", "SPEED_ACCURACY",
                                                   "HAS_SPEED_ACCURACY", "BEARING_ACCURACY", "HAS_BEARING_ACCURACY"};
        headers[name.ROT.ordinal()] = new String[]{"timestamp", "ROT_X", "ROT_Y", "ROT_Z", "COS"};
        headers[name.LBL.ordinal()] = new String[]{"TYPE", "START", "END"};
        try{
            for (name n: name.values()){
                writers[n.ordinal()] = get_writer(path, filenames[n.ordinal()]);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        for (name n: name.values()){
            writers[n.ordinal()].writeNext(headers[n.ordinal()]);
        }
    }

    public static CSVWriter get_writer(String path, String fn) throws IOException {
        String full_path = path + File.separator + fn;
        File f = new File(full_path);
        return new CSVWriter(new FileWriter(f, true));
    }

    public void writeLBL(String type, long start, long finish){
        String[] line = new String[] {type, String.valueOf(start), String.valueOf(finish)};
        writers[name.LBL.ordinal()].writeNext(line);
    }
    public void writeRAC(long time, float[] rac){
        String[] line = new String [] {String.valueOf(time), String.valueOf(rac[0])
                , String.valueOf(rac[1]), String.valueOf(rac[2])};
        writers[name.RAC.ordinal()].writeNext(line);
    }
    public void writeACC(long time, float[] acc){
        String[] line = new String [] {String.valueOf(time), String.valueOf(acc[0])
                , String.valueOf(acc[1]), String.valueOf(acc[2])};
        writers[name.ACC.ordinal()].writeNext(line);
    }
    public void writeGYR(long time, float[] gyr){
        String[] line = new String [] {String.valueOf(time), String.valueOf(gyr[0])
                , String.valueOf(gyr[1]), String.valueOf(gyr[2])};
        writers[name.GYR.ordinal()].writeNext(line);
    }
    public void writeMGM(long time, float[] mgm){
        String[] line = new String [] {String.valueOf(time), String.valueOf(mgm[0])
                , String.valueOf(mgm[1]), String.valueOf(mgm[2])};
        writers[name.MGM.ordinal()].writeNext(line);
    }
    public void writeROT(long time, float[] rot){
        String[] line = new String [] {String.valueOf(time), String.valueOf(rot[0])
                , String.valueOf(rot[1]), String.valueOf(rot[2]), String.valueOf(rot[3])};
        writers[name.ROT.ordinal()].writeNext(line);
    }
    public void writeGPS(long time, Location location){
        if (location == null) {
            return;
        }
        String hasSpeedAccuracy, speedAccuracy, hasBearingAccuracy, bearingAccuracy;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            hasSpeedAccuracy = String.valueOf(location.hasSpeedAccuracy());
            speedAccuracy = String.valueOf(location.getSpeedAccuracyMetersPerSecond());
            hasBearingAccuracy = String.valueOf(location.hasBearingAccuracy());
            bearingAccuracy = String.valueOf(location.getBearingAccuracyDegrees());
        } else {
            hasSpeedAccuracy = "NOT SUPPORTED";
            speedAccuracy = "NOT SUPPORTED";
            hasBearingAccuracy = "NOT SUPPORTED";
            bearingAccuracy = "NOT SUPPORTED";
        }
        String[] line = new String[]{String.valueOf(time), String.valueOf(location.getLongitude()),
                String.valueOf(location.getLatitude()), String.valueOf(location.getSpeed()),
                String.valueOf(location.hasSpeed()), String.valueOf(location.getBearing()), String.valueOf(location.hasBearing()),
                String.valueOf(location.getAccuracy()), String.valueOf(location.hasAccuracy()), speedAccuracy, hasSpeedAccuracy,
                bearingAccuracy, hasBearingAccuracy};
        writers[name.GPS.ordinal()].writeNext(line);
    }

    public void saveAndRemove(String fn){
        try {
            for (name n : name.values()) {
                writers[n.ordinal()].close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        String [] files = new String[name.values().length];
        for (name n: name.values()){
            files[n.ordinal()] = path + File.separator + filenames[n.ordinal()];
        }
        zip(files, path + File.separator + fn);
        for (int i = 0; i < files.length; i++) {
            File file = new File(files[i]);
            file.delete();
        }
    }

    public void zip(String[] _files, String zipFileName) {
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipFileName);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            byte data[] = new byte[BUFFER];

            for (int i = 0; i < _files.length; i++) {
                FileInputStream fi = new FileInputStream(_files[i]);
                origin = new BufferedInputStream(fi, BUFFER);

                ZipEntry entry = new ZipEntry(_files[i].substring(_files[i].lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;

                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
