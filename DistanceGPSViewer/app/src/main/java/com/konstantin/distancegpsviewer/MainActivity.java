package com.konstantin.distancegpsviewer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    final String LOG_TAG = "LOG_TAG";

    public TCPClient mTcpClient;
    ConnectTask connectTask;
    private LocationManager locationManager;
    GpsChangeReceiver m_gpsChangeReceiver;

    public Coordinate currentCoordinate;

    EditText editText;
    EditText editText1;
    TextView editConnection;
    TextView textView3;
    Button connect_button;
    TextView tvEnabledGPS;

    SharedPreferences sPref;

    ArrayList<PhoneInformation> phoneInformations = new ArrayList<>();
    BoxAdapter boxAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.d(LOG_TAG, "onCreate");

        editText = (EditText) findViewById(R.id.editText);
        editText1 = (EditText) findViewById(R.id.editText1);
        connect_button = (Button) findViewById(R.id.connect_button);
        textView3 = (TextView) findViewById(R.id.textView3);
        tvEnabledGPS = (TextView) findViewById(R.id.tvEnabledGPS);
        editConnection = (TextView) findViewById(R.id.connection);
        editText1.setWidth(editText.getWidth());

        editConnection.setTextColor(getResources().getColor(R.color.red));
        //Вкладки
        TabHost tabs = (TabHost) findViewById(android.R.id.tabhost);

        tabs.setup();

        TabHost.TabSpec spec = tabs.newTabSpec("tag1");

        spec.setContent(R.id.tab1);
        spec.setIndicator("Подключение");
        tabs.addTab(spec);

        spec = tabs.newTabSpec("tag2");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Просмотр расстояния");
        tabs.addTab(spec);

        tabs.setCurrentTab(0);

        //////////////

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        m_gpsChangeReceiver = new GpsChangeReceiver();
        MainActivity.this.registerReceiver(m_gpsChangeReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        checkEnabledGPS();
        currentCoordinate = new Coordinate();

        loadSettings();

        // TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        // final String IMEI = telephonyManager.getDeviceId();

        // mTimer = new Timer();
        // mTimer.schedule(new MyTimerTask(), 0, 30000);


        connect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                if ((editText.length() != 0) && (editText1.length() != 0)) {

                    // connect to the server
                    if (connect_button.getText().toString().equals(getString(R.string.Connect))) {

                        if (!isOnline()) {
                            Toast.makeText(getApplicationContext(), getString(R.string.isOnline), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        connectTask = new ConnectTask();
                        connectTask.execute(editText.getText().toString(),
                                editText1.getText().toString());


                        ChangeButton(false);
                    } else {

                        if (mTcpClient != null) {
                            mTcpClient.stopClient();
                            mTcpClient.messageReceived("TCP:NoConnected");
                        }
                        phoneInformations.clear();
                        boxAdapter.notifyDataSetChanged();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.Fill_fields), Toast.LENGTH_SHORT).show();
                }

            }

        });


        boxAdapter = new BoxAdapter(this, phoneInformations);

        // настраиваем список
        ListView lvMain = (ListView) findViewById(R.id.listView);
        lvMain.setAdapter(boxAdapter);

        lvMain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.d(LOG_TAG, "itemClick: position = " + position + ", id = "
                        + id);

            }
        });


        lvMain.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "onItemLongClick: position = " + position + ", id = "
                        + id);

                if (phoneInformations.get((int) view.getTag()).isEnableGPS.equals("1")) {
                    if (phoneInformations.get((int) view.getTag()).isTracking) {

                        mTcpClient.sendMessage("TrackingStop:" + phoneInformations.get((int) view.getTag()).ip);
                        phoneInformations.get((int) view.getTag()).isTracking = false;
                        phoneInformations.get((int) view.getTag()).distance = "";
                        phoneInformations.get((int) view.getTag()).accuracy = "";
                        Toast.makeText(getApplicationContext(), "Отслеживание цели отменено", Toast.LENGTH_SHORT).show();

                    } else {
                        mTcpClient.sendMessage("TrackingStart:" + phoneInformations.get((int) view.getTag()).ip);
                        phoneInformations.get((int) view.getTag()).isTracking = true;
                        Toast.makeText(getApplicationContext(), "Отслеживание цели запущено", Toast.LENGTH_SHORT).show();
                    }

                    boxAdapter.notifyDataSetChanged();

                } else {
                    Toast.makeText(getApplicationContext(), "У цели не включен GPS", Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });


    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();

        saveSettings();
        Log.d(LOG_TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mTcpClient != null)
        {
            mTcpClient.stopClient();
        }

        locationManager.removeUpdates(locationListener);

        Log.d(LOG_TAG, "onDestroy");
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        if (nInfo != null && nInfo.isConnected()) {
            Log.d(LOG_TAG, "ONLINE");
            return true;
        }
        else {
            Log.d(LOG_TAG, "OFFLINE");
            return false;
        }
    }

    @Override
    public void onBackPressed() {

        openQuitDialog();
    }

    private void openQuitDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(
                MainActivity.this);
        quitDialog.setTitle(getString(R.string.Exit));

        quitDialog.setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                finish();
            }
        });

        quitDialog.setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
            }
        });

        quitDialog.show();
    }

    private void saveSettings() {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString("IP", editText.getText().toString());
        ed.putString("PORT", editText1.getText().toString());
        ed.apply();
    }

    private void loadSettings() {
        sPref = getPreferences(MODE_PRIVATE);
        editText.setText(sPref.getString("IP", ""));
        editText1.setText(sPref.getString("PORT", ""));
    }

    private void ChangeButton(boolean state) {
        if (state) {
            connect_button.setText(getString(R.string.Connect));
            editText.setEnabled(true);
            editText1.setEnabled(true);
            connect_button.setEnabled(true);

        } else {
            connect_button.setText(getString(R.string.disconnect));
            editConnection.setText(getString(R.string.NoConnected));
            editConnection.setTextColor(getResources().getColor(R.color.red));
            editText.setEnabled(false);
            editText1.setEnabled(false);
            connect_button.setEnabled(false);
        }
    }

    private GpsStatus.NmeaListener gpsStatus = new GpsStatus.NmeaListener() {

        long timeGPS = 0;
        String status = "";
        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            if ((timestamp - timeGPS >= 5000) & (nmea.substring(0,6).equals("$GPGGA") || nmea.substring(0,6).equals("$GNGGA")))
            {
                timeGPS = timestamp;
                Log.e(LOG_TAG, "gpsStatus: " + nmea.split(",")[6]);

                if (!status.equals(nmea.split(",")[6])) {
                    if (nmea.split(",")[6].equals("0")) {
                        tvEnabledGPS.setText(getString(R.string.gpsOnFindingCoordinates));
                        tvEnabledGPS.setTextColor(getResources().getColor(R.color.yellow));
                        currentCoordinate.setActual(false);
                    } else {
                        tvEnabledGPS.setText(getString(R.string.gpsOnReceiveCoordinates));
                        tvEnabledGPS.setTextColor(getResources().getColor(R.color.green));
                    }
                    status = nmea.split(",")[6];
                }
            }

        }
    };

    public class GpsChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            if (StatusGPS(context))
            {
                tvEnabledGPS.setText(getString(R.string.gpsOn));
                tvEnabledGPS.setTextColor(getResources().getColor(R.color.yellow));
            } else {
                tvEnabledGPS.setText(getString(R.string.gpsOff));
                tvEnabledGPS.setTextColor(getResources().getColor(R.color.red));
                currentCoordinate.setActual(false);
            }
        }
    }

    private boolean StatusGPS(Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER );
    }

    private void checkEnabledGPS() {
        if (StatusGPS(MainActivity.this)) {
            tvEnabledGPS.setText(getString(R.string.gpsOn));
            tvEnabledGPS.setTextColor(getResources().getColor(R.color.yellow));
        } else {
            tvEnabledGPS.setText(getString(R.string.gpsOff));
            tvEnabledGPS.setTextColor(getResources().getColor(R.color.red));
        }
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            // mTcpClient.sendMessage(showLocation(location));
            Log.d(LOG_TAG, "onLocationChanged");
            if ((location.getProvider().equals(LocationManager.GPS_PROVIDER))) {
                currentCoordinate.setLongitude(location.getLongitude());
                currentCoordinate.setLatitude(location.getLatitude());
                currentCoordinate.setAltitude(location.getAccuracy());
                currentCoordinate.setActual(true);

                for (int i = 0; i < phoneInformations.size(); i++) {
                    if (!phoneInformations.get(i).distance.equals("")) {
                        phoneInformations.get(i).distance = String.format("%.1f", getDistanceBetweenTwoPoints(currentCoordinate, phoneInformations.get(i).coordinate, false));
                    }
                }
                boxAdapter.notifyDataSetChanged();
            }

        }

        @Override
        public void onProviderDisabled(String provider) {
            checkEnabledGPS();
        }

        @Override
        public void onProviderEnabled(String provider) {
            checkEnabledGPS();
            //showLocation(locationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };

    class ConnectTask extends AsyncTask<String, String, Void> {

        PowerManager pm;
        PowerManager.WakeLock wl;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Log.d(LOG_TAG, "onPreExecute");

            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
            wl.acquire();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    5000, 3, locationListener);
            locationManager.addNmeaListener(gpsStatus);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            Log.d(LOG_TAG, "onPostExecute");
            locationManager.removeUpdates(locationListener);
            locationManager.removeNmeaListener(gpsStatus);
            currentCoordinate.setActual(false);
            wl.release();

        }

        @Override
        protected Void doInBackground(String[] param) {
            Log.d(LOG_TAG, "doInBackground BEGIN");

            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    publishProgress(message);
                    Log.d(LOG_TAG, "messageReceived: " + message);
                }

            }, param[0], param[1]);



            try {
                mTcpClient.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, "doInBackground END");
            return null;
        }


        @Override
        protected void onProgressUpdate(String[] values) {
            super.onProgressUpdate(values);

            if (values[0] != null) {

                String[] command = values[0].split(":", 2);

                if (command[0].equals("TCP")) {

                    switch (command[1]) {

                        case "ReConnect":
                            textView3.setText(getString(R.string.ReConnect));
                            editConnection.setText(getString(R.string.ReConnect));
                            editConnection.setTextColor(getResources().getColor(R.color.yellow));
                            connect_button.setEnabled(true);
                            if (phoneInformations.size() != 0) {
                                phoneInformations.clear();
                                boxAdapter.notifyDataSetChanged();
                            }
                            break;

                        case "ServerError":
                            textView3.setText(getString(R.string.ServerError));
                            editConnection.setText(getString(R.string.ServerError));
                            editConnection.setTextColor(getResources().getColor(R.color.red));
                            ChangeButton(true);
                            break;

                        case "Connecting":
                            textView3.setText(getString(R.string.Connecting));
                            editConnection.setText(getString(R.string.Connecting));
                            editConnection.setTextColor(getResources().getColor(R.color.yellow));
                            break;

                        case "Connected":
                            textView3.setText(getString(R.string.Connected));
                            editConnection.setText(getString(R.string.Connected));
                            editConnection.setTextColor(getResources().getColor(R.color.green));
                            connect_button.setEnabled(true);

                            break;

                        case "NoConnected":
                            textView3.setText(getString(R.string.NoConnected));
                            editConnection.setText(getString(R.string.NoConnected));
                            editConnection.setTextColor(getResources().getColor(R.color.red));
                            ChangeButton(true);
                            break;
                    }
                }

                if (command[0].equals("Disconnect")) {
                    String ip = command[1];

                    Log.d(LOG_TAG, ip);

                    for (int i = 0; i < phoneInformations.size(); i++) {
                        if (phoneInformations.get(i).ip.equals(ip)) {
                            phoneInformations.remove(i);
                            break;
                        }
                    }
                    boxAdapter.notifyDataSetChanged();
                    return;
                }

                if (command[0].equals("Information")) {

                    String[] inf = command[1].split("\t", 10);

                    for (int i = 0; i < phoneInformations.size(); i++) {
                        if (phoneInformations.get(i).ip.equals(inf[1])) {

                            phoneInformations.get(i).isEnableGPS = inf[2];

                            //phoneInformations.get(i).gpsStatus = inf[3];
                            if (!inf[3].equals("")) {
                                if (currentCoordinate.getActual()) {
                                    phoneInformations.get(i).distance = getDistance(inf[3], inf[4], inf[5]);
                                    phoneInformations.get(i).coordinate.setLatitude(Double.valueOf(inf[3]));
                                    phoneInformations.get(i).coordinate.setLongitude(Double.valueOf(inf[4]));
                                    phoneInformations.get(i).coordinate.setAltitude(Double.valueOf(inf[5]));
                                    phoneInformations.get(i).accuracy = inf[6];
                                } else {
                                    phoneInformations.get(i).distance = "";
                                }
                            } else {
                                phoneInformations.get(i).distance = "";
                            }


                            boxAdapter.notifyDataSetChanged();
                            return;
                        }
                    }
                    if ((currentCoordinate.getActual()) & (!inf[3].equals(""))) {
                        phoneInformations.add(new PhoneInformation(inf[0], inf[1], inf[2],
                                getDistance(inf[3], inf[4], inf[5]), inf[6],
                                new Coordinate(Double.valueOf(inf[3]), Double.valueOf(inf[4]), Double.valueOf(inf[5]))));
                    } else {
                        phoneInformations.add(new PhoneInformation(inf[0], inf[1], inf[2], "", "", new Coordinate()));
                    }
                    //phoneInformations.add(new PhoneInformation(inf[0], inf[1], inf[2], inf[3], inf[6], inf[7]));
                    boxAdapter.notifyDataSetChanged();
                }

            }
        }

    }

    private String getDistance(String latitude, String longitude, String altitude) {
             return String.format("%.1f", getDistanceBetweenTwoPoints(currentCoordinate,
                     new Coordinate(Double.valueOf(latitude), Double.valueOf(longitude), Double.valueOf(altitude)), false));
    }

    final double MAJOR_AXIS = 6378137.0; //meters
    final double MINOR_AXIS = 6356752.3142; //meters
    final double MAJOR_AXIS_POW_2 = Math.pow(MAJOR_AXIS, 2); //meters
    final double MINOR_AXIS_POW_2 = Math.pow(MINOR_AXIS, 2); //meters

    private double getTrueAngle(double latitude)
    {
        return Math.atan(((MINOR_AXIS_POW_2 / MAJOR_AXIS_POW_2) * Math.tan(deg2rad(latitude)))) * 180/Math.PI;
    }

    private double getPointRadius(double altitude, double trueAngle)
    {
        return (1 / Math.sqrt((Math.pow(Math.cos(deg2rad(trueAngle)), 2) / MAJOR_AXIS_POW_2) + (Math.pow(Math.sin(deg2rad(trueAngle)), 2) / MINOR_AXIS_POW_2))) + altitude;
    }

    public double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    public double getDistanceBetweenTwoPoints(Coordinate coord1, Coordinate coord2, boolean decard)
    {
        if (!decard) {
            double trueAngle1 = getTrueAngle(coord1.getLatitude());
            double trueAngle2 = getTrueAngle(coord2.getLatitude());

            double pointRadius1 = getPointRadius(coord1.getAltitude(), trueAngle1);
            double pointRadius2 = getPointRadius(coord2.getAltitude(), trueAngle2);

            double earthPoint1x = pointRadius1 * Math.cos(deg2rad(trueAngle1));
            double earthPoint1y = pointRadius1 * Math.sin(deg2rad(trueAngle1));

            double earthPoint2x = pointRadius2 * Math.cos(deg2rad(trueAngle2));
            double earthPoint2y = pointRadius2 * Math.sin(deg2rad(trueAngle2));

            double x = getDistanceBetweenTwoPoints(new Coordinate(earthPoint1x, earthPoint1y, 0), new Coordinate(earthPoint2x, earthPoint2y, 0), true);
            double y = Math.PI * (  (earthPoint1x + earthPoint2x) / 360 ) * ( coord1.getLongitude() - coord2.getLongitude());

            return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        } else {
            return Math.sqrt(Math.pow((coord1.getLatitude() - coord2.getLatitude()), 2) + Math.pow((coord1.getLongitude() - coord2.getLongitude()), 2));
        }
    }
}
