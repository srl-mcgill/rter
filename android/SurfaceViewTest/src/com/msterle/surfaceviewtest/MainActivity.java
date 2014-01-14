package com.msterle.surfaceviewtest;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends Activity {

	private DrawingPanel myPanel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myPanel = new DrawingPanel(this);
        setContentView(myPanel);
	}
	
	@Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}

class DrawingPanel extends SurfaceView implements SurfaceHolder.Callback {

	private PanelThread _thread;
	private final Bitmap mBitmapFromSdcard;
	
	public DrawingPanel(Context context) {
		super(context);
		getHolder().addCallback(this);
		mBitmapFromSdcard = BitmapFactory.decodeFile("/mnt/sdcard/Pictures/beachball.jpg");
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		setWillNotDraw(false); //Allows us to use invalidate() to call onDraw()

	    _thread = new PanelThread(getHolder(), this); //Start the thread that
        _thread.setRunning(true);                     //will make calls to 
        _thread.start();                              //onDraw()
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		try {
            _thread.setRunning(false);                //Tells thread to stop
		    _thread.join();                           //Removes thread from mem.
		} catch (InterruptedException e) {}
	}
	
	@Override 
    public void onDraw(Canvas canvas) { 
		drawCamera(canvas);
    }
	
	private void drawSimple(Canvas canvas) {
		Canvas g = canvas;
        if (mBitmapFromSdcard != null) {
            g.drawBitmap(mBitmapFromSdcard, 0, 0, null);
        }
	}
	
	private void drawCamera(Canvas canvas) {
		Paint mPaint = new Paint(); 
        mPaint.setAntiAlias(true); 
        mPaint.setFilterBitmap(true); 

        Camera mCamera = new Camera(); 
        mCamera.save(); 
        
        float angle = (System.currentTimeMillis() / 100 % 180) - 90;
        float translate = angle * 10;
        
        // Here is the rotation in 3D space
        mCamera.translate(translate, 0, 0);
        mCamera.rotateY(-angle);

        Matrix mMatrix = new Matrix(); 
        mCamera.getMatrix(mMatrix); 
        mCamera.restore();
        canvas.drawBitmap(mBitmapFromSdcard, mMatrix, mPaint); 
	}

}


class PanelThread extends Thread {
    private SurfaceHolder _surfaceHolder;
    private DrawingPanel _panel;
    private boolean _run = false;


    public PanelThread(SurfaceHolder surfaceHolder, DrawingPanel panel) {
        _surfaceHolder = surfaceHolder;
        _panel = panel;
    }


    public void setRunning(boolean run) { //Allow us to stop the thread
        _run = run;
    }


    @Override
    public void run() {
        Canvas c;
        while (_run) {     //When setRunning(false) occurs, _run is 
            c = null;      //set to false and loop ends, stopping thread
            try {
                c = _surfaceHolder.lockCanvas(null);
                synchronized (_surfaceHolder) {
	                 //Insert methods to modify positions of items in onDraw()
                	_panel.postInvalidate();
                }
            } finally {
                if (c != null) {
                    _surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }
}