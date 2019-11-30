package com.sharif.armin.drivingeventlabeler.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.opencsv.CSVWriter;
import com.sharif.armin.drivingeventlabeler.R;
import com.sharif.armin.drivingeventlabeler.detection.BrakeEventDetector;
import com.sharif.armin.drivingeventlabeler.detection.Detector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.opencsv.CSVReader;
import com.sharif.armin.drivingeventlabeler.detection.LaneChangeDetector;
import com.sharif.armin.drivingeventlabeler.detection.TurnEventDetector;
import com.sharif.armin.drivingeventlabeler.sensor.Sensors;
import com.sharif.armin.drivingeventlabeler.sensor.linearAcceleration.LinearAcceleration;
import com.sharif.armin.drivingeventlabeler.sensor.orientation.Madgwick;
import com.sharif.armin.drivingeventlabeler.sensor.orientation.Orientation;

public class Setting extends AppCompatActivity {
    private static String dir = "setting";
    private static String fn = "setting.csv";
    private CSVWriter writer;
    private CSVReader reader;
    File f;
    Switch customSensors;
    private Map<String, Float> map= Setting.getInitialMap();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        customSensors = (Switch) findViewById(R.id.customSwitch);

        customSensors.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked){
                            findViewById(R.id.lacTimeConstant_te).setEnabled(true);
                            findViewById(R.id.orientationTimeConstant_te).setEnabled(true);
                            findViewById(R.id.madgwickBeta_te).setEnabled(true);
                        }
                        else{
                            findViewById(R.id.lacTimeConstant_te).setEnabled(false);
                            findViewById(R.id.orientationTimeConstant_te).setEnabled(false);
                            findViewById(R.id.madgwickBeta_te).setEnabled(false);
                        }
                    }
                }
        );
        File directory = new File(MainActivity.directory.getPath() + File.separator + dir);
        if(!directory.exists()){
            directory.mkdirs();
        }
        try{
            reader = new CSVReader(new FileReader(MainActivity.directory.getPath() + File.separator + dir + File.separator + fn));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                map.put(nextLine[0], Float.parseFloat(nextLine[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ((Switch) findViewById(R.id.customSwitch)).setChecked(!(map.get("customSensors") == 0f));
        ((EditText) findViewById(R.id.lacTimeConstant_te)).setText(map.get("lacTimeConstant").toString());
        ((EditText) findViewById(R.id.orientationTimeConstant_te)).setText(map.get("orientationTimeConstant").toString());
        ((EditText) findViewById(R.id.madgwickBeta_te)).setText(map.get("madgwickBeta").toString());

        ((Switch) findViewById(R.id.vehicleSensors)).setChecked(!(map.get("vehicleSensors") == 0f));
        ((EditText) findViewById(R.id.windowSize_te)).setText(Integer.toString(map.get("windowSize").intValue()));
        ((EditText) findViewById(R.id.overlap_te)).setText(Integer.toString(map.get("overlap").intValue()));
        ((EditText) findViewById(R.id.filterLeftSize_te)).setText(Integer.toString(map.get("filterLeftSize").intValue()));
        ((EditText) findViewById(R.id.filterRightSize_te)).setText(Integer.toString(map.get("filterRightSize").intValue()));
        ((EditText) findViewById(R.id.filterDegree_te)).setText(Integer.toString(map.get("filterDegree").intValue()));

        ((EditText) findViewById(R.id.brakeMinDuration_te)).setText(Integer.toString(map.get("brakeMinDuration").intValue()));
        ((EditText) findViewById(R.id.brakeMaxDuration_te)).setText(Integer.toString(map.get("brakeMaxDuration").intValue()));
        ((EditText) findViewById(R.id.brakeLacYEnergyThreshold_te)).setText(map.get("brakeLacYEnergyThreshold").toString());
        ((EditText) findViewById(R.id.brakeVarThreshold)).setText(map.get("brakeVarThreshold").toString());
        ((EditText) findViewById(R.id.brakeAcceptFunctionThreshold_te)).setText(map.get("brakeAcceptFunctionThreshold").toString());

        ((EditText) findViewById(R.id.turnMinDuration_te)).setText(Integer.toString(map.get("turnMinDuration").intValue()));
        ((EditText) findViewById(R.id.turnMaxDuration_te)).setText(Integer.toString(map.get("turnMaxDuration").intValue()));
        ((EditText) findViewById(R.id.turnGyrZEnergyThreshold_te)).setText(map.get("turnGyrZEnergyThreshold").toString());
        ((EditText) findViewById(R.id.turnAcceptFunctionThreshold_te)).setText(map.get("turnAcceptFunctionThreshold").toString());

        ((EditText) findViewById(R.id.laneMinDuration_te)).setText(Integer.toString(map.get("laneMinDuration").intValue()));
        ((EditText) findViewById(R.id.laneMaxDuration_te)).setText(Integer.toString(map.get("laneMaxDuration").intValue()));
        ((EditText) findViewById(R.id.laneLacXEnergyThreshold_te)).setText(map.get("laneLacXEnergyThreshold").toString());
        ((EditText) findViewById(R.id.laneGyrZEnergyThreshold_te)).setText(map.get("laneGyrZEnergyThreshold").toString());
        ((EditText) findViewById(R.id.laneDtwThreshold_te)).setText(map.get("laneDtwThreshold").toString());
        ((EditText) findViewById(R.id.laneSubSampleParameter_te)).setText(Integer.toString(map.get("laneSubSampleParameter").intValue()));
    }

    public void save(View view){
        map.put("customSensors", ((Switch)findViewById(R.id.customSwitch)).isChecked() ? 1f : 0f);
        map.put("lacTimeConstant", Float.parseFloat(((EditText) findViewById(R.id.lacTimeConstant_te)).getText().toString()));
        map.put("orientationTimeConstant", Float.parseFloat(((EditText) findViewById(R.id.orientationTimeConstant_te)).getText().toString()));
        map.put("madgwickBeta", Float.parseFloat(((EditText) findViewById(R.id.madgwickBeta_te)).getText().toString()));

        map.put("vehicleSensors", ((Switch)findViewById(R.id.vehicleSensors)).isChecked() ? 1f : 0f);
        map.put("windowSize", Float.parseFloat(((EditText) findViewById(R.id.windowSize_te)).getText().toString()));
        map.put("overlap", Float.parseFloat(((EditText) findViewById(R.id.overlap_te)).getText().toString()));
        map.put("filterLeftSize", Float.parseFloat(((EditText) findViewById(R.id.filterLeftSize_te)).getText().toString()));
        map.put("filterRightSize", Float.parseFloat(((EditText) findViewById(R.id.filterRightSize_te)).getText().toString()));
        map.put("filterDegree", Float.parseFloat(((EditText) findViewById(R.id.filterDegree_te)).getText().toString()));

        map.put("brakeMinDuration", Float.parseFloat(((EditText) findViewById(R.id.brakeMinDuration_te)).getText().toString()));
        map.put("brakeMaxDuration", Float.parseFloat(((EditText) findViewById(R.id.brakeMaxDuration_te)).getText().toString()));
        map.put("brakeLacYEnergyThreshold", Float.parseFloat(((EditText) findViewById(R.id.brakeLacYEnergyThreshold_te)).getText().toString()));
        map.put("brakeVarThreshold", Float.parseFloat(((EditText) findViewById(R.id.brakeVarThreshold)).getText().toString()));
        map.put("brakeAcceptFunctionThreshold", Float.parseFloat(((EditText) findViewById(R.id.brakeAcceptFunctionThreshold_te)).getText().toString()));

        map.put("turnMinDuration", Float.parseFloat(((EditText) findViewById(R.id.turnMinDuration_te)).getText().toString()));
        map.put("turnMaxDuration", Float.parseFloat(((EditText) findViewById(R.id.turnMaxDuration_te)).getText().toString()));
        map.put("turnLacYEnergyThreshold", Float.parseFloat(((EditText) findViewById(R.id.turnGyrZEnergyThreshold_te)).getText().toString()));
        map.put("turnAcceptFunctionThreshold", Float.parseFloat(((EditText) findViewById(R.id.turnAcceptFunctionThreshold_te)).getText().toString()));

        map.put("laneMinDuration", Float.parseFloat(((EditText) findViewById(R.id.laneMinDuration_te)).getText().toString()));
        map.put("laneMaxDuration", Float.parseFloat(((EditText) findViewById(R.id.laneMaxDuration_te)).getText().toString()));
        map.put("laneLacXEnergyThreshold", Float.parseFloat(((EditText) findViewById(R.id.laneLacXEnergyThreshold_te)).getText().toString()));
        map.put("laneGyrZEnergyThreshold", Float.parseFloat(((EditText) findViewById(R.id.laneGyrZEnergyThreshold_te)).getText().toString()));
        map.put("laneDtwThreshold", Float.parseFloat(((EditText) findViewById(R.id.laneDtwThreshold_te)).getText().toString()));
        map.put("laneSubSampleParameter", Float.parseFloat(((EditText) findViewById(R.id.laneSubSampleParameter_te)).getText().toString()));

        f = new File(MainActivity.directory.getPath() + File.separator + dir + File.separator + fn);
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer = new CSVWriter(new FileWriter(f, false));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Map.Entry<String, Float> entry: map.entrySet()){
            String[] line = new String [] {entry.getKey(), entry.getValue().toString()};
            writer.writeNext((line));
        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Setting.setParameters();
        finish();
        Context context = getApplicationContext();
        CharSequence text = "Saved successfully.";
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void reset(View view){
        map = getInitialMap();
        ((Switch) findViewById(R.id.customSwitch)).setChecked(!(map.get("customSensors") == 0f));
        ((EditText) findViewById(R.id.lacTimeConstant_te)).setText(map.get("lacTimeConstant").toString());
        ((EditText) findViewById(R.id.orientationTimeConstant_te)).setText(map.get("orientationTimeConstant").toString());
        ((EditText) findViewById(R.id.madgwickBeta_te)).setText(map.get("madgwickBeta").toString());

        ((Switch) findViewById(R.id.vehicleSensors)).setChecked(!(map.get("vehicleSensors") == 0f));
        ((EditText) findViewById(R.id.windowSize_te)).setText(Integer.toString(map.get("windowSize").intValue()));
        ((EditText) findViewById(R.id.overlap_te)).setText(Integer.toString(map.get("overlap").intValue()));
        ((EditText) findViewById(R.id.filterLeftSize_te)).setText(Integer.toString(map.get("filterLeftSize").intValue()));
        ((EditText) findViewById(R.id.filterRightSize_te)).setText(Integer.toString(map.get("filterRightSize").intValue()));
        ((EditText) findViewById(R.id.filterDegree_te)).setText(Integer.toString(map.get("filterDegree").intValue()));

        ((EditText) findViewById(R.id.brakeMinDuration_te)).setText(Integer.toString(map.get("brakeMinDuration").intValue()));
        ((EditText) findViewById(R.id.brakeMaxDuration_te)).setText(Integer.toString(map.get("brakeMaxDuration").intValue()));
        ((EditText) findViewById(R.id.brakeLacYEnergyThreshold_te)).setText(map.get("brakeLacYEnergyThreshold").toString());
        ((EditText) findViewById(R.id.brakeVarThreshold)).setText(map.get("brakeVarThreshold").toString());
        ((EditText) findViewById(R.id.brakeAcceptFunctionThreshold_te)).setText(map.get("brakeAcceptFunctionThreshold").toString());

        ((EditText) findViewById(R.id.turnMinDuration_te)).setText(Integer.toString(map.get("turnMinDuration").intValue()));
        ((EditText) findViewById(R.id.turnMaxDuration_te)).setText(Integer.toString(map.get("turnMaxDuration").intValue()));
        ((EditText) findViewById(R.id.turnGyrZEnergyThreshold_te)).setText(map.get("turnGyrZEnergyThreshold").toString());
        ((EditText) findViewById(R.id.turnAcceptFunctionThreshold_te)).setText(map.get("turnAcceptFunctionThreshold").toString());

        ((EditText) findViewById(R.id.laneMinDuration_te)).setText(Integer.toString(map.get("laneMinDuration").intValue()));
        ((EditText) findViewById(R.id.laneMaxDuration_te)).setText(Integer.toString(map.get("laneMaxDuration").intValue()));
        ((EditText) findViewById(R.id.laneLacXEnergyThreshold_te)).setText(map.get("laneLacXEnergyThreshold").toString());
        ((EditText) findViewById(R.id.laneGyrZEnergyThreshold_te)).setText(map.get("laneGyrZEnergyThreshold").toString());
        ((EditText) findViewById(R.id.laneDtwThreshold_te)).setText(map.get("laneDtwThreshold").toString());
        ((EditText) findViewById(R.id.laneSubSampleParameter_te)).setText(Integer.toString(map.get("laneSubSampleParameter").intValue()));
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public static void setParameters(){
        Map<String, Float> map = Setting.getInitialMap();
        try{
            CSVReader reader = new CSVReader(new FileReader(MainActivity.directory.getPath() + File.separator + dir + File.separator + fn));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                map.put(nextLine[0], Float.parseFloat(nextLine[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Sensors.setUseAndroidDefaultSensors(map.get("customSensors")==0f);
        LinearAcceleration.setTimeConstant(map.get("lacTimeConstant"));
        Orientation.setTimeConstant(map.get("orientationTimeConstant"));
        Madgwick.setBeta(map.get("madgwickBeta"));

        Detector.setUseVehicleSensors(!(map.get("vehicleSensors")==0f));
        Detector.setWindowSize(map.get("windowSize").intValue());
        Detector.setOverLap(map.get("overlap").intValue());
        Detector.setSavgolNl(map.get("filterLeftSize").intValue());
        Detector.setSavgolNr(map.get("filterRightSize").intValue());
        Detector.setSavgolDegree(map.get("filterDegree").intValue());

        BrakeEventDetector.setMinDuration(map.get("brakeMinDuration").intValue());
        BrakeEventDetector.setMaxDuration(map.get("brakeMaxDuration").intValue());
        BrakeEventDetector.setLacYEnergyThreshold(map.get("brakeLacYEnergyThreshold"));
        BrakeEventDetector.setVarThreshold(map.get("brakeVarThreshold"));
        BrakeEventDetector.setAcceptFunctionThreshold(map.get("brakeAcceptFunctionThreshold"));

        TurnEventDetector.setMinDuration(map.get("turnMinDuration").intValue());
        TurnEventDetector.setMaxDuration(map.get("turnMaxDuration").intValue());
        TurnEventDetector.setGyrZEnergyThreshold(map.get("turnGyrZEnergyThreshold"));
        TurnEventDetector.setAcceptFunctionThreshold(map.get("turnAcceptFunctionThreshold"));

        LaneChangeDetector.setMinDuration(map.get("laneMinDuration").intValue());
        LaneChangeDetector.setMaxDuration(map.get("laneMaxDuration").intValue());
        LaneChangeDetector.setLacXEnergyThreshold(map.get("laneLacXEnergyThreshold"));
        LaneChangeDetector.setGyrZEnergyThreshold(map.get("laneGyrZEnergyThreshold"));
        LaneChangeDetector.setDtwThreshold(map.get("laneDtwThreshold"));
        LaneChangeDetector.setSubSampleParameter(map.get("laneSubSampleParameter").intValue());
    }

    private static Map<String, Float> getInitialMap(){
        Map<String, Float> map = new HashMap<String, Float>() {{
            put("customSensors", 1f);
            put("lacTimeConstant", 0.2f);
            put("orientationTimeConstant", 0.1f);
            put("madgwickBeta", 0.01f);

            put("vehicleSensors", 1f);
            put("windowSize", 40f);
            put("overlap", 30f);
            put("filterLeftSize", 15f);
            put("filterRightSize", 15f);
            put("filterDegree", 1f);

            put("brakeMinDuration", 1000f);
            put("brakeMaxDuration", 10000f);
            put("brakeLacYEnergyThreshold", 0.1f);
            put("brakeVarThreshold", 0.02f);
            put("brakeAcceptFunctionThreshold", -0.1f);

            put("turnMinDuration", 2000f);
            put("turnMaxDuration", 10000f);
            put("turnGyrZEnergyThreshold", 0.01f);
            put("turnAcceptFunctionThreshold", 0.1f);

            put("laneMinDuration", 2000f);
            put("laneMaxDuration", 10000f);
            put("laneLacXEnergyThreshold", 0.04f);
            put("laneGyrZEnergyThreshold", 0.001f);
            put("laneDtwThreshold", 0.2f);
            put("laneSubSampleParameter", 10f);
        }};
        return map;
    }
}
