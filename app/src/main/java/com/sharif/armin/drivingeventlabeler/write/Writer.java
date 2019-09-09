package com.sharif.armin.drivingeventlabeler.write;

import android.location.Location;

import com.opencsv.CSVWriter;
import com.sharif.armin.drivingeventlabeler.sensor.SensorSample;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Writer {
    private static final int BUFFER = 2048;

    private enum name{
        RAC, GYR, MGM, ACC, GPS, ROT, ROTV, ROT2, LAC, LBL, BNG;
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
        filenames[name.LAC.ordinal()] = "AccelerometerAndroid.csv";
        filenames[name.GPS.ordinal()] = "GPS.csv";
        filenames[name.ROT.ordinal()] = "RotationVector.csv";
        filenames[name.ROTV.ordinal()] = "RotationVectorVehicle.csv";
        filenames[name.ROT2.ordinal()] = "RotationVectorAndroid.csv";
        filenames[name.BNG.ordinal()] = "Bearing.csv";
        filenames[name.LBL.ordinal()] = "Label.csv";
        headers[name.RAC.ordinal()] = new String[]{"timestamp", "X", "Y", "Z"};
        headers[name.GYR.ordinal()] = new String[]{"timestamp", "X", "Y", "Z"};
        headers[name.MGM.ordinal()] = new String[]{"timestamp", "X", "Y", "Z"};
        headers[name.ACC.ordinal()] = new String[]{"timestamp", "X", "Y", "Z"};
        headers[name.GPS.ordinal()] = new String[]{"timestamp", "LONG", "LAT", "SPEED", "HAS_SPEED",
                "BEARING", "HAS_BEARING", "LOCATION_ACCURACY",
                "HAS_LOCATION_ACCURACY", "SPEED_ACCURACY",
                "HAS_SPEED_ACCURACY", "BEARING_ACCURACY", "HAS_BEARING_ACCURACY"};
        headers[name.ROT.ordinal()] = new String[]{"timestamp", "Q0", "Q1", "Q2", "Q3"};
        headers[name.ROTV.ordinal()] = new String[]{"timestamp", "Q0", "Q1", "Q2", "Q3"};
        headers[name.ROT2.ordinal()] = new String[]{"timestamp", "Q0", "Q1", "Q2", "Q3"};
        headers[name.LAC.ordinal()] = new String[]{"timestamp", "X", "Y", "Z"};
        headers[name.LBL.ordinal()] = new String[]{"TYPE", "START", "END"};
        headers[name.BNG.ordinal()] = new String[]{"timestamp", "theta"};
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
    public void writeRAC(SensorSample rac){
        String[] line = new String [] {String.valueOf(rac.time), String.valueOf(rac.values[0])
                , String.valueOf(rac.values[1]), String.valueOf(rac.values[2])};
        writers[name.RAC.ordinal()].writeNext(line);
    }
    public void writeACC(SensorSample acc){
        String[] line = new String [] {String.valueOf(acc.time), String.valueOf(acc.values[0])
                , String.valueOf(acc.values[1]), String.valueOf(acc.values[2])};
        writers[name.ACC.ordinal()].writeNext(line);
    }
    public void writeGYR(SensorSample gyr){
        String[] line = new String [] {String.valueOf(gyr.time), String.valueOf(gyr.values[0])
                , String.valueOf(gyr.values[1]), String.valueOf(gyr.values[2])};
        writers[name.GYR.ordinal()].writeNext(line);
    }
    public void writeMGM(SensorSample mgm){
        String[] line = new String [] {String.valueOf(mgm.time), String.valueOf(mgm.values[0])
                , String.valueOf(mgm.values[1]), String.valueOf(mgm.values[2])};
        writers[name.MGM.ordinal()].writeNext(line);
    }
    public void writeROT(SensorSample rot){
        String[] line = new String [] {String.valueOf(rot.time), String.valueOf(rot.values[0])
                , String.valueOf(rot.values[1]), String.valueOf(rot.values[2]), String.valueOf(rot.values[3])};
        writers[name.ROT.ordinal()].writeNext(line);
    }
    public void writeROTV(SensorSample rotV){
        String[] line = new String [] {String.valueOf(rotV.time), String.valueOf(rotV.values[0])
                , String.valueOf(rotV.values[1]), String.valueOf(rotV.values[2]), String.valueOf(rotV.values[3])};
        writers[name.ROTV.ordinal()].writeNext(line);
    }
    public void writeROT2(SensorSample rot2){
        String[] line = new String [] {String.valueOf(rot2.time), String.valueOf(rot2.values[0])
                , String.valueOf(rot2.values[1]), String.valueOf(rot2.values[2]), String.valueOf(rot2.values[3])};
        writers[name.ROT2.ordinal()].writeNext(line);
    }
    public void writeLAC(SensorSample lac){
        String[] line = new String [] {String.valueOf(lac.time), String.valueOf(lac.values[0])
                , String.valueOf(lac.values[1]), String.valueOf(lac.values[2])};
        writers[name.LAC.ordinal()].writeNext(line);
    }
    public void writeBNG(SensorSample bng){
        String[] line = new String [] {String.valueOf(bng.time), String.valueOf(bng.values[0])};
        writers[name.BNG.ordinal()].writeNext(line);
    }

    public void writeGPS(Location location){
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
        String[] line = new String[]{String.valueOf(location.getTime()), String.valueOf(location.getLongitude()),
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
