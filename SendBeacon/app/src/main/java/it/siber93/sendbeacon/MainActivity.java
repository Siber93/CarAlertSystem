package it.siber93.sendbeacon;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;

import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        new Thread(new Runnable() {
            public void run() {
                Beacon beacon = new Beacon.Builder()
                        .setId1("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")
                        .setId2("1")
                        .setId3("2")
                        .setManufacturer(0x0118)
                        .setTxPower(-59)
                        .setDataFields(Arrays.asList(new Long[] {0l}))
                        .build();
                BeaconParser beaconParser = new BeaconParser()
                        .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
                BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
                beaconTransmitter.setAdvertiseMode(ADVERTISE_MODE_LOW_LATENCY);
                beaconTransmitter.setAdvertiseTxPowerLevel(ADVERTISE_TX_POWER_HIGH);
                beaconTransmitter.startAdvertising(beacon);
            }
        }).start();

    }
}
