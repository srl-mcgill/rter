package ca.nehil.rter.streamingapp2.overlay;

import ca.nehil.rter.streamingapp2.R;
import ca.nehil.rter.streamingapp2.SensorSource;
import ca.nehil.rter.streamingapp2.overlay.CameraGLRenderer.Indicate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * NORTH: 0 deg
 * EAST: +90 deg
 * WEST: -90 deg
 * SOUTH: +/- 180 deg
 * 
 * @author stepan
 *
 */
public class OverlayController {
	protected float desiredOrientation;
	protected float currentOrientation;
	protected float deviceOrientation;
	protected boolean rightSideUp = true;
	private boolean freeRoam = true;

	protected CameraGLSurfaceView mGLView;
	protected CameraGLRenderer mGLRenderer;
	protected Context context;
	private SensorSource sensorSource;

	float declination = 0;	//geo magnetic declinationf from true North

	public float orientationTolerance = 10.0f; // Max orientation tolerance in degrees
	
	public OverlayController(Context context) {
		this.context = context;
		this.mGLView = new CameraGLSurfaceView(context);
		this.mGLRenderer = this.mGLView.getGLRenderer();
		
		sensorSource = SensorSource.getInstance(context);
		
		/* Register for location and sensor broadcasts */
		LocalBroadcastManager.getInstance(context).registerReceiver(locationBroadcastReceiver, 
				new IntentFilter(context.getString(R.string.LocationEvent)));
		LocalBroadcastManager.getInstance(context).registerReceiver(sensorBroadcastReceiver, 
				new IntentFilter(context.getString(R.string.SensorEvent)));
	}
	
	/*
	 * Receiver for sensor broadcasts
	 */
	private BroadcastReceiver sensorBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			deviceOrientation = sensorSource.getDeviceOrientation();
			currentOrientation = sensorSource.getCurrentOrientation();
			
			if (freeRoam) {
				mGLRenderer.indicateTurn(Indicate.FREE, 0.0f);
				return;
			}

			// check orientation of device
			if (deviceOrientation <= 90.0f && deviceOrientation >= -90.0f) {
				rightSideUp = true;
			} else
				rightSideUp = false;

			// graphics logic
			boolean rightArrow = true;
			float difference = fixAngle(desiredOrientation - currentOrientation);
			if (Math.abs(difference) > orientationTolerance) {
				
				if (difference > 0) {
					// turn right
					rightArrow = true;
				} else {
					// turn left
					rightArrow = false;
				}

				// flip arrow incase device is flipped
				if (!rightSideUp) {
					rightArrow = !rightArrow;
				}

				if (rightArrow) {
					mGLRenderer.indicateTurn(Indicate.RIGHT,
							Math.abs(difference) / 180.0f);
				} else {
					mGLRenderer.indicateTurn(Indicate.LEFT,
							Math.abs(difference) / 180.0f);
				}

			} else {
				mGLRenderer.indicateTurn(Indicate.NONE, 0.0f);
			}
		}
	};
	
	/*
	 * Receiver for location broadcasts
	 */
	private BroadcastReceiver locationBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {

			declination = sensorSource.getDeclination();
		}
	};

	/**
	 * @return the camera GLView
	 */
	public CameraGLSurfaceView getGLView() {
		return this.mGLView;
	}

	/**
	 * when set to 'true', no indicator arrows will be given, and frame will be
	 * blue
	 * 
	 * @param freeRoam
	 */
	public void letFreeRoam(boolean freeRoam) {
		this.freeRoam = freeRoam;
	}
	
	public float getCurrentOrientation() {
		return this.currentOrientation;
	}
	
	/**
	 * Set the desired absolute bearing Should be between +180 and -180, but
	 * will work otherwise
	 * 
	 * @param orientation
	 */
	public void setDesiredOrientation(float orientation) {
		orientation = fixAngle(orientation);
		desiredOrientation = orientation;
	}

	/**
	 * makes sure angle is between -180 and 180
	 * 
	 * @param angle
	 * @return fixed angle
	 */
	protected float fixAngle(float angle) {
		if (angle > 180.0f) {
			angle = -180.0f + angle % 180;
		} else if (angle < -180.0f) {
			angle = 180.0f - Math.abs(angle) % 180;
		}

		return angle;
	}

	/**
	 * Set the desired offset from the current bearing should be between +180
	 * and -180, but will work otherwise
	 * 
	 * @param offset
	 */
	public void setOrientationOffset(float offset) {
		this.setDesiredOrientation(currentOrientation + offset);
	}

}
