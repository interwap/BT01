package com.praescient.components.bluetoothservice;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothReader extends DialogFragment implements AdapterView.OnItemClickListener{

    private static Activity activity;
    public static BluetoothAdapter bluetoothAdapter;
    private boolean connectionStatus = false;
    private boolean on = false;
    private Intent intent;
    private static Bundle args;
    private boolean showTitle = false;
    private boolean setCancelable = false;
    private boolean shown = false;
    public boolean isConnected = false;

    public ArrayAdapter<String> deviceList = null;
    public Set<BluetoothDevice> pairedDevices;
    public ArrayList<BluetoothDevice> unpairedDevices;

    private IntentFilter intentFilter;
    private BroadcastReceiver broadcastReceiver;

    // Unique UUID for this application
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Intent request codes
    private static final int REQUEST_ENABLE_BLUETOOTH = 2;

    private ListView bluetoothList;
    private Button scanButton;

    public BluetoothService bluetoothService;

    // Name of the connected device
    public String mConnectedDeviceName = null;
    private BluetoothDevice connectedDevice = null;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String CONNECTED_DEVICE = "connected_device";
    public static final String TOAST = "toast";

    // Debugging
    private static final String TAG = "BluetoothReader";
    private static final boolean D = true;

    //Reader
    private Runnable readCommand;
    private Runnable sendCommand;
    private Message message;

    private int run = 0;

    // Constructor
    public BluetoothReader(){
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static BluetoothReader newInstance(Activity act, String title){
        BluetoothReader fragment = new BluetoothReader();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        args = new Bundle();
        args.putString("title", title);
        fragment.setArguments(args);
        activity = act;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bluetooth, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bluetoothList = (ListView) view.findViewById(R.id.bluetoothList);
        bluetoothList.setDivider(null);
        bluetoothList.setDividerHeight(20);
        bluetoothList.setOnItemClickListener(this);

        scanButton = (Button) view.findViewById(R.id.scanButton);
        scanButton.setHeight(70);

        bluetoothService = new BluetoothService(activity.getApplicationContext(), mHandler);
        deviceList = new ArrayAdapter<String>( activity.getApplicationContext(), android.R.layout.simple_list_item_1, 0);
        unpairedDevices = new ArrayList<BluetoothDevice>();

        isOn();

        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String pairedInfo = device.getName() + "\n" + device.getAddress() + "\n" + "(Paired)";
                    String deviceInfo = device.getName() + "\n" + device.getAddress();

                    if(deviceList.getCount() <= 0){

                        deviceList.add(deviceInfo);


                    } else {

                        if(deviceList.getPosition(pairedInfo) < 0 ){

                            if (deviceList.getPosition(deviceInfo) <= 0) {
                                deviceList.add(deviceInfo);
                                unpairedDevices.add(device);
                            }
                        }

                    }

                    bluetoothList.setAdapter(deviceList);

                } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    //Device searching

                } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){


                } else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){

                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                    switch (state){
                        case BluetoothAdapter.STATE_OFF:
                            on = false;
                            connectionStatus = false;
                            isOn();
                            break;

                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Toast.makeText(context, "Bluetooth Turned Off", Toast.LENGTH_SHORT).show();
                            on = false;
                            connectionStatus = false;
                            break;
                    }

                } else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){

                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                    if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                        Toast.makeText(context, "Pairing Sucessful", Toast.LENGTH_SHORT).show();

                        deviceList.clear();
                        deviceList.notifyDataSetChanged();
                        pairedDevices();
                        findDevices();

                    } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                        Toast.makeText(context, "Pairing Unsucessful", Toast.LENGTH_SHORT).show();
                    }

                } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
                    //Toast.makeText(context, "Device Connected", Toast.LENGTH_SHORT).show();
                }

            }
        };

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        activity.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        activity.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        activity.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);

        intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        activity.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(on){
                    Toast.makeText(activity.getApplicationContext(), "Scanning for new devices...", Toast.LENGTH_SHORT).show();
                    findDevices();
                } else {
                    isOn();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        //safety check
        if (getDialog() == null){
            return;
        }

        int width = 600;
        int height = 912;

        if(args.containsKey("width")){
            width = getArguments().getInt("width");
        }

        if(args.containsKey("height")){
            height = getArguments().getInt("height");
        }

        try {
            getDialog().getWindow().setLayout(width, height);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (shown) return;
        super.show(manager, tag);
        shown = true;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        shown = false;
        super.onDismiss(dialog);
    }

    public boolean isShowing(){
        return shown;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = super.onCreateDialog(savedInstanceState);

        if(!showTitle){
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        if(!setCancelable){
            dialog.setCanceledOnTouchOutside(setCancelable);
        }

        return dialog;
    }

    public boolean showTitle(boolean value){
        return showTitle = value;
    }

    public boolean cancelable(boolean value){
        return setCancelable = value;
    }

    public void setStatus(String value){
        args.putString("status", value);
    }

    public void setResult(String value){
        args.putString("result", value);
    }

    public void setBackgroundColor(int color){
        args.putInt("color", color);
    }

    public void setTextSize(float size) { args.putFloat("textSize", size); }

    public void setWidth(int width) { args.putInt("width", width); }

    public void setHeight(int height) { args.putInt("height", height); }

    public boolean isConnected(){
        return connectionStatus;
    }

    public BluetoothDevice getConnectedDevice(){
        return connectedDevice;
    }

    public interface ConnectionListener{
        void isConnected(boolean value);
    }

    private Runnable getReadCommand() {
        return readCommand;
    }

    public void setReadCommand(final Runnable readCommand) {
        this.readCommand = readCommand;
    }

    private Runnable getSendCommand() {
        return sendCommand;
    }

    public void setSendCommand(Runnable sendCommand) {
        this.sendCommand = sendCommand;
    }

    public Message getMessage() {
        return message;
    }

    private void setMessage(Message message) {
        this.message = message;
    }

    //Bluetooth is off
    private void isOn(){

        if (bluetoothAdapter == null){
            Toast.makeText(activity.getApplicationContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if(!bluetoothAdapter.isEnabled()){
                Toast.makeText(activity.getApplicationContext(), "Bluetooth Disabled", Toast.LENGTH_SHORT).show();
                intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
            } else {

                on = true;
                pairedDevices();

            }
        }
    }

    public boolean getResult(int resultCode){

        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(activity.getApplicationContext(), "Bluetooth Cancelled", Toast.LENGTH_SHORT).show();

            if(deviceList != null ){

                if(deviceList.getCount() > 0){
                    deviceList.clear();
                    deviceList = null;
                }

            }

        } else {

            on = true;
            pairedDevices();
        }

        return on;
    }

    // Show Paired Devices
    public void pairedDevices(){

        pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceList = new ArrayAdapter<String>( activity.getApplicationContext(), android.R.layout.simple_list_item_1, 0);

        if(pairedDevices.size() > 0){

            for (BluetoothDevice device : pairedDevices){
                deviceList.add(device.getName() + "\n" + device.getAddress() + "\n" + "(Paired)");
            }

            bluetoothList.setAdapter(deviceList);
        }

    }

    // Find Bluetooth Devices Within Range
    public void findDevices(){

        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        } else {
            bluetoothAdapter.startDiscovery();
        }

    }

    public void reconnect(BluetoothDevice bluetoothDevice){

        if(bluetoothDevice != null){

           // bluetoothService.connect(bluetoothDevice);
            try {
                ConnectionService.getBluetoothSocket().connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        try {

            if(bluetoothAdapter.isDiscovering()){
                bluetoothAdapter.cancelDiscovery();
            }
            if(deviceList.getItem(i).contains("Paired")){

                Object[] objects = pairedDevices.toArray();
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(String.valueOf(objects[i]));

                bluetoothService.connect(device);

            } else {

                try {
                    Toast.makeText(activity.getApplicationContext(), "Initializing..", Toast.LENGTH_SHORT).show();
                    for (BluetoothDevice device : unpairedDevices){
                        if(deviceList.getItem(i).contains(device.getAddress())){
                            Method method = device.getClass().getMethod("createBond", (Class[]) null);
                            method.invoke(device, (Object[]) null);
                        }
                    }
                } catch (Exception e) {
                        e.printStackTrace();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // Unregister broadcast receiver
    public void minimize(){
        activity.getApplicationContext().unregisterReceiver(broadcastReceiver);
    }

    public void disconnect(){
        bluetoothService.stop();
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            setMessage(msg);

            switch (msg.what) {

                case MESSAGE_STATE_CHANGE:

                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);

                    switch (msg.arg1) {

                        case BluetoothService.STATE_CONNECTED:

                            connectionStatus = true;

                            if(getSendCommand() != null){
                                getSendCommand().run();
                            }

                            break;

                    }

                    break;
                case MESSAGE_WRITE:

                    byte[] writeBuf = (byte[]) msg.obj;

                    break;
                case MESSAGE_READ:

                    if(getReadCommand() != null){
                        getReadCommand().run();
                    };

                    break;
                case MESSAGE_DEVICE_NAME:

                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);

                    Toast.makeText(activity.getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                    ConnectionListener cL = (ConnectionListener) getActivity();
                    isConnected = true;
                    cL.isConnected(isConnected);

                    break;

                case MESSAGE_TOAST:

                    Toast.makeText(activity.getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


}
