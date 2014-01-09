package ca.nehil.rter.streamingapp2.overlay;

import java.util.ArrayList;
import java.util.Arrays;

import ca.nehil.rter.streamingapp2.POI;
import ca.nehil.rter.streamingapp2.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.support.v4.content.LocalBroadcastManager;

public class CameraGLSurfaceView extends GLSurfaceView {
	protected CameraGLRenderer camGLRenderer;
	protected ArrayList<POI> oldPoi;
	
	public CameraGLSurfaceView(Context context) {
		super(context);
		
		//needed to overlay gl view over camera preview
		this.setZOrderMediaOverlay(true);
		
        // Create an OpenGL ES 1.0 context
        this.setEGLContextClientVersion(1);
        this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        this.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        
        // Set the Renderer for drawing on the GLSurfaceView
        this.camGLRenderer = new CameraGLRenderer(context);
        this.setRenderer(camGLRenderer);
           
        // Render the view only when there is a change in the drawing data
        this.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        LocalBroadcastManager.getInstance(this.getContext()).registerReceiver(sensorBroadcastReceiver,
    			new IntentFilter(getResources().getString(R.string.SensorEvent)));
	}

	/* Receiver for Sensor broadcast events */ 
	BroadcastReceiver sensorBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// Extract data included in the Intent
		}
	};
    
	public CameraGLRenderer getGLRenderer() {
		return this.camGLRenderer;
	}
	
	public void updatePOIList(ArrayList<POI> newPoi){
		oldPoi = new ArrayList<POI>(newPoi);
	}

}
