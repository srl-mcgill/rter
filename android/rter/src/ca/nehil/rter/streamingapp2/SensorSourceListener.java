package ca.nehil.rter.streamingapp2;

import android.hardware.SensorEvent;
import android.location.Location;

public interface SensorSourceListener {
	/*
	 * Tiggered when SensorSource receives changed sensor values.
	 */
	void onSensorSourceEvent(SensorEvent event);
	
	/*
	 * Triggered when SensorSource receives changed location values.
	 */
	void onLocationSourceEvent(Location location);
}
