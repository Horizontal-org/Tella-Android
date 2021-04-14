package com.hzontal.tella_vault;

import android.location.Location;

import java.io.Serializable;


public final class MyLocation implements Serializable {
    private static final long UNKNOWN = -1;

    private long timestamp; // UTC
    private double latitude;
    private double longitude;
    private Double altitude;
    private Float accuracy;
    private String provider;
    private Float speed;


    public MyLocation() {
        this.timestamp = UNKNOWN;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Float getSpeed() {
        return speed;
    }

    public void setSpeed(Float speed) {
        this.speed = speed;
    }

    public boolean isEmpty() {
        return timestamp == UNKNOWN;
    }

    public static MyLocation createEmpty() {
        MyLocation empty = new MyLocation();
        empty.setTimestamp(UNKNOWN);

        return empty;
    }

    public static MyLocation fromLocation(Location location) {
        MyLocation el = new MyLocation();
        el.setTimestamp(location.getTime());
        el.setAccuracy(location.getAccuracy());
        el.setAltitude(location.getAltitude());
        el.setLatitude(location.getLatitude());
        el.setLongitude(location.getLongitude());
        el.setProvider(location.getProvider());
        el.setSpeed(location.getSpeed());

        return el;
    }

    @Override
    public String toString() {
        return "Location [ Latitude: " + latitude +
                ", Longitude: " + longitude +
                ", Altitude: " + altitude +
                ", Accuracy: " + accuracy +
                ", Time: " + timestamp +
                " ]";
    }
}
