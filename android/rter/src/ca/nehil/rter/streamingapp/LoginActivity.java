package ca.nehil.rter.streamingapp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import ca.nehil.rter.streamingapp.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity{
	/**
	 * A dummy authentication store containing known user names and passwords.
	 * TODO: remove after connecting to a real authentication system.
	 */
	private static final String[] DUMMY_CREDENTIALS = new String[] {
			"anonymous", "anonymous" };

	private String server_url;
	
	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_USERNAME = "anonymous";
	
	private static final String TAG = "LOGIN ACTIVITY";
	private String rterCreds=null;
	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private UserLoginTask mAuthTask = null;

	// Values for email and password at the time of the login attempt.
	private String mUsername;
	private String mPassword;

	// UI references.
	private EditText mUsernameView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	private SharedPreferences cookies; //To store cookie data related to a user.
	private SharedPreferences storedValues; //To store general values related to the application.
	private SharedPreferences.Editor cookieEditor; //SharedPrefs editor for the cookies.
	private SharedPreferences.Editor storedValuesEditor; //SharedPrefs editor for the stored values.
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);
		serverSetup();
		cookies = getSharedPreferences("RterUserCreds", MODE_PRIVATE);
		cookieEditor = cookies.edit();
		
		String setUsername = cookies.getString("Username", "not-set");
		String setPassword = cookies.getString("Password", "not-set");
		String setrter = cookies.getString("RterCreds", "not-set");
		Log.d(TAG, "Prefs ==> Username:"+setUsername+" :: Password:" + setPassword +" :: rter cred:" + setrter);
		
		// Set up the login form.
		mUsername = getIntent().getStringExtra(EXTRA_USERNAME);
		if(!(setUsername.equalsIgnoreCase("not-set"))){
			mUsername = setUsername;		
		}
		mUsernameView = (EditText) findViewById(R.id.username);
		mUsernameView.setText(mUsername);

		if(!(setPassword.equalsIgnoreCase("not-set"))){
			mPassword = setPassword;		
		}
		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView.setText(mPassword);
		
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id,
					KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		// BEGIN Auto-login
		/*
		mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
		showProgress(true);
		Log.d(TAG, "Username:"+"video"+" :: Password:" + "video");
		mAuthTask = new UserLoginTask();
		mAuthTask.execute("video", "video");
		*/
		// END Auto-login
		
		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
		
//		startService(new Intent(LoginActivity.this, BackgroundService.class));
	}
	
	/*
	 * This method is called within onCreate. It simply sets up the server list, which server is active and stores
	 * server values in sharedpreferences.
	 */
	private void serverSetup(){
		ArrayList<String> serverList;
		
		storedValues = getSharedPreferences(getString(R.string.sharedPreferences_filename), MODE_PRIVATE);
		storedValuesEditor = storedValues.edit();
		
		//Get server url.
		server_url = storedValues.getString("server_url", "not-set");
		
		if(server_url.equals("not-set")){
			// This means there is no server data present, likely because the app is newly installed.
			serverList = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.servers)));
			server_url = serverList.get(0); //If no server set, set it by default as the first one
			storedValuesEditor.putString("server_url", server_url); 
			Set<String> set = new HashSet<String>();
			set.addAll(serverList); // Converting serverList from an ArrayList to a HashSet since sharedprefs takes only sets, not arrays.
			storedValuesEditor.putStringSet("server_list", set);
			storedValuesEditor.commit();
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}
	

	/*
	 * Handle options menu item selection.
	 * (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.action_forgot_password:
			//TODO: Implement forgot password.
			return true;
			
		case R.id.action_change_server:
			Intent listIntent = new Intent(this, ServerList.class);
			startActivity(listIntent);
//			View menuItemView = findViewById(R.id.login_status); //login_status is the view to which the popup is anchored to.
//			changeServerPopup(menuItemView);
			return true;
			
		default:
            return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mUsernameView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mUsername = mUsernameView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			Log.d(TAG, "Username:"+mUsername+" :: Password:" + mPassword);
			mAuthTask = new UserLoginTask();
			mAuthTask.execute(mUsername, mPassword);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate
	 * the user.
	 */
	public class UserLoginTask extends AsyncTask<String, Void, Boolean> {
		private static final String TAG = "LOGIN ASYNCTASK";
		
		@Override
		protected Boolean doInBackground(String... params) {
			// TODO: attempt authentication against a network service.
			Log.d(TAG, "Username:"+params[0]+" :: Password:" + params[0]);
			JSONObject jsonObjSend = new JSONObject();

			int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
			HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
			HttpClient client = new DefaultHttpClient(httpParams);
			HttpPost post_request = new HttpPost(server_url+"/auth");
			Header[] headers= null;
			HttpResponse response = null;
			try {
				jsonObjSend.put("Username", params[0]);
				jsonObjSend.put("Password", params[1]);
				// Output the JSON object we're sending to Logcat:
				Log.i(TAG, jsonObjSend.toString(2));
				 
				post_request.setEntity(new ByteArrayEntity(
						jsonObjSend.toString().getBytes("UTF8")));
				response = client.execute(post_request);
				
				headers = response.getAllHeaders();
				Log.i(TAG, "response from "
						+ server_url 
						+" = status line : "+ response.getStatusLine().getStatusCode());
				
				
				
				if(response.getStatusLine().getStatusCode() == 200){
					for (Header header : headers) {
						if(header.getName().equalsIgnoreCase("Set-Cookie")){
							String key = header.getName();
							String value = header.getValue();
							int indexOfEndOfCreds = header.getValue().indexOf(';');
							rterCreds = value.substring(0, indexOfEndOfCreds);
							String cookieName = rterCreds.substring(0, rterCreds.indexOf("="));
						    String cookieValue = rterCreds.substring(rterCreds.indexOf("=") + 1, rterCreds.length());
							
							
							Log.i(TAG,"Key : " + key 
								      + " ,Value : " + value); 
							
							Log.i(TAG,"The value of rter-creds is" + rterCreds);
							cookieEditor.putString("Username", params[0]);  
							cookieEditor.putString("Password", params[1]);
							cookieEditor.putString("RterCreds", rterCreds);
							cookieEditor.commit(); 
						}								
					}
					return true; 
				}else{
					return false;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// TODO: register the new account here.
			return true;
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			showProgress(false);

			if (success) {				
				Log.i(TAG, "Calling Intent to Streaming ACtivity");
				Intent intent = new Intent(LoginActivity.this, StreamingActivity.class);
		        startActivity(intent);
				
			} else {
				mPasswordView
						.setError(getString(R.string.error_incorrect_password));
				mPasswordView.requestFocus();
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}

}
