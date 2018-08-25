package it.siber93.wifidiirectbroadcast;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class WiFiServiceDiscoveryActivity extends AppCompatActivity implements
        WiFiDirectServicesList.DeviceClickListener, Handler.Callback, WiFiChatFragment.MessageTarget,
        WifiP2pManager.ConnectionInfoListener {


    // Software modules enablers
    public static final boolean LISTENER_MODULE = true;
    public static final boolean PUBLISHER_MODULE = true;

    /**
     * Indicates when the app is looking for other p2p devices
     */
    public boolean isServiceDiscoveryActive = false;
    /**
     * Indicates if the local service is published on the network
     */
    public boolean isServiceBroadcasting = false;

    public static final String TAG = "wifidirectdemo";

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_SERVICE_INSTANCE          = "service";
    public static final String TXTRECORD_PROP_AVAILABLE                 = "available";
    public static final String TXTRECORD_PROP_POSITION                  = "position";
    public static final String TXTRECORD_PROP_TIMESTAMP_POS             = "timestamp_pos";
    public static final String TXTRECORD_PROP_ACCURACY                  = "accuracy";
    public static final String TXTRECORD_PROP_TIMESTAMP                 = "timestamp";
    public static final String TXTRECORD_PROP_DIRECTION                 = "direction";
    public static final String TXTRECORD_PROP_SPEED                     = "speed";

    public static final String SERVICE_INSTANCE                         = "_humanpresence";
    public static final String SERVICE_REG_TYPE                         = "_presence._tcp";
    public int AUTOINCREMENT_NUMBER = 0;

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private WifiP2pManager manager;

    static final int SERVER_PORT = 4545;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    WifiP2pDnsSdServiceInfo service;

    private Handler handler = new Handler(this);
    private WiFiChatFragment chatFragment;
    private WiFiDirectServicesList servicesList;

    private TextView statusTxtView;

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Start/Stop discovering P2P devices", Snackbar.LENGTH_LONG)
                        .setAction(
                                (isServiceDiscoveryActive ? "Stop discovering": "Start discovering"),
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        /*if(LISTENER_MODULE) {
                                            // Check if the discovery is already running
                                            if (!isServiceDiscoveryActive) {
                                                // Check for other P2P devices on the network
                                                prepareServiceDiscovery();
                                                startServiceDiscovery();
                                                // Initializing the fragmentlist with the device founded on the network
                                                servicesList = new WiFiDirectServicesList();
                                                // Show the fragment
                                                getFragmentManager().beginTransaction()
                                                        .add(R.id.devices_root, servicesList, "services").commit();

                                            }else{
                                                // Clear all the devices discovered in the adapterlist
                                                WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                                                        .findFragmentByTag("services");
                                                WiFiDirectServicesList.WiFiDevicesAdapter adapter = ((WiFiDirectServicesList.WiFiDevicesAdapter) fragment
                                                        .getListAdapter());
                                                adapter.clear();
                                                adapter.notifyDataSetChanged();
                                            }
                                            // Toggle boolean discovery variable (NOT == disable discovery)
                                            isServiceDiscoveryActive = !isServiceDiscoveryActive;
                                        }*/
                                    }
                                }).show();
            }
        });

        statusTxtView = (TextView) findViewById(R.id.status_text);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Get the Wifi Direct manager
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        // Get an initialized channel where to communicate
        channel = manager.initialize(this, getMainLooper(), null);

        if(PUBLISHER_MODULE) {
            // Publish this device on the network sending beacon
            serviceBroadcastingHandler.postDelayed(serviceBroadcastRunnable,200);
        }
        if(LISTENER_MODULE) {
            // Initiates callbacks for service discovery
            prepareServiceDiscovery();
            // Check if the discovery is already running
            if (!isServiceDiscoveryActive) {
                // Check for other P2P devices on the network
                prepareServiceDiscovery();
                startServiceDiscovery();
                // Initializing the fragmentlist with the device founded on the network
                servicesList = new WiFiDirectServicesList();
                // Show the fragment
                getFragmentManager().beginTransaction()
                        .add(R.id.devices_root, servicesList, "services").commit();

            }else{
                // Clear all the devices discovered in the adapterlist
                WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                        .findFragmentByTag("services");
                WiFiDirectServicesList.WiFiDevicesAdapter adapter = ((WiFiDirectServicesList.WiFiDevicesAdapter) fragment
                        .getListAdapter());
                adapter.clear();
                adapter.notifyDataSetChanged();
            }
            // Toggle boolean discovery variable (NOT == disable discovery)
            isServiceDiscoveryActive = !isServiceDiscoveryActive;
        }

    }
    @Override
    protected void onRestart() {
        if(LISTENER_MODULE) {
            Fragment frag = getFragmentManager().findFragmentByTag("services");
            if (frag != null) {
                getFragmentManager().beginTransaction().remove(frag).commit();
            }
        }
        super.onRestart();
    }

    @Override
    protected void onStop() {
        if (manager != null && channel != null) {
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });
        }
        super.onStop();
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
                    record.put(TXTRECORD_PROP_POSITION, String.valueOf(AUTOINCREMENT_NUMBER++));
                    record.put(TXTRECORD_PROP_ACCURACY, String.valueOf(AUTOINCREMENT_NUMBER++));
                    record.put(TXTRECORD_PROP_TIMESTAMP, String.valueOf(AUTOINCREMENT_NUMBER++));
                    record.put(TXTRECORD_PROP_TIMESTAMP_POS, String.valueOf(AUTOINCREMENT_NUMBER++));
                    record.put(TXTRECORD_PROP_DIRECTION, String.valueOf(AUTOINCREMENT_NUMBER++));
                    record.put(TXTRECORD_PROP_SPEED, String.valueOf(AUTOINCREMENT_NUMBER++));

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
                                    4000);
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
                            WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
                                    .findFragmentByTag("services");
                            if (fragment != null) {
                                WiFiDirectServicesList.WiFiDevicesAdapter adapter = ((WiFiDirectServicesList.WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                boolean found = false;

                                // Search if this device with this service already exists in the list
                                for(int i = 0; i < adapter.getCount(); i++)
                                {
                                    // Compare MAC address
                                    if(adapter.getItem(i).device.deviceAddress.equalsIgnoreCase(device.deviceAddress) )
                                    {
                                        // Update values for this entity
                                        adapter.getItem(i).device = device;
                                        adapter.getItem(i).instanceName = record.get(TXTRECORD_PROP_SERVICE_INSTANCE);
                                        adapter.getItem(i).accuracy = record.get(TXTRECORD_PROP_ACCURACY);
                                        adapter.getItem(i).direction = record.get(TXTRECORD_PROP_DIRECTION);
                                        adapter.getItem(i).position = record.get(TXTRECORD_PROP_POSITION);
                                        adapter.getItem(i).speed = record.get(TXTRECORD_PROP_SPEED);
                                        adapter.getItem(i).timestamp = record.get(TXTRECORD_PROP_TIMESTAMP);
                                        adapter.getItem(i).timestampPos = record.get(TXTRECORD_PROP_TIMESTAMP_POS);
                                        found = true;
                                        break;
                                    }
                                }
                                if(!found) {
                                    WiFiP2pService service = new WiFiP2pService();

                                    service.device = device;
                                    service.instanceName = record.get(TXTRECORD_PROP_SERVICE_INSTANCE);
                                    service.accuracy = record.get(TXTRECORD_PROP_ACCURACY);
                                    service.direction = record.get(TXTRECORD_PROP_DIRECTION);
                                    service.position = record.get(TXTRECORD_PROP_POSITION);
                                    service.speed = record.get(TXTRECORD_PROP_SPEED);
                                    service.timestamp = record.get(TXTRECORD_PROP_TIMESTAMP);
                                    service.timestampPos = record.get(TXTRECORD_PROP_TIMESTAMP_POS);

                                    adapter.add(service);
                                }
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

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
                        if(!isServiceDiscoveryActive) {
                            // Disable service discovery
                            appendStatus("[D] Service discovery stopped");
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
                                                                5000);
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

    @Override
    public void connectP2p(WiFiP2pService service) {
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
    }

    @Override
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
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */

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
    }

    public void appendStatus(String status) {
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(status + "\n" + current);
    }


}
