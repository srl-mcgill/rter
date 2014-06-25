package ca.nehil.rter.streamingapp;

import ca.nehil.rter.streamingapp.R;
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
	
	private Location[] fakeLocations;
	private long startTime;
	private final long DEMO_TIME_MS = 30 * 1000;

	public SensorSource(Context context){
		
		buildFakeLocations();
		this.startTime = System.currentTimeMillis();
		
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);

		provider = mLocationManager.getBestProvider(criteria, true);
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

		locationIntent = new Intent (context.getString(R.string.LocationEvent));
		sensorIntent = new Intent (context.getString(R.string.SensorEvent));
		localBroadcastManager = LocalBroadcastManager.getInstance(context);
	}
	
	private void buildFakeLocations() {
		fakeLocations = new Location[23];
		
		fakeLocations[0] = new Location("aaa");
		fakeLocations[0].setLatitude(37.408770);
		fakeLocations[0].setLongitude(-122.028503);


		fakeLocations[1] = new Location("aaa");
		fakeLocations[1].setLatitude(37.408763);
		fakeLocations[1].setLongitude(-122.028517);


		fakeLocations[2] = new Location("aaa");
		fakeLocations[2].setLatitude(37.408751);
		fakeLocations[2].setLongitude(-122.028521);


		fakeLocations[3] = new Location("aaa");
		fakeLocations[3].setLatitude(37.408748);
		fakeLocations[3].setLongitude(-122.028533);


		fakeLocations[4] = new Location("aaa");
		fakeLocations[4].setLatitude(37.408742);
		fakeLocations[4].setLongitude(-122.028537);


		fakeLocations[5] = new Location("aaa");
		fakeLocations[5].setLatitude(37.408732);
		fakeLocations[5].setLongitude(-122.028548);


		fakeLocations[6] = new Location("aaa");
		fakeLocations[6].setLatitude(37.408723);
		fakeLocations[6].setLongitude(-122.028560);


		fakeLocations[7] = new Location("aaa");
		fakeLocations[7].setLatitude(37.408728);
		fakeLocations[7].setLongitude(-122.028570);


		fakeLocations[8] = new Location("aaa");
		fakeLocations[8].setLatitude(37.408730);
		fakeLocations[8].setLongitude(-122.028592);


		fakeLocations[9] = new Location("aaa");
		fakeLocations[9].setLatitude(37.408743);
		fakeLocations[9].setLongitude(-122.028603);


		fakeLocations[10] = new Location("aaa");
		fakeLocations[10].setLatitude(37.408763);
		fakeLocations[10].setLongitude(-122.028592);


		fakeLocations[11] = new Location("aaa");
		fakeLocations[11].setLatitude(37.408766);
		fakeLocations[11].setLongitude(-122.028584);


		fakeLocations[12] = new Location("aaa");
		fakeLocations[12].setLatitude(37.408781);
		fakeLocations[12].setLongitude(-122.028570);


		fakeLocations[13] = new Location("aaa");
		fakeLocations[13].setLatitude(37.408779);
		fakeLocations[13].setLongitude(-122.028564);


		fakeLocations[14] = new Location("aaa");
		fakeLocations[14].setLatitude(37.408794);
		fakeLocations[14].setLongitude(-122.028553);


		fakeLocations[15] = new Location("aaa");
		fakeLocations[15].setLatitude(37.408799);
		fakeLocations[15].setLongitude(-122.028542);


		fakeLocations[16] = new Location("aaa");
		fakeLocations[16].setLatitude(37.408801);
		fakeLocations[16].setLongitude(-122.028532);


		fakeLocations[17] = new Location("aaa");
		fakeLocations[17].setLatitude(37.408800);
		fakeLocations[17].setLongitude(-122.028519);


		fakeLocations[18] = new Location("aaa");
		fakeLocations[18].setLatitude(37.408796);
		fakeLocations[18].setLongitude(-122.028509);


		fakeLocations[19] = new Location("aaa");
		fakeLocations[19].setLatitude(37.408795);
		fakeLocations[19].setLongitude(-122.028501);


		fakeLocations[20] = new Location("aaa");
		fakeLocations[20].setLatitude(37.408791);
		fakeLocations[20].setLongitude(-122.028509);


		fakeLocations[21] = new Location("aaa");
		fakeLocations[21].setLatitude(37.408788);
		fakeLocations[21].setLongitude(-122.028515);


		fakeLocations[22] = new Location("aaa");
		fakeLocations[22].setLatitude(37.408792);
		fakeLocations[22].setLongitude(-122.028503);
		
	}

	public static SensorSource getInstance(Context context){
		if (singleton == null){
			singleton = new SensorSource(context);
			mLocationManager.requestLocationUpdates(provider, 1000, 0, singleton); //register singleton with locationmanager
			mcontext = context;
		}
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
		long t = System.currentTimeMillis() - startTime;
		int i = (int) ((double)(t) / (double)DEMO_TIME_MS * (fakeLocations.length - 1));
		if(i >= fakeLocations.length - 1) {
			Log.d("MATH", "lat: " + (fakeLocations[fakeLocations.length - 1].getLatitude()) + ", lng: " + (fakeLocations[fakeLocations.length - 1].getLongitude()));
			return fakeLocations[fakeLocations.length - 1];
		}
		
		long td = DEMO_TIME_MS / (fakeLocations.length - 1);
		
		long ti = i * td;
		long tint = t - ti;
		double ratio = (double)(tint) / (double)(td);
		
		Log.d("MATH", "t: " + t + ", i: " + i + ", ti: " + ti + ", tint: " + tint + ", ratio: " + ratio);
		
		Location newLoc = new Location("aaa");
		newLoc.setLatitude(fakeLocations[i].getLatitude() * (1 - ratio) + fakeLocations[i + 1].getLatitude() * ratio);
		newLoc.setLongitude(fakeLocations[i].getLongitude() * (1 - ratio) + fakeLocations[i + 1].getLongitude() * ratio);
		
		Log.d("MATH", "lat: " + (newLoc.getLatitude()) + ", lng: " + (newLoc.getLongitude()));
		return newLoc;
		/*
		if(this.location != null){
			Log.d("Location: ", this.location+"");
			return this.location;
		}else{
			return mLocationManager.getLastKnownLocation(provider);
		}
		*/
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
