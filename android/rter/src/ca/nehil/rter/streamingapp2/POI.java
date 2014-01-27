package ca.nehil.rter.streamingapp2;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import ca.nehil.rter.streamingapp2.overlay.IndicatorFrame;
import ca.nehil.rter.streamingapp2.overlay.Triangle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class POI {

	SensorSource sensorSource;
	protected ArrayList<POI> poiList;
//	double camAngle = 59; //glass
	double camAngle = 60; //nexus 5
	IndicatorFrame squareFrame;
	Triangle triangleFrame;
	boolean showLog;
	int fooCount;
	public POI(Context context, int _poiId, double _remoteBearing, double _lat, double _lng, String _color, String _curThumbnailURL, String _type) {
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
		fooCount=0;
	}
	
	public void updatePOIList(ArrayList<POI> newPoi){
		poiList = new ArrayList<POI>(newPoi);
	}
    
	public int poiId;
	Location loc;
	public double remoteBearing; //angle of device relative to N
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
        Log.d("DegreeDelta", "Delta: "+ delta + " deg1: " + deg1 + " deg2: " + deg2);
        	
        return delta;
	}
	
	public int getId(){
		return poiId;
	}
	
	public Location getLocation(){
		return loc;
	}
	
	public double getRemoteBearing(){
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
		int screenHeight = screenSize.y;
		float bearingToPoi;
		float distance;
		if(userLocation != null){
//			if(showLog){
				Log.d("LocationDebug", "POI received userLocation");
				showLog = false;
//			}
			bearingToPoi = this.relativeBearingTo(userLocation);
//			distance = this.distanceTo(userLocation);
			distance = -6.0f;
		}else{
//			if(showLog){
				Log.d("alok", "userLocation was null- POI");
				showLog = false;
//			}
			
			bearingToPoi = 0f;
			distance = -6.0f;
		}
		double left = (bearingToPoi/(camAngle/2.0)) * (screenWidth/2.0);
		
		if(fooCount++ % 50 == 0) Log.d("LocationDebug", "left: "+left+" bearing: "+bearingToPoi+"camAngle: "+camAngle+" screenWidth: "+screenWidth);
//		if(fooCount++ % 40 == 0) Log.d("LocationDebug", "Heading: " + (float)sensorSource.getHeading());
		
		float width = (screenWidth - (distance*screenWidth));
		if(width < 30){
			width = 30;
		}
		double height = 0;
		
//		Log.d("alok", "rec coords: "+ left+" "+screenHeight+" "+width+" "+height);

		gl.glLoadIdentity();
		gl.glTranslatef((float)left, (float)height, distance);
        
        if(this.type.equals("type1")){
        	squareFrame = new IndicatorFrame();
        	squareFrame.draw(gl); // Using indicator frame object to draw square around POI
        }else if (this.type.equals("type2")){
        	triangleFrame = new Triangle();
        	triangleFrame.draw(gl);
        }
        
	}
}
