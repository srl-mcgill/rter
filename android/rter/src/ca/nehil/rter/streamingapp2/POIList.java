/**
 * 
 */
package ca.nehil.rter.streamingapp2;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.khronos.opengles.GL10;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;

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
	
	/**
	 * 
	 */
	public POIList(Context context, URL baseURL, String rterCredentials) {
		this.context = context;
		items = new ConcurrentHashMap<Integer, POI>();
		try {
			serverURI = new URI("ws", baseURL.getHost(), baseURL.getPath() + "/1.0/streaming/items/websocket", null);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "URISyntaxException");
			e.printStackTrace();
		}
		this.rterCredentials = rterCredentials;
		sensorSource = SensorSource.getInstance(context);
		initClient();
		Log.d(TAG, "Connecting to " + serverURI.toString());
		// initializeList();
		client.connect();
		generateTestList();
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
							if(!hasGeo) {
								return;
							}
							String type = item.getString("Type");
							boolean live = item.getBoolean("Live");
							if(type.equals("streaming-video-v1") && live || type.equals("beacon")) {
								int id = item.getInt("ID");
								POI poi = new POI(context, 
										id, 
										null, //item.getBoolean("HasHeading") ? item.getDouble("Heading") : null,
										item.getDouble("Lat"),
										item.getDouble("Lng"), "red", 
										"", 
										type);
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
	
	private void connect() {
		client.connect();
	}
	
	private void disconnect() {
		client.disconnect();
	}
	
	private void generateTestList() {
		POI poi1 = new POI(context , 1, 1.5, 45.5056, -73.5769, "", "http://rter.zapto.org:8080/v1/videos/385/thumb/000000001.jpg", "type1");
		POI poi2 = new POI(context, 2, 2.5, 45.5060, -73.5769, "","","type2");
//		POI poi3 = new POI(context, 3, 3.5, 45.5047, -73.5762, "", "", "");
		
		items.put(Integer.valueOf(1), poi1);
		items.put(Integer.valueOf(2), poi2);
	}
	
	public void render(GL10 gl, Location userLocation, Point screenSize) {
		// foreach POI as poi, poi.render(gl, userLocation, screenSize)
		for (POI item : items.values()) {
		    item.render(gl, userLocation, screenSize);
		}
		/*
		for (Map.Entry<Integer, POI> entry : items.entrySet()) {
		    // ...
		}
		*/
	}

}
