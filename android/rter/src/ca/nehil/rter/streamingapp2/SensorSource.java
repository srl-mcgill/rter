package ca.nehil.rter.streamingapp2;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class SensorSource implements SensorEventListener, LocationListener{

	private static SensorSource singleton = null;
	private List<SensorSourceListener> sensorListeners;
	static Context mcontext; 	// Need context for broadcast manager

	public SensorSource(){
		sensorListeners = new CopyOnWriteArrayList<SensorSourceListener>(); // Thread safe.
		
	}

	public static SensorSource getInstance(Context context){
		if (singleton == null)
		{
			singleton = new SensorSource();
		}
		mcontext = context;
		return singleton;
	}

	/*
	 * Subsribe a listener to the sensors.
	 */
	public void subscribeListener(SensorSourceListener listener){
		sensorListeners.add(listener);
	}

	/*
	 * Unsubscibe a listener.
	 */
	public void unSubscribeListener(SensorSourceListener listener){
		sensorListeners.remove(listener);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		for (SensorSourceListener sensorListener : sensorListeners) {
			sensorListener.onSensorSourceEvent(event);
		}
		Log.d("alok", "sending sensor broadcast message");
		sendMessage();
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.d("alok", "sensorsource locationchanged " + sensorListeners.get(0));
		for (SensorSourceListener locationListener : sensorListeners) {
			locationListener.onLocationSourceEvent(location);
		}
		Log.d("alok", "sending location broadcast message");
		sendMessage();
	}
	
	// Send an Intent with an action named "my-event". 
	private void sendMessage() {
	  Intent intent = new Intent("my-event");
	  // add data
	  intent.putExtra("message", "data");
	  LocalBroadcastManager.getInstance(mcontext).sendBroadcast(intent);
	} 
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

}
