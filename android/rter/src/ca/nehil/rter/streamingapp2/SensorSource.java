package ca.nehil.rter.streamingapp2;

import ca.nehil.rter.streamingapp2.util.MovingAverageCompass;

import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class SensorSource implements SensorEventListener, LocationListener{

	private static SensorSource singleton = null;
	static Context mcontext; 	// Need context for broadcast manager
	private Location location;
	private float declination = 0;
	private float currentOrientation, deviceOrientation;
	private SensorEvent sensorEvent;
	private float[] rotationMatrix = new float[16];
	private float[] orientationValues = new float[3];
	private float[] outRotationMatrix = new float[16];
	private float[] aValues = new float[3];
	private float[] mValues = new float[3];

	public static SensorSource getInstance(Context context){
		if (singleton == null)
		{
			singleton = new SensorSource();
		}
		mcontext = context;
		return singleton;
	}

	public Location getLocation(){
		return this.location;
	}
	
	public SensorEvent getSensorEvent(){
		return this.sensorEvent;
	}
	
	public float getCurrentOrientation(){
		return this.currentOrientation;
	}
	
	public float getDeviceOrientation(){
		return this.deviceOrientation;
	}

	public float getDeclination(){
		return declination;
	}
	

	@Override
	public void onSensorChanged(SensorEvent event) {

		this.sensorEvent = event;
		doMath();
	}

	@Override
	public void onLocationChanged(Location location) {

		GeomagneticField gmf = new GeomagneticField(
				(float)location.getLatitude(), (float)location.getLongitude(), (float)location.getAltitude(), System.currentTimeMillis());
		declination = gmf.getDeclination();
		this.location = location;
		sendLocationBroadcast();
	}

	/* Send broadcast for sensor changed */ 
	private void sendSensorBroadcast() {
		Intent sensorIntent = new Intent (mcontext.getString(R.string.SensorEvent));
		LocalBroadcastManager.getInstance(mcontext).sendBroadcast(sensorIntent);
	} 

	/* Send broadcast for location changed */
	private void sendLocationBroadcast(){
		Intent locationIntent = new Intent (mcontext.getString(R.string.LocationEvent));
		LocalBroadcastManager.getInstance(mcontext).sendBroadcast(locationIntent);
	}
	
	/*
	 *  Checks if sensor values present, calculates orientations and then
	 *  sends broadcast for sensor data
	 */
	private void doMath(){
		
		switch (sensorEvent.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			System.arraycopy(sensorEvent.values, 0, aValues, 0, 3);
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			System.arraycopy(sensorEvent.values, 0, mValues, 0, 3);
			break;
		}
		
		if (aValues == null || mValues == null)
			return;
		
		if (!SensorManager.getRotationMatrix(rotationMatrix, null, aValues, mValues))
			return;
		SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Z,
				SensorManager.AXIS_MINUS_X, outRotationMatrix);
		SensorManager.getOrientation(outRotationMatrix, orientationValues);
		MovingAverageCompass orientationFilter = new MovingAverageCompass(30);
		orientationFilter.pushValue((float) Math.toDegrees(orientationValues[0]));
		currentOrientation = orientationFilter.getValue() + this.getDeclination();
		deviceOrientation = (float) Math.toDegrees(orientationValues[2]);
		
		sendSensorBroadcast();
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
