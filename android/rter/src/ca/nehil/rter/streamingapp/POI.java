package ca.nehil.rter.streamingapp;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import ca.nehil.rter.streamingapp.overlay.IndicatorFrame;
import ca.nehil.rter.streamingapp.overlay.Triangle;

import android.content.Context;
import android.location.Location;
import android.util.Log;

public class POI {
	long framecount = 0;
	SensorSource mSensorSource;
	protected ArrayList<POI> poiList;
	IndicatorFrame squareFrame;
	Triangle triangleFrame;
	private final static String TAG = "POIObject";

	public POI(Context context, int _poiId, double _lat, double _lng, String _color, String _curThumbnailURL, String _type, SensorSource mSensorSource) {
		poiId = _poiId;
		loc = new Location("poi");
		loc.setLatitude(_lat);
		loc.setLongitude(_lng);
		color = _color;
		curThumbnailURL = _curThumbnailURL;
		type = _type;
		this.mSensorSource = mSensorSource;
	}

	public void updatePOIList(ArrayList<POI> newPoi){
		poiList = new ArrayList<POI>(newPoi);
	}

	public int poiId;
	Location loc;
	public String curThumbnailURL;
	public String color;
	public String type;
	
	public float bearingTo(Location fromLoc) {
		return fromLoc.bearingTo(loc);
	}
	public float relativeBearingTo(Location fromLoc) { //bearing relative to user position
		return minDegreeDelta(fromLoc.bearingTo(loc), mSensorSource.getCurrentOrientation());
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
	public void render(GL10 gl, Location userLocation, float[] displacement){
		if(userLocation == null) { // || distanceTo(userLocation) < 0.2) {
			return;
		}

		gl.glLoadIdentity();
		gl.glRotatef(270, 0, 0, 1);
		gl.glMultMatrixf(mSensorSource.getLandscapeRotationMatrix(), 0);
		float scale = 10000.0f; /* Scale to world. Increasing this to 10^5 will make the world bigger, and hence the POIs smaller. It will also push the POI outside
		 						* the limit that OpenGL renders objects. So, if changed to 10^5, you will see some POIs dissappear. If you want to change the sizes
		 						* of the POI, instead change the glScalef below.*/
//			gl.glTranslatef(displacement[0], displacement[1], 0.0f); // If you want to auto-walk close to a POI and demo the size increase
		float size = 0.005f;
		
		if(this.type.equals("breadcrumb")) {
			size = size / 4;
			/*
			if(framecount++ % 30 == 0){
				Log.d("RENDER", this.poiId +" poi.lng: " + loc.getLongitude() + ", user.lng: " + userLocation.getLongitude());
			}
			*/
			
		}
		
		//Log.d("RENDER", "x: " + (float)(loc.getLongitude() - userLocation.getLongitude()) * scale + ", y: " + (float)(loc.getLatitude() - userLocation.getLatitude()) * scale);
		gl.glTranslatef((float)(loc.getLongitude() - userLocation.getLongitude()) * scale, (float)(loc.getLatitude() - userLocation.getLatitude()) * scale, -0.5f);
		gl.glScalef(size, size, size); // Scaling the POI to a suitable size. This may need to be adjusted if you change the 'scale' variable.

		if(squareFrame == (null) || triangleFrame == (null)){
			squareFrame = new IndicatorFrame();
			triangleFrame = new Triangle();
		}

		if(squareFrame == (null) || triangleFrame == (null)){
			squareFrame = new IndicatorFrame();
			triangleFrame = new Triangle();
		}
		
		if(this.type.equals("streaming-video-v1") || this.type.equals("type1")){
			squareFrame.draw(gl);
			gl.glPushMatrix();
			gl.glRotatef(90, 1, 0, 0);
			gl.glScalef(5, 5, 5);
			for( int i = 0; i < 8; i++ ){
				gl.glRotatef(360.0f/8.0f, 0, 1, 0);
				squareFrame.draw(gl);
			}
			gl.glPopMatrix();
		}else if (this.type.equals("beacon") || this.type.equals("type2")){
			triangleFrame.draw(gl);
			gl.glPushMatrix();
			triangleFrame.colour(Triangle.Colour.RED);
			gl.glRotatef(90, 1, 0, 0);
			gl.glScalef(2, 2, 2);
			for ( int i = 0; i < 8; i++ ){
				gl.glRotatef(360.0f/8.0f, 0, 1, 0);
				triangleFrame.draw(gl);
			}
			gl.glPopMatrix();
		}else if (this.type.equals("breadcrumb")){
			triangleFrame.draw(gl);
			gl.glPushMatrix();
			triangleFrame.colour(Triangle.Colour.BLUE);
			gl.glRotatef(90, 1, 0, 0);
			gl.glScalef(5, 5, 5);
			for ( int i = 0; i < 8; i++ ){
				gl.glRotatef(360.0f/8.0f, 0, 1, 0);
				triangleFrame.draw(gl);
			}
			gl.glPopMatrix();
		}
	}
}
