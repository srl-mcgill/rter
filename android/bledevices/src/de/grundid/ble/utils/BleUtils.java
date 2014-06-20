package de.grundid.ble.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.bluetooth.BluetoothGattCharacteristic;

public class BleUtils {

    private static final Map<Integer, String> PROPERTIES = new LinkedHashMap<Integer, String>();

    static {
        PROPERTIES.put(BluetoothGattCharacteristic.PROPERTY_BROADCAST, "BC");
        PROPERTIES.put(BluetoothGattCharacteristic.PROPERTY_READ, "RD");
        PROPERTIES.put(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, "WN");
        PROPERTIES.put(BluetoothGattCharacteristic.PROPERTY_WRITE, "WR");
        PROPERTIES.put(BluetoothGattCharacteristic.PROPERTY_NOTIFY, "NY");
        PROPERTIES.put(BluetoothGattCharacteristic.PROPERTY_INDICATE, "IN");
        PROPERTIES.put(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE, "SW");
        PROPERTIES.put(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS, "EP");
    }

    public static String createPropertiesField(int flag) {
        StringBuilder sb = new StringBuilder();
        for (Entry<Integer, String> entry : PROPERTIES.entrySet()) {
            int flagBit = entry.getKey().intValue();
            if ((flag & flagBit) == flagBit) {
                sb.append(entry.getValue());
            } else {
                sb.append("--");
            }
        }
        return sb.toString();
    }

    public static String convertBinToASCII(byte[] bin) {
        return convertBinToASCII(bin, 0, bin.length);
    }

    public static String convertBinToASCII(byte[] bin, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int x = offset; x < offset + length; x++) {
            String s = Integer.toHexString(bin[x]);
            if (s.length() == 1)
                sb.append('0');
            else
                s = s.substring(s.length() - 2);
            sb.append(s);
        }
        return sb.toString().toUpperCase();
    }

    /*
     * Generic functions from TI code snippets on their wiki, added to support TISensorTag
     * (mostly to support awkward format values)
     *
     * http://processors.wiki.ti.com/index.php/SensorTag_User_Guide
     */

    /**
     * Gyroscope, Magnetometer, Barometer, IR temperature
     * all store 16 bit two's complement values in the awkward format
     * LSB MSB, which cannot be directly parsed as getIntValue(FORMAT_SINT16, offset)
     * because the bytes are stored in the "wrong" direction.
     * <p/>
     * This function extracts these 16 bit two's complement values.
     */
    public static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 1); // Note: interpret MSB as signed.

        return (upperByte << 8) + lowerByte;
    }

    public static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.

        return (upperByte << 8) + lowerByte;
    }

}