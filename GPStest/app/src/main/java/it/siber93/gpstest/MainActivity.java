package it.siber93.gpstest;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.location.LocationListener;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }


    long startTime = 0;
    // Acquire a reference to the system Location Manager
    LocationManager locationManager;

    // Get the current location a print it in the view
    public void getCurrentLocation(View view) {
        startTime = System.nanoTime();

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                long endTime = System.nanoTime();

                try
                {
                    if(locationManager!= null)
                    {
                        Location l = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        TextView locationTxt = (TextView) findViewById(R.id.locationTxt);
                        locationTxt.setText("Lat " + l.getLatitude() + "\n" + "Lon " + l.getLongitude());

                        TextView timeTxt = (TextView) findViewById(R.id.timeTxt);
                        timeTxt.setText((endTime-startTime)/Math.pow(10,6) + " ns");

                        Log.d("DEBUG", "DONE");

                    }
                }
                catch (SecurityException e)
                {
                    Log.d("DEBUG", e.getMessage());
                }

                locationManager.removeUpdates(this);

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        try
        {
            // Register the listener with the Location Manager to receive location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
        catch (SecurityException e)
        {
            Log.d("DEBUG", e.getMessage());
        }



        Location l;

    }


}
