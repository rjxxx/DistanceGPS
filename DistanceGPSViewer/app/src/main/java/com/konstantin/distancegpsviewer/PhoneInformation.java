package com.konstantin.distancegpsviewer;

public class PhoneInformation {

    String name;
    String ip;
    String gpsStatus;
    String distance;
    String accuracy;
    String isEnableGPS;
    boolean isTracking;
    Coordinate coordinate;

    PhoneInformation(String name, String ip, String isEnableGPS, String distance, String accuracy, Coordinate coordinate) {
        this.name = name;
        this.ip = ip;
        this.gpsStatus = gpsStatus;
        this.isEnableGPS = isEnableGPS;
        this.distance = distance;
        this.accuracy = accuracy;
        this.isTracking = false;
        this.coordinate = coordinate;
    }
}
