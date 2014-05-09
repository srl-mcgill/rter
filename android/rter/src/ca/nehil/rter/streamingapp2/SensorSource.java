package ca.nehil.rter.streamingapp2;

import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class SensorSource implements SensorEventListener, LocationListener{

	private static SensorSource singleton = null;
	static Context mcontext; 	// Need context for broadcast manager
	private Location location;
	private float currentOrientation = 0;
	private float[] rotationMatrix = new float[16]; //Change to 9 if using sensorFusion, 16 otherwise
	private float[] outRotationMatrix = new float[16];
	private static LocationManager mLocationManager;
	private static SensorManager mSensorManager;
	private static String provider;

	private Intent locationIntent;
	private Intent sensorIntent;
	private LocalBroadcastManager localBroadcastManager;
	private float[] orientationValues = new float[3];
	private float declination = 0;

	public SensorSource(Context context){
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);

		provider = mLocationManager.getBestProvider(criteria, true);
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

		locationIntent = new Intent (context.getString(R.string.LocationEvent));
		sensorIntent = new Intent (context.getString(R.string.SensorEvent));
		localBroadcastManager = LocalBroadcastManager.getInstance(context);
	}

	public static SensorSource getInstance(Context context){

		if (singleton == null){
			singleton = new SensorSource(context);
		}
		mLocationManager.requestLocationUpdates(provider, 1000, 0, singleton); //register singleton with locationmanager
		mcontext = context;
		return singleton;
	}

	public void initListeners(){
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	/**
	 * @return The rotation matrix received from the sensors. Sent directly to openGL for rendering.
	 */
	public float[] getLandscapeRotationMatrix(){
		return outRotationMatrix;
	}

	public void stopListeners(){
		mSensorManager.unregisterListener(this);
		mLocationManager.removeUpdates(this);
	}

	public Location getLocation(){
		if(this.location != null){
			Log.d("Location: ", this.location+"");
			return this.location;
		}else{
			return mLocationManager.getLastKnownLocation(provider);
		}
	}

	//TODO: [Urgent]; Alok- Need to get current orientation here. This value is sent to the server in StreamingActivity
	/**
	 * @return Angle, in degrees, of the horizontal orientation. 
	 */
	public float getCurrentOrientation(){
		return this.currentOrientation;
	}
	
	public float getDeclination(){
		return declination;
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		switch (sensorEvent.sensor.getType()) {

		case Sensor.TYPE_ROTATION_VECTOR:
			rotationMatrix=new float[16];
			SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);
		}

		SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Y,
				SensorManager.AXIS_MINUS_X, outRotationMatrix);  	// Remap coordinate System to compensate for the landscape position of device
		SensorManager.getOrientation(outRotationMatrix, orientationValues);
		currentOrientation = (float) (Math.toDegrees(orientationValues[0]) + this.getDeclination());
		sendSensorBroadcast();  // Let other classes know of update to sensor data.
	}

	@Override
	public void onLocationChanged(Location location) {
		GeomagneticField gmf = new GeomagneticField(
				(float)location.getLatitude(), (float)location.getLongitude(), (float)location.getAltitude(), System.currentTimeMillis());
		declination = gmf.getDeclination();
		this.location = location;
		Log.d("LocationDebug", "location broadcast: "+location);
		sendLocationBroadcast();
	}

	/*
	 *  Send broadcast for sensor changed
	 */
	private void sendSensorBroadcast() {
		localBroadcastManager.sendBroadcast(sensorIntent);
	} 

	/*
	 *  Send broadcast for location changed
	 */
	private void sendLocationBroadcast(){
		localBroadcastManager.sendBroadcast(locationIntent);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {

	}

	@Override
	public void onProviderDisabled(String arg0) {

	}

	@Override
	public void onProviderEnabled(String arg0) {

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

	}
}
