/*
 * Support for TI SensorTag device, added by Jeffrey Blum <jeffbl@cim.mcgill.ca>
 * based on TI code snippets from the TI SensorTag User Guide:
 *
 * http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
 * http://processors.wiki.ti.com/images/a/a8/BLE_SensorTag_GATT_Server.pdf
 *
 */

package de.grundid.ble.sensors.tisensortag;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.SystemClock;
import android.util.Log;

import java.io.Serializable;
import java.util.UUID;

import de.grundid.ble.utils.BleUtils;

//TODO:

public class TISensorTagAccelerometer implements Serializable {
    private static final long serialVersionUID = 1L;
    private int period;

    private static final UUID MY_UUID_SERVICE   = UUID.fromString("F000AA10-0451-4000-B000-000000000000");
    private static final UUID MY_UUID_DATA      = UUID.fromString("F000AA11-0451-4000-B000-000000000000");
    private static final UUID MY_UUID_CONFIG    = UUID.fromString("F000AA12-0451-4000-B000-000000000000");
    private static final UUID MY_UUID_PERIOD    = UUID.fromString("F000AA13-0451-4000-B000-000000000000");
    private static final UUID CCC               = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // GATT_CLIENT_CHAR_CFG_UUID

    public class Point3D {
        public double x, y, z;
        public Point3D(double xVal, double yVal, double zVal) {
            x = xVal;
            y = yVal;
            z = zVal;
        }
    }
    private Point3D instant, gravity, linear;

	public TISensorTagAccelerometer(double xVal, double yVal, double zVal) {
        instant = new Point3D(xVal, yVal, zVal);
        gravity = new Point3D(0,0,0);
        linear =  new Point3D(0,0,0);
	}

    public static boolean isUuid(UUID uuid) {
        //return MY_UUID_SERVICE.equals(uuid);
        return MY_UUID_DATA.equals(uuid);
    }

    //instantaneous acceleration (direct off the sensors, sum of gravity + linear)
    public double x() { return instant.x; }
    public double y() { return instant.y; }
    public double z() { return instant.z; }

    //gravity vector
    public double gravityX() { return gravity.x; }
    public double gravityY() { return gravity.y; }
    public double gravityZ() { return gravity.z; }

    //linear acceleration (instant - gravity)
    public double linearX() { return linear.x; }
    public double linearY() { return linear.y; }
    public double linearZ() { return linear.z; }


    public static void enable(BluetoothGatt bleGatt, byte enable) {
        // turn on sensor
        BluetoothGattService service = bleGatt.getService(MY_UUID_SERVICE);
        BluetoothGattCharacteristic config = service.getCharacteristic(MY_UUID_CONFIG);
        config.setValue(new byte[]{enable});
        bleGatt.writeCharacteristic(config);
        SystemClock.sleep(200);

        //turn on local notifications
        BluetoothGattCharacteristic dataCharacteristic = service.getCharacteristic(MY_UUID_DATA);
        bleGatt.setCharacteristicNotification(dataCharacteristic, true); //Enabled locally
        SystemClock.sleep(200);

        //turn on remote notifications
        BluetoothGattDescriptor descriptor = dataCharacteristic.getDescriptor(CCC);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bleGatt.writeDescriptor(descriptor); //Enabled remotely
        SystemClock.sleep(200);
    }

    //takes period in milliseconds (gets floored to nearest 10, since TISensorTag gets them in )
    public static void setPeriod(BluetoothGatt bleGatt, int newPeriod) {
        BluetoothGattService accelService = bleGatt.getService(MY_UUID_SERVICE);
        BluetoothGattCharacteristic config = accelService.getCharacteristic(MY_UUID_PERIOD);
        config.setValue(new byte[]{(byte)(newPeriod/10)});
        bleGatt.writeCharacteristic(config);
        SystemClock.sleep(200);
    }

    /*
     * The accelerometer has the range [-2g, 2g] with unit (1/64)g.
     * jeffbl: Correction to TI information: latest firmware (1.5) has 8g range, so added /4.0 in the scaling conversion
     *
     * To convert from unit (1/64)g to unit g we divide by 64.
     *
     * (g = 9.81 m/s^2)
     *
     * The z value is multiplied with -1 to coincide
     * with how we have arbitrarily defined the positive y direction.
     * (illustrated by the apps accelerometer image)
     * */

    final float accelerometer_alpha = (float)0.8; //https://developer.android.com/guide/topics/sensors/sensors_motion.html
    public TISensorTagAccelerometer updateFromCharacteristic(BluetoothGattCharacteristic characteristic) {

        //parse the raw (instant) value out of the bluetooth GATT characteristic
        instant.x = characteristic.getIntValue(android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8, 0) / (64.0 / 4.0);
        instant.y = characteristic.getIntValue(android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8, 1) / (64.0 / 4.0);
        instant.z = characteristic.getIntValue(android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8, 2) / (64.0 / 4.0) * -1;

        // Use Android sample code from Google to calculate gravity and linear acceleration:
        // https://developer.android.com/guide/topics/sensors/sensors_motion.html
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        // Isolate the force of gravity with the low-pass filter.
        gravity.x = accelerometer_alpha * gravity.x + (1 - accelerometer_alpha) * instant.x;
        gravity.y = accelerometer_alpha * gravity.y + (1 - accelerometer_alpha) * instant.y;
        gravity.z = accelerometer_alpha * gravity.z + (1 - accelerometer_alpha) * instant.z;

        // Remove the gravity contribution with the high-pass filter.
        linear.x = instant.x - gravity.x;
        linear.y = instant.y - gravity.y;
        linear.z = instant.z - gravity.z;

        //Log.d("debug", asString());

        return this;
    }

	public static TISensorTagAccelerometer fromCharacteristic(BluetoothGattCharacteristic characteristic) {
        TISensorTagAccelerometer tiSensorTagAccelerometer = new TISensorTagAccelerometer(0, 0, 0);
        tiSensorTagAccelerometer.updateFromCharacteristic(characteristic);
        return tiSensorTagAccelerometer;
    }

    public String asString() {
        String str;
        str = "TISensorTagAccelerometer inst : " + instant.x + ", " + instant.x + ", " + instant.z ;
        str+= "  grav : " + gravity.x  + ", " + gravity.y + ", " + gravity.z;
        str+= "  lina : " + linear.x  + ", " + linear.y + ", " + linear.z;
        str+= "\n";
        return str;
    }
}
