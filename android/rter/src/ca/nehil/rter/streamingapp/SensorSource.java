package ca.nehil.rter.streamingapp;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.codebutler.android_websockets.WebSocketClient;

import ca.nehil.rter.streamingapp.R;
import android.app.Activity;
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
import android.widget.Toast;

public class SensorSource implements SensorEventListener, LocationListener{

	private final static String TAG = "SensorSource"; 
	
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
	
	private boolean get_location_from_server;
	private WebSocketClient client;
	private URI serverURI;
	private String rterCredentials;
	private String username;

	public SensorSource(Context context, boolean get_location_from_server, URL baseURL, String rterCredentials, String username){
		mcontext = context;
		
		this.get_location_from_server = get_location_from_server;
		
		if(get_location_from_server) {
			try {
				serverURI = new URI("ws", "", baseURL.getHost(), baseURL.getPort(), baseURL.getPath() + "/1.0/streaming/users/websocket", "", "");
			} catch (URISyntaxException e) {
				Log.e(TAG, "URISyntaxException");
				e.printStackTrace();
			}
			this.rterCredentials = rterCredentials;
			this.username = username;
			initClient();
			Log.d(TAG, "Connecting to " + serverURI.toString());
			client.connect();
		} else {
			mLocationManager.requestLocationUpdates(provider, 1000, 0, singleton);
			mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			provider = mLocationManager.getBestProvider(criteria, true);
		}
		
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

		locationIntent = new Intent (context.getString(R.string.LocationEvent));
		sensorIntent = new Intent (context.getString(R.string.SensorEvent));
		localBroadcastManager = LocalBroadcastManager.getInstance(context);
	}

	public static SensorSource getInstance(Context context, boolean get_location_from_server, URL baseURL, String rterCredentials, String username){
		if (singleton == null){
			singleton = new SensorSource(context, get_location_from_server, baseURL, rterCredentials, username);
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
		if(!get_location_from_server) {
			mLocationManager.removeUpdates(this);
		}
	}

	public Location getLocation(){
		if(location == null) {
			Log.w(TAG, "userlocation is null");
			location = new Location("rtER");
			location.setLatitude(45.505958f);
			location.setLongitude(-73.576254f);
		}
		return location;
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
	
	private void initClient() {
		List<BasicNameValuePair> extraHeaders = Arrays.asList(
			    new BasicNameValuePair("Cookie", rterCredentials)
			);

			client = new WebSocketClient(serverURI, new WebSocketClient.Listener() {
			    @Override
			    public void onConnect() {
			        Log.d(TAG, "Connected!");
			        //client.send("hello!");
			    }

			    @Override
			    public void onMessage(String message) {
			        Log.d(TAG, String.format("Got string message! %s", message));
			        try {
			        	JSONObject event = new JSONObject(message);
						String action = event.getString("Action");
						JSONObject user = event.getJSONObject("Val");
						if(action.equals("update") && user.getString("Username").equals(username)) {
							Log.d(TAG, "setting location to " + user.getDouble("Lat") + ", " + user.getDouble("Lng"));
							Location userLocation = new Location("rtER Server");
							userLocation.setLatitude(user.getDouble("Lat"));
							userLocation.setLongitude(user.getDouble("Lng"));
							onLocationChanged(userLocation);
						}				
					} catch (JSONException e) {
						Log.e(TAG, "Malformed JSON received on websocket: " + e.toString());
						return;
					}
			        
			    }

			    @Override
			    public void onMessage(byte[] data) {
			        Log.d(TAG, String.format("Got binary message! %s", new String(data)));
			    }

			    @Override
			    public void onDisconnect(int code, String reason) {
			        Log.d(TAG, String.format("Disconnected! Code: %d Reason: %s", code, reason));
			    }

			    @Override
			    public void onError(Exception error) {
			        Log.e(TAG, "Error!", error);
			    }

			}, extraHeaders);

			//client.send(new byte[] { (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF });		
	}
}
