package ca.mcgill.srl.bledevices;

/**
 * Created by Alok on 6/13/2014.
 */
//Ignore for now. Not using this. Currently DeviceScan is broadcasting the device list every 5 seconds, but
// in future this will be used. Follow: http://stackoverflow.com/questions/13323880/pass-data-from-class-to-activity
public interface ScannedDevicesListener {

    void onDevicesScanned();
}
