/**
 * 
 */
package ca.nehil.rter.streamingapp;

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
import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;
import ca.nehil.rter.streamingapp.overlay.Triangle;

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
	private SensorSource mSensorSource;
	private Context context;
	Triangle triangleFrame;
	float[] displacement = new float[2];
	
	public POIList(Context context, URL baseURL, String rterCredentials, SensorSource mSensorSource) {
		this.context = context;
		items = new ConcurrentHashMap<Integer, POI>();
		try {
			serverURI = new URI("ws", baseURL.getHost(), baseURL.getPath() + "/1.0/streaming/items/websocket", null);
		} catch (URISyntaxException e) {
			Log.e(TAG, "URISyntaxException");
			e.printStackTrace();
		}
		this.rterCredentials = rterCredentials;
		this.mSensorSource = mSensorSource;
		initClient();
		Log.d(TAG, "Connecting to " + serverURI.toString());
		client.connect();
		generateTestList();
		
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
										type,
										mSensorSource);
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
		Location userLocation = mSensorSource.getLocation();
		POI poi1 = new POI(context , 1, userLocation.getLatitude() + 0.0003, userLocation.getLongitude() + 0.0001, "", "http://rter.zapto.org:8080/v1/videos/385/thumb/000000001.jpg", "type1", mSensorSource);
		POI poi2 = new POI(context, 2, userLocation.getLatitude() - 0.0005, userLocation.getLongitude() + 0.0003, "","","type2", mSensorSource);
		POI poi3 = new POI(context, 3, userLocation.getLatitude() - 0.0003, userLocation.getLongitude() + 0.0008, "","","type2", mSensorSource);
		
		items.put(Integer.valueOf(1), poi1);
		items.put(Integer.valueOf(2), poi2);
		items.put(Integer.valueOf(3), poi3);
	}
	
	public void render(GL10 gl, Location userLocation) {

		gl.glLineWidth(2);
		
		float[] lrm = mSensorSource.getLandscapeRotationMatrix();
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
