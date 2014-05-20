/*


 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.nehil.rter.streamingapp2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import ca.nehil.rter.streamingapp2.overlay.CameraGLSurfaceView;
import ca.nehil.rter.streamingapp2.overlay.OverlayController;
import android.view.OrientationEventListener;
import static com.googlecode.javacv.cpp.opencv_core.*;

public class StreamingActivity extends Activity implements OnClickListener {

	private static String server_url;
	private SharedPreferences storedValues;
	private HandShakeTask handshakeTask = null;
	private int PutHeadingTimer = 2000; //	Updating the User location, heading and orientation every 4 secs.
	private SharedPreferences cookies;
	private SharedPreferences.Editor prefEditor;
	private Boolean PutHeadingBool = false;
	private String setUsername = null;
	private String setRterCredentials = null;
	private String recievedRterResource = null;
	private String recievedItemID = null;
	private String recievedRterSignature;
	private String recievedRterValidUntil = null;
	private String authToken;
	private Handler handler = new Handler();
	private Thread putHeadingfeed;
	private CameraGLSurfaceView mGLView;
	private OverlayController overlay;
	private SensorSource sensorSource;
	private SensorManager mSensorManager;
	private Sensor mAcc, mMag;
	private Camera mCamera;
	private int numberOfCameras;
	private float lati;
	private float longi;
	private LocationManager locationManager;
	private String provider;

	/*************
	 * Mikes variables for JAVACV testing
	 */
	private final static String CLASS_LABEL = "RecordActivity";
	private final static String LOG_TAG = CLASS_LABEL;
	private PowerManager.WakeLock mWakeLock;
	private long startTime = 0;
	private boolean recording = false;
	private volatile FFmpegFrameSender recorder;
	private boolean isPreviewOn = false;
	private int imageWidth = 320;
	private int imageHeight = 240;
	private int frameRate = 30;

	/* video data getting thread */
	private Camera cameraDevice;
	private CameraView cameraView;
	private OrientationEventListener myOrientationEventListener;
	private Boolean flipVideo = false;
	private IplImage yuvIplimage = null;
	private final int live_width = 640;
	private final int live_height = 480;
	private int screenWidth, screenHeight;
	/* mikes variables ends */

	private static final String TAG = "Streaming Activity";
	private FrameLayout topLayout;

	public class NotificationRunnable implements Runnable {
		private String message = null;
		public void run() {
			if (message != null && message.length() > 0) {
				showNotification(message);
			}
		}
		public void setMessage(String message) {
			this.message = message;
		}
	}

	/*	Post this to the Handler when the background thread notifies */
	private final NotificationRunnable notificationRunnable = new NotificationRunnable();

	public void showNotification(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_streaming);

		storedValues = getSharedPreferences("CommonValues", MODE_PRIVATE);
		server_url = storedValues.getString("server_url", "not-set");
		/* Orientation listenever implementation to orient video */
		myOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
			@Override
			public void onOrientationChanged(int orientation) {
				int rotation = getWindowManager().getDefaultDisplay().getRotation();
				if(rotation == Surface.ROTATION_270) {
					flipVideo = true;
				}
				else {
					flipVideo = false;
				}
			}
		};
		myOrientationEventListener.enable();

		Log.e(TAG, "onCreate");

		overlay = new OverlayController(this); // OpenGL overlay 
		sensorSource = SensorSource.getInstance(this);

		/* Orientation */
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		/* Retrieve user auth data from cookie */
		cookies = getSharedPreferences("RterUserCreds", MODE_PRIVATE);
		prefEditor = cookies.edit();
		setUsername = cookies.getString("Username", "not-set");
		setRterCredentials = cookies.getString("RterCreds", "not-set");
		if (setRterCredentials.equalsIgnoreCase("not-set")
				|| setRterCredentials == null) {
			Log.e("PREFS", "Login Not successful, please restart");
		}
		Log.d("PREFS", "Prefs ==> rter_Creds:" + setRterCredentials);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Log.e(TAG, "GPS not available");
		}


		/* Get last known location if possible and initialize location variables */
		Criteria criteria = new Criteria();
		provider = locationManager.getBestProvider(criteria, true);
		if (provider != null) {
			Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			if (location != null) {
				lati = (float) (location.getLatitude());
				longi = (float) (location.getLongitude());
			} else {
				Toast toast = Toast.makeText(this, "Location not available",
						Toast.LENGTH_LONG);
				toast.setGravity(Gravity.TOP, 0, 0);
				toast.show();
				lati = (float) (45.505958f); // Hard coded location for testing purposes.
				longi = (float) (-73.576254f);
			}
		}

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, CLASS_LABEL);
		mWakeLock.acquire();

		/* Test, set desired orienation to north */
		overlay.letFreeRoam(false);
		overlay.setDesiredOrientation(0.0f);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
					CLASS_LABEL);
			mWakeLock.acquire();
		}

		locationManager.requestLocationUpdates(provider, 0, 1000, sensorSource); // Register sensorSource to listen to location events
		
		/* Register SensorSource to listen to accelerometer and magnetic field sensors */
		mSensorManager.registerListener(sensorSource, mAcc, SensorManager.SENSOR_DELAY_NORMAL); 
		mSensorManager.registerListener(sensorSource, mMag, SensorManager.SENSOR_DELAY_NORMAL);

		/* Registering a listener for the SensorEvent and LocationEvent broadcasts sent by SensorSource */
		LocalBroadcastManager.getInstance(this).registerReceiver(sensorBroadcastReceiver,
				new IntentFilter(getString(R.string.SensorEvent)));
		LocalBroadcastManager.getInstance(this).registerReceiver(locationBroadcastReceiver, 
				new IntentFilter(getString(R.string.LocationEvent)));

		initLayout();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		locationManager.removeUpdates(sensorSource);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorBroadcastReceiver);	// Stop listening for broadcast from SensorSource
		topLayout.removeAllViews(); // Removes the camera view from the layout, as it is re-added in initlayout from onResume.

		if (putHeadingfeed != null) {
			if (putHeadingfeed.isAlive()) {
				putHeadingfeed.interrupt();
			}
		}

		// @Nehil should this be removed?
		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;

		}

		if (mSensorManager != null) {
			mSensorManager.unregisterListener(sensorSource);
		}

		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (putHeadingfeed != null) {
			if (putHeadingfeed.isAlive()) {
				putHeadingfeed.interrupt();
			}
		}

		if (mCamera != null) {
			mCamera.release();
			mCamera = null;

		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		recording = false;
		myOrientationEventListener.disable();

		if (cameraView != null) {
			cameraDevice.release();
			cameraDevice = null;
		}
	}

	private void initLayout() {
		/* get size of screen */
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();
		FrameLayout.LayoutParams layoutParam = null;
		LayoutInflater myInflate = null;
		myInflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		topLayout = new FrameLayout(this);
		setContentView(topLayout);

		mGLView = overlay.getGLView(); // OpenGLview

		int display_width_d = (int) (1.0 * screenWidth);
		int display_height_d = (int) (1.0 * screenHeight);
		int button_width = 0;
		int button_height = 0;
		int prev_rw, prev_rh;

		if (1.0 * display_width_d / display_height_d > 1.0 * live_width
				/ live_height) {
			prev_rh = display_height_d;
			button_height = display_height_d;
			prev_rw = (int) (1.0 * display_height_d * live_width / live_height);
			button_width = display_width_d - prev_rw;

		} else {
			prev_rw = display_width_d;
			prev_rh = (int) (1.0 * display_width_d * live_height / live_width);
		}

		layoutParam = new FrameLayout.LayoutParams(prev_rw, prev_rh, Gravity.CENTER);
		Log.d("LAYOUT", "display_width_d:" + display_width_d
				+ ":: display_height_d:" + display_height_d + ":: prev_rw:"
				+ prev_rw + ":: prev_rh:" + prev_rh + ":: live_width:"
				+ live_width + ":: live_height:" + live_height
				+ ":: button_width:" + button_width + ":: button_height:"
				+ button_height);
		cameraDevice = openCamera();
		cameraView = new CameraView(this, cameraDevice);

		topLayout.addView(cameraView, layoutParam);
		topLayout.addView(mGLView, layoutParam);

		FrameLayout preViewLayout = (FrameLayout) myInflate.inflate(R.layout.activity_streaming, null);
		layoutParam = new FrameLayout.LayoutParams(screenWidth, screenHeight);
		topLayout.addView(preViewLayout, layoutParam);
		Log.i(LOG_TAG, "cameara preview start: OK");

		final Button recorderButton = (Button) findViewById(R.id.recorder_control);
		recorderButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!recording) {
					Log.d(TAG, "attemptHandshaking");
					attemptHandshake();
					Log.w(LOG_TAG, "Start Button Pushed");
					recorderButton.setText("Stop");
				} else {
					stopRecording();
					Log.w(LOG_TAG, "Stop Button Pushed");
					recorderButton.setText("Start");
				}
			}
		});
	}

	/*
	 * Creates and initiates the Camera object.
	 */
	private Camera openCamera() {
		Camera cameraDevice = Camera.open();
		numberOfCameras = Camera.getNumberOfCameras();
		for(int i = 0; i < numberOfCameras && cameraDevice == null; i++) {
			Log.d(LOG_TAG, "opening camera #" + String.valueOf(i));
			cameraDevice = Camera.open(i);
		}
		try {
			if(cameraDevice == null) {
				throw new Exception("No camera device found");
			}
		} catch (Exception e) {
			cameraDevice.release();
			Log.e(LOG_TAG, e.getMessage());
			e.printStackTrace();
		}
		return cameraDevice;
	}

	// ---------------------------------------
	// Initialize ffmpeg_recorder
	// ---------------------------------------
	private void initRecorder() {

		Log.w(LOG_TAG, "init recorder");
		if (yuvIplimage == null) {
			yuvIplimage = IplImage.create(imageWidth, imageHeight,
					IPL_DEPTH_8U, 2);
			Log.i(LOG_TAG, "create yuvIplimage");
		}
		if (recievedRterResource != null) {
			Log.e(LOG_TAG, "rterResource" + recievedRterResource);
			recorder = new FFmpegFrameSender(recievedRterResource, authToken,
					imageWidth, imageHeight);
			recorder.setVideoCodec(28); // H264
			// Set in the surface changed method
			recorder.setFrameRate(frameRate);
			Log.i(LOG_TAG, "recorder initialize success");
		} else {
			Log.e(LOG_TAG, "else rterResource is null");
		}
	}

	public void startRecording() {
		putHeadingfeed = new PutSensorsFeed(this.handler,
				this.notificationRunnable);
		try {
			PutHeadingBool = true;
			putHeadingfeed.start();
			recorder.start();
			startTime = System.currentTimeMillis();
			recording = true;
			// audioThread.start();

		} catch (FFmpegFrameSender.Exception e) {
			e.printStackTrace();
		}
	}

	public void stopRecording() {
		PutHeadingBool = false;
		if (putHeadingfeed != null) {
			Log.v(LOG_TAG, "Stopping Put Heading Feed");
			putHeadingfeed.interrupt();

		} else {
			Log.v(LOG_TAG, "Some Issue with this thread");
		}

		putHeadingfeed = null;
		CloseFeed closefeed = new CloseFeed(this.handler,
				this.notificationRunnable);
		closefeed.start();

		if (recorder != null && recording) {
			recording = false;
			Log.v(LOG_TAG,
					"Finishing recording, calling stop and release on recorder");
			try {
				recorder.stop();
				recorder.release();
			} catch (FFmpegFrameSender.Exception e) {
				e.printStackTrace();
			}
			recorder = null;

		}
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (recording) {
				stopRecording();
			}
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/* Inflate the menu; this adds items to the action bar if it is present. */
		menu.add(0, 0, 0, "Start");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().equals("Start")) {
			if (!recording) {
				Log.d(TAG, "attemptHandshaking");
				attemptHandshake();
				Log.w(LOG_TAG, "Start Button Pushed");
				item.setTitle("Stop");
			}

		} else if (item.getTitle().equals("Stop")) {
			stopRecording();
			Log.w(LOG_TAG, "Stop Button Pushed");
			item.setTitle("Start");
		}
		return super.onOptionsItemSelected(item);
	}

	public void attemptHandshake() {
		// Show a progress spinner, and kick off a background task to
		// perform the user login attempt.
		handshakeTask = new HandShakeTask();
		handshakeTask.execute();

	}

	/* Receiver for Sensor broadcast events */ 
	private BroadcastReceiver sensorBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// Extract data included in the Intent
		}
	};

	/* Receiver for Location broadcast events  */
	private BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Location location;
			location = sensorSource.getLocation();
			lati = (float) (location.getLatitude());
			longi = (float) (location.getLongitude());
		}
	};

	public static byte[] convertStringToByteArray(String s) {
		byte[] theByteArray = s.getBytes();
		Log.e(TAG, "length of byte array" + theByteArray.length);
		return theByteArray;
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	
	class CloseFeed extends Thread {
		private Handler handler = null;
		private NotificationRunnable runnable = null;

		public CloseFeed(Handler handler, NotificationRunnable runnable) {
			this.handler = handler;
			this.runnable = runnable;
			this.handler.post(this.runnable);
		}

		/**
		 * Show UI notification.
		 * 
		 * @param message
		 */
		private void showMessage(String message) {
			this.runnable.setMessage(message);
			this.handler.post(this.runnable);
		}

		@Override
		public void run() {
			showMessage("Closing feed");

			JSONObject jsonObjSend = new JSONObject();

			Date date = new Date();
			SimpleDateFormat dateFormatUTC = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss'Z'");
			dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
			String formattedDate = dateFormatUTC.format(date);
			Log.i(TAG, "The Stop Timestamp " + formattedDate);

			try {
				jsonObjSend.put("Live", false);
				jsonObjSend.put("StopTime", formattedDate);
				// Output the JSON object we're sending to Logcat:
				Log.i(TAG,
						"Body of closefeed json = " + jsonObjSend.toString(2));

				int TIMEOUT_MILLISEC = 10000; // = 10 seconds
				URL url = new URL(server_url + "/1.0/items/" + recievedItemID);
				HttpURLConnection httpcon = (HttpURLConnection) url
						.openConnection();

				httpcon.setRequestProperty("Cookie", setRterCredentials);
				Log.i(TAG, "Cookie being sent" + setRterCredentials);
				httpcon.setRequestMethod("PUT");
				httpcon.setConnectTimeout(TIMEOUT_MILLISEC);
				httpcon.setReadTimeout(TIMEOUT_MILLISEC);
				httpcon.connect();
				byte[] outputBytes = jsonObjSend.toString().getBytes("UTF-8");
				OutputStream os = httpcon.getOutputStream();
				os.write(outputBytes);

				os.close();

				int status = httpcon.getResponseCode();
				Log.i(TAG, "Status of response " + status);
				switch (status) {
				case 200:
				case 201:
					Log.i(TAG, "Feed Close successful");

				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	class PutSensorsFeed extends Thread {
		private Handler handler = null;
		private NotificationRunnable runnable = null;

		public PutSensorsFeed(Handler handler, NotificationRunnable runnable) {
			this.handler = handler;
			this.runnable = runnable;
			this.handler.post(this.runnable);
		}

		/**
		 * Show UI notification.
		 * 
		 * @param message
		 */
		private void showMessage(String message) {
			this.runnable.setMessage(message);
			this.handler.post(this.runnable);
		}

		private void postHeading() {
			JSONObject jsonObjSend = new JSONObject();

			try {

				float lat = lati;
				float lng = longi;
				float heading = overlay.getCurrentOrientation();
				jsonObjSend.put("Lat", lat);
				jsonObjSend.put("Lng", lng);
				jsonObjSend.put("Heading", heading);

				// Output the JSON object we're sending to Logcat:
				Log.i(TAG, "PUTHEADNG::Body of update heading feed json = "
						+ jsonObjSend.toString(2));

				int TIMEOUT_MILLISEC = 1000; // = 1 seconds
				Log.i(TAG, "postHeading()Put Request being sent" + server_url
						+ "/1.0/items/" + recievedItemID);
				URL url = new URL(server_url + "/1.0/items/" + recievedItemID + "/geolocations");
				HttpURLConnection httpcon = (HttpURLConnection) url
						.openConnection();
				httpcon.setRequestProperty("Cookie", setRterCredentials);
				httpcon.setRequestProperty("Content-Type", "application/json");
				httpcon.setRequestMethod("POST");
				httpcon.setConnectTimeout(TIMEOUT_MILLISEC);
				httpcon.setReadTimeout(TIMEOUT_MILLISEC);
				httpcon.connect();
				byte[] outputBytes = jsonObjSend.toString().getBytes("UTF-8");
				OutputStream os = httpcon.getOutputStream();
				os.write(outputBytes);

				os.close();

				int status = httpcon.getResponseCode();
				Log.i(TAG, "PUTHEADNG Status of response " + status);
				switch (status) {
				case 200:
				case 304:
					Log.i(TAG, "PUTHEADNG sensor Feed response = successful");

				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void getHeading() {

			try {

				// Getting the user orientation
				int TIMEOUT_MILLISEC = 1000; // = 1 seconds
				URL getUrl = new URL(server_url + "/1.0/users/" + setUsername
						+ "/direction");
				Log.i(TAG, "Get user heading URL" + getUrl);

				HttpURLConnection httpcon2 = (HttpURLConnection) getUrl
						.openConnection();
				httpcon2.setRequestProperty("Cookie", setRterCredentials);
				httpcon2.setRequestMethod("GET");
				httpcon2.setConnectTimeout(TIMEOUT_MILLISEC);
				httpcon2.setReadTimeout(TIMEOUT_MILLISEC);
				httpcon2.connect();

				int getStatus = httpcon2.getResponseCode();
				Log.i(TAG, "Status of response " + getStatus);
				switch (getStatus) {
				case 200:
					Log.i(TAG, "GET sensor Feed response = successful");
					BufferedReader br = new BufferedReader(
							new InputStreamReader(httpcon2.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						sb.append(line + "\n");
					}
					String result = sb.toString();
					br.close();

					JSONObject jObject = new JSONObject(result);
					Log.i(TAG,
							"Response from connection " + jObject.toString(2));

					float heading = Float.parseFloat(jObject
							.getString("Heading"));

					Log.i(TAG,
							"Response from PutHeading Thread for heading is : "
									+ heading);
					overlay.setDesiredOrientation(heading);

				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			Log.d(TAG, " Update heading and location thread started");
			Log.d(TAG, " PutHedingBool : " + PutHeadingBool);
			while (PutHeadingBool) {
				long millis = System.currentTimeMillis();
				this.postHeading();
				this.getHeading();

				try {
					Thread.sleep((PutHeadingTimer - millis % 1000));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	// ---------------------------------------------
	// camera thread, gets and encodes video data
	// ---------------------------------------------
	class CameraView extends SurfaceView implements SurfaceHolder.Callback,
	PreviewCallback {

		private SurfaceHolder mHolder;
		private Camera mCamera;

		public CameraView(Context context, Camera camera) {
			super(context);
			Log.w("camera", "camera view");
			mCamera = camera;
			mHolder = getHolder();
			mHolder.addCallback(CameraView.this);
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			mCamera.setPreviewCallback(CameraView.this);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				stopPreview();
				mCamera.setPreviewDisplay(holder);
			} catch (IOException exception) {
				mCamera.release();
				mCamera = null;
			}
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.v(LOG_TAG, "Setting imageWidth: " + imageWidth
					+ " imageHeight: " + imageHeight + " frameRate: "
					+ frameRate);
			Camera.Parameters camParams = mCamera.getParameters();
			camParams.setPreviewSize(imageWidth, imageHeight);

			Log.v(LOG_TAG,
					"Preview Framerate: " + camParams.getPreviewFrameRate());

			camParams.setPreviewFrameRate(frameRate);
			mCamera.setParameters(camParams);
			startPreview();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			try {
				mHolder.addCallback(null);
				mCamera.setPreviewCallback(null);

				if (mCamera != null) { 
					mCamera.release();
				}
			} catch (RuntimeException e) {
				// The camera has probably just been released, ignore.
			}
		}

		public void startPreview() {
			if (!isPreviewOn && mCamera != null) {
				isPreviewOn = true;
				mCamera.startPreview();
			}
		}

		public void stopPreview() {
			if (isPreviewOn && mCamera != null) {
				isPreviewOn = false;
				mCamera.stopPreview();
			}
		}

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			/* get video data */
			if(flipVideo) {
				camera.setDisplayOrientation(180);
				data = rotateYUV420Degree90(data, imageWidth, imageHeight);
				data = rotateYUV420Degree90(data, imageHeight, imageWidth);
			}
			else {
				camera.setDisplayOrientation(0);
			}
			if (yuvIplimage != null && recording) {
				yuvIplimage.getByteBuffer().put(data);

				Log.v(LOG_TAG, "Writing Frame");
				try {
					long t = 1000 * (System.currentTimeMillis() - startTime);
					if (t > recorder.getTimestamp()) {
						recorder.setTimestamp(t);
					}
					recorder.record(yuvIplimage);

				} catch (FFmpegFrameSender.Exception e) {
					Log.v(LOG_TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		if (!recording) {
			attemptHandshake();
			Log.w(LOG_TAG, "Start Button Pushed");
		} else {
			// This will trigger the audio recording loop to stop and then set
			// isRecorderStart = false;
			stopRecording();
			Log.w(LOG_TAG, "Stop Button Pushed");
		}

	}

	public class HandShakeTask extends AsyncTask<Void, Void, Boolean> {
		private static final String TAG = "GetTokenActivity HandshakeTask";

		@Override
		protected Boolean doInBackground(Void... params) {
			// TODO: attempt authentication against a network service.

			JSONObject jsonObjSend = new JSONObject();

			Date date = new Date();
			SimpleDateFormat dateFormatUTC = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss'Z'");
			dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
			String formattedDate = dateFormatUTC.format(date);
			Log.i(TAG, "The Time stamp " + formattedDate);
			try {
				jsonObjSend.put("Type", "streaming-video-v1");
				jsonObjSend.put("Live", true);
				jsonObjSend.put("StartTime", formattedDate);
				jsonObjSend.put("HasGeo", true);
				jsonObjSend.put("HasHeading", true);
				// Output the JSON object we're sending to Logcat:
				Log.i(TAG, jsonObjSend.toString(2));
				Log.i(TAG, "Cookie being sent" + setRterCredentials);

				int TIMEOUT_MILLISEC = 10000; // = 10 seconds
				URL url = new URL(server_url + "/1.0/items");
				HttpURLConnection httpcon = (HttpURLConnection) url
						.openConnection();
				// httpcon.setDoOutput(true);

				httpcon.setRequestProperty("Cookie", setRterCredentials);
				httpcon.setRequestMethod("POST");
				httpcon.setConnectTimeout(TIMEOUT_MILLISEC);
				httpcon.setReadTimeout(TIMEOUT_MILLISEC);
				httpcon.connect();
				byte[] outputBytes = jsonObjSend.toString().getBytes("UTF-8");
				OutputStream os = httpcon.getOutputStream();
				os.write(outputBytes);

				os.close();

				int status = httpcon.getResponseCode();
				Log.i(TAG, "Status of response " + status);
				switch (status) {
				case 200:
				case 201:
					BufferedReader br = new BufferedReader(
							new InputStreamReader(httpcon.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						sb.append(line + "\n");
					}
					String result = sb.toString();
					br.close();

					JSONObject jObject = new JSONObject(result);
					Log.i(TAG,
							"Response from connection " + jObject.toString(2));

					recievedItemID = jObject.getString("ID");
					String uploadURI = jObject.getString("UploadURI");
					JSONObject token = jObject.getJSONObject("Token");
					recievedRterResource = token.getString("rter_resource");
					recievedRterSignature = token.getString("rter_signature");
					recievedRterValidUntil = token
							.getString("rter_valid_until");
					authToken = "rtER rter_resource=\"" + recievedRterResource
							+ "\"," + " rter_valid_until=\""
							+ recievedRterValidUntil + "\", "
							+ "rter_signature=\"" + recievedRterSignature
							+ "\"";
					Log.v("PREFS", "authToken  : " + authToken);
					Log.v("PREFS",
							"Response after starting item on server rter_resource  : "
									+ recievedRterResource);
					Log.i(TAG, "Response from starting item rter_signature : "
							+ recievedRterSignature);

					prefEditor.putString("ID", recievedItemID);
					prefEditor.putString("rter_resource", recievedRterResource);
					prefEditor.putString("rter_signature",
							recievedRterSignature);
					prefEditor.putString("rter_valid_until",
							recievedRterValidUntil);
					prefEditor.commit();

				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// TODO: register the new account here.
			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			handshakeTask = null;

			Log.d(TAG, "in OnPostExecute of Handshake");
			if (success) {
				Log.d(TAG, "Success of Handshake");
				initRecorder();
				startRecording();

			} else {

			}
		}

		@Override
		protected void onCancelled() {
			handshakeTask = null;

		}
	}

	private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) 
	{
		byte [] yuv = new byte[imageWidth*imageHeight*3/2];
		// Rotate the Y luma
		int i = 0;
		for(int x = 0;x < imageWidth;x++)
		{
			for(int y = imageHeight-1;y >= 0;y--)                               
			{
				yuv[i] = data[y*imageWidth+x];
				i++;
			}
		}
		// Rotate the U and V color components 
		i = imageWidth*imageHeight*3/2-1;
		for(int x = imageWidth-1;x > 0;x=x-2)
		{
			for(int y = 0;y < imageHeight/2;y++)                                
			{
				yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
				i--;
				yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
				i--;
			}
		}
		return yuv;
	}

}

class FrameInfo {
	public byte[] uid;
	public byte[] lat;
	public byte[] lon;
	public byte[] orientation;
}
