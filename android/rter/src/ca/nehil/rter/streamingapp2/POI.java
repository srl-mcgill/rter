package ca.nehil.rter.streamingapp2;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import ca.nehil.rter.streamingapp2.overlay.IndicatorFrame;
import ca.nehil.rter.streamingapp2.overlay.Triangle;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

public class POI {

	SensorSource sensorSource;
	protected ArrayList<POI> poiList;
	double camAngleHorizontal;
	double camAngleVertical;
	IndicatorFrame squareFrame;
	Triangle triangleFrame;
	boolean showLog;
	int fooCount, debugCount;
	private SharedPreferences storedValues;
	private float screenHeight;
	private float screenWidth;
	
	public POI(Context context, int _poiId, Double _remoteBearing, double _lat, double _lng, String _color, String _curThumbnailURL, String _type) {
		poiId = _poiId;
		remoteBearing = _remoteBearing; //orientation of device relative to N
		loc = new Location("poi");
		loc.setLatitude(_lat);
		loc.setLongitude(_lng);
		color = _color;
		curThumbnailURL = _curThumbnailURL;
		type = _type;
		sensorSource = SensorSource.getInstance(context);
		showLog = true;
		fooCount= 0;
		debugCount = 0;
		
		storedValues = context.getSharedPreferences(context.getString(R.string.sharedPreferences_filename), Context.MODE_PRIVATE);
		camAngleVertical = storedValues.getFloat("CamVerticalViewAngle", 46);
		camAngleHorizontal = storedValues.getFloat("CamHorizontalViewAngle", 60);
		screenWidth = storedValues.getFloat("screenSize.x", 0);
		screenHeight = storedValues.getFloat("screenSize.y", 0);
		Log.d("CameraDebug", "scrn ht poi: " + screenHeight);
	}

	public void updatePOIList(ArrayList<POI> newPoi){
		poiList = new ArrayList<POI>(newPoi);
	}

	public int poiId;
	Location loc;
	public Double remoteBearing; //angle of device relative to N
	public String curThumbnailURL;
	public String color;
	public String type;
	public float bearingTo(Location fromLoc) {
		return fromLoc.bearingTo(loc);
	}
	public float relativeBearingTo(Location fromLoc) { //bearing relative to user position
//		Log.d("SensorDebug", "curr orien: " + sensorSource.getCurrentOrientation());
		return minDegreeDelta(fromLoc.bearingTo(loc), sensorSource.getCurrentOrientation());
		//		return minDegreeDelta(fromLoc.bearingTo(loc), (float)sensorSource.getHeading());
	}
	public float distanceTo(Location fromLoc) {
		return fromLoc.distanceTo(loc);
	}

	private float minDegreeDelta(float deg1, float deg2) {
		float delta = deg1-deg2;
		if(delta > 180) delta -= 360;
		if(delta < -180) delta += 360;
		Log.d("DegreeDelta", "Delta: "+ delta + " deg1: " + deg1 + " deg2: " + deg2);

		return delta;
	}

	public int getId(){
		return poiId;
	}

	public Location getLocation(){
		return loc;
	}

	public Double getRemoteBearing(){
		return remoteBearing;
	}

	public String getThumbnailUrl(){
		return curThumbnailURL;
	}

	public String getColor(){
		return color;
	}

	public String getType(){
		return type;
	}

	/*
	 * Use this method to render each POI, called from the frame render in CameraGLRenderer.java
	 */
	public void render(GL10 gl, Location userLocation, Point screenSize){
		gl.glLoadIdentity();
		float bearingToPoi;
		float distance;
		double framePositionY = 0;
		float scale = 1;
		
		if(userLocation != null){
			if(debugCount++ % 50 == 0) Log.d("LocationDebug", "POI received userLocation");
			bearingToPoi = this.relativeBearingTo(userLocation);
//			Log.d("SensorDebug", "bearing to: " + bearingToPoi);
			distance = this.distanceTo(userLocation);
			calculateFrameSize(distance);
			// Interpolate the values from 0.5 meters - 20 meters to lie between 4.2 and 0.5.
			if(distance <= 20 && distance >= 0.5){
				scale = 0.5f - ( 0.189744f * ( distance - 20));
			} else if (distance > 20){
				scale = 0.5f;
			} else if (distance < 0.5){
				scale  = 4.2f;
			}
			framePositionY = calculateFramePositionY(distance);
		}else{
			if(debugCount++ % 50 == 0) Log.d("alok", "userLocation was null- POI");
			bearingToPoi = 0f;
			scale = 1;
		}
		double framePositionX = (bearingToPoi/(camAngleHorizontal/2.0)) * (screenWidth/2.0);

		//		if(fooCount++ % 40 == 0) Log.d("LocationDebug", "Heading: " + (float)sensorSource.getHeading());

//		float width = (screenWidth - (scale*screenWidth));
//		if(width < 30){
//			width = 30;
//		}
		//		Log.d("alok", "rec coords: "+ left+" "+screenHeight+" "+width+" "+height);

		gl.glLoadIdentity();
		gl.glTranslatef((float)framePositionX, (float)framePositionY, -6.0f);
		gl.glScalef(scale, scale, scale);
		
		if(squareFrame == (null) || triangleFrame == (null)){
			squareFrame = new IndicatorFrame();
			triangleFrame = new Triangle();
			Log.d("CameraDebug", "created new objects");
		}
		
		if(this.type.equals("streaming-video-v1") || this.type.equals("type1")){
			squareFrame.draw(gl);
		}else if (this.type.equals("beacon") || this.type.equals("type2")){
			triangleFrame.draw(gl);
		}
	}
	
	/**
	 * Calculates the vertical position of the frame to render on screen. 
	 * @param distanceToPoi Distance from the user to the POI (in meters).
	 * @return  Vertical position of the frame <b>below</b> the center (0,0). 
	 */
	private double calculateFramePositionY(float distanceToPoi){
		double angleVerticalFov = camAngleVertical; // Vertical field of view angle of the camera, in degrees.
		double angleInclination = sensorSource.getEyeLevelInclination(); // Angle below horizon where user is looking. Is 0 for testing, means user is looking straight at horizon
		double angleMarker; // Angle below horizon where marker is located. AngleActual on the paper.
		double heightPerson = 2; // Meters
		double heightMarker = 0.3; // Meters
		double framePositionY;
		
		angleMarker = Math.atan((heightPerson - (heightMarker/2))/distanceToPoi); // angleMarker will be between -pi/2 and pi/2 here.
		angleMarker = Math.toDegrees(angleMarker);
		framePositionY = ((angleMarker - angleInclination)/ angleVerticalFov) * screenHeight;
		
		if(fooCount++ % 30 == 0) Log.d("AngleDebug", "below the center: " + framePositionY + ", verticalAngle: " + angleVerticalFov + ", marker angle below horizon: " + angleMarker + ", ScreenHeight: " + screenHeight + ", distance: " + distanceToPoi);
		
		return -framePositionY; // Returning negative as the frame's position should be moving in the opposite direction to the movement of the users head inclination.
	}
	
	/**
	 * Calculates the size of the frame to render, based on the distance of the user from the POI, and the height of
	 * the POI w.r.t. the user.
	 * @param distanceToPoi Distance from the user to the POI(in meters).
	 * @return the frame size
	 */
	private double calculateFrameSize(float distanceToPoi){
		double angleVerticalFov = camAngleVertical;
		double angleObject; // The angle created by height of the object
		double heightPerson = 2;
		double heightMarker = 0.3;
		double frameSize;
		
		angleObject = Math.atan(heightPerson/distanceToPoi) - Math.atan((heightPerson - heightMarker) / distanceToPoi);
		frameSize = (angleObject/angleVerticalFov) * screenHeight;
		Log.d("alok", "frame size: " + frameSize + " distance poi: " + distanceToPoi + " scrn ht: " + screenHeight + " angle o: " + angleObject);
		return frameSize;
	}
}
