/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.test;

import com.apigee.sdk.ApigeeClient;
import com.apigee.sdk.data.client.DataClient;
import com.apigee.sdk.data.client.callbacks.ApiResponseCallback;
//import com.apigee.sdk.data.client.callbacks.ApiResponseCallback;
import com.apigee.sdk.data.client.callbacks.DeviceRegistrationCallback;
import com.apigee.sdk.data.client.entities.Device;
import com.apigee.sdk.data.client.entities.Entity;
import com.apigee.sdk.data.client.response.ApiResponse;
import com.apigee.sdk.data.client.utils.JsonUtils;
//import com.apigee.sdk.data.client.entities.Entity;
//import com.apigee.sdk.data.client.response.ApiResponse;
//import com.apigee.sdk.data.client.utils.JsonUtils;

//import java.util.HashMap;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main UI for the demo app.
 */
public class MainActivity extends Activity {

    /**
     * Tag used on log messages.
     */
    static final String TAG = "Sape-Connect";

    //TextView mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    static Context context;

    String regid;
    private static DataClient client;
    private static Device device;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mDisplay = (TextView) findViewById(R.id.display);
        context = getApplicationContext();

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context); // from db

            if (regid.isEmpty()) {
                registerInBackground();
            }
            else
            {
                //mDisplay.setText(regid);
                //checkNotificationFromSapient();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }
    
    void showADialog(final String msg, String type)
    {
    	String option = getLikedOrUnlike();
    	Toast.makeText(context, option, Toast.LENGTH_LONG);
    	if(type.equals("Survey") && option.isEmpty())
    	{
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	
	        builder.setTitle("Your Feedback is important");
	        builder.setMessage(msg);
	
	        builder.setPositiveButton("Like", new DialogInterface.OnClickListener() {
	
	            public void onClick(DialogInterface dialog, int which) {
	                // Do nothing but close the dialog
	            	sendResponseToServer(msg, "Like");
	            	storeLikedOrUnlike("Like");
	                dialog.dismiss();
	            }
	
	        });
	
	        builder.setNegativeButton("UnLike", new DialogInterface.OnClickListener() {
	
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	                // Do nothing
	            	sendResponseToServer(msg, "UnLike");
	            	storeLikedOrUnlike("UnLike");
	                dialog.dismiss();
	            }
	        });
	
	        AlertDialog alert = builder.create();
	        alert.show();
        }
    	else
    	{
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);	    	
	        builder.setTitle("Sape-Connect");
	        if(type.equals("Survey"))
	        {
	        	String temp  = msg + "\n You " + option + "d it !" ;
	        	builder.setMessage(temp);
	        }
	        else
	        {
	        	builder.setMessage(msg);
	        }
	     // Setting OK Button
	        builder.setNeutralButton("Ok GotIt!", null);	                
	        AlertDialog alert = builder.create();
	        alert.show();    		
    	}
    }
    
    void showASuccessDialog(String aMessage)
    {	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);	    	
        builder.setTitle("Sape-Connect");
        builder.setMessage(aMessage);	        
     // Setting OK Button
        builder.setNeutralButton("Ok", null);	                
        AlertDialog alert = builder.create();
        alert.show();
    }

    
    void sendResponseToServer(String subject, String response)
    {
	    if (client == null) {
	    	ApigeeClient apigeeClient = new ApigeeClient(Utility.ORG,Utility.APP,Utility.API_URL,context);
	    	client = apigeeClient.getDataClient();
	    }
    	
    	Entity surveyResponse = new Entity();
    	surveyResponse.setType("SurveyResponse");
    	surveyResponse.setProperty("SurveyName", JsonUtils.toJsonNode(subject));
    	surveyResponse.setProperty("Response", JsonUtils.toJsonNode(response));
    	surveyResponse.setProperty("DeviceID", JsonUtils.toJsonNode(regid));
    	client.createEntityAsync(surveyResponse, new ApiResponseCallback() {
    	        @Override
    	        public void onException(Exception ex) {
    	                Log.i("apigee", ex.toString());
    	        }
				@Override
				public void onResponse(ApiResponse arg0) {
					// TODO Auto-generated method stub
					showASuccessDialog("Thanks for your time !");
				}        
    	});
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
        checkNotificationFromSapient();
    }

    void checkNotificationFromSapient()
    {
		String savedMessage = getSavedMessage(this);
    	String messageType = getSavedMessageType(this);
   		showADialog(savedMessage, messageType);
    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                		Utility.PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    void storeLikedOrUnlike(String message)
    {
    	 final SharedPreferences prefs = getGcmPreferences(context);
            Log.i(TAG, "Saving storeLikedOrUnlike" + message);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Utility.PROPERTY_MESSAGE_OPTION, message);
            editor.commit();    	
    }
    
    String getLikedOrUnlike()
    {
    	 final SharedPreferences prefs = getGcmPreferences(context);
    	 String option = prefs.getString(Utility.PROPERTY_MESSAGE_OPTION, "");
         if (option.isEmpty()) {
             Log.i(TAG, "Like Option not found.");
             return "";
         }
         return option;
     }

    
    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utility.PROPERTY_REG_ID, regId);
        editor.putInt(Utility.PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(Utility.PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(Utility.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }
    
    private String getSavedMessage(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String savedMessage = prefs.getString(Utility.PROPERTY_MESSAGE_CONTENT, "");
        if (savedMessage.isEmpty()) {
            Log.i(TAG, "savedMessage not found.");
            return "";
        }
        return savedMessage;
    }    
    private String getSavedMessageType(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String savedMessage = prefs.getString(Utility.PROPERTY_MESSAGE_TYPE, "");
        if (savedMessage.isEmpty()) {
            Log.i(TAG, "savedMessageType not found.");
            return "";
        }
        return savedMessage;
    }    


    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(Utility.SENDER_ID);
                	if(!regid.isEmpty())
                	{
                        msg = "Device registered with GCM, registration ID=" + regid;
                        sendRegistrationIdToBackend(context, regid);
                	}

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    //sendRegistrationIdToBackend(regid);

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                //mDisplay.append(msg + "\n");
                //GCMIntentService.setDisplay(mDisplay);
            }
        }.execute(null, null, null);
    }
        
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    static void sendRegistrationIdToBackend(Context aContext, String regId) {
    	    if (client == null) {
    	    	ApigeeClient apigeeClient = new ApigeeClient(Utility.ORG,Utility.APP,Utility.API_URL,aContext);
    	    	client = apigeeClient.getDataClient();
    	    }
    	    client.registerDeviceForPushAsync(aContext, Utility.NOTIFIER, regId, null, new DeviceRegistrationCallback() {

				@Override
				public void onException(Exception e) {
					Log.i(TAG, "register exception: " + e);
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onResponse(Device aDevice) {
					// TODO Auto-generated method stub
			    	  Log.i(TAG, "device registered with Guuid: " + aDevice.getUuid());
			    	  device = aDevice;
			    	  Toast tst = Toast.makeText(context, "Device Successfully Registered", Toast.LENGTH_SHORT);
			    	  tst.show();
				}

				@Override
				public void onDeviceRegistration(Device arg0) {
					// TODO Auto-generated method stub
					
				}
    	    }
    	    );

    }
}
