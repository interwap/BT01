package com.praescient.components.bluetoothservice;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

public class ConnectionService extends Service {

    private static BluetoothDevice bluetoothDevice = null;
    private static BluetoothSocket bluetoothSocket = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        bluetoothDevice = getBluetoothDevice();
        bluetoothSocket = getBluetoothSocket();
        //Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(bluetoothDevice != null && bluetoothSocket != null){
            bluetoothDevice = null;
            bluetoothSocket = null;
        }
        //Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }

    public static BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public static void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        ConnectionService.bluetoothDevice = bluetoothDevice;
    }

    public static BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public static void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        ConnectionService.bluetoothSocket = bluetoothSocket;
    }
}
