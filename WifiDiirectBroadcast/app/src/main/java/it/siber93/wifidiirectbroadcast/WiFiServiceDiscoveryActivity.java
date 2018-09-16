package it.siber93.wifidiirectbroadcast;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;



public class WiFiServiceDiscoveryActivity extends AppCompatActivity implements OnMapReadyCallback {


    /**
     * Software modules enablers
     */
    public boolean VEHICLE_MODULE                                       = false;
    public boolean HUMAN_MODULE                                         = false;



    public static final String TAG                                      = "wifidirectdemo";

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_SERVICE_INSTANCE          = "service";            // Service exposed in the TxtMap in order to improve discovery speed
    public static final String TXTRECORD_PROP_AVAILABLE                 = "available";          // Reserved field
    public static final String TXTRECORD_PROP_LONG                      = "longitude";          // Last know longitude
    public static final String TXTRECORD_PROP_LAT                       = "latitude";           // Last know latitude
    public static final String TXTRECORD_PROP_TIMESTAMP_POS             = "timestamp_pos";      // Timestamp on which the position has been retrieved
    public static final String TXTRECORD_PROP_ACCURACY                  = "accuracy";           // Position accuracy: each time the position estimation occurs the accuracy decrease. On GPS realignment it comes back to the maximum value
    public static final String TXTRECORD_PROP_TIMESTAMP                 = "timestamp";          // Timestamp before start the notification. It is used to compare age of position (timestamp - timestamp_pos)
    public static final String TXTRECORD_PROP_BEARING                   = "direction";          // Current human direction (angle from the North)
    public static final String TXTRECORD_PROP_SPEED                     = "speed";              // Current human speed (m/s)

    public static final String SERVICE_INSTANCE                         = "_humanpresence";
    public static final String SERVICE_REG_TYPE                         = "_presence._tcp";


    public ArrayList<HumanService> humans_discovered                  = new ArrayList<HumanService>();   // List of all devices recently discovered that must be processed
    //public ArrayList<HumanService> humans_reported                    = new ArrayList<HumanService>();   // List of all devices that have been reported in the map


    /**
     * Number that should be auto-incremented each time is used
     */
    public int AUTOINCREMENT_NUMBER                                     = 0;

    //public static final int MESSAGE_READ                                = 0x400 + 1;
    //public static final int MY_HANDLE                                   = 0x400 + 2;
    public static final int PUBLISH_TIME                                = 10000;
    public static final int DISCOVERY_TIME                              = 10000;

    private WifiP2pManager manager;


    //static final int SERVER_PORT                                        = 4545;

    private final IntentFilter intentFilter                             = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver                                  = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    WifiP2pDnsSdServiceInfo service;


    VehicleService vServ;                                                                       // Manager of the vehicle data
    HumanLocalService hServ;                                                                    // Manager of the local human data


    //private Handler handler = new Handler(this);
    //private WiFiChatFragment chatFragment;
    /**
     * List of all the discovered device
     */
    //private WiFiDirectServicesList servicesList;

    /**
     * TextView of the graphic log
     */
    private TextView statusTxtView;

    /**
     * Switch for vehicle mode
     */
    private Switch swv;
    /**
     * Switch for human mode
     */
    private Switch swh;


    /**
     * View of the graphic map
     */
    private MapView map;

    private GoogleMap gmap;

    /*public Handler getHandler() {
        return handler;
    }*/

    /*public void setHandler(Handler handler) {
        this.handler = handler;
    }*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        statusTxtView = (TextView) findViewById(R.id.status_text);

        // Map view initialization
        map = (MapView) findViewById(R.id.mapView) ;
        map.onCreate(null);
        map.onResume();
        map.getMapAsync(this);


        // Human switch button initialization
        swh = (Switch) findViewById(R.id.switchV);
        swv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                HUMAN_MODULE = b;
                if (b) {
                    // Start publishing + discovery
                    // Create service manager
                    hServ = new HumanLocalService();
                    // TODO start position management
                    // Publish this device on the network sending beacon
                    serviceBroadcastRunnable.run();
                    // Initiates callbacks for service discovery
                    prepareServiceDiscovery();
                    startServiceDiscovery();

                }
                else{
                    // Disable switch for few seconds
                    compoundButton.setEnabled(false);
                }
            }
        });


        // Vehicle switch button initialization
        swv = (Switch) findViewById(R.id.switchV);
        swv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                VEHICLE_MODULE = b;
                if(b)
                {
                    // Check if discovery service can be enabled now
                    // Check if wifi is enabled
                    WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (!wifi.isWifiEnabled()){
                        // Ask for enabling it
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                getApplicationContext());
                        builder.setTitle("Save the Humanity");
                        builder.setMessage("The Wifi service is off. Do you want to turn it on?");
                        builder.setPositiveButton("Enable Wifi",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            final DialogInterface dialogInterface,
                                            final int i) {
                                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                    }
                                });
                        builder.setNegativeButton("Exit",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        System.exit(0);
                                    }
                                });
                        builder.show();
                    }else{

                        // Create vehicle manager
                        vServ = new VehicleService(getApplicationContext(), gmap);

                        // Initiates callbacks for service discovery
                        prepareServiceDiscovery();
                        startServiceDiscovery();

                    }
                }
                else{
                    // Disable switch for few seconds
                    compoundButton.setEnabled(false);
                    // Stop listening position
                    vServ.stop();
                    vServ = null;
                }
            }
        });





        // Check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?

            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }


        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Get the Wifi Direct manager
        manager = (WifiP2pManager) getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);

        // Get an initialized channel where to communicate
        channel = manager.initialize(this, getMainLooper(), null);

    }

    /**
     * Handler for the thread/runnable of broadcasting
     */
    private Handler serviceBroadcastingHandler = new Handler();


    /**
     * Runnable that removes all the old services and registers a new one with custom data
     * Recursive thread (5 sec tick)
     */
    private Runnable serviceBroadcastRunnable = new Runnable() {
        @Override
        public void run() {
            // Clear all the old services
            manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    // Give a feedback
                    Log.d("Publish","[P] Services cleared");

                    // Create the data that must be published
                    Map<String, String> record = new HashMap<String, String>();
                    record.put(TXTRECORD_PROP_SERVICE_INSTANCE, SERVICE_INSTANCE);
                    record.put(TXTRECORD_PROP_AVAILABLE, "visible");
                    record.put(TXTRECORD_PROP_LONG, String.valueOf(hServ.getCurrentLongitude()));
                    record.put(TXTRECORD_PROP_LAT, String.valueOf(hServ.getCurrentLatitude()));
                    record.put(TXTRECORD_PROP_ACCURACY, String.valueOf(hServ.getAccuracy()));
                    record.put(TXTRECORD_PROP_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
                    record.put(TXTRECORD_PROP_TIMESTAMP_POS, String.valueOf(hServ.getCurrentPositionTimeStamp()));
                    record.put(TXTRECORD_PROP_BEARING, String.valueOf(hServ.getCurrentBearing()));
                    record.put(TXTRECORD_PROP_SPEED, String.valueOf(hServ.getCurrentspeed()));

                    service = WifiP2pDnsSdServiceInfo.newInstance(
                            SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
                    manager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            // Give a feedback
                            Log.d("Publish","[P] Added Local Service");
                            // Relaunch service broadcasting
                            serviceBroadcastingHandler.postDelayed(
                                    serviceBroadcastRunnable,
                                    PUBLISH_TIME);
                        }

                        @Override
                        public void onFailure(int error) {
                            appendStatus("[P-Err] Failed to add a service");
                        }
                    });
                }

                @Override
                public void onFailure(int i) {
                    // Error, give a feedback
                    appendStatus("[P-Err] Error on services cleaning");
                }
            });
        }
    };



    /**
     * Initialize callbacks for service discovery
     */
    private void prepareServiceDiscovery() {

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
        // Only in vehicle mode the callbacks for human discovery are enabled
        if(VEHICLE_MODULE) {
            manager.setDnsSdResponseListeners(channel,
                    new WifiP2pManager.DnsSdServiceResponseListener() {

                        @Override
                        public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {


                        }
                    }, new WifiP2pManager.DnsSdTxtRecordListener() {

                        /**
                         * A new TXT record is available. Pick up the advertised
                         * buddy name.
                         */
                        @Override
                        public void onDnsSdTxtRecordAvailable(
                                String fullDomainName, Map<String, String> record,
                                WifiP2pDevice device) {


                            // A service has been discovered. Is this our app?
                            if (record.containsKey(TXTRECORD_PROP_SERVICE_INSTANCE) &&
                                    record.get(TXTRECORD_PROP_SERVICE_INSTANCE).equalsIgnoreCase(SERVICE_INSTANCE)) {

                                // update the UI and add the item the discovered
                                // device.
                           /*WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                                    .findFragmentByTag("services");
                            if (fragment != null) {
                                WiFiDirectServicesList.WiFiDevicesAdapter adapter = ((WiFiDirectServicesList.WiFiDevicesAdapter) fragment
                                        .getListAdapter());*/
                                boolean found = false;

                                // Search if this device with this service already exists in the list of the discovered
                                for (int i = 0; i < humans_discovered.size(); i++) {
                                    // Block the resources in order to read it values
                                    humans_discovered.get(i).Lock();
                                    // Compare MAC address
                                    if (humans_discovered.get(i).device.deviceAddress.equalsIgnoreCase(device.deviceAddress)) {
                                        // Update values for this entity
                                        humans_discovered.get(i).device = device;
                                        humans_discovered.get(i).instanceName = record.get(TXTRECORD_PROP_SERVICE_INSTANCE);
                                        humans_discovered.get(i).accuracy = Double.parseDouble(record.get(TXTRECORD_PROP_ACCURACY));
                                        humans_discovered.get(i).bearing = Double.parseDouble(record.get(TXTRECORD_PROP_BEARING));
                                        humans_discovered.get(i).longitude = Double.parseDouble(record.get(TXTRECORD_PROP_LONG));
                                        humans_discovered.get(i).latitude = Double.parseDouble(record.get(TXTRECORD_PROP_LAT));
                                        humans_discovered.get(i).speed = Double.parseDouble(record.get(TXTRECORD_PROP_SPEED));
                                        humans_discovered.get(i).timestamp = Long.parseLong(record.get(TXTRECORD_PROP_TIMESTAMP));
                                        humans_discovered.get(i).timestampPos = Long.parseLong(record.get(TXTRECORD_PROP_TIMESTAMP_POS));
                                        found = true;
                                        humans_discovered.get(i).posMarker.setPosition(humans_discovered.get(i).getHumanPositionIn(1));
                                        humans_discovered.get(i).Unlock();
                                        break;
                                    }
                                    humans_discovered.get(i).Unlock();
                                }
                                // Search if this device with this service already exists in the list of the reported
                            /*for(int i = 0; i < humans_reported.size(); i++)
                            {
                                // Block the resources in order to read it values
                                humans_reported.get(i).Lock();
                                // Compare MAC address
                                if(humans_reported.get(i).device.deviceAddress.equalsIgnoreCase(device.deviceAddress) )
                                {
                                    // Delete it and the report it again
                                    humans_reported.get(i).obsolate  = true;
                                    humans_reported.get(i).Unlock();
                                    break;
                                }
                                humans_reported.get(i).Unlock();
                            }*/
                                if (!found) {
                                    // If not found, create it and add it to the discovered list
                                    HumanService service = new HumanService();

                                    service.device = device;
                                    service.instanceName = record.get(TXTRECORD_PROP_SERVICE_INSTANCE);
                                    service.accuracy = Double.parseDouble(record.get(TXTRECORD_PROP_ACCURACY));
                                    service.bearing = Double.parseDouble(record.get(TXTRECORD_PROP_BEARING));
                                    service.longitude = Double.parseDouble(record.get(TXTRECORD_PROP_LONG));
                                    service.latitude = Double.parseDouble(record.get(TXTRECORD_PROP_LAT));
                                    service.speed = Double.parseDouble(record.get(TXTRECORD_PROP_SPEED));
                                    service.timestamp = Long.parseLong(record.get(TXTRECORD_PROP_TIMESTAMP));
                                    service.timestampPos = Long.parseLong(record.get(TXTRECORD_PROP_TIMESTAMP_POS));

                                    service.posMarker = gmap.addMarker(new MarkerOptions()
                                            .position(service.getHumanPositionIn(3))
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.man))
                                            .draggable(false));
                                    appendStatus("[D] Human found");
                                    humans_discovered.add(service);
                                    //adapter.add(service);
                                }
                                //adapter.notifyDataSetChanged();
                                //}
                            }
                        }
                    });
        }
        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
    }

    /**
     * Handler for the service discovery
     */
    private Handler serviceDiscoveringHandler = new Handler();



    /**
     * Runnable for the service discovery
     */
    private Runnable serviceDiscoveringRunnable = new Runnable() {
        @Override
        public void run() {
            startServiceDiscovery();
        }
    };

    /**
     * Start discovery of nearby devices every 5 sec
     */
    private void startServiceDiscovery() {
        // Removing older service request
        manager.removeServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("Discovery","[D] Service request removing completed");
                        if(!VEHICLE_MODULE && !HUMAN_MODULE) {
                            // Disable service discovery
                            appendStatus("[D] Vehicle mode stopped");
                            if(swv != null)
                            {
                                // Enable switch button again
                                swv.setEnabled(true);
                            }

                            return;
                        }
                        if(!HUMAN_MODULE) {
                            // Disable service discovery
                            appendStatus("[D] Human mode stopped");
                            if(swh != null)
                            {
                                // Enable switch button again
                                swh.setEnabled(true);
                            }

                            return;
                        }
                        // Creating new service request to broadcast
                        manager.addServiceRequest(channel, serviceRequest,
                                new WifiP2pManager.ActionListener() {

                                    @Override
                                    public void onSuccess() {
                                        Log.d("Discovery","[D] Added service discovery request");
                                        // Try to broadcast the new service request
                                        manager.discoverServices(channel,
                                                new WifiP2pManager.ActionListener() {
                                                    @Override
                                                    public void onSuccess() {
                                                        Log.d("Discovery","[D] Service discovery initiated");
                                                        serviceDiscoveringHandler.postDelayed(
                                                                serviceDiscoveringRunnable,
                                                                DISCOVERY_TIME);
                                                    }

                                                    @Override
                                                    public void onFailure(int arg0) {
                                                        appendStatus("[D-Err] Service discovery failed");

                                                    }
                                                }
                                        );
                                    }

                                    @Override
                                    public void onFailure(int arg0) {
                                        appendStatus("[D-Err] Failed adding service discovery request");
                                    }
                                });
                    }

                    @Override
                    public void onFailure(int i) {
                        appendStatus("[D-Err] Service request removing failed");
                    }
                });
    }

    /**
     * Handler for the thread/runnable of humans lists cleaning
     */
    private Handler cleaningThreadHandler = new Handler();


    /**
     * Runnable that removes all the old humans data from the lists
     * Recursive thread (5 sec tick)
     */
    private Runnable cleaningThreadRunnable = new Runnable() {
        @Override
        public void run() {

        }
    };





    /*@Override
    public void connectP2p(HumanService service) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if (serviceRequest != null)
            manager.removeServiceRequest(channel, serviceRequest,
                    new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int arg0) {
                        }
                    });

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Connecting to service");
            }

            @Override
            public void onFailure(int errorCode) {
                appendStatus("Failed connecting to service");
            }
        });
    }*/

    /*@Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, readMessage);
                (chatFragment).pushMessage("Buddy: " + readMessage);
                break;

            case MY_HANDLE:
                Object obj = msg.obj;
                (chatFragment).setChatManager((ChatManager) obj);

        }
        return true;
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        map.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        map.onStop();
    }
    @Override
    protected void onPause() {
        map.onPause();
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        map.onDestroy();
        super.onDestroy();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        map.onLowMemory();
    }


    /*
     * The group owner accepts connections using a server socket and then spawns a
     * client socket for every client. This is handled by {@code
     * GroupOwnerSocketHandler}
     */
    /*@Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;

        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(
                        ((WiFiChatFragment.MessageTarget) this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            handler = new ClientSocketHandler(
                    ((WiFiChatFragment.MessageTarget) this).getHandler(),
                    p2pInfo.groupOwnerAddress);
            handler.start();
        }
        chatFragment = new WiFiChatFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.container_root, chatFragment).commit();
        statusTxtView.setVisibility(View.GONE);
    }*/

    /**
     * Append a line of text to the head of the displayed log in the main activity
     * @param status Line to append
     */
    public void appendStatus(String status) {
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(status + "\n" + current);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        gmap.setMinZoomPreference(12);
        gmap.getUiSettings().setZoomGesturesEnabled(true);
        if(swv != null)
        {
            swv.setEnabled(true);
        }
    }
}
