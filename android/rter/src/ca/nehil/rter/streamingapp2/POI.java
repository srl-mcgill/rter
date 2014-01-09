package ca.nehil.rter.streamingapp2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class POI {

	SensorSource sensorSource = new SensorSource();
	
	public POI(Context context, int _poiId, double _remoteBearing, double _lat, double _lng, String _color, String _curThumbnailURL, String _type) {
		Log.d("alok", "POI class init");
		poiId = _poiId;
		remoteBearing = _remoteBearing; //orientation of device relative to N
		loc = new Location("poi");
		loc.setLatitude(_lat);
		loc.setLongitude(_lng);
		color = _color;
		curThumbnailURL = _curThumbnailURL;
		type = _type;
		LocalBroadcastManager.getInstance(context).registerReceiver(sensorBroadcastReceiver,
    			new IntentFilter(context.getString(R.string.SensorEvent)));
	}
	
	/* Receiver for Sensor broadcast events */ 
	BroadcastReceiver sensorBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("alok", "sensor broadcast received in POI class");
		}
	};
    
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
	
	/*
	 * Use this method to render each POI, called from the frame render in CameraGLRenderer.java
	 */
	private void render(){
		
	}
}
