/**
 * 
 */
package ca.nehil.rter.streamingapp;

import de.grundid.ble.sensors.tisensortag.TISensorTagTemperature;
import de.grundid.ble.sensors.tisensortag.TISensorTagAccelerometer;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.khronos.opengles.GL10;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import ca.nehil.rter.streamingapp.overlay.Triangle;
import ca.mcgill.srl.bledevices.BluetoothLeService;
import ca.mcgill.srl.bledevices.DeviceScanService;
import ca.mcgill.srl.bledevices.TISensorTagService;

import com.codebutler.android_websockets.WebSocketClient;

/**
 * @author mike
 *
 */
public class POIList {

	private final static String CLASS_LABEL = "POIList";
	private final static String TAG = CLASS_LABEL;
	private WebSocketClient client;
	private URI serverURI;
	private String rterCredentials;
	private ConcurrentHashMap<Integer, POI> items;
	private SensorSource sensorSource;
	private Context context;
	Triangle triangleFrame;
	float[] displacement = new float[2];
	private double sensorTagTemperatureAmbient;
	private double sensorTagTemperatureIR;
	StreamingActivity mStreamingActivity;
	private boolean mSensorTagBound = false;
	private Intent scanIntent;
	
	private TISensorTagService mTISensorTagService;
	
	public POIList(Context context, URL baseURL, String rterCredentials) {
		Log.d("SensorDebug", "Made a POIlist");
		this.context = context;
		this.mStreamingActivity = (StreamingActivity)context; // Need instance of streaming activity to alter textview and send back data.
		items = new ConcurrentHashMap<Integer, POI>();
		try {
			serverURI = new URI("ws", baseURL.getHost(), baseURL.getPath() + "/1.0/streaming/items/websocket", null);
		} catch (URISyntaxException e) {
			Log.e(TAG, "URISyntaxException");
			e.printStackTrace();
		}
		this.rterCredentials = rterCredentials;
		sensorSource = SensorSource.getInstance(context);
		initClient();
		Log.d(TAG, "Connecting to " + serverURI.toString());
		client.connect();
		generateTestList();
		
		//SensorTag stuff begins
		registerSensortagReceiver();
		
		//SensorTag ends
		
		triangleFrame = new Triangle();
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
			        // TODO: use gson library instead for object mapping
			        try {
			        	JSONObject event = new JSONObject(message);
						String action = event.getString("Action");
						JSONObject item = event.getJSONObject("Val");
						Log.d("alok", "From server: " + item.getDouble("Temperature"));
						if(action.equals("create") || action.equals("update")) {
							boolean hasGeo = item.getBoolean("HasGeo");
							String type = item.getString("Type");
							boolean live = item.getBoolean("Live");
							if(!hasGeo) {
								final String[] parts = type.split(":", 2);
								if(parts[0].equals("message")){
									
									((Activity)context).runOnUiThread(new Runnable(){
									    public void run() {
									    	Toast.makeText(context, "Message: " + parts[1], Toast.LENGTH_SHORT).show();
									    }
									 });
									
									return;
								} else {
									return;
								}
							} else if(type.equals("streaming-video-v1") && live || type.equals("beacon")) {
								int id = item.getInt("ID");
								POI poi = new POI(context, 
										id, 
										item.getDouble("Lat"),
										item.getDouble("Lng"), "red", 
										"", 
										type, null);
								items.put(Integer.valueOf(id), poi);
							} else {
								return;
							}
						} else if(action.equals("delete")) {
							items.remove(Integer.valueOf(item.getInt("ID")));
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
	
	//TODO: Why arent these used?
	private void connect() {
		client.connect();
	}
	
	private void disconnect() {
		client.disconnect();
	}
	
	/*
	 * Hard-coded geo points representing the two types of POIs (type 1 and type2), for demo purposes. This data would be fetched from the server
	 * in a real scenario.
	 */
	private void generateTestList() {
		POI poi1 = new POI(context , 1, 45.5056, -73.5769, "", "http://rter.zapto.org:8080/v1/videos/385/thumb/000000001.jpg", "type1", null);
		POI poi2 = new POI(context, 2, 45.5058, -73.5755, "","","type2", null);
//		POI poi3 = new POI(context, 3, 3.5, 45.5047, -73.5762, "", "", "");
		
		items.put(Integer.valueOf(1), poi1);
		items.put(Integer.valueOf(2), poi2);
	}
	
	/**
	 * [SensorTag] 
	 */
	public void registerSensortagReceiver(){
		Log.d("alok", "registered app from tags");
		//Start the device scan service
		scanIntent = new Intent(context, DeviceScanService.class);
		context.startService(scanIntent);
		
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction("TISensorTag_characteristic");
		LocalBroadcastManager.getInstance(context).registerReceiver(sensorTagDataReceiver,  intentFilter);
		LocalBroadcastManager.getInstance(context).registerReceiver(sensorTagDiscoveryReceiver, new IntentFilter("DeviceFound"));
	}
	
	/**
	 * [SensorTag]
	 * Parent activity (StreamingActivity in this case) should call this method in its onPause/onDestroy.
	 */
	public void unregisterSensortagReceiver(){
		Log.d("alok", "unregistered app from tags");
		LocalBroadcastManager.getInstance(context).unregisterReceiver(sensorTagDataReceiver);
		LocalBroadcastManager.getInstance(context).unregisterReceiver(sensorTagDiscoveryReceiver);
		
		if(mSensorTagBound){
			context.unbindService(sensorTagConnection);
		}
		
		//Stop the service.
		context.stopService(scanIntent);
	}
	
	/**
	 * [SensorTag]
	 */
	private BroadcastReceiver sensorTagDiscoveryReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("alok", "Device discovered broadcast");
			String device = intent.getStringExtra("device");
			sensorTagConnect(device);
		}
	};
	
	/**
	 * [SensorTag] This is where we get temperature (and possibly other) data from the tags.
	 * Can also figure out if we connected, disconnected or discovered services from the tags.
	 */
	private BroadcastReceiver sensorTagDataReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(final Context context, Intent intent) {
			final String action = intent.getAction();
			if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
				mTISensorTagService.temperatureEnable((byte)1); //Enable the temperature sensor.
				mTISensorTagService.temperatureSetPeriod(1000); 
				
				mTISensorTagService.accelerometerEnable((byte)1);
				mTISensorTagService.accelerometerSetPeriod(200);
			}else if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
				mStreamingActivity.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						Toast.makeText(context, "Connected to a sensor tag", Toast.LENGTH_SHORT).show();
					}
				});
			}else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
			
				if(mSensorTagBound){
					context.unbindService(sensorTagConnection);
					mSensorTagBound = false;
				}
			}else if("TISensorTag_characteristic".equals(action)){
				// Get extra data included in the Intent
                Bundle bundle = intent.getExtras();
                switch(bundle.getInt("type")) {
                case TISensorTagService.TISENSORTAG_SENSORTYPE_ACCELEROMETER:
                	TISensorTagAccelerometer tiSensorTagAccelerometer = (TISensorTagAccelerometer)bundle.getSerializable("sensor_object");
//                	Log.d("SensorDebug", "gravityx: " + tiSensorTagAccelerometer.gravityX());
//                	Log.d("SensorDebug", "linearx: " + tiSensorTagAccelerometer.linearX());
//                	Log.d("SensorDebug", "x: " + tiSensorTagAccelerometer.x());
                	double x = tiSensorTagAccelerometer.linearX();
                	double y = tiSensorTagAccelerometer.linearY();
                	double z = tiSensorTagAccelerometer.linearZ();
                	double test = Math.sqrt((x*x) + (y*y) + (z*z));
//                	Log.d("SensorDebug", "srqt: " + test);
                	Log.d("SensorDebug", "x: " + x + " y: " + y + " z" + z);
                	break;
                case TISensorTagService.TISENSORTAG_SENSORTYPE_TEMPERATURE:
                    TISensorTagTemperature tiSensorTagTemperature = (TISensorTagTemperature)bundle.getSerializable("sensor_object");
                    
                    Log.d("alok", "Got temperature event: " + tiSensorTagTemperature.asString());
                    sensorTagTemperatureAmbient = Math.round(tiSensorTagTemperature.getAmbient());
                    sensorTagTemperatureIR = Math.round(tiSensorTagTemperature.getTarget());
                    mStreamingActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        	mStreamingActivity.tempTextView.setText("Ambient: " + convertCtoF(sensorTagTemperatureAmbient) + "F Focus:" + convertCtoF(sensorTagTemperatureIR) + "F");
                        	mStreamingActivity.tempTextView.setTextColor(getColor(sensorTagTemperatureIR));
                        	mStreamingActivity.sensorTagTemperatureIR = sensorTagTemperatureIR;
                        }
                    });
                    break;
                }
			}
		}
	};
	
	private int getColor(double temperature){
		int color = Color.BLUE;
		if(temperature >60){
			color = Color.RED;
			POI poi3 = new POI(context, 3, sensorSource.getLocation().getLatitude(), sensorSource.getLocation().getLongitude(), "", "", "sensorTag", (int)temperature);
			items.put(Integer.valueOf(3), poi3);
		}else if(temperature>=40 && temperature<=60){
			color = Color.YELLOW;
		}else if(temperature<40){
			color = Color.CYAN;
		}
		return color;
	}
	
	private double convertCtoF(double temperatureC){
		double temperatureF;
		temperatureF = (9*temperatureC)/5 + 32.0;
		return temperatureF;
	}
	
	/**
	 * [SensorTag]
	 * @param address Address of the SensorTag to connect to.
	 */
	public void sensorTagConnect(String address){
		Intent tagConnectIntent = new Intent(context, TISensorTagService.class);
		tagConnectIntent.putExtra("deviceAddress", address);
		tagConnectIntent.putExtra("flushGatt", false);
		tagConnectIntent.putExtra("retryTimeout", 60);
		context.bindService(tagConnectIntent, sensorTagConnection, context.BIND_AUTO_CREATE);
	}
	
	/**
	 * [SensorTag]
	 */
	private ServiceConnection sensorTagConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			
			TISensorTagService.LocalBinder binder = (TISensorTagService.LocalBinder) service;
			mTISensorTagService = binder.getService();
			mSensorTagBound = true;
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mSensorTagBound = true;
		}
	};
	
	public void render(GL10 gl, Location userLocation) {
		gl.glLineWidth(2);
		
		float[] lrm = sensorSource.getLandscapeRotationMatrix();
		float scale = 0.1f;
		displacement[0] += lrm[2] * scale; // used for the auto-walk demo feature to show POI size increase
		displacement[1] += lrm[6] * scale;
		
		// foreach POI as poi, poi.render(gl, userLocation, screenSize)
		for (POI item : items.values()) {
		    item.render(gl, userLocation, displacement);
		}
//		drawAxes(gl, lrm);
	}
	
	/*
	 * Draw axes for debug purposes.
	 */
	private void drawAxes(GL10 gl, float[] lrm){
		
		gl.glLineWidth(1);
		
		gl.glLoadIdentity();
		gl.glPushMatrix();
		gl.glTranslatef(-1.5f, 0.5f, -6.0f);
		gl.glScalef(0.3f, 0.3f, 0.3f);
		gl.glMultMatrixf(lrm, 0);
		
		gl.glPushMatrix();
		gl.glRotatef(90, 0, 0, -1);
		gl.glTranslatef(0.0f, 2.0f, 0.0f);
		triangleFrame.colour(Triangle.Colour.RED);
		for( int i = 0; i < 8; i++ ) {
			 gl.glRotatef(360.0f/8.0f, 0, 1, 0);
			 triangleFrame.draw(gl, true);
		}
		gl.glPopMatrix();
		
		gl.glPushMatrix();
		gl.glTranslatef(0.0f, 1.0f, 0.0f);
		 triangleFrame.colour(Triangle.Colour.GREEN);
		 for( int i = 0; i < 8; i++ ) {
			 gl.glRotatef(360.0f/8.0f, 0, 1, 0);
			 triangleFrame.draw(gl, true);
		 }
		 gl.glPopMatrix();
		 		
		 gl.glPushMatrix();
		 gl.glRotatef(90, 1, 0, 0);
		 gl.glTranslatef(0.0f, 1.0f, 0.0f);
		 triangleFrame.colour(Triangle.Colour.BLUE);
		 for( int i = 0; i < 8; i++ ) {
			 gl.glRotatef(360.0f/8.0f, 0, 1, 0);
			 triangleFrame.draw(gl, true);
		 }
		 gl.glPopMatrix();
		 
		 gl.glPopMatrix();
		 
	}

}
