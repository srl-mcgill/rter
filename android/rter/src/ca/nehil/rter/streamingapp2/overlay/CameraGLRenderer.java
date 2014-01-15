package ca.nehil.rter.streamingapp2.overlay;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ca.nehil.rter.streamingapp2.POI;
import ca.nehil.rter.streamingapp2.R;
import ca.nehil.rter.streamingapp2.SensorSource;
import ca.nehil.rter.streamingapp2.overlay.IndicatorFrame.Colour;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLU;

import android.opengl.GLSurfaceView.Renderer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class CameraGLRenderer implements Renderer {
	public static enum Indicate {
		LEFT, RIGHT, NONE, FREE
	}

	private Object lock;

	Arrow arrowLeft;
	Arrow arrowRight;
	IndicatorFrame indicatorFrame;
	IndicatorFrame mFrame;
	IndicatorFrame poiFrame;

	Context context; // Application's context

	float aspect;
	float xTotal, yTotal, distance;

	float arrowScale = 1.0f;
	float arrowScaleMax = 1.2f;
	float arrowScaleMin = 0.2f;
	
	// pulsating variables
	float arrowPulsateScale = 1.0f;
	float arrowPulsateSpeed = 0.1f;
	float arrowPulsateSpeedMin = 0.01f;
	float arrowPulsateMax = 1.2f;
	float arrowPulsateMin = 0.9f;
	boolean arrowPulsateIncrease = true;

	boolean displayLeft = false;
	boolean displayRight = false;
	ArrayList<POI> dummyPoiList = new ArrayList<POI>();
	
	float lati, longi;
	Location userLocation;
	LocationManager locationMan;
	private SensorSource sensorSource;
	Point screenSize;

	// Constructor with global application context
	public CameraGLRenderer(Context context) {
		this.context = context;
		locationMan = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		arrowLeft = new Arrow();
		arrowRight = new Arrow();
		indicatorFrame = new IndicatorFrame();
		mFrame = new IndicatorFrame();
		poiFrame = new IndicatorFrame();
		
		this.lock = new Object();
		
		POI poi1 = new POI(context , 1, 1.5, 1.5, 1.5, "", "http://rter.zapto.org:8080/v1/videos/385/thumb/000000001.jpg", "streaming-video-v1");
		POI poi2 = new POI(context, 2, 2.5, 2.5, 2.5, "","","");
		POI poi3 = new POI(context, 3, 3.5, 3.5, 3.5, "", "", "");
		dummyPoiList.add(poi1);dummyPoiList.add(poi2);dummyPoiList.add(poi3);
		
		sensorSource = SensorSource.getInstance(context);
		LocalBroadcastManager.getInstance(context).registerReceiver(locationBroadcastReceiver, 
				new IntentFilter(context.getString(R.string.LocationEvent)));
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		screenSize = new Point();
//		display.getSize(screenSize);
	}

	public void indicateTurn(Indicate direction, float percentage) {
		synchronized (lock) {
			arrowScale = (arrowScaleMax - arrowScaleMin) * percentage +arrowScaleMin;
			switch (direction) {
			case LEFT:
				displayLeft = true;
				displayRight = false;
				indicatorFrame.colour(Colour.RED);
				break;
			case RIGHT:
				displayLeft = false;
				displayRight = true;
				indicatorFrame.colour(Colour.RED);
				break;
			case NONE:
				displayLeft = false;
				displayRight = false;
				indicatorFrame.colour(Colour.GREEN);
				break;
			case FREE:
				displayLeft = false;
				displayRight = false;
				indicatorFrame.colour(Colour.BLUE);
				break;
			}
		}
	}

	/* Called when the surface is first created or re-created */
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // Set color's clear-value to
													// black
		gl.glClearDepthf(1.0f); // Set depth's clear-value to farthest
		gl.glEnable(GL10.GL_DEPTH_TEST); // Enables depth-buffer for hidden
											// surface removal
		gl.glDepthFunc(GL10.GL_LEQUAL); // The type of depth testing to do
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST); // nice
																		// perspective
																		// view
		gl.glShadeModel(GL10.GL_SMOOTH); // Enable smooth shading of color
		gl.glDisable(GL10.GL_DITHER); // Disable dithering for better performance

		// Your OpenGL|ES initialization code here
		// ......
	}

	/* Called after onSurfaceCreated() or whenever the window's size changes */
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if (height == 0)
			height = 1; // To prevent divide by zero
		aspect = (float) width / height;
		
		// get the total x and y at distance
		distance = 6.0f;
		xTotal = (float) (aspect * Math.tan(Math.toRadians(45.0 / 2))
				* distance * 2);
		yTotal = (float) (Math.tan(Math.toRadians(45.0 / 2)) * distance * 2);

		screenSize.x = (int) xTotal;
		screenSize.y = (int) yTotal;
		indicatorFrame.resize(xTotal, yTotal, distance);
		mFrame.resize(xTotal/2, yTotal/2, height);
		// Set the viewport (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);

		// Setup perspective projection, with aspect ratio matches viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); // Select projection matrix
		gl.glLoadIdentity(); // Reset projection matrix
		// Use perspective projection
		GLU.gluPerspective(gl, 45, aspect, 0.1f, 100.f);

		gl.glMatrixMode(GL10.GL_MODELVIEW); // Select model-view matrix
		gl.glLoadIdentity(); // Reset

		// You OpenGL|ES display re-sizing code here
		// ......

	}

	// Call back to draw the current frame.
	public void onDrawFrame(GL10 gl) {
		// Clear color and depth buffers using clear-value set earlier
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		// Your OpenGL|ES rendering code here
		// ......

		// pulsate arrows
		if (arrowPulsateIncrease) {
			float speed = (arrowPulsateMax - arrowPulsateScale) * arrowPulsateSpeed;
			if (speed < arrowPulsateSpeedMin)
				speed = arrowPulsateSpeedMin;
			arrowPulsateScale += speed;
			if (arrowPulsateScale >= arrowPulsateMax) {
				arrowPulsateScale = arrowPulsateMax;
				arrowPulsateIncrease = false;
			}
		} else {
			float speed = (arrowPulsateScale - arrowPulsateMin) * arrowPulsateSpeed;
			if (speed < arrowPulsateSpeedMin)
				speed = arrowPulsateSpeedMin;
			arrowPulsateScale -= speed;
			if (arrowPulsateScale <= arrowPulsateMin) {
				arrowPulsateScale = arrowPulsateMin;
				arrowPulsateIncrease = true;
			}
		}
		
		float arrowScale_tmp = arrowPulsateScale * arrowScale;

		synchronized(lock) {
			// FRAME
			gl.glLoadIdentity();
			indicatorFrame.draw(gl);

			//poi render here. POI class maintains list of POIS, iterates through them and renders each.
	        //poi class will need sensor data.
			for(int i= 0; i < dummyPoiList.size(); i++){
				if(userLocation == null){
//					userLocation = locationMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				}
				dummyPoiList.get(i).render(gl, userLocation, screenSize);
//				gl.glLoadIdentity();
//				gl.glTranslatef(3f, 2.5f, -distance);
//		        arrowRight.draw(gl);
			}

			// RIGHT ARROW
			if(displayRight) {
				gl.glLoadIdentity(); // Reset model-view matrix ( NEW )
				gl.glTranslatef(xTotal / 2.0f - 0.1f*xTotal, 0.0f, -distance);
				gl.glScalef(arrowScale_tmp, arrowScale_tmp, 1.0f);
				arrowRight.draw(gl); // Draw triangle ( NEW )
			}
			// LEFT
			if(displayLeft) {
				gl.glLoadIdentity();
				gl.glTranslatef(-xTotal / 2.0f + 0.1f*xTotal, 0.0f, -distance);
				gl.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
				gl.glScalef(arrowScale_tmp, arrowScale_tmp, 1.0f);
				arrowLeft.draw(gl); // Draw quad ( NEW )
			}
		}
	}
	
	/* Receiver for Location broadcast events  */
	private BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			userLocation = sensorSource.getLocation();
			Log.d("LocationDebug", "renderer received broadcast: "+userLocation);
			lati = (float) (userLocation.getLatitude());
			longi = (float) (userLocation.getLongitude());
		}
	};

}
