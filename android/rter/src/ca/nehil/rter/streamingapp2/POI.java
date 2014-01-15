package ca.nehil.rter.streamingapp2;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import ca.nehil.rter.streamingapp2.overlay.IndicatorFrame;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.Camera;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class POI {

	SensorSource sensorSource;
	protected ArrayList<POI> poiList;
//	double camAngle = 54.8; //glass
	double camAngle = 30; //nexus 5
	IndicatorFrame mFrame;
	
	public POI(Context context, int _poiId, double _remoteBearing, double _lat, double _lng, String _color, String _curThumbnailURL, String _type) {
		poiId = _poiId;
		remoteBearing = _remoteBearing; //orientation of device relative to N
		loc = new Location("poi");
		loc.setLatitude(_lat);
		loc.setLongitude(_lng);
		color = _color;
		curThumbnailURL = _curThumbnailURL;
		type = _type;
		mFrame = new IndicatorFrame();
		LocalBroadcastManager.getInstance(context).registerReceiver(sensorBroadcastReceiver,
    			new IntentFilter(context.getString(R.string.SensorEvent)));
		sensorSource = SensorSource.getInstance(context);
	}
	
	/* Receiver for Sensor broadcast events */ 
	BroadcastReceiver sensorBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
		}
	};
	
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
		Log.d("alok", "in relative bearing" + fromLoc+" current or: "+ sensorSource.getCurrentOrientation());
		return minDegreeDelta(fromLoc.bearingTo(loc), sensorSource.getCurrentOrientation());
	}
	public float distanceTo(Location fromLoc) {
		return fromLoc.distanceTo(loc);
	}

	private float minDegreeDelta(float deg1, float deg2) {
        float delta = deg1-deg2;
        if(delta > 180) delta -= 360;
        if(delta < -180) delta += 360;
        
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
			Log.d("alok", "POI received userLocation");
			bearingToPoi = this.relativeBearingTo(userLocation);
			distance = this.distanceTo(userLocation);
		}else{
//			Log.d("alok", "userLocation was null- POI");
			bearingToPoi = 0f;
			distance = 3.5f;
		}
		double remoteBearing = this.getRemoteBearing();
		double left = (screenWidth/2)+(bearingToPoi/camAngle)*screenWidth;
		float width = (screenWidth - (distance*screenWidth));
		if(width < 30){
			width = 30;
		}
		double height = screenHeight*0.3;
		
//		Log.d("alok", "rec coords: "+ left+" "+screenHeight+" "+width+" "+height);

		gl.glLoadIdentity();
		gl.glTranslatef((float)left, (float)height, -6.0f);
        mFrame.draw(gl);
	}
}
