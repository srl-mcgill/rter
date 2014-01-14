package ca.nehil.rter.streamingapp2;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import ca.nehil.rter.streamingapp2.util.MovingAverageCompass;

import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SensorSource implements SensorEventListener, LocationListener{

	private static SensorSource singleton = null;
	static Context mcontext; 	// Need context for broadcast manager
	private Location location;
	private float declination = 0;
	private float currentOrientation, deviceOrientation;
	private float[] rotationMatrix = new float[16];
	private float[] orientationValues = new float[3];
	private float[] outRotationMatrix = new float[16];
	private float[] mValues = new float[3];
	private static LocationManager locationManager;
	private static String provider;
	private static SensorManager mSensorManager;

	//SensorFusion Variables
	// angular speeds from gyro
	private float[] gyro = new float[3];

	// rotation matrix from gyro data
	private static float[] gyroMatrix = new float[9];

	// orientation angles from gyro matrix
	private static float[] gyroOrientation = new float[3];

	// magnetic field vector
	private float[] magnet = new float[3];

	// accelerometer vector
	private float[] accel = new float[3];

	// orientation angles from accel and magnet
	private static float[] accMagOrientation = new float[3];

	// final orientation angles from sensor fusion
	private static float[] fusedOrientation = new float[3];

	// accelerometer and magnetometer based rotation matrix
	private float[] rotationMatrixFusion = new float[9];

	// accelerometer and magnetometer based initial rotation matrix
	private float[] initMatrix = new float[9];
	private float[] initMatrixTranspose = new float[9];

	public static final float EPSILON = 0.000000001f;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private float timestamp;
	private boolean initState = true;

	public static final int TIME_CONSTANT = 30;
	public static final float FILTER_COEFFICIENT = 0.98f;
	private static Timer fuseTimer = new Timer();

	// The following members are only for displaying the sensor output.
	public Handler mHandler;
	private RadioGroup mRadioGroup;
	private TextView mAzimuthView;
	private TextView mPitchView;
	private TextView mRollView;
	private int radioSelection;
	DecimalFormat d = new DecimalFormat("#.##");

	public static SensorSource getInstance(Context context){
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Criteria criteria = new Criteria();
		provider = locationManager.getBestProvider(criteria, true);

		// wait for one second until gyroscope and magnetometer/accelerometer
		// data is initialised then scedule the complementary filter task

		if (singleton == null)
		{
			singleton = new SensorSource();
		}
		locationManager.requestLocationUpdates(provider, 0, 1000, singleton); //register singleton with locationmanager
		
		fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
				1000, TIME_CONSTANT);
		//		LocationClient loca;
		//		Location loc = new Location(provider);
		//		loc.setLatitude(15.0000);
		//		loc.setLongitude(15.0000);
		//		loc.setAccuracy(3.0f);
		//		loc.setTime(System.currentTimeMillis());
		//		try {
		//			Location.class.getMethod("makeComplete").invoke(loc);
		//		} catch (Exception e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		//		locationManager.setTestProviderLocation(provider, loc);
		Log.d("alok", "registered for location updates");
		mcontext = context;
		return singleton;
	}

	public void init() {
		gyroOrientation[0] = 0.0f;
		gyroOrientation[1] = 0.0f;
		gyroOrientation[2] = 0.0f;

		// initialise gyroMatrix with identity matrix
		gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
		gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
		gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

		// initialise initMatrix with identity matrix
		initMatrix[0] = 1.0f; initMatrix[1] = 0.0f; initMatrix[2] = 0.0f;
		initMatrix[3] = 0.0f; initMatrix[4] = 1.0f; initMatrix[5] = 0.0f;
		initMatrix[6] = 0.0f; initMatrix[7] = 0.0f; initMatrix[8] = 1.0f;

		initListeners();
	}

	public void initListeners(){
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);

		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	private void initGyroListener() {
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	public Location getLocation(){
		return this.location;
	}

	public float getCurrentOrientation(){
		return this.currentOrientation;
	}

	public float getDeviceOrientation(){
		return this.deviceOrientation;
	}

	public float getDeclination(){
		return declination;
	}


	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		switch (sensorEvent.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			System.arraycopy(sensorEvent.values, 0, accel, 0, 3);
            calculateAccMagOrientation();
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			System.arraycopy(sensorEvent.values, 0, mValues, 0, 3);
			break;
		}

		if (accel == null || mValues == null)
			return;

		if (!SensorManager.getRotationMatrix(rotationMatrix, null, accel, mValues))
			return;
		SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Z,
				SensorManager.AXIS_MINUS_X, outRotationMatrix);
		SensorManager.getOrientation(outRotationMatrix, orientationValues);
		MovingAverageCompass orientationFilter = new MovingAverageCompass(30);
		orientationFilter.pushValue((float) Math.toDegrees(orientationValues[0]));
		currentOrientation = orientationFilter.getValue() + this.getDeclination();
		deviceOrientation = (float) Math.toDegrees(orientationValues[2]);

		Log.d("alok", "sensor source sensors broadcast"+currentOrientation+" "+deviceOrientation);
		sendSensorBroadcast(); 
	}

	@Override
	public void onLocationChanged(Location location) {

		GeomagneticField gmf = new GeomagneticField(
				(float)location.getLatitude(), (float)location.getLongitude(), (float)location.getAltitude(), System.currentTimeMillis());
		declination = gmf.getDeclination();
		this.location = location;
		Log.d("alok", "sensor source location broadcast: "+location);
		sendLocationBroadcast();
	}

	/* Send broadcast for sensor changed */ 
	private void sendSensorBroadcast() {
		Intent sensorIntent = new Intent (mcontext.getString(R.string.SensorEvent));
		LocalBroadcastManager.getInstance(mcontext).sendBroadcast(sensorIntent);
	} 

	/* Send broadcast for location changed */
	private void sendLocationBroadcast(){
		Intent locationIntent = new Intent (mcontext.getString(R.string.LocationEvent));
		LocalBroadcastManager.getInstance(mcontext).sendBroadcast(locationIntent);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}
	
	// calculates orientation angles from accelerometer and magnetometer output
	public void calculateAccMagOrientation() {
		if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
			if(initState) {
				SensorManager.getOrientation(rotationMatrix, accMagOrientation);
				initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
				initMatrixTranspose = transposeMatrix(initMatrix);
				//Log.d("MSC", matrixToString(initMatrix));
				//Log.d("MSC", "init: " + orientationToString(accMagOrientation));
				float[] test = new float[3];
				SensorManager.getOrientation(initMatrix, test);
				//gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
				initState = false;
				initGyroListener();
			}
			else {
				SensorManager.getOrientation(matrixMultiplication(rotationMatrix, initMatrixTranspose), accMagOrientation);
			}
		}
	}
	
	private float[] transposeMatrix(float[] m) {
		float[] result = new float[9];

		result[0] = m[0]; result[1] = m[3]; result[2] = m[6];
		result[3] = m[1]; result[4] = m[4]; result[5] = m[7];
		result[6] = m[2]; result[7] = m[5]; result[8] = m[8];

		return result;
	}

	private static float[] getRotationMatrixFromOrientation(float[] o) {
		float[] xM = new float[9];
		float[] yM = new float[9];
		float[] zM = new float[9];

		float sinX = (float)Math.sin(o[1]);
		float cosX = (float)Math.cos(o[1]);
		float sinY = (float)Math.sin(o[2]);
		float cosY = (float)Math.cos(o[2]);
		float sinZ = (float)Math.sin(o[0]);
		float cosZ = (float)Math.cos(o[0]);

		// rotation about x-axis (pitch)
		xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
		xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
		xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

		// rotation about y-axis (roll)
		yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
		yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
		yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

		// rotation about z-axis (azimuth)
		zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
		zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
		zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

		// rotation order is y, x, z (roll, pitch, azimuth)
		float[] resultMatrix = matrixMultiplication(xM, yM);
		resultMatrix = matrixMultiplication(zM, resultMatrix);
		return resultMatrix;
	}
	
	private static float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];
     
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
     
        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
     
        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
     
        return result;
    }
	
	static class calculateFusedOrientationTask extends TimerTask {
		public void run() {
			float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

			/*
			 * Fix for 179 <--> -179 transition problem:
			 * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
			 * If so, add 360 (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360 from the result
			 * if it is greater than 180. This stabilizes the output in positive-to-negative-transition cases.
			 */

			// azimuth
			if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
				fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
				fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
			}
			else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
				fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
				fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
			}
			else {
				fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
			}

			// pitch
			if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
				fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
				fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
			}
			else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
				fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
				fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
			}
			else {
				fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
			}

			// roll
			if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
				fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
				fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
			}
			else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
				fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
				fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
			}
			else {
				fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
			}

			// overwrite gyro matrix and orientation with fused orientation
			// to comensate gyro drift
			gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
			System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);

		}
	}

}
