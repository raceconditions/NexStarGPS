package net.raceconditions.telescopegps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Criteria;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.CameraUpdateFactory;

import java.util.Calendar;

public class GPSLocationSyncActivity extends FragmentActivity implements LocationListener, TouchableWrapper.UpdateMapAfterUserInterection {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationManager locationManager;
    private Criteria criteria;
    private Context context = this;
    private String provider;
    private Boolean userHasInteracted = false;
    private AlertUtils alertUtils = new AlertUtils();
    TCPClient mTcpClient;
    Button controlButton = null;

    private static final int RESULT_SETTINGS = 1;


    //region Activity Overrides

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
    //endregion

    //region FragmentActivity Overrides
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
        MapsInitializer.initialize(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_gpslocation_sync);
        setUpMapIfNeeded();
        createConnectButton();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);

        setupLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
        userHasInteracted = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setupLocation();
    }
    //endregion

    //region LocationListener Overrides
    @Override
    public void onLocationChanged(Location location) {
        zoomMap(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
    //endregion

    /**
     * Configure location updates and update UI
     */
    private void setupLocation() {
        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                alertUtils.buildAlertMessageNoGps();
            Location location = locationManager.getLastKnownLocation(provider);
            locationManager.requestLocationUpdates(provider, 5000, 0, this);

            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                zoomMap(location);
            }
        } catch(final Exception ex) {
            Log.e("GPSLocationSync", "Error Enabling Location", ex);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertUtils.alertOkDialog(context, "Error Enabling Location", ex.getMessage());
            }});
        }

    }

    /**
     * Connect to telescope
     */
    private void startConnection() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Connecting to telescope...", Toast.LENGTH_SHORT).show();
            }
        });
        try {
            new TcpConnectTask(new TaskListener(), new ConnectionListener(), this.context).execute("");
        } catch (final Exception ex) {
            Log.e("GPSLocationSync", "Telescope Connection Error", ex);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertUtils.alertOkDialog(context, "Telescope Connection Error", ex.getMessage());
                }
            });

        }
    }

    /**
     * Send the current dateTime and GPS location to telescope
     */
    private void sendGPSUpdate()
    {
        try {
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
                    Log.i("GPSLocationSync", "GPS coordinates sent");
                    Toast.makeText(context, "GPS coordinates and current time sent to telescope.", Toast.LENGTH_LONG).show();
                }
            });
        } catch (final Exception ex) {
            Log.e("GPSLocationSync", "GPS Update Error", ex);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertUtils.alertOkDialog(context, "GPS Update Error", ex.getMessage());
                }
            });
        }
    }

    /**
     * Get a map instance from maps fragment manager
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
    }

    /**
     * Center and zoom map, unless user has zoomed, then just center
     *
     * @param location Location to center and zoom to
     */
    private void zoomMap(Location location) {
        if(location != null) {
            LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
            if (!userHasInteracted) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 16));
            } else
                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentPosition));
        }
    }

    /**
     * Present a button to the user for sending GPS updates to telescope
     */
    private void createUpdateButton() {
        if(controlButton == null) {
            controlButton = new Button(this);
            addContentView(controlButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        controlButton.setText("Update GPS");
        controlButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                sendGPSUpdate();
            }
        });
    }

    /**
     * Present a button to the user for creating a connection to telescope
     */
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

    /**
     * Async cleanup/alert UI when closing TCP connection to telescope
     */
    private class TaskListener implements TaskEventHandler {
        public void onTaskCompleted(TCPClient c) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mTcpClient != null) {
                        mTcpClient = null;
                        Log.e("GPSLocationSync", "Closed Connection");
                        Toast.makeText(context, "The telescope connection is closed.", Toast.LENGTH_LONG).show();
                    }
                    createConnectButton();
                }
            });
        }
    }

    /**
     * Async handle connection event/failure for TCP connection to telescope
     */
    private class ConnectionListener implements ConnectionEventHandler {
        @Override
        public void messageReceived(String message) {
            //do nothing with messages
        }

        @Override
        public void connectionEstablished(TCPClient tcpClient) {
            mTcpClient = tcpClient;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "The telescope is successfully connected.", Toast.LENGTH_LONG).show();
                    createUpdateButton();
                }
            });
            Log.i("GPSLocationSync", "Telescope connected");
        }

        @Override
        public void connectionFailed() {
            Log.e("GPSLocationSync", "Telescope Connection Failed");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertUtils.alertOkDialog(context, "Connection Failed", "Unable to connect to the telescope. Please check your connection and settings.");
                }
            });
        }
    }

    // Implement the interface method
    public void onUpdateMapAfterUserInterection() {
        userHasInteracted = true;
    }
}
