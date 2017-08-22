package com.distancegps.konstantin.distancegps;


import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.location.LocationListener;
import android.os.Vibrator;
import android.util.Log;



public class TCPService extends Service {

    public final String LOG_TAG = "LOG_TAG";

    public TCPClient mTcpClient;
    ConnectTask connectTask;
    private LocationManager locationManager;
    public PendingIntent PI;
    GpsChangeReceiver m_gpsChangeReceiver;
    Vibrator myVibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        myVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        m_gpsChangeReceiver = new GpsChangeReceiver();
        this.registerReceiver(m_gpsChangeReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

    }

    @Override
    public void onDestroy() {

        Log.d(LOG_TAG, "onDestroy");

        super.onDestroy();

        unregisterReceiver(m_gpsChangeReceiver);
        locationManager.removeNmeaListener(gpsStatus);

        if (mTcpClient != null)
        {
            mTcpClient.stopClient();
            mTcpClient.messageReceived(6);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.e(LOG_TAG, "onStartCommand");

        String IP = intent.getStringExtra("IP");
        String PORT = intent.getStringExtra("PORT");
        String NAME = intent.getStringExtra("NAME");
        PI = intent.getParcelableExtra("PI");

        connectTask = new ConnectTask();
        connectTask.execute(IP, PORT, NAME);

        //return super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }


    public class GpsChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            if (StatusGPS(context))
            {
                mTcpClient.sendMessage("isEnableGPS:1");
            } else {
                mTcpClient.sendMessage("isEnableGPS:0");
                locationManager.removeUpdates(locationListener);
                locationManager.removeNmeaListener(gpsStatus);
            }
        }
    }

    private boolean StatusGPS(Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER );
    }


    private GpsStatus.NmeaListener gpsStatus = new GpsStatus.NmeaListener() {

        long timeGPS = 0;
        String status = "";

        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            if (((timestamp - timeGPS >= 5000) ||  (timestamp < 0)) & ((nmea.substring(0,6).equals("$GPGGA") || nmea.substring(0,6).equals("$GNGGA"))))
            {
                timeGPS = timestamp;
                Log.e(LOG_TAG, "gpsStatus: " + nmea.split(",")[6]);

                if (!status.equals(nmea.split(",")[6])) {
                    //mTcpClient.sendMessage("gpsStatus:" + nmea.split(",")[6]);

                    status = nmea.split(",")[6];
                    if (status.equals("0")) {
                        mTcpClient.sendMessage("Coordinates: Lat:; Lon:; Alt:; Acc:;");
                    }
                }
            }

        }
    };


    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            mTcpClient.sendMessage(showLocation(location));
            Log.e(LOG_TAG,showLocation(location));
        }

        @Override
        public void onProviderDisabled(String provider) {
           // mTcpClient.sendMessage("GPS Disabled");
        }

        @Override
        public void onProviderEnabled(String provider) {
           // mTcpClient.sendMessage("GPS Enabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // работает когда активен главный экран

        }
    };

    private String showLocation(Location location) {
        if (location == null)
            return null;
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            return String.format(
                    "Coordinates: Lat:%f; Lon:%f; Alt:%f; Acc:%f;",
                    location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy());

        }
        return null;
    }


    class ConnectTask extends AsyncTask<String, Integer, Void> {

        PowerManager pm;
        PowerManager.WakeLock wl;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Log.d(LOG_TAG, "onPreExecute");

            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
            wl.acquire();
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            Log.d(LOG_TAG, "onPostExecute");
            locationManager.removeUpdates(locationListener);

            wl.release();
            stopSelf();
        }

        @Override
        protected Void doInBackground(String[] param) {
            Log.d(LOG_TAG, "doInBackground BEGIN");

            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                public void messageReceived(int message) {
                    publishProgress(message);
                    Log.d(LOG_TAG, "messageReceived: " + String.valueOf(message));
                }
            }, param[0], param[1], param[2]);



            try {
                mTcpClient.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, "doInBackground END");
            return null;
        }


        @Override
        protected void onProgressUpdate(Integer[] values) {
            super.onProgressUpdate(values);
            try {
                switch (values[0]) {
                    case 0:
                        myVibrator.vibrate(999999);
                        break;

                    case 1:
                        myVibrator.cancel();
                        break;

                    case 2:
                        locationManager.removeUpdates(locationListener);
                        locationManager.removeNmeaListener(gpsStatus);
                        PI.send(values[0]);
                        break;
                    case 5:
                        if (StatusGPS(TCPService.this)) {
                            mTcpClient.sendMessage("isEnableGPS:1");
                        } else {
                            mTcpClient.sendMessage("isEnableGPS:0");
                        }
                        PI.send(values[0]);

                        break;

                    case 10:
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                5000, 2, locationListener);
                        locationManager.addNmeaListener(gpsStatus);
                        break;

                    case 11:
                        locationManager.removeUpdates(locationListener);
                        locationManager.removeNmeaListener(gpsStatus);
                        //mTcpClient.sendMessage("gpsStatus:-1");
                        break;

                    default:
                        PI.send(values[0]);
                        break;
                }
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }

        }

    }

}
