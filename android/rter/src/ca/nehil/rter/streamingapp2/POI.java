package ca.nehil.rter.streamingapp2;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import ca.nehil.rter.streamingapp2.overlay.IndicatorFrame;
import ca.nehil.rter.streamingapp2.overlay.Triangle;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;

public class POI {

	SensorSource sensorSource;
	protected ArrayList<POI> poiList;
	//	double camAngle = 59; //glass
	double camAngle = 60; //nexus 5
	IndicatorFrame squareFrame;
	Triangle triangleFrame;
	boolean showLog;
	int fooCount, debugCount;
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
		//Log.d("DegreeDelta", "Delta: "+ delta + " deg1: " + deg1 + " deg2: " + deg2);

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
		int screenWidth = screenSize.x;
		//		int screenHeight = screenSize.y; // Not moving POI frame in the vertical direction for Glass. Should be implemented later.
		float bearingToPoi;
		float scale;
		if(userLocation != null){
			if(debugCount++ % 50 == 0) Log.d("LocationDebug", "POI received userLocation");
			bearingToPoi = this.relativeBearingTo(userLocation);
			scale = this.distanceTo(userLocation);
			//Log.d("CameraDebug", "distanceTo: " + scale);
			String stat = "none";
			// Interpolate values from 0.5 meters - 20 meters to lie between 4.2 and 0.5.
			if(scale <= 20 && scale >= 0.5){
				scale = 0.5f - (0.189744f * ( scale - 20));
				stat = "set";
			} else if (scale > 20){
				scale = 0.5f;
				stat = "greater";
			} else if (scale < 0.5){
				scale  = 4.2f;
				stat = "smaller";
			}
			//Log.d("CameraDebug", stat + "scale: " + scale);
			
		}else{
			if(debugCount++ % 50 == 0) Log.d("alok", "userLocation was null- POI");
			bearingToPoi = 0f;
			scale = 1;
		}
		double left = (bearingToPoi/(camAngle/2.0)) * (screenWidth/2.0);

		if(fooCount++ % 50 == 0) Log.d("LocationDebug", "left: "+left+" bearing: "+bearingToPoi+"camAngle: "+camAngle+" screenWidth: "+screenWidth);
		//		if(fooCount++ % 40 == 0) Log.d("LocationDebug", "Heading: " + (float)sensorSource.getHeading());

//		float width = (screenWidth - (scale*screenWidth));
//		if(width < 30){
//			width = 30;
//		}
		double height = 0;

		//		Log.d("alok", "rec coords: "+ left+" "+screenHeight+" "+width+" "+height);

		gl.glLoadIdentity();
		gl.glTranslatef((float)left, (float)height, -6.0f);
		gl.glScalef(scale, scale, scale);
		
		if(this.type.equals("streaming-video-v1") || this.type.equals("type1")){
			squareFrame = new IndicatorFrame();
			squareFrame.draw(gl);
		}else if (this.type.equals("beacon") || this.type.equals("type2")){
			triangleFrame = new Triangle();
			triangleFrame.draw(gl);
		}

	}
}
