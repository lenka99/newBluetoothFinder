package com.example.newbluetoothfinder;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "MainActivity";
    Button onoffButton;
    ListView listView;
    TextView status_textView;
    Button searchButton;
    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    ArrayList<String> bluetoothlist = new ArrayList<>();
    Set<String> addresses = new HashSet<>();
    ArrayAdapter arrayAdapter;

    BluetoothAdapter bluetoothAdapter;

    /****
     *
     * 1.enable/disable bluetooth
     */
    protected void enableDisableBT(){
        if(bluetoothAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities");
        }
        if(!bluetoothAdapter.isEnabled()){ // if bluetooth is enabled
            Log.d(TAG, "enableDisableBT: enabling BT.");
            // use an Intent to enable bluetooth
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
            // a filter
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // register a BroadcastReceiver
            registerReceiver(broadcastReceiver1, BTIntent); // broadcastReceiver1 will catch the state change BTIntent : on - off
        }else if(bluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: disabling BT.");
            bluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(broadcastReceiver1, BTIntent); // broadcastReceiver1 will catch the state change BTIntent : on - off
        }
    }

    /****
     *
     * 2. search for devices when blue tooth is enabled
     */
    public void searchClicked(View view){
        if(bluetoothAdapter.isEnabled()){
            status_textView.setText("Searching... ");
            searchButton.setEnabled(false); // restrict button from user for a moment
            bluetoothDevices.clear();
            addresses.clear();
            bluetoothAdapter.startDiscovery();
        }else{
            status_textView.setText("pls enable bluetooth first");
        }
    }

    /****
     *
     * 3. click on listview and look for
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
        bluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = bluetoothDevices.get(i).getName();
        String deviceAddress = bluetoothDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);
            bluetoothDevices.get(i).createBond();
        }
    }

/*****************************
 *
 * ALL BroadcastReceiver
 *
 *
 */
    // 1. on/off
    /**
     * Broadcast Receiver that listen for the ACTION_STATE_CHANGED broadcast intent
     */
    protected final BroadcastReceiver broadcastReceiver1 =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)){
                //
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    // Don't forget to unregister all receivers.
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(broadcastReceiver1);
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(broadcastReceiver4);
    }

    // 2. discover devices
    /**
     * Broadcast Receiver that detects information of discovering bluetooth devices
     * Create a BroadcastReceiver for ACTION_FOUND.
     */
    protected final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("Action", action);

            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                status_textView.setText("Finished ");
                searchButton.setEnabled(true);
            }else if(BluetoothDevice.ACTION_FOUND.equals(action)){
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // any bluetooth device have this method
                String name = device.getName();
                String address = device.getAddress();
                String rssi = Integer.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)); // distance
                Log.i("Device Found","Name: " + name + "Address: " + address + "RSSI: " + rssi);
                // avoid duplicates
                if(!addresses.contains(address)){
                    addresses.add(address);
                    String deviceString = "";
                    if(name == null || name.equals("")){
                        deviceString = address + " - RSSI " + rssi + "dBm";
                    }else{
                        deviceString = name + " - RSSI " +  rssi + "dBm";
                    }
                    // add device to list bluetoothDevices
                    bluetoothDevices.add(device);
                    bluetoothlist.add(deviceString);
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    // 3.pair with devices
    /**3.pair with devices
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver broadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                }
                //case2: creating a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 0. initialize a bluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 1. on/off: initialize on/off button
        onoffButton = findViewById(R.id.onoffButton);

        // 1. on/off: set a listener
        onoffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT(); // call a method
            }
        });

        // 2.initiate basic buttons and views to discovery devices
        listView = findViewById(R.id.listView);
        status_textView = findViewById(R.id.status_textView);
        searchButton = findViewById(R.id.searchButton);


        arrayAdapter = new DeviceListAdapter(this, R.layout.device_adapter_view, bluetoothDevices);

        listView.setAdapter(arrayAdapter);

        // filter for discovery
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver,intentFilter);

        // 3.Broadcasts when bond state changes (ie:pairing)

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver4, filter);

        listView.setOnItemClickListener(MainActivity.this);
    }
}