/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova.device;

import java.util.TimeZone;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;

import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest;

import android.provider.Settings;

public class Device extends CordovaPlugin {
    public static final String TAG = "Device";

    public static String platform;                            // Device OS
    public static String uuid;                                // Device UUID

    private static final String ANDROID_PLATFORM = "Android";
    private static final String AMAZON_PLATFORM = "amazon-fireos";
    private static final String AMAZON_DEVICE = "Amazon";

    // API 29+ no longer supports Build.SERIAL (returns UNKNOWN); see
    // https://developer.android.com/reference/android/os/Build.html#SERIAL
    // as getSerial() is supported from API 26 and SERIAL seems to disfunction 
    // (return "unknown") from API 28, the last API before forcing the use of
    // getSerial() will be set to API 27
    public static int LAST_SDK_BEFORE_FORCE_READ_PHONE_STATE_NEEDED = 27; // android.os.Build.VERSION_CODES.O_MR1
    String [] permissions = { Manifest.permission.READ_PHONE_STATE };
    public static boolean bReadPhoneStatePermissionNeeded = false;  // will set to true if permissions needed, based on the device skd api level
    CallbackContext context;    // save the CallbackContext on execute for permission callback

    /**
     * Constructor.
     */
    public Device() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Device.uuid = getUuid();
        // if device runs Android with SDK API Level greater than LAST_SDK_BEFORE_FORCE_READ_PHONE_STATE_NEEDED (above API 27)
        Device.bReadPhoneStatePermissionNeeded = (!(android.os.Build.VERSION.SDK_INT <= LAST_SDK_BEFORE_FORCE_READ_PHONE_STATE_NEEDED));
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        context = callbackContext;
        if ("getDeviceInfo".equals(action)) {
            if (!Device.bReadPhoneStatePermissionNeeded || hasPermisssion()) {
                JSONObject r = makeDeviceJSONObject(true);  // get device with serial
                callbackContext.success(r);
            } else {
                PermissionHelper.requestPermissions(this, 0, permissions);
            }
        } else if(action.equals("getPermission")) {
            if(!Device.bReadPhoneStatePermissionNeeded || hasPermisssion()) {
                PluginResult r = new PluginResult(PluginResult.Status.OK);
                context.sendPluginResult(r);
                return true;
            } else {
                PermissionHelper.requestPermissions(this, 0, permissions);
            }
            return true;
        } else {
            return false;
        }
        return true;
    }

    /**
     * Function to create the Device JSON object with/without serial number checking
     * @param canCheckSerial            true to perform serial check, false to set 'no_permission'
     * 
     * @return
     */
    private JSONObject makeDeviceJSONObject(boolean canCheckSerial) throws JSONException {
        JSONObject r = new JSONObject();
        r.put("uuid", Device.uuid);
        r.put("version", this.getOSVersion());
        r.put("platform", this.getPlatform());
        r.put("model", this.getModel());
        r.put("manufacturer", this.getManufacturer());
            r.put("isVirtual", this.isVirtual());
        r.put("serial", (canCheckSerial ? this.getSerialNumber() : "no_permission"));
        r.put("sdk_version", this.getSDKINTVersion());
        return r;
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * Get the OS name.
     *
     * @return
     */
    public String getPlatform() {
        String platform;
        if (isAmazonDevice()) {
            platform = AMAZON_PLATFORM;
        } else {
            platform = ANDROID_PLATFORM;
        }
        return platform;
    }

    /**
     * Get the device's Universally Unique Identifier (UUID).
     *
     * @return
     */
    public String getUuid() {
        String uuid = Settings.Secure.getString(this.cordova.getActivity().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        return uuid;
    }

    public String getModel() {
        String model = android.os.Build.MODEL;
        return model;
    }

    public String getProductName() {
        String productname = android.os.Build.PRODUCT;
        return productname;
    }

    public String getManufacturer() {
        String manufacturer = android.os.Build.MANUFACTURER;
        return manufacturer;
    }

    public String getSerialNumber() {
        // API 29+ no longer supports Build.SERIAL (returns UNKNOWN); see
        // https://developer.android.com/reference/android/os/Build.html#SERIAL
        String serial = (!Device.bReadPhoneStatePermissionNeeded) ?
                            android.os.Build.SERIAL :
                            android.os.Build.getSerial();
        return serial;
    }

    /**
     * Get the OS version.
     *
     * @return
     */
    public String getOSVersion() {
        String osversion = android.os.Build.VERSION.RELEASE;
        return osversion;
    }

    public String getSDKVersion() {
        @SuppressWarnings("deprecation")
        String sdkversion = android.os.Build.VERSION.SDK;
        return sdkversion;
    }

    public String getSDKINTVersion() {
        int isdkversion = android.os.Build.VERSION.SDK_INT;
        String sdkversion = String.valueOf(isdkversion);
        return sdkversion;
    }

    public String getTimeZoneID() {
        TimeZone tz = TimeZone.getDefault();
        return (tz.getID());
    }

    /**
     * Function to check if the device is manufactured by Amazon
     *
     * @return
     */
    public boolean isAmazonDevice() {
        if (android.os.Build.MANUFACTURER.equals(AMAZON_DEVICE)) {
            return true;
        }
        return false;
    }

    public boolean isVirtual() {
	return android.os.Build.FINGERPRINT.contains("generic") ||
	    android.os.Build.PRODUCT.contains("sdk");
    }

    /**
     * Handle permission requesting result. If permission granted, populate the 
     * Device JSON Object.
     *
     */
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException
    {
        PluginResult result;
        //This is important if we're using Cordova without using Cordova, but we have the geolocation plugin installed
        if(context != null) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    // will return a result anyway (iso. exception)
                    //result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    JSONObject rp = makeDeviceJSONObject(false);        // get device without serial
                    context.success(rp);
                    result = new PluginResult(PluginResult.Status.OK);
                    context.sendPluginResult(result);
                    return;
                }

            }
            JSONObject r = makeDeviceJSONObject(true);  // if has permission, get device with serial
            context.success(r);
            result = new PluginResult(PluginResult.Status.OK);
            context.sendPluginResult(result);
        }
    }

    /**
     * Check if required permissions granted.
     *
     */
    public boolean hasPermisssion() {
        for(String p : permissions) {
            if(!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    /*
     * We override this so that we can access the permissions variable, which no longer exists in
     * the parent class, since we can't initialize it reliably in the constructor!
     */

    public void requestPermissions(int requestCode) {
        PermissionHelper.requestPermissions(this, requestCode, permissions);
    }
}
