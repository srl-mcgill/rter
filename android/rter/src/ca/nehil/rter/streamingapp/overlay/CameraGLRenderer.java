package ca.nehil.rter.streamingapp.overlay;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ca.nehil.rter.streamingapp.POIList;
import ca.nehil.rter.streamingapp.SensorSource;
import ca.nehil.rter.streamingapp.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.opengl.GLU;

import android.opengl.GLSurfaceView.Renderer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CameraGLRenderer implements Renderer {
	private Object lock;
	Context context;
	float aspect;
	private POIList POIs;
	Location userLocation;
	private SensorSource mSensorSource;
	
	// Constructor with global application context
	public CameraGLRenderer(Context context, POIList POIs, SensorSource mSensorSource) {
		this.context = context;
		this.POIs = POIs;
		this.lock = new Object();
		
		this.mSensorSource = mSensorSource;
		LocalBroadcastManager.getInstance(context).registerReceiver(locationBroadcastReceiver, 
				new IntentFilter(context.getString(R.string.LocationEvent)));
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
	}

	/* Called after onSurfaceCreated() or whenever the window's size changes */
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if (height == 0)
			height = 1; // To prevent divide by zero
		aspect = (float) width / height;
		
		gl.glViewport(0, 0, width, height); //Set the viewport (display area) to cover the entire window

		// Setup perspective projection, with aspect ratio matches viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); // Select projection matrix
		gl.glLoadIdentity(); // Reset projection matrix
		GLU.gluPerspective(gl, 45, aspect, 0.1f, 100.f); //Use perspective projection

		gl.glMatrixMode(GL10.GL_MODELVIEW); // Select model-view matrix
		gl.glLoadIdentity(); // Reset
	}

	public void onDrawFrame(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT); //Clear color and depth buffers using clear-value set earlier
		
		synchronized(lock) {
			userLocation = mSensorSource.getLocation();
			gl.glLoadIdentity();
			POIs.render(gl, userLocation);
		}
	}
	
	/* Receiver for Location broadcast events  */
	private BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			userLocation = mSensorSource.getLocation();
		}
	};

}
