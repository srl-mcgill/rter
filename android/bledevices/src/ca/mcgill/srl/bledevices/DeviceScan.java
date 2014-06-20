package ca.mcgill.srl.bledevices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Alok on 5/28/2014.
 */
public class DeviceScan {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mLeDevices;
    private ArrayList<ScannedDevicesListener> scanListeners = new ArrayList<ScannedDevicesListener>();

    BluetoothManager bluetoothManager;
    Context context;

    public DeviceScan(Context context){

        this.context = context;
        mLeDevices = new ArrayList<BluetoothDevice>();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //return an error, notifying that we dont have BLE. The caller can decide to make a toast with it or just log it.
        }

        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            //return an error, notifying no BL.
        }
    }

    public void startScan(){
        //set a flag
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        Log.d("alok", "DeviceScan Starting scan");
    }

    public void stopScan(){
        //set a flag
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mLeDevices.clear();
        Log.d("alok", "DeviceScan stopping scan");
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    public int getCount() {
        return mLeDevices.size();
    }

    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    // add device to the list.
                    addDevice(device);
                    //return result to whichever activity/service called me.
                    Log.d("alok", "DeviceScan onLeScan got device " + device);
                }

            };
//Send broadcasts everytime you find a device, or send a broadcast every 5 seconds, sending a list of devices?
    // Add the discovered device to the list of BLE devices seen.
    public void addDevice(BluetoothDevice device) {
//    	mLeDevices.add(device);
    	// send a broadcast for every device you see.
    	sendMessage(device.toString());
    }

    /**
     * Sends a local broadcast for every device found, sending the address.
     */
    private void sendMessage(String device){
        Log.d("alok", "DeviceScan sending broadcast");
        Intent intent = new Intent("DeviceFound");
        intent.putExtra("device", device);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    //Add listeners for the scanned device list.
    public void addListener(ScannedDevicesListener listener){
        scanListeners.add(listener);
    }
}
