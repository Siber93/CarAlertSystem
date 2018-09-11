package it.siber93.wifidiirectbroadcast;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class VehicleService implements LocationListener {

    private static final int WIFI_MAX_RANGE = 60;                                                      // Max wifi covering range
    private static final int LOCATION_OBSOLETE_TIME = 1000 * 5;                                        // Time after that a location must be considered obsolete

    public static ArrayList<LatLng> route = new ArrayList<>();                                         // List of all point belonging to the Encoded polyline of the car trip

    private Location lastLocationSaved = null;                                                         // Get the location before the current one

    private Location currentLocation = null;                                                           // Current Vehicle location

    private Context context = null;                                                                    // Application context

    /**
     * Constructor
     * @param cntx Application context
     */
    public VehicleService(Context cntx) {
        if (context == null) {
            throw new SecurityException("Context is null");
        }
        context = cntx;
    }


    /**
     * Get the last know speed of this vehicle
     * @return value in m/s
     */
    public double getCurrentSpeed() {
        // TODO: Verificare
        if(currentLocation!= null)
        {
            if(currentLocation.hasSpeed())
            {
                return currentLocation.getSpeed();
            }else{
                // Convert degrees to radians
                double lat1 = lastLocationSaved.getLatitude() * Math.PI / 180.0;
                double lon1 = lastLocationSaved.getLongitude() * Math.PI / 180.0;

                double lat2 = currentLocation.getLatitude() * Math.PI / 180.0;
                double lon2 = currentLocation.getLongitude() * Math.PI / 180.0;

                // radius of earth in metres
                double r = 6378100;

                // P
                double rho1 = r * Math.cos(lat1);
                double z1 = r * Math.sin(lat1);
                double x1 = rho1 * Math.cos(lon1);
                double y1 = rho1 * Math.sin(lon1);

                // Q
                double rho2 = r * Math.cos(lat2);
                double z2 = r * Math.sin(lat2);
                double x2 = rho2 * Math.cos(lon2);
                double y2 = rho2 * Math.sin(lon2);

                // Dot product
                double dot = (x1 * x2 + y1 * y2 + z1 * z2);
                double cos_theta = dot / (r * r);

                double theta = Math.acos(cos_theta);

                // Distance in Metres
                double dist = r * theta;

                // Calculate speed
                double time_s = (currentLocation.getTime() - lastLocationSaved.getTime()) / 1000.0;
                return dist / time_s;
            }
        }
        return 0;
    }


    /**
     * Get last known position of the vehicle
     * @return LatLng Object
     */
    public LatLng getCurrentPosition() {
        return new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
    }


    /**
     * Calculate the exact polyline segment where the vehicle is going on
     * @return 2 LatLng Objects
     */
    public ArrayList<LatLng> getCurrentPolylineSegment() {
        // TODO: Implementare
        return null;
    }

    /**
     * Check if the human will intersect the vehicle
     * @param h human service Object
     * @return True -> Intersection
     */
    public boolean willHumanIntersectVehicle(HumanService h) {

        // Get current segment points (exactly 2)
        ArrayList<LatLng> currentSegment = getCurrentPolylineSegment();
        if (currentSegment.size() >= 2) {
            doIntersect(
                    currentSegment.get(0),                                                  // P1 of the vehicle
                    currentSegment.get(1),                                                  // P2 of the vehicle
                    new LatLng(h.latitude, h.longitude),                                     // P1 of the human
                    h.getHumanPositionIn((int) Math.ceil(WIFI_MAX_RANGE / getCurrentSpeed()))  // P2 of the human (estimated)
            );
        }

        return false;
    }


    // A C++ program to check if two given line segments intersect

    // Given three colinear points p, q, r, the function checks if
    // point q lies on line segment 'pr'
    private boolean onSegment(LatLng p, LatLng q, LatLng r) {
        if (q.latitude <= Math.max(p.latitude, r.latitude) && q.latitude >= Math.min(p.latitude, r.latitude) &&
                q.longitude <= Math.max(p.longitude, r.longitude) && q.longitude >= Math.min(p.longitude, r.longitude))
            return true;

        return false;
    }

    // To find orientation of ordered triplet (p, q, r).
    // The function returns following values
    // 0 --> p, q and r are colinear
    // 1 --> Clockwise
    // 2 --> Counterclockwise
    private int orientation(LatLng p, LatLng q, LatLng r) {
        // See https://www.geeksforgeeks.org/orientation-3-ordered-points/
        // for details of below formula.
        double val = (q.longitude - p.longitude) * (r.latitude - q.latitude) -
                (q.latitude - p.latitude) * (r.longitude - q.longitude);

        if (val == 0) return 0;  // colinear

        return (val > 0) ? 1 : 2; // clock or counterclock wise
    }

    // The main function that returns true if line segment 'p1q1'
    // and 'p2q2' intersect.
    private boolean doIntersect(LatLng p1, LatLng q1, LatLng p2, LatLng q2) {
        // Find the four orientations needed for general and
        // special cases
        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        // General case
        if (o1 != o2 && o3 != o4)
            return true;

        // Special Cases
        // p1, q1 and p2 are colinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1, p2, q1)) return true;

        // p1, q1 and q2 are colinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1, q2, q1)) return true;

        // p2, q2 and p1 are colinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2, p1, q2)) return true;

        // p2, q2 and q1 are colinear and q1 lies on segment p2q2
        if (o4 == 0 && onSegment(p2, q1, q2)) return true;

        return false; // Doesn't fall in any of the above cases
    }


    @Override
    public void onLocationChanged(Location location) {

        if(currentLocation == null)
        {
            currentLocation = location;
            lastLocationSaved = location;
        }

        // Check if the new location is better then older one
        if(isBetterLocation(location,currentLocation))
        {
            lastLocationSaved = currentLocation;
            currentLocation = location;
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
        //Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            // Check permission
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            // Add listener
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    @Override
    public void onProviderDisabled(String s) {
        //Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager != null)
        {
            // Remove listener
            locationManager.removeUpdates(this);
        }
    }


    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > LOCATION_OBSOLETE_TIME;
        boolean isSignificantlyOlder = timeDelta < -LOCATION_OBSOLETE_TIME;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }



}
