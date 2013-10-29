package ca.nehil.rter.streamingapp2;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class JSInterface {
	
	Context mContext;

    /** Instantiate the interface and set the context */
    JSInterface(Context c) {
        mContext = c;
    }

    /** Receive any data from JS. Method called by the JS. */
    @JavascriptInterface
    public void getDataFromJS(String data) {
        Log.d("alok", "received data from JS: " + data);
    }
    
    /** Send any data to the JS. Method called by the JS */
    @JavascriptInterface
    public String sendDataToJS(){
    	String data = "So much data";
    	return data;
    }
}
