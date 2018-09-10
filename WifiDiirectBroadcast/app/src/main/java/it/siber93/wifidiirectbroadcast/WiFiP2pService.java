
package it.siber93.wifidiirectbroadcast;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A structure to hold service information.
 */
public class WiFiP2pService {

    // Global data
    boolean obsolate                = false;
    private final ReentrantLock lock = new ReentrantLock();

    // Retrived data
    WifiP2pDevice device;
    String instanceName             = null;
    String serviceRegistrationType  = null;
    String longitude                = null;
    String latitude                 = null;
    String timestampPos             = null;
    String accuracy                 = null;
    String timestamp                = null;
    String direction                = null;
    String speed                    = null;


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
     * @return [0] logitude, [1] latitude
     */
    String[] GetHumanPositionIn(int sec)
    {
        return null;
    }
}
