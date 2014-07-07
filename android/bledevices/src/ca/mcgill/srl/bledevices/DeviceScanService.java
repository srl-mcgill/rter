package ca.mcgill.srl.bledevices;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DeviceScanService extends Service {
    private DeviceScan deviceScan;
    private InitiateScanThread scan;
    boolean doScan = false;

    public DeviceScanService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if(!doScan){
    		Log.d("alok", "Service onStart");
    		doScan = true;
    		scan.start();
    	}
    	return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("alok", "Service onCreate");
        scan = new InitiateScanThread();
        deviceScan = new DeviceScan(this);
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
    	deviceScan.stopScan();
        doScan = false;
    	return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deviceScan.stopScan();
        doScan = false;
    }
    
    @Override
    public void onLowMemory() {
    	super.onLowMemory();
    	deviceScan.stopScan();
    	doScan = false;
    }

    private class InitiateScanThread extends Thread{
        @Override
        public void run() {
            super.run();
            try{
                while(doScan) {
                    deviceScan.startScan();
                    Thread.sleep(5 * 1000); //Sleeping for 5 seconds
                    deviceScan.stopScan();
                    Thread.sleep(5 * 1000);
                }
            }catch(Exception e){
                Log.d("alok", "Error: " + e.getMessage());
            }
        }
    }
}
