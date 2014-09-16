package net.raceconditions.telescopegps;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.sql.Connection;

/**
 * Created by ubuntu on 9/16/14.
 */
public class TcpConnectTask extends AsyncTask<String,String,TCPClient> {
    private TaskEventHandler taskListener;
    private ConnectionEventHandler connectionListener;
    private Context context;

    public TcpConnectTask(TaskEventHandler listener, ConnectionEventHandler connectionListener, Context context) {
        this.taskListener = listener;
        this.context = context;
        this.connectionListener = connectionListener;
    }

    @Override
    protected TCPClient doInBackground(String... message) {

        //we create a TCPClient object and
        TCPClient mTcpClient = new TCPClient(connectionListener, context);

        try {
            mTcpClient.startClient();
        } catch (Exception ex) {
            Log.e("TcpConnectTask", "Error", ex);
            Utils.alertOkDialog(context, ex.toString(), ex.getMessage());
            return null;
        }

        return mTcpClient;
    }

    @Override
    protected void onPostExecute(TCPClient c) {
        taskListener.onTaskCompleted(c);
    }
}
