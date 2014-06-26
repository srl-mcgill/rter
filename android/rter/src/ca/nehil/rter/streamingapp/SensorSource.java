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
	private final long DEMO_TIME_MS = 2 * 60 * 1000;

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
		fakeLocations = new Location[60];
		
		int[] fix = {4, 3, 0, 1, 2, 5, 6, 7, 8, 11, 9, 10, 12, 13, 14, 16, 15, 17, 18, 19, 20, 21, 22, 23, 24, 27, 28, 25, 26, 29, 30, 31, 32, 33, 34, 35, 36, 38, 37, 40, 39, 41, 42, 43, 44, 45, 46, 47, 48, 49, 51, 50, 52, 53, 54, 55, 56, 57, 58, 59};
		
		fakeLocations[fix[0]] = new Location("rter");
		fakeLocations[fix[0]].setLatitude(37.411955);
		fakeLocations[fix[0]].setLongitude(-122.028786);


		fakeLocations[fix[1]] = new Location("rter");
		fakeLocations[fix[1]].setLatitude(37.411923);
		fakeLocations[fix[1]].setLongitude(-122.028805);


		fakeLocations[fix[2]] = new Location("rter");
		fakeLocations[fix[2]].setLatitude(37.411815);
		fakeLocations[fix[2]].setLongitude(-122.028802);


		fakeLocations[fix[3]] = new Location("rter");
		fakeLocations[fix[3]].setLatitude(37.411860);
		fakeLocations[fix[3]].setLongitude(-122.028816);


		fakeLocations[fix[4]] = new Location("rter");
		fakeLocations[fix[4]].setLatitude(37.411894);
		fakeLocations[fix[4]].setLongitude(-122.028821);


		fakeLocations[fix[5]] = new Location("rter");
		fakeLocations[fix[5]].setLatitude(37.411989);
		fakeLocations[fix[5]].setLongitude(-122.028762);


		fakeLocations[fix[6]] = new Location("rter");
		fakeLocations[fix[6]].setLatitude(37.412034);
		fakeLocations[fix[6]].setLongitude(-122.028738);


		fakeLocations[fix[7]] = new Location("rter");
		fakeLocations[fix[7]].setLatitude(37.412066);
		fakeLocations[fix[7]].setLongitude(-122.028719);


		fakeLocations[fix[8]] = new Location("rter");
		fakeLocations[fix[8]].setLatitude(37.412096);
		fakeLocations[fix[8]].setLongitude(-122.028708);


		fakeLocations[fix[9]] = new Location("rter");
		fakeLocations[fix[9]].setLatitude(37.412222);
		fakeLocations[fix[9]].setLongitude(-122.028631);


		fakeLocations[fix[10]] = new Location("rter");
		fakeLocations[fix[10]].setLatitude(37.412134);
		fakeLocations[fix[10]].setLongitude(-122.028682);


		fakeLocations[fix[11]] = new Location("rter");
		fakeLocations[fix[11]].setLatitude(37.412175);
		fakeLocations[fix[11]].setLongitude(-122.028657);


		fakeLocations[fix[12]] = new Location("rter");
		fakeLocations[fix[12]].setLatitude(37.412271);
		fakeLocations[fix[12]].setLongitude(-122.028601);


		fakeLocations[fix[13]] = new Location("rter");
		fakeLocations[fix[13]].setLatitude(37.412279);
		fakeLocations[fix[13]].setLongitude(-122.028558);


		fakeLocations[fix[14]] = new Location("rter");
		fakeLocations[fix[14]].setLatitude(37.412281);
		fakeLocations[fix[14]].setLongitude(-122.028507);


		fakeLocations[fix[15]] = new Location("rter");
		fakeLocations[fix[15]].setLatitude(37.412339);
		fakeLocations[fix[15]].setLongitude(-122.028462);


		fakeLocations[fix[16]] = new Location("rter");
		fakeLocations[fix[16]].setLatitude(37.412307);
		fakeLocations[fix[16]].setLongitude(-122.028478);


		fakeLocations[fix[17]] = new Location("rter");
		fakeLocations[fix[17]].setLatitude(37.412367);
		fakeLocations[fix[17]].setLongitude(-122.028446);


		fakeLocations[fix[18]] = new Location("rter");
		fakeLocations[fix[18]].setLatitude(37.412352);
		fakeLocations[fix[18]].setLongitude(-122.028413);


		fakeLocations[fix[19]] = new Location("rter");
		fakeLocations[fix[19]].setLatitude(37.412330);
		fakeLocations[fix[19]].setLongitude(-122.028376);


		fakeLocations[fix[20]] = new Location("rter");
		fakeLocations[fix[20]].setLatitude(37.412313);
		fakeLocations[fix[20]].setLongitude(-122.028346);


		fakeLocations[fix[21]] = new Location("rter");
		fakeLocations[fix[21]].setLatitude(37.412292);
		fakeLocations[fix[21]].setLongitude(-122.028306);


		fakeLocations[fix[22]] = new Location("rter");
		fakeLocations[fix[22]].setLatitude(37.412275);
		fakeLocations[fix[22]].setLongitude(-122.028269);


		fakeLocations[fix[23]] = new Location("rter");
		fakeLocations[fix[23]].setLatitude(37.412243);
		fakeLocations[fix[23]].setLongitude(-122.028258);


		fakeLocations[fix[24]] = new Location("rter");
		fakeLocations[fix[24]].setLatitude(37.412211);
		fakeLocations[fix[24]].setLongitude(-122.028258);


		fakeLocations[fix[25]] = new Location("rter");
		fakeLocations[fix[25]].setLatitude(37.412107);
		fakeLocations[fix[25]].setLongitude(-122.028255);


		fakeLocations[fix[26]] = new Location("rter");
		fakeLocations[fix[26]].setLatitude(37.412073);
		fakeLocations[fix[26]].setLongitude(-122.028252);


		fakeLocations[fix[27]] = new Location("rter");
		fakeLocations[fix[27]].setLatitude(37.412168);
		fakeLocations[fix[27]].setLongitude(-122.028255);


		fakeLocations[fix[28]] = new Location("rter");
		fakeLocations[fix[28]].setLatitude(37.412136);
		fakeLocations[fix[28]].setLongitude(-122.028255);


		fakeLocations[fix[29]] = new Location("rter");
		fakeLocations[fix[29]].setLatitude(37.412049);
		fakeLocations[fix[29]].setLongitude(-122.028255);


		fakeLocations[fix[30]] = new Location("rter");
		fakeLocations[fix[30]].setLatitude(37.412041);
		fakeLocations[fix[30]].setLongitude(-122.028277);


		fakeLocations[fix[31]] = new Location("rter");
		fakeLocations[fix[31]].setLatitude(37.412034);
		fakeLocations[fix[31]].setLongitude(-122.028306);


		fakeLocations[fix[32]] = new Location("rter");
		fakeLocations[fix[32]].setLatitude(37.412034);
		fakeLocations[fix[32]].setLongitude(-122.028330);


		fakeLocations[fix[33]] = new Location("rter");
		fakeLocations[fix[33]].setLatitude(37.412024);
		fakeLocations[fix[33]].setLongitude(-122.028360);


		fakeLocations[fix[34]] = new Location("rter");
		fakeLocations[fix[34]].setLatitude(37.412038);
		fakeLocations[fix[34]].setLongitude(-122.028379);


		fakeLocations[fix[35]] = new Location("rter");
		fakeLocations[fix[35]].setLatitude(37.412073);
		fakeLocations[fix[35]].setLongitude(-122.028430);


		fakeLocations[fix[36]] = new Location("rter");
		fakeLocations[fix[36]].setLatitude(37.412073);
		fakeLocations[fix[36]].setLongitude(-122.028483);


		fakeLocations[fix[37]] = new Location("rter");
		fakeLocations[fix[37]].setLatitude(37.412009);
		fakeLocations[fix[37]].setLongitude(-122.028585);


		fakeLocations[fix[38]] = new Location("rter");
		fakeLocations[fix[38]].setLatitude(37.412045);
		fakeLocations[fix[38]].setLongitude(-122.028529);


		fakeLocations[fix[39]] = new Location("rter");
		fakeLocations[fix[39]].setLatitude(37.411955);
		fakeLocations[fix[39]].setLongitude(-122.028682);


		fakeLocations[fix[40]] = new Location("rter");
		fakeLocations[fix[40]].setLatitude(37.411977);
		fakeLocations[fix[40]].setLongitude(-122.028641);


		fakeLocations[fix[41]] = new Location("rter");
		fakeLocations[fix[41]].setLatitude(37.411928);
		fakeLocations[fix[41]].setLongitude(-122.028722);


		fakeLocations[fix[42]] = new Location("rter");
		fakeLocations[fix[42]].setLatitude(37.411900);
		fakeLocations[fix[42]].setLongitude(-122.028773);


		fakeLocations[fix[43]] = new Location("rter");
		fakeLocations[fix[43]].setLatitude(37.411900);
		fakeLocations[fix[43]].setLongitude(-122.028773);


		fakeLocations[fix[44]] = new Location("rter");
		fakeLocations[fix[44]].setLatitude(37.411900);
		fakeLocations[fix[44]].setLongitude(-122.028773);


		fakeLocations[fix[45]] = new Location("rter");
		fakeLocations[fix[45]].setLatitude(37.411900);
		fakeLocations[fix[45]].setLongitude(-122.028773);


		fakeLocations[fix[46]] = new Location("rter");
		fakeLocations[fix[46]].setLatitude(37.411900);
		fakeLocations[fix[46]].setLongitude(-122.028773);


		fakeLocations[fix[47]] = new Location("rter");
		fakeLocations[fix[47]].setLatitude(37.411947);
		fakeLocations[fix[47]].setLongitude(-122.028743);


		fakeLocations[fix[48]] = new Location("rter");
		fakeLocations[fix[48]].setLatitude(37.411981);
		fakeLocations[fix[48]].setLongitude(-122.028719);


		fakeLocations[fix[49]] = new Location("rter");
		fakeLocations[fix[49]].setLatitude(37.412021);
		fakeLocations[fix[49]].setLongitude(-122.028695);


		fakeLocations[fix[50]] = new Location("rter");
		fakeLocations[fix[50]].setLatitude(37.412100);
		fakeLocations[fix[50]].setLongitude(-122.028639);


		fakeLocations[fix[51]] = new Location("rter");
		fakeLocations[fix[51]].setLatitude(37.412058);
		fakeLocations[fix[51]].setLongitude(-122.028668);


		fakeLocations[fix[52]] = new Location("rter");
		fakeLocations[fix[52]].setLatitude(37.412143);
		fakeLocations[fix[52]].setLongitude(-122.028609);


		fakeLocations[fix[53]] = new Location("rter");
		fakeLocations[fix[53]].setLatitude(37.412190);
		fakeLocations[fix[53]].setLongitude(-122.028582);


		fakeLocations[fix[54]] = new Location("rter");
		fakeLocations[fix[54]].setLatitude(37.412243);
		fakeLocations[fix[54]].setLongitude(-122.028550);


		fakeLocations[fix[55]] = new Location("rter");
		fakeLocations[fix[55]].setLatitude(37.412290);
		fakeLocations[fix[55]].setLongitude(-122.028521);


		fakeLocations[fix[56]] = new Location("rter");
		fakeLocations[fix[56]].setLatitude(37.412337);
		fakeLocations[fix[56]].setLongitude(-122.028491);


		fakeLocations[fix[57]] = new Location("rter");
		fakeLocations[fix[57]].setLatitude(37.412394);
		fakeLocations[fix[57]].setLongitude(-122.028462);


		fakeLocations[fix[58]] = new Location("rter");
		fakeLocations[fix[58]].setLatitude(37.412445);
		fakeLocations[fix[58]].setLongitude(-122.028494);


		fakeLocations[fix[59]] = new Location("rter");
		fakeLocations[fix[59]].setLatitude(37.412507);
		fakeLocations[fix[59]].setLongitude(-122.028478);
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
		
		Location newLoc = new Location("rter");
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
