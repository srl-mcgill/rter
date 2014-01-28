/**
 * 
 */
package ca.nehil.rter.streamingapp2;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
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
	
	private ConcurrentHashMap<Integer, POI> POIs;
	private SensorSource sensorSource;
	
	/**
	 * 
	 */
	public POIList(Context context, URL baseURL, String rterCredentials) {
		try {
			serverURI = new URI("ws", "", baseURL.getHost(), 80, baseURL.getPath(), "", "");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "URISyntaxException");
			e.printStackTrace();
		}
		this.rterCredentials = rterCredentials;
		sensorSource = SensorSource.getInstance(context);
		initClient();
		client.connect();
	}
	
	private void initClient() {
		List<BasicNameValuePair> extraHeaders = Arrays.asList(
			    new BasicNameValuePair("Cookie", "rter-credentials=" + rterCredentials)
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

}
