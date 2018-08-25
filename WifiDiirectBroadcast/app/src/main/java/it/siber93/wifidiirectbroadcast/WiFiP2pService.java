
package it.siber93.wifidiirectbroadcast;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * A structure to hold service information.
 */
public class WiFiP2pService {
    WifiP2pDevice device;
    String instanceName             = null;
    String serviceRegistrationType  = null;
    String position                 = null;
    String timestampPos             = null;
    String accuracy                 = null;
    String timestamp                = null;
    String direction                = null;
    String speed                    = null;
}
