package ca.nehil.rter.streamingapp.overlay;

import ca.nehil.rter.streamingapp.POIList;
import ca.nehil.rter.streamingapp.SensorSource;
import android.content.Context;

/**
 * NORTH: 0 deg
 * EAST: +90 deg
 * WEST: -90 deg
 * SOUTH: +/- 180 deg
 * 
 * @author stepan
 */

public class OverlayController {
	protected float desiredOrientation;
	protected float currentOrientation;

	protected CameraGLSurfaceView mGLView;
	protected CameraGLRenderer mGLRenderer;
	protected Context context;

	float declination = 0;	//geo magnetic declinationf from true North

	public float orientationTolerance = 10.0f; // Max orientation tolerance in degrees

	public OverlayController(Context context, POIList POIs, SensorSource mSensorSource) {
		this.context = context;
		this.mGLView = new CameraGLSurfaceView(context, POIs, mSensorSource);
		this.mGLRenderer = this.mGLView.getGLRenderer();
	}

	/**
	 * @return the camera GLView
	 */
	public CameraGLSurfaceView getGLView() {
		return this.mGLView;
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
