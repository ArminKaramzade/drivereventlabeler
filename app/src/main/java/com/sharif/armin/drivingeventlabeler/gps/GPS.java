package com.sharif.armin.drivingeventlabeler.gps;

import android.annotation.SuppressLint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.sharif.armin.drivingeventlabeler.write.Writer;

public class GPS {
    private LocationManager locationManager;
    private LocationListener locationListener;
    Writer writer;

    private int gps_delay;

    public GPS(LocationManager locationManager, Writer writer, int gps_delay){
        this.locationManager = locationManager;
        this.gps_delay = gps_delay;
        this.locationListener = new GPSListener();
        this.writer = writer;
    }


    @SuppressLint("MissingPermission")
    private void register() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, this.gps_delay, 0, this.locationListener);
    }

    public void start() {
        register();
    }

    public void stop() {
        unRegister();
    }

    private void unRegister(){
        locationManager.removeUpdates(this.locationListener);
    }

    private class GPSListener implements LocationListener{
        @Override
        public void onLocationChanged(Location location) {
            writer.writeGPS(location.getTime(), location);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onProviderDisabled(String provider) { }
    }
}
