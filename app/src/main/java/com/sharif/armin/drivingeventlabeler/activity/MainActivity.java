package com.sharif.armin.drivingeventlabeler.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.sharif.armin.drivingeventlabeler.R;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final String sensor_frequency = "com.drivingeventlabeler.mainActivity.sensor_frequency";
    public static final String gps_delay = "com.drivingeventlabeler.mainActivity.gps_delay";
    public static final String TestFlag = "com.drivingeventlabeler.mainActivity.TestFlag";
    public static final String Direction = "com.drivingeventlabeler.mainActivity.Direction";
    public static File directory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        requestForPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0 : {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    init();

                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    public void init(){
        directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DrivingEventLabeler");
        if (!directory.exists()) {
            directory.mkdirs();
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, (Environment.getExternalStorageDirectory().toString() + File.separator + "DrivingEventLabeler"), duration);
            toast.show();
        }
        Setting.setParameters();
    }

    public void requestForPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO}, 0);
        }
        else{
            init();
        }
    }

    public void manualLabeling(View view){
        Intent intent = new Intent(this, ManualLabeling.class);
        String sns_f = ((EditText)findViewById(R.id.sns_te)).getText().toString();
        String gps_f = ((EditText)findViewById(R.id.gps_te)).getText().toString();

        intent.putExtra(sensor_frequency, sns_f);
        intent.putExtra(gps_delay, gps_f);
        startActivity(intent);
    }

    public void visualization(View view){
        Intent intent = new Intent(this, Visualization.class);
        String sns_f = ((EditText)findViewById(R.id.sns_te)).getText().toString();
        String gps_f = ((EditText)findViewById(R.id.gps_te)).getText().toString();

        intent.putExtra(sensor_frequency, sns_f);
        intent.putExtra(gps_delay, gps_f);
        startActivity(intent);
    }

    public void guidedLabeling(View view){
        Intent intent = new Intent(this, GuidedLabeling.class);
        String sns_f = ((EditText)findViewById(R.id.sns_te)).getText().toString();
        String gps_f = ((EditText)findViewById(R.id.gps_te)).getText().toString();
        String dir = ((EditText)findViewById(R.id.dir)).getText().toString();
        CheckBox check = ((CheckBox)findViewById(R.id.TestFlag));
        boolean flag = check.isChecked();


        intent.putExtra(sensor_frequency, sns_f);
        intent.putExtra(gps_delay, gps_f);
        intent.putExtra(TestFlag, flag);
        intent.putExtra(Direction, dir);
        startActivity(intent);
    }

    public void biasEstimation(View view){
        Intent intent = new Intent(this, BiasEstimation.class);
        startActivity(intent);
    }

    public void setting(View view){
        Intent intent = new Intent(this, Setting.class);
        startActivity(intent);
    }
}
