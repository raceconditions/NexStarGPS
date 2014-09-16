package net.raceconditions.telescopegps;

/**
 * Created by ubuntu on 9/7/14.
 */
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient implements TelescopeClient {

    private String serverMessage;
    //public static final String SERVERIP = "192.168.1.12"; //your computer IP address
    //public static final int SERVERPORT = 5000;
    private ConnectionEventHandler mMessageListener = null;
    private boolean mRun = false;
    private Context mContext;
    private String host = "0.0.0.0";
    private int port = 0;

    OutputStream out;
    BufferedReader in;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient(ConnectionEventHandler listener, Context context) {
        mMessageListener = listener; mContext = context;

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        host = sharedPrefs.getString("host", "0.0.0.0");
        try {
            port = Integer.valueOf(sharedPrefs.getString("port_number", "0"));
        }
        catch (Exception ex){
            Utils.alertOkDialog(mContext, "Settings Error", "Port number is invalid");
        }
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    @Override
    public void sendMessage(byte[] message) {
        try {
            if (out != null) {
                out.write(message);
                out.flush();
            }
        }
        catch(IOException e)
        {
            Log.e("TCP", e.getMessage());
            mRun = false;
            mMessageListener.connectionFailed();
        }
    }

    @Override
    public void stopClient() {
        mRun = false;
    }

    @Override
    public void startClient() {

        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(host);

            Log.e("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, port);

            try {

                //send the message to the server
                out = socket.getOutputStream();

                mMessageListener.connectionEstablished(this);

                Log.e("TCP Client", "C: Sent.");

                Log.e("TCP Client", "C: Done.");

                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //in this while the client listens for the messages sent by the server
                while (mRun) {
                    serverMessage = in.readLine();

                    if (serverMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(serverMessage);
                    }
                    serverMessage = null;

                }

                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");

            } catch (Exception e) {

                Log.e("TCP", "S: Error", e);
                mMessageListener.connectionFailed();

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (Exception e) {
            Log.e("TCP", "C: Error", e);
            mMessageListener.connectionFailed();
        }
    }
}