package com.distancegps.konstantin.distancegps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity
{

    EditText editText;
    EditText editText1;
    EditText editText2;
    TextView textView3;
    Button connect_button;
    SharedPreferences sPref;
    Intent TCP;



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //final Button stop_button = (Button)findViewById(R.id.stop_button);
        editText = (EditText) findViewById(R.id.editText);
        editText1 = (EditText) findViewById(R.id.editText1);
        editText2 = (EditText) findViewById(R.id.editText2);
        connect_button = (Button)findViewById(R.id.connect_button);
        textView3 = (TextView) findViewById(R.id.textView3);



        loadSettings();

        /// TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        // final String IMEI = telephonyManager.getDeviceId();

        // mTimer = new Timer();
        // mTimer.schedule(new MyTimerTask(), 0, 30000);

        connect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if ((editText.length() != 0) && (editText1.length() != 0) && (editText2.length() != 0)) {

                    // connect to the server
                    if (connect_button.getText().toString().equals(getString(R.string.Connect))) {

                        if (!isOnline()) {
                            Toast.makeText(getApplicationContext(), getString(R.string.isOnline), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        PendingIntent PI = createPendingResult(1, new Intent(),0);

                        TCP = new Intent(MainActivity.this, TCPService.class);
                        TCP.putExtra("IP", editText.getText().toString());
                        TCP.putExtra("PORT",editText1.getText().toString());
                        TCP.putExtra("NAME",editText2.getText().toString());
                        TCP.putExtra("PI",PI);

                        startService(TCP);

                        ChangeButton(false);


                    } else {
                        stopService(TCP);
                    /*
                        connect_button.setEnabled(false);
                        if (mTcpClient != null) {
                            mTcpClient.stopClient();
                        }

                        ChangeButton(true);
                        // textView3.setText(getString(R.string.NoConnected));
                    */

                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.Fill_fields), Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*
        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                myVibrator.cancel();

            }
        });
        */
    }

    @Override
    protected void onDestroy() {
        Log.e("onDestroy", "onDestroyMAIN");
        super.onDestroy();

        try {
            stopService(TCP);
        } catch (Exception e) {
            //
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (resultCode) {
            case 0:

                break;

            case 1:

                break;

            case 2:
                textView3.setText(getString(R.string.ReConnect));
                connect_button.setEnabled(true);
                break;

            case 3:
                textView3.setText(getString(R.string.ServerError));
                ChangeButton(true);
                break;

            case 4:
                textView3.setText(getString(R.string.Connecting));
                break;

            case 5:
                textView3.setText(getString(R.string.Connected));
                connect_button.setEnabled(true);
                break;

            case 6:
                textView3.setText(getString(R.string.NoConnected));
                ChangeButton(true);
                break;


        }
        Log.d("onActivityResult ",String.valueOf(resultCode));
    }


    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        if (nInfo != null && nInfo.isConnected()) {
            Log.v("status", "ONLINE");
            return true;
        }
        else {
            Log.v("status", "OFFLINE");
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
                try {
                    stopService(TCP);
                } catch (Exception e) {
                    //
                }
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

    void saveSettings() {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString("IP", editText.getText().toString());
        ed.putString("PORT", editText1.getText().toString());
        ed.putString("NAME", editText2.getText().toString());
        ed.apply();
    }

    void loadSettings() {
        sPref = getPreferences(MODE_PRIVATE);
        editText.setText(sPref.getString("IP", ""));
        editText1.setText(sPref.getString("PORT", ""));
        editText2.setText(sPref.getString("NAME", ""));
    }

    private void ChangeButton(boolean state) {
        if (state) {
            connect_button.setText(getString(R.string.Connect));
            editText.setEnabled(true);
            editText1.setEnabled(true);
            editText2.setEnabled(true);
            connect_button.setEnabled(true);

        } else {
            connect_button.setText(getString(R.string.disconnect));
            editText.setEnabled(false);
            editText1.setEnabled(false);
            editText2.setEnabled(false);
            connect_button.setEnabled(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        saveSettings();
    }


}