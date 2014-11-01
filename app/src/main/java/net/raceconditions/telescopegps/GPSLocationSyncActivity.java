package net.raceconditions.telescopegps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Criteria;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.Result;

public class GPSLocationSyncActivity extends FragmentActivity implements LocationListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private Criteria criteria;
    private Context context = this;
    private String provider;
    private Boolean isFirstZoom = true;
    TCPClient mTcpClient;
    Button controlButton = null;

    private static final int RESULT_SETTINGS = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gpssync, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;

        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_gpslocation_sync);
        setUpMapIfNeeded();
        createConnectButton();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            buildAlertMessageNoGps();
        criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        locationManager.requestLocationUpdates(provider, 5000, 0, this);

        if(mMap != null)
        {
            mMap.setMyLocationEnabled(true);
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            zoomMap(location);
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        zoomMap(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void startConnection() {

        try {
            new TcpConnectTask(new TaskListener(), new ConnectionListener(), this.context).execute("");
        } catch (Exception ex)
        {
            Utils.alertOkDialog(this.context, ex.toString(), ex.getMessage());
        }
    }

    private void sendGPSUpdate()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.getTime();
        byte[] dateTime = DateSerializer.serialize(calendar);

        Location location = locationManager.getLastKnownLocation(provider);
        byte[] gps = LocationSerializer.serialize(location);

        mTcpClient.sendMessage(dateTime);
        mTcpClient.sendMessage(gps);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    Toast.makeText(context, "GPS coordinates and current time sent to telescope.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void zoomMap(Location location) {
        if(location != null) {
            LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
            if (isFirstZoom) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 16));
                isFirstZoom = false;
            } else
                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentPosition));
        }
    }

    private void createUpdateButton() {
        if(controlButton == null) {
            controlButton = new Button(this);
            addContentView(controlButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        controlButton.setText("Update GPS");
        controlButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setTitle("Send GPS")
                        .setMessage("Are you sure you want to send GPS coordinates and current time to your telescope?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                sendGPSUpdate();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    private void createConnectButton() {
        if (controlButton == null) {
            controlButton = new Button(this);
            addContentView(controlButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        controlButton.setText("Connect");
        controlButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startConnection();
            }
        });
    }

    public class TaskListener implements TaskEventHandler {
        public void onTaskCompleted(TCPClient c) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mTcpClient != null) {
                        mTcpClient = null;
                        Toast.makeText(context, "The telescope connection is closed.", Toast.LENGTH_LONG).show();
                    }
                    createConnectButton();
                }
            });
        }
    }

    public class ConnectionListener implements ConnectionEventHandler {
        @Override
        //here the messageReceived method is implemented
        public void messageReceived(String message) {
        }

        @Override
        //here the messageReceived method is implemented
        public void connectionEstablished(TCPClient tcpClient) {
            mTcpClient = tcpClient;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "The telescope is successfully connected.", Toast.LENGTH_LONG).show();
                    createUpdateButton();
                }
            });
        }

        @Override
        //here the messageReceived method is implemented
        public void connectionFailed() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.alertOkDialog(context, "Connection Failed", "Unable to connect to the telescope. Please check your connection and settings.");
                }
            });
        }
    }
}
