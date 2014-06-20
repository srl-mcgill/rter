package ca.mcgill.srl.bledevices;

import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import de.grundid.ble.sensors.tisensortag.TISensorTagGyro;
import de.grundid.ble.sensors.tisensortag.TISensorTagAccelerometer;
import de.grundid.ble.sensors.tisensortag.TISensorTagTemperature;


//TODO: hardcoded 200ms pause is inserted after every bluetooth write - this should be a callback
//TODO: Could probably generalize the TISensorTag* Sensor classes for common functionality
//TODO: Accelerometer is instantiated, whereas gyro is not (until event callback occurs). OK?
//TODO: Is there a Point3D class we can actually use?
//TODO: Does not report back connection/disconnection events
//TODO: Much error handling
//TODO: The TISensorTag* sensor objects are serializable - parcelable would be more efficient (but too much coding hassle?)
//TODO: Once we have multiple devices, will the broadcasts from all the BluetoothLeService objects interfere with each other?

/**
 * Service for a single TI SensorTag device
 */
public class TISensorTagService extends Service {

    public static final int TISENSORTAG_SENSORTYPE_GYRO             = 0;
    public static final int TISENSORTAG_SENSORTYPE_ACCELEROMETER    = 1;
    public static final int TISENSORTAG_SENSORTYPE_TEMPERATURE      = 2;

    private long mBindTimestamp; //timestamp of initial bind attempt, to calculate when to stop retrying to connect

    //TODO: private some unique identifier here - the device address? how do we tell multiple sensortags apart?

    private DeviceControl mDeviceControl;
    private String mDeviceAddress;
    private int mDeviceRetryTimeout;

    //make an actual instance of Accelerometer since we need the gravity filter to be persistent
    TISensorTagAccelerometer tiSensorTagAccelerometer = null;

    @Override
    public void onCreate() {
        super.onCreate();

        tiSensorTagAccelerometer = new TISensorTagAccelerometer(0,0,0);
    }

    /*
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mSensorUpdateReceiver, new IntentFilter("characteristicUpdate"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSensorUpdateReceiver);
    }
    */

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mDeviceControl != null) {
            mDeviceControl.close();
        }
        unregisterReceiver(mSensorUpdateReceiver);
    }

    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    private final BroadcastReceiver mSensorUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d("jeffbl", "TISensorTagService: BroadcastReceiver");
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //Log.d("jeffbl", "TISensorTagService: BroadcastReceiver got ACTION_DATA_AVAILABLE");
                parseAndSendCharacteristic(mDeviceControl.getBluetoothLeService().getCurBluetoothGattCharacteristic());
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                if(System.currentTimeMillis() - mBindTimestamp < mDeviceRetryTimeout) { //BUGBUG unfinished - is this exactly right?
                    mDeviceControl.connect();
                }
            }
            else { //send everything else up the chain as-is so app can respond to bluetooth events
                Log.d("debug", "TISensorTagService mSensorUpdateReceiver passing up the chain...");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        }
    };

    private final void parseAndSendCharacteristic(BluetoothGattCharacteristic characteristic) {
        Intent intent;
        Bundle bundle = new Bundle();
        bundle.putString("device_address", mDeviceAddress);

        if (TISensorTagGyro.isUuid(characteristic.getUuid())) {
            TISensorTagGyro tiSensorTagGyro = TISensorTagGyro.fromCharacteristic(characteristic);
            //Log.d("jeffbl", tiSensorTagGyro.asString());
            bundle.putInt("type", TISENSORTAG_SENSORTYPE_GYRO);
            bundle.putSerializable  ("sensor_object", tiSensorTagGyro);
        }
        else if (TISensorTagAccelerometer.isUuid(characteristic.getUuid())) {
            bundle.putInt("type", TISENSORTAG_SENSORTYPE_ACCELEROMETER);
            bundle.putSerializable("sensor_object", tiSensorTagAccelerometer.updateFromCharacteristic(characteristic));
        }

        else if (TISensorTagTemperature.isUuid(characteristic.getUuid())){
            TISensorTagTemperature tiSensorTagTemperature = TISensorTagTemperature.fromCharacteristic(characteristic);
            bundle.putInt("type",TISENSORTAG_SENSORTYPE_TEMPERATURE);
            bundle.putSerializable("sensor_object", tiSensorTagTemperature.updateFromCharacteristic(characteristic));
        }
        else {
            Log.d("jeffbl", "error: TISensorTagService::parseAndSendCharacteristic : unknown characteristic value");
        }

        if(bundle != null) {
            intent = new Intent("TISensorTag_characteristic");
            intent.putExtras(bundle);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    // https://developer.android.com/guide/components/bound-services.html
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public TISensorTagService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TISensorTagService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mDeviceAddress = intent.getStringExtra("deviceAddress");
        mDeviceControl = new DeviceControl(this, mDeviceAddress, intent.getBooleanExtra("flushGatt", false));
        mDeviceRetryTimeout = intent.getIntExtra("retryTimeout", 10) * 1000; //convert to milliseconds
        mBindTimestamp = System.currentTimeMillis();
        mDeviceControl.connect();
        registerReceiver(mSensorUpdateReceiver, makeGattUpdateIntentFilter());

        return mBinder;
    }

    public void gyroEnable(byte val) {
        TISensorTagGyro.enable(mDeviceControl.getBluetoothLeService().mBluetoothGatt, val);
    }

    public void accelerometerEnable(byte enable) {
        TISensorTagAccelerometer.enable( mDeviceControl.getBluetoothLeService().mBluetoothGatt, enable);
    }

    public void accelerometerSetPeriod(int period) {
        TISensorTagAccelerometer.setPeriod(mDeviceControl.getBluetoothLeService().mBluetoothGatt, period);
    }

    public void temperatureEnable(byte enable) {
        TISensorTagTemperature.enable(mDeviceControl.getBluetoothLeService().mBluetoothGatt, enable);
    }

    public void temperatureSetPeriod(int period) {
        TISensorTagTemperature.setPeriod(mDeviceControl.getBluetoothLeService().mBluetoothGatt, period);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void flushGatt() {
        mDeviceControl.getBluetoothLeService().refreshDeviceCache();
    }
}