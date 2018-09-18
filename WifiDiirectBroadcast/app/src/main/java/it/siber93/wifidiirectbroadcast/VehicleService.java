package it.siber93.wifidiirectbroadcast;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VehicleService implements LocationListener {

    //region CONSTAMTS
    private static final int WIFI_MAX_RANGE = 60;                                                      // Max wifi covering range
    private static final int LOCATION_OBSOLETE_TIME = 1000 * 5;                                        // Time after that a location must be considered obsolete
    public static final List<LatLng> route = Arrays.asList(
            new LatLng(44.74313,10.58337),
            new LatLng(44.74310,10.58323),
            new LatLng(44.74299,10.58305),
            new LatLng(44.74225,10.58195),
            new LatLng(44.74212,10.58178),
            new LatLng(44.74150,10.58092),
            new LatLng(44.74161,10.58051),
            new LatLng(44.74186,10.57980),
            new LatLng(44.74235,10.57839),
            new LatLng(44.74261,10.57766)
    );                                   // List of all point belonging to the Encoded polyline of the car trip
    //endregion

    //region PROPERTIES
    private Location lastLocationSaved = null;                                                         // Get the location before the current one

    private Location currentLocation = null;                                                           // Current Vehicle location

    public Context context = null;                                                                     // Application context


    private GoogleMap gMap = null;                                                                     // Google map to update when necessary

    private Marker posMarker = null;                                                                   // Marker of the vehicle current position


    // endregion

    //region CONSTRUCTORS

    /**
     * Constructor
     * @param cntx Application context
     */
    public VehicleService(Context cntx, GoogleMap gmap) {
        if (cntx == null || gmap == null) {
            throw new SecurityException("Context is null");
        }
        context = cntx;
        gMap = gmap;
        gmap.addPolyline(new PolylineOptions().addAll(route).color(Color.BLUE).width(10));
        start();
    }

    /**
     * Start the position listening
     */
    public void start()
    {
        //Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            // Check permission
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted


                return;
            }
            // Add listener
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    /**
     * Stop the position listening
     */
    public void stop()
    {
        //Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager != null)
        {
            // Remove listener
            locationManager.removeUpdates(this);
        }
    }
    //endregion

    //region GETTERS


    /**
     * Get last known position of the vehicle
     * @return LatLng Object
     */
    public LatLng getCurrentPosition() {
        //return new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        return new LatLng(44.742139, 10.578990);
    }


    /**
     * Get last saved position of the vehicle
     * @return LatLng Object
     */
    public LatLng getLastSavedPosition() {
        return new LatLng(lastLocationSaved.getLatitude(),lastLocationSaved.getLongitude());
    }



    /**
     * Calculate the exact polyline segment where the vehicle is going on
     * @return First Polyline segment point position
     */
    public int getCurrentPolylineSegment() {
        LatLng loc = getCurrentPosition();
        double minD = getPointSegmentProjectionDistance(route.get(0),route.get(1),loc);
        int index = 0;

        for(int i = 1; i < route.size()-1; i++ )
        {
            double d = getPointSegmentProjectionDistance(route.get(i),route.get(i+1),loc);
            if(d<minD)
            {
                index = i;
                minD=d;
            }
        }

        return index;
    }

    //endregion

    //region SPEED_CALCULATION

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
                double dist = getLocationsDistance(getLastSavedPosition(),getCurrentPosition());

                // Calculate speed
                double time_s = (currentLocation.getTime() - lastLocationSaved.getTime()) / 1000.0;
                return dist / time_s;
            }
        }
        //return 0; TODO ripristinare
        return 15;
    }


    //endregion


    //region DISTANCE

    /**
     * Get the distance between 2 coordinates points
     * @param l1 Point 1
     * @param l2 Point 2
     * @return Distance in meters
     */
    public double getLocationsDistance(LatLng l1,LatLng l2)
    {
        // Convert degrees to radians
        double lat1 = l1.latitude * Math.PI / 180.0;
        double lon1 = l1.longitude * Math.PI / 180.0;

        double lat2 = l2.latitude * Math.PI / 180.0;
        double lon2 = l2.longitude * Math.PI / 180.0;

        // radius of earth in metres
        double r = 6378137;

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
        return r * theta;
    }



    /**
     * Given a segment and a point this function return the distance point-segment if the point is
     * on the segment, otherwise the distance to the nearest edge of the segment.
     * REFERENCE https://stackoverflow.com/questions/1299567/how-to-calculate-distance-from-a-point-to-a-line-segment-on-a-sphere
     * @param s1 First segment point
     * @param s2 Second segment point
     * @param p Point tha must be projected
     * @return point-segment distance
     */
    public double getPointSegmentProjectionDistance(LatLng s1, LatLng s2, LatLng p)
    {
        // Earth radius in km
        double R = 6378.137;

        // minimum onSegment Error
        double Precision = 1.0;


        // Convert coordinates to cartesian
        double a[] = {
                R * Math.cos(Math.toRadians(s1.latitude)) * Math.cos(Math.toRadians(s1.longitude)),
                R * Math.cos(Math.toRadians(s1.latitude)) * Math.sin(Math.toRadians(s1.longitude)),
                R * Math.sin(Math.toRadians(s1.latitude))
        };
        double b[] = {
                R * Math.cos(Math.toRadians(s2.latitude)) * Math.cos(Math.toRadians(s2.longitude)),
                R * Math.cos(Math.toRadians(s2.latitude)) * Math.sin(Math.toRadians(s2.longitude)),
                R * Math.sin(Math.toRadians(s2.latitude))
        };
        double c[] = {
                R * Math.cos(Math.toRadians(p.latitude)) * Math.cos(Math.toRadians(p.longitude)),
                R * Math.cos(Math.toRadians(p.latitude)) * Math.sin(Math.toRadians(p.longitude)),
                R * Math.sin(Math.toRadians(p.latitude))
        };

        // Create vectors
        Vector A = new Vector(a);
        Vector B = new Vector(b);
        Vector C = new Vector(c);

        // Calculate T, the point on the line AB that is nearest to C, using the following 3 vector products
        Vector G = new Vector(
                new double[] {
                        A.cartesian(1)*B.cartesian(2)-A.cartesian(2)*B.cartesian(1),
                        A.cartesian(2)*B.cartesian(0)-A.cartesian(0)*B.cartesian(2),
                        A.cartesian(0)*B.cartesian(1)-A.cartesian(1)*B.cartesian(0)
                });

        Vector F = new Vector(
                new double[] {
                        C.cartesian(1)*G.cartesian(2)-C.cartesian(2)*G.cartesian(1),
                        C.cartesian(2)*G.cartesian(0)-C.cartesian(0)*G.cartesian(2),
                        C.cartesian(0)*G.cartesian(1)-C.cartesian(1)*G.cartesian(0)
                });

        Vector T = new Vector(
                new double[] {
                        G.cartesian(1)*F.cartesian(2)-G.cartesian(2)*F.cartesian(1),
                        G.cartesian(2)*F.cartesian(0)-G.cartesian(0)*F.cartesian(2),
                        G.cartesian(0)*F.cartesian(1)-G.cartesian(1)*F.cartesian(0)
                });


        // Normalize T
        /*Vector T_N = new Vector(
                new double[] {
                        T.cartesian(0)/T.magnitude(),
                        T.cartesian(1)/T.magnitude(),
                        T.cartesian(0)/T.magnitude()
                });*/
        Vector T_N = T.normalize();

        // Multiply by R
        Vector T_S = T_N.scale(R);

        // Convert T back to longitude\latitude.
        LatLng new_p = new LatLng(
                Math.toDegrees(Math.asin(T_S.cartesian(2) / R)),
                Math.toDegrees(Math.atan2(T_S.cartesian(1),T_S.cartesian(0)))
        );



        // check if the new point is on the segment A-B
        if(Math.abs(getLocationsDistance(s1,s2)-getLocationsDistance(s1,new_p)-getLocationsDistance(s2,new_p))<Precision)
        {
            // Point on the segment
            return getLocationsDistance(new_p,p);
        }
        else {
            // Point out of the segment
            // Give back the distance to the nearest edge of the segment
            return Math.abs(
                    Math.min(
                            getLocationsDistance(s2,p),
                            getLocationsDistance(s1,p)
                    )
            );
        }
    }

    //endregion


    //region INTERSECTION_CHECK

    /**
     * Check if the human will intersect the vehicle
     * @param h human service Object
     * @return True -> Intersection
     */
    public boolean willHumanIntersectVehicle(HumanService h) {
        LatLng loc = getCurrentPosition();
        int start = getCurrentPolylineSegment();
        int end = start;
        // Search indexes on which check intersection
        for(int i = start; i < route.size()-1; i++)
        {
            double d = getPointSegmentProjectionDistance(route.get(i),route.get(i+1),loc);
            if(d < WIFI_MAX_RANGE)
            {
                end = i+1;
            }else{
                break;
            }
        }
        if(end ==  start)
        {
            // All segments are inside 60 meters range
            end = route.size()-1;
        }

        // Intersection check
        for(int i = start; i < end; i++)
        {
            if(doIntersect(
                    route.get(i),                                                               // P1 of the vehicle
                    route.get(i+1),                                                             // P2 of the vehicle
                    new LatLng(h.latitude, h.longitude),                                        // P1 of the human
                    h.getHumanPositionIn((int) Math.ceil(WIFI_MAX_RANGE / getCurrentSpeed()))  // P2 of the human (estimated)
            ))
            {
                return true;
            }
        }
        return false;       // No intersections
    }




    // Check if two given line segments intersect

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

    /**
     * http://www.edwilliams.org/intersect.htm
     * @param p1
     * @param q1
     * @param p2
     * @param q2
     * @return
     */
    private boolean doIntersect2(LatLng p1, LatLng q1, LatLng p2, LatLng q2)
    {
        // TODO provare nel caso
        Vector e1xe2 =new Vector( new double[]
            {
                    Math.sin(Math.toRadians(p1.latitude - q1.latitude)) * Math.sin(Math.toRadians((p1.longitude + q1.longitude) / 2))
                            * Math.cos(Math.toRadians((p1.longitude - q1.longitude) / 2))  - Math.sin(Math.toRadians(p1.latitude + q1.latitude))
                            * Math.cos(Math.toRadians((p1.longitude + q1.longitude) / 2)) * Math.sin(Math.toRadians((p1.longitude - q1.longitude) / 2)),
                    Math.sin(Math.toRadians(p1.latitude - q1.latitude)) * Math.cos(Math.toRadians((p1.longitude + q1.longitude) / 2)) * Math.cos(Math.toRadians((p1.longitude - q1.longitude) / 2)) +
                            Math.sin(Math.toRadians(p1.latitude + q1.latitude)) * Math.sin(Math.toRadians((p1.longitude + q1.longitude) / 2)) * Math.sin(Math.toRadians((p1.longitude - q1.longitude) / 2)),
                    Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(q1.latitude)) * Math.sin(Math.toRadians(p1.longitude - q1.longitude))
            });


        Vector e3xe4 = new Vector(new double[]
                {
                        Math.sin(Math.toRadians(p2.latitude - q2.latitude)) * Math.sin(Math.toRadians((p2.longitude + q2.longitude) / 2))
                                * Math.cos(Math.toRadians((p2.longitude - q2.longitude) / 2))  - Math.sin(Math.toRadians(p2.latitude + q2.latitude))
                                * Math.cos(Math.toRadians((p2.longitude + q2.longitude) / 2)) * Math.sin(Math.toRadians((p2.longitude - q2.longitude) / 2)),
                        Math.sin(Math.toRadians(p2.latitude - q2.latitude)) * Math.cos(Math.toRadians((p2.longitude + q2.longitude) / 2)) * Math.cos(Math.toRadians((p2.longitude - q2.longitude) / 2)) +
                                Math.sin(Math.toRadians(p2.latitude + q2.latitude)) * Math.sin(Math.toRadians((p2.longitude + q2.longitude) / 2)) * Math.sin(Math.toRadians((p2.longitude - q2.longitude) / 2)),
                        Math.cos(Math.toRadians(p2.latitude)) * Math.cos(Math.toRadians(q2.latitude)) * Math.sin(Math.toRadians(p2.longitude - q2.longitude))
                });
        Vector ea = e1xe2.normalize();
        Vector eb = e3xe4.normalize();


        Vector eaxeb = new Vector(new double[]{ea.cartesian(1)*eb.cartesian(2) -eb.cartesian(1) *ea.cartesian(2), ea.cartesian(2) *eb.cartesian(0) -eb.cartesian(2) *ea.cartesian(0), ea.cartesian(0) *eb.cartesian(1) -ea.cartesian(1) *eb.cartesian(0)});

        double lat = Math.toDegrees(Math.atan2(eaxeb.cartesian(2), Math.sqrt(eaxeb.cartesian(0)*eaxeb.cartesian(0) + eaxeb.cartesian(1)*eaxeb.cartesian(1))));
        double lon = Math.toDegrees(Math.atan2(eaxeb.cartesian(1), eaxeb.cartesian(0)));
        return true;
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

    //endregion


    //region LOCATION_LISTENER
    @Override
    public void onLocationChanged(Location location) {

        if(currentLocation == null)
        {
            currentLocation = location;
            lastLocationSaved = location;
            // Change Position in the map
            if(posMarker == null)
            {
                posMarker = gMap.addMarker(new MarkerOptions()
                        .position(getCurrentPosition())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                        .draggable(false));
            }else{
                posMarker.setPosition(getCurrentPosition());
            }
            gMap.moveCamera(CameraUpdateFactory.newLatLng(getCurrentPosition()));
        }

        // Check if the new location is better then older one
        if(isBetterLocation(location,currentLocation))
        {
            // TODO Man mano che vado avanti con la posizione cancello i segmenti di polyline vecchi
            lastLocationSaved = currentLocation;
            currentLocation = location;
            // Change Position in the map
            if(posMarker == null)
            {
                posMarker = gMap.addMarker(new MarkerOptions()
                        .position(getCurrentPosition())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                        .draggable(false));
            }else{
                posMarker.setPosition(getCurrentPosition());
            }
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

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

    //endregion

}
