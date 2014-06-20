/*
 * Support for TI SensorTag device, added by Jeffrey Blum <jeffbl@cim.mcgill.ca>
 * based on TI code snippets from the TI SensorTag User Guide:
 *
 * http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
 * http://processors.wiki.ti.com/images/a/a8/BLE_SensorTag_GATT_Server.pdf
 *
 */
package de.grundid.ble.sensors.tisensortag;
//package de.grundid.ble.sensors.tisensortag;

import java.io.Serializable;
import java.util.UUID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.SystemClock;

import de.grundid.ble.utils.BleUtils;

//TODO: implement setting period. (should be basically identical to accelerometer...)

public class TISensorTagGyro implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final UUID MY_UUID_SERVICE   = UUID.fromString("F000AA50-0451-4000-B000-000000000000");
    private static final UUID MY_UUID_DATA      = UUID.fromString("F000AA51-0451-4000-B000-000000000000");
    private static final UUID MY_UUID_CONFIG    = UUID.fromString("F000AA52-0451-4000-B000-000000000000");
    private static final UUID MY_UUID_PERIOD    = UUID.fromString("F000AA53-0451-4000-B000-000000000000");
    private static final UUID CCC               = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // GATT_CLIENT_CHAR_CFG_UUID

	private float xAxis;
	private float yAxis;
	private float zAxis;

	public TISensorTagGyro(float xAxis, float yAxis, float zAxis) {
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.zAxis = zAxis;
	}

	public float x() {
		return xAxis;
	}

	public float y() {
		return yAxis;
	}

	public float z() {
		return zAxis;
	}

	public static boolean isUuid(UUID uuid) {
        //return MY_UUID_SERVICE.equals(uuid);
        return MY_UUID_DATA.equals(uuid);
	}

    /*
     *   NB: Gyroscope is special as it has a different "enable-code" from the other sensors.
     *   Gyroscope is unique in that you can enable any combination of the the 3 axes when
     *   you write to the configuration characteristic. Write :
     *      0 to turn off gyroscope
     *      1 to enable X axis only,
     *      2 to enable Y axis only,
     *      3 = X and Y,
     *      4 = Z only,
     *      5 = X and Z,
     *      6 = Y and Z,
     *      7 = X, Y and Z
     */
    public static void enable(BluetoothGatt bleGatt, byte val) {
        //mBluetoothLeService.writeCharacteristic(mBluetoothLeService.getBtGatt().getService(MY_UUID).getCharacteristic(MY_UUID_CONFIG), val);

        // turn on sensor
        BluetoothGattService gyroService = bleGatt.getService(MY_UUID_SERVICE);
        BluetoothGattCharacteristic config = gyroService.getCharacteristic(MY_UUID_CONFIG);
        config.setValue(new byte[]{val});
        bleGatt.writeCharacteristic(config);
        SystemClock.sleep(200);

        //turn on local notifications
        BluetoothGattCharacteristic gyroDataCharacteristic = gyroService.getCharacteristic(MY_UUID_DATA);
        bleGatt.setCharacteristicNotification(gyroDataCharacteristic, true); //Enabled locally
        SystemClock.sleep(200);

        //turn on remote notifications
        BluetoothGattDescriptor descriptor = gyroDataCharacteristic.getDescriptor(CCC);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bleGatt.writeDescriptor(descriptor); //Enabled remotely
        SystemClock.sleep(200);
    }

	public static TISensorTagGyro fromCharacteristic(BluetoothGattCharacteristic characteristic) {
        // NB: x,y,z has a weird order.
        //-- calculate rotation, unit deg/s, range -250, +250
        float y = BleUtils.shortSignedAtOffset(characteristic, 0) * (500f / 65536f) * -1;
        float x = BleUtils.shortSignedAtOffset(characteristic, 2) * (500f / 65536f);
        float z = BleUtils.shortSignedAtOffset(characteristic, 4) * (500f / 65536f);

        return new TISensorTagGyro(x, y, z);
    }


    public String asString() {
        String str;
        str = "TISensorTagGyro x:" + xAxis + "  y:" + yAxis + "  z:" + zAxis + "\n";
        return str;
    }
}
