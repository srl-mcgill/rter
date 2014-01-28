package ca.nehil.rter.streamingapp2;

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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class SensorSource implements SensorEventListener, LocationListener{

	private static SensorSource singleton = null;
	static Context mcontext; 	// Need context for broadcast manager
	private Location location;
	private float declination = 0;
	private float currentOrientation;
	private float tempCurrentOrientation = 0;
	private float deviceOrientation;
	private float[] rotationMatrix = new float[16]; //Change to 9 if using sensor fusion methods, 16 otherwise
	private float[] orientationValues = new float[3];
	private float[] outRotationMatrix = new float[16];
	private static LocationManager locationManager;
	private static SensorManager mSensorManager;
	private static String provider;
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

    public static final int TIME_CONSTANT = 20;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer = new Timer();

    DecimalFormat d = new DecimalFormat("#.##");

    private Intent locationIntent;
    private Intent sensorIntent;
    private LocalBroadcastManager localBroadcastManager;
    private MovingAverageCompass orientationFilter;
	
	public SensorSource(Context context){
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		orientationFilter = new MovingAverageCompass(30);
		
		provider = locationManager.getBestProvider(criteria, true);
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		
		locationIntent = new Intent (context.getString(R.string.LocationEvent));
		sensorIntent = new Intent (context.getString(R.string.SensorEvent));
		localBroadcastManager = LocalBroadcastManager.getInstance(context);
		
		//SensorFusion initiliazations
		initValues();
		initListeners();
		// wait for one second until gyroscope and magnetometer/accelerometer
		// data is initialised then scedule the complementary filter task
		fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
				500, TIME_CONSTANT);
	}

	public static SensorSource getInstance(Context context){
		
		if (singleton == null)
		{
			singleton = new SensorSource(context);
		}
		
		locationManager.requestLocationUpdates(provider, 1000, 0, singleton); //register singleton with locationmanager
		mcontext = context;
		return singleton;
	}

	public void initValues() {
		gyroOrientation[0] = 0.0f;
		gyroOrientation[1] = 0.0f;
		gyroOrientation[2] = 0.0f;

		// initialise gyroMatrix with identity matrix
		gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
		gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
		gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

	}
	
	public void resetHeading(){
		initState = true;
		initValues();
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
	
	public void stopListeners(){
		mSensorManager.unregisterListener(this);
		initState = true;
		initValues();
	}

	public Location getLocation(){
		if(this.location != null){
			Log.d("Location: ", this.location+"");
			return this.location;
		}else{
			return locationManager.getLastKnownLocation(provider);
		}
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
	
	public double getHeading(){
		return getGyroHeading();
	}
	
	public double getGyroHeading(){
		return gyroOrientation[0] * 180/Math.PI;
	}
	
	public double getFusedHeading(){
		return fusedOrientation[0] * 180/Math.PI;
	}
	
	public double getMagHeading(){
		return accMagOrientation[0] * 180/Math.PI;
	}


	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		switch (sensorEvent.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			// copy new acceleroeter data into accel array and calculate orientation
			Log.d("SensorDebug", "ACC: " + sensorEvent.values[0] + ", " + sensorEvent.values[1] + ", " + sensorEvent.values[2]);
//			01-27 14:45:04.451: D/SensorDebug(4854): ACC: 9.760902, 0.6372249, 2.0146606
//			sensorEvent.values[0] = 9.760902f;
//			sensorEvent.values[1] = 0.6372249f;
//			sensorEvent.values[2] = 2.0146606f;
			System.arraycopy(sensorEvent.values, 0, accel, 0, 3);
            calculateAccMagOrientation();
			break;
			
		case Sensor.TYPE_MAGNETIC_FIELD:
			// copy new magnetometer data into magnet array
			Log.d("SensorDebug", "MAG: " + sensorEvent.values[0] + ", " + sensorEvent.values[1] + ", " + sensorEvent.values[2]);
//			01-27 14:45:04.888: D/SensorDebug(4854): MAG: -30.5, -3.4130096, -15.726303
//			sensorEvent.values[0] = -30.5f;
//			sensorEvent.values[1] = -3.4130096f;
//			sensorEvent.values[2] = -15.726303f;
			System.arraycopy(sensorEvent.values, 0, magnet, 0, 3);
			break;
			
		case Sensor.TYPE_GYROSCOPE:
			//process gyro data
			Log.d("SensorDebug", "GYRO: " + sensorEvent.values[0] + ", " + sensorEvent.values[1] + ", " + sensorEvent.values[2]);
//			01-27 14:45:09.005: D/SensorDebug(4854): GYRO: -0.027712587, 0.0948602, 0.001065797
//			sensorEvent.values[0] = -0.027712587f;
//			sensorEvent.values[1] = 0.0948602f;
//			sensorEvent.values[2] = 0.001065797f;
			gyroFunction(sensorEvent);
			break;
		}

		if (accel == null || magnet == null)
			return;

		if (!SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet))
			return;
		SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Z,
				SensorManager.AXIS_MINUS_X, outRotationMatrix);
		SensorManager.getOrientation(outRotationMatrix, orientationValues);
		orientationFilter.pushValue((float) Math.toDegrees(orientationValues[0]));
//		currentOrientation = orientationFilter.getValue() + this.getDeclination();
		currentOrientation = (float) (Math.toDegrees(orientationValues[0]) + this.getDeclination());
		Log.d("SensorDebug", "" + currentOrientation);
		
//		// Method 3: Ignore all differences of less than 10 degrees
//		if(Math.abs(tempCurrentOrientation - currentOrientation) > 10){
//			tempCurrentOrientation = currentOrientation;
//		}
//		currentOrientation = tempCurrentOrientation;
//		// End of Method 3
		
		deviceOrientation = (float) Math.toDegrees(orientationValues[2]);

		sendSensorBroadcast(); 
	}

	@Override
	public void onLocationChanged(Location location) {
		GeomagneticField gmf = new GeomagneticField(
				(float)location.getLatitude(), (float)location.getLongitude(), (float)location.getAltitude(), System.currentTimeMillis());
		declination = gmf.getDeclination();
		this.location = location;
		Log.d("LocationDebug", "location broadcast: "+location);
		sendLocationBroadcast();
	}

	/* Send broadcast for sensor changed */ 
	private void sendSensorBroadcast() {
		localBroadcastManager.sendBroadcast(sensorIntent);
	} 

	/* Send broadcast for location changed */
	private void sendLocationBroadcast(){
		localBroadcastManager.sendBroadcast(locationIntent);
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
	
	// This function performs the integration of the gyroscope data.
	// It writes the gyroscope based orientation into gyroOrientation.
	public void gyroFunction(SensorEvent event) {
		// copy the new gyro values into the gyro array
		// convert the raw gyro data into a rotation vector
		float[] deltaVector = new float[4];
		if(timestamp != 0) {
			final float dT = (event.timestamp - timestamp) * NS2S;
			System.arraycopy(event.values, 0, gyro, 0, 3);
			getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
		}

		// measurement done, save current time for next interval
		timestamp = event.timestamp;

		// convert rotation vector into rotation matrix
		float[] deltaMatrix = new float[9];
		SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

		// apply the new rotation interval on the gyroscope based rotation matrix
		gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

		// get the gyroscope based orientation from the rotation matrix
		SensorManager.getOrientation(gyroMatrix, gyroOrientation);
	}
	
	
	// This function is borrowed from the Android reference
	// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	// It calculates a rotation vector from the gyroscope angular speed values.
	private void getRotationVectorFromGyro(float[] gyroValues,
			float[] deltaRotationVector,
			float timeFactor)
	{
		float[] normValues = new float[3];

		// Calculate the angular speed of the sample
		float omegaMagnitude =
				(float)Math.sqrt(gyroValues[0] * gyroValues[0] +
						gyroValues[1] * gyroValues[1] +
						gyroValues[2] * gyroValues[2]);

		// Normalize the rotation vector if it's big enough to get the axis
		if(omegaMagnitude > EPSILON) {
			normValues[0] = gyroValues[0] / omegaMagnitude;
			normValues[1] = gyroValues[1] / omegaMagnitude;
			normValues[2] = gyroValues[2] / omegaMagnitude;
		}

		// Integrate around this axis with the angular speed by the timestep
		// in order to get a delta rotation from this sample over the timestep
		// We will convert this axis-angle representation of the delta rotation
		// into a quaternion before turning it into the rotation matrix.
		float thetaOverTwo = omegaMagnitude * timeFactor;
		float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
		float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
		deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
		deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
		deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
		deltaRotationVector[3] = cosThetaOverTwo;
	}
	
	// calculates orientation angles from accelerometer and magnetometer output
	public void calculateAccMagOrientation() {
		if(SensorManager.getRotationMatrix(rotationMatrixFusion, null, accel, magnet)) {
			if(initState) {
				SensorManager.getOrientation(rotationMatrixFusion, accMagOrientation);
				initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
				initMatrixTranspose = transposeMatrix(initMatrix);
				//Log.d("MSC", matrixToString(initMatrix));
				//Log.d("MSC", "init: " + orientationToString(accMagOrientation));
//				float[] test = new float[3];
//				SensorManager.getOrientation(initMatrix, test);
				//gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
				initState = false;
				initGyroListener();
			}
			else {
				SensorManager.getOrientation(matrixMultiplication(rotationMatrixFusion, initMatrixTranspose), accMagOrientation);
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
