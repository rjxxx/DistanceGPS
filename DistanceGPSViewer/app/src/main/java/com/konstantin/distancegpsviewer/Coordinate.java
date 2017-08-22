package com.konstantin.distancegpsviewer;

public class Coordinate {

    private double latitude;
    private double longitude;
    private double altitude;
    private boolean actual;

    public Coordinate()
    {
        latitude = 0;
        longitude = 0;
        altitude = 0;
        actual = false;
    }

    public Coordinate(double latitude, double longitude, double altitude)
    {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.actual = false;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean getActual() {
        return actual;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setActual(boolean actual) {
        this.actual = actual;
    }

}
