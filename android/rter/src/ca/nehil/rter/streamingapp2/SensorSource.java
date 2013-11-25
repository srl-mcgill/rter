package ca.nehil.rter.streamingapp2;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class SensorSource implements SensorEventListener, LocationListener{

	private static SensorSource singleton = null;
	private List<SensorSourceListener> sensorListeners, locationListeners;

	public SensorSource(){
		sensorListeners = new CopyOnWriteArrayList<SensorSourceListener>(); // Thread safe.
	}

	public static SensorSource getInstance(){
		if (singleton == null)
		{
			singleton = new SensorSource();
		}
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
	}

	@Override
	public void onLocationChanged(Location location) {

		for (SensorSourceListener locationListener : locationListeners) {
			locationListener.onLocationSourceEvent(location);
		}
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
