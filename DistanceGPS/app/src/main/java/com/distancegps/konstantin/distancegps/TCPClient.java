package com.distancegps.konstantin.distancegps;


import android.util.Log;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

public class TCPClient {

    private int serverMessage;
    public static String SERVERIP;  //your computer IP address
    public static int SERVERPORT;
    public static String TelNAME;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    Timer mTimer;
    Boolean State = true;

    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient(OnMessageReceived listener, String ip, String port, String NAME) {
        SERVERIP = ip;
        SERVERPORT = Integer.parseInt(port);
        TelNAME = NAME;
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     * @param message text entered by client
     */
    public void sendMessage(final String message){
        if (out != null && !out.checkError() && message != null) {
            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        out.println(message);
                        out.flush();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        Log.i("TCP Client", "Message send failed");
                    }
                }
            }).start();

        }
    }

    public void stopClient(){
        mRun = false;
        State = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void isStop(Boolean state)
    {
        State = state;
    }

    public void messageReceived(int num){
        mMessageListener.messageReceived(num);
    }

    public void run() throws InterruptedException {
        mTimer = new Timer();
        mTimer.schedule(new MyTimerTask(), 60000, 60000);

        mRun = true;

        while (State) {

            try {
                //here you must put your computer's IP address.
                InetAddress serverAddr = InetAddress.getByName(SERVERIP);

                Log.e("TCP Client", "C: Connecting...");
                mMessageListener.messageReceived(4);

                //create a socket to make the connection with the server
                socket = new Socket();
                socket.setKeepAlive(true);
                socket.connect(new InetSocketAddress(serverAddr, SERVERPORT), 20000);


                try {

                    //send the message to the server
                    out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                    sendMessage("NAME" + TelNAME);

                    Log.e("TCP Client", "C: Sent.");
                    mMessageListener.messageReceived(5);
                    //receive the message which the server sends back

                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    //in this while the client listens for the messages sent by the server
                    while (mRun) {

                        serverMessage = Integer.parseInt(in.readLine());

                        if (mMessageListener != null) {
                            //call the method messageReceived from MyActivity class
                            mMessageListener.messageReceived(serverMessage);
                        } else {
                            mRun = false;
                        }
                    }

                    Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
                    mMessageListener.messageReceived(2);


                } catch (Exception e) {

                    Log.e("TCP", "S: Error", e);
                    //mMessageListener.messageReceived(2);

                } finally {
                    //the socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.
                    socket.close();
                }


            } catch (SocketTimeoutException e) {

                Log.e("TCP", "C: Error", e);
                mMessageListener.messageReceived(3);
                return;

            } catch (Exception e) {
                Log.e("TCP", "C: Error", e);
                mMessageListener.messageReceived(2);

            }

            if (State) {
                Thread.sleep(10000);
            }
        }

    }

    public interface OnMessageReceived {
        void messageReceived(int message);
    }



    class MyTimerTask extends TimerTask {
        @Override
        public void run() {

            sendMessage("9");
        }

    }
}