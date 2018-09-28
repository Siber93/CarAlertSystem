
package it.siber93.wifidiirectbroadcast;

import android.net.wifi.p2p.WifiP2pDevice;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A structure to hold service information.
 */
public class HumanService {

    // Global data
    boolean obsolate                = false;
    private final ReentrantLock lock = new ReentrantLock();

    // Retrived data
    WifiP2pDevice device;
    String instanceName             = null;
    String serviceRegistrationType  = null;
    double longitude                = 0;
    double latitude                 = 0;
    long timestampPos               = 0;
    double accuracy                 = 0;
    long timestamp                  = 0;
    double bearing                  = 0;
    double speed                    = 0;
    Marker posMarker                = null;

    /**
     * Get the current resource reserved for read/write
     */
    public void Lock()
    {
        lock.lock();
    }
    /**
     * Release the resource
     */
    public void Unlock()
    {
        lock.unlock();
    }

    /**
     * Get the user position on the next n seconds
     * @param sec number of seconds
     * @return [1] logitude, [0] latitude
     * REFERENCE: https://code-examples.net/it/q/283cdf
     */
    LatLng getHumanPositionIn(int sec)
    {
        // Get human position
        double lat_t = latitude;
        double long_t = longitude;
        // Get the bearing (direction)
        double b = bearing;

        // Get current time stamp + sec
        Long t_now = System.currentTimeMillis() + (1000 * sec);
        /*Long t_now = Long.parseLong("1538141083491");
        t_now += 1000 * sec;*/ //DEBUG
        // Get Position time stamp
        Long t = timestampPos;


        // Earth radius
        int R = 6378137;

        // Calculate delta time in seconds
        double delta_s = (t_now-t)/1000.0;
        // Get human speed in m/s
        double s_ms = speed;
        // Calculate max human distance reached in meters
        double d_m = s_ms*delta_s;;

        // Calculate new coordinates
        double lat_now = Math.toDegrees(Math.asin( Math.sin(Math.toRadians(lat_t))*Math.cos(d_m/R) +
                Math.cos(Math.toRadians(lat_t))*Math.sin(d_m/R)*Math.cos(Math.toRadians(b))));
        double long_now = long_t + Math.toDegrees(Math.atan2(Math.sin(Math.toRadians(b))*Math.sin(d_m/R)*Math.cos(Math.toRadians(lat_t)),
                Math.cos(d_m/R)-Math.sin(Math.toRadians(lat_t))*Math.sin(Math.toRadians(lat_now))));

        return new LatLng(lat_now, long_now);
    }
}
