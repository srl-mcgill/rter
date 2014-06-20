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

public class TISensorTagTemperature implements Serializable {
    private static final long serialVersionUID = 1L;
    private int period;

    private static final UUID MY_UUID_SERVICE   = UUID.fromString("F000AA00-0451-4000-B000-000000000000");
    private static final UUID MY_UUID_DATA      = UUID.fromString("F000AA01-0451-4000-B000-000000000000");
    private static final UUID MY_UUID_CONFIG    = UUID.fromString("F000AA02-0451-4000-B000-000000000000");
    private static final UUID MY_UUID_PERIOD    = UUID.fromString("F000AA03-0451-4000-B000-000000000000");
    private static final UUID CCC               = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // GATT_CLIENT_CHAR_CFG_UUID

    private double ambient;
    private double target;

	public TISensorTagTemperature(double ambient, double target) {
        this.ambient = ambient;
        this.target = target;
	}

    public double getAmbient() {
        return ambient;
    }

    public double getTarget(){
        return target;
    }


    public static boolean isUuid(UUID uuid) {
        //return MY_UUID_SERVICE.equals(uuid);
        return MY_UUID_DATA.equals(uuid);
    }

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

    public static TISensorTagTemperature fromCharacteristic(BluetoothGattCharacteristic characteristic) {
        TISensorTagTemperature tiSensorTagTemperature = new TISensorTagTemperature(0, 0);
        tiSensorTagTemperature.updateFromCharacteristic(characteristic);

        return tiSensorTagTemperature;
    }

    public TISensorTagTemperature updateFromCharacteristic(BluetoothGattCharacteristic characteristic){
        ambient = extractAmbientTemperature(characteristic);
        target = extractTargetTemperature(characteristic, ambient);

        return this;
    }

    /*
    Code for extracting temperature referred from http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
     */
    private double extractAmbientTemperature(BluetoothGattCharacteristic c) {
        int offset = 2;
        return BleUtils.shortUnsignedAtOffset(c, offset) / 128.0;
    }

    private double extractTargetTemperature(BluetoothGattCharacteristic c, double ambient) {
        Integer twoByteValue = BleUtils.shortSignedAtOffset(c, 0);

        double Vobj2 = twoByteValue.doubleValue();
        Vobj2 *= 0.00000015625;

        double Tdie = ambient + 273.15;

        double S0 = 5.593E-14;	// Calibration factor
        double a1 = 1.75E-3;
        double a2 = -1.678E-5;
        double b0 = -2.94E-5;
        double b1 = -5.7E-7;
        double b2 = 4.63E-9;
        double c2 = 13.4;
        double Tref = 298.15;
        double S = S0*(1+a1*(Tdie - Tref)+a2*Math.pow((Tdie - Tref), 2));
        double Vos = b0 + b1*(Tdie - Tref) + b2*Math.pow((Tdie - Tref), 2);
        double fObj = (Vobj2 - Vos) + c2*Math.pow((Vobj2 - Vos), 2);
        double tObj = Math.pow(Math.pow(Tdie, 4) + (fObj / S), .25);

        return tObj - 273.15;
    }

    public String asString() {
        String str;
        str = "Temperature ambient:" + ambient + " temperture target:" + target;
        str+= "\n";
        return str;
    }
}
