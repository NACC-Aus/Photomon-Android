package com.appiphany.nacc.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.appiphany.nacc.screens.LoginActivity;
import com.appiphany.nacc.ui.controls.NaccBitmapDisplayer;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

public class Config {
	public static final String LOG_TAG = "nacc";
    public static final String DEFAULT_SERVER = "https://photomon.nacc.com.au";
    public static final long[] VIBRATE_PATTERN = new long[] {
            0, 500, 200, 500, 200, 500, 200
    };

    public static final long INTERVAL_DAY = 24 * 3600;
    public static final long INTERVAL_WEEK = INTERVAL_DAY * 7;
    public static final long INTERVAL_FORTNIGHTLY = INTERVAL_WEEK * 2;
    public static final int LOCATION_REFRESH_TIME = 3 * 60 * 1000; // 3 minutes


    public static final int CONFIG_WIDTH = 1280;
    public static final int CONFIG_HEIGHT = 960;
    public static final int HTTP_CONNECT_TIMEOUT = 3000;
    public static final int HTTP_UPLOAD_IMAGE_TIMEOUT = 1 * 30 * 1000;

    public static final int LOCATION_DISTANCE = 50;
    public static final int LOCATION_NEAREST_DISTANCE = 100;
    public static final int MAP_ZOOM_DISTANCE = 1000; // meters

    private static final String USER_INFO_ACCESS_TAG = "user-access-tag";
    private static final String ACCESS_TOKEN_TAG = "access-token";
    private static final String ACTIVE_SERVER_TAG = "active-server";
    private static final String ACTIVE_USER_TAG = "user-name";
    private static final String DEMO_TAG = "demo_tag";
    private static final String LAST_SYNC = "last_sync";
    private static final String SHOULD_SHOW_DIRECTION_DIALOG = "should_show_direction";
    private static final String CURRENT_PROJECT_ID = "current_project_id";
    
    public static final String ZOOZ_KEY = "ZOOZ_KEY";
    public static final String DONATE_URL = "https://photomon.nacc.com.au/mobile/index.html";
    public static final String INFO_URL = "https://www.nacc.com.au/photomon";
    
    public static final DisplayImageOptions FULL_OPTIONS;
    static {
        FULL_OPTIONS = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(true)
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .bitmapConfig(Bitmap.Config.RGB_565) // default
                .displayer(new NaccBitmapDisplayer())
                .considerExifParams(true)
                .build();
    }

    public static String getAccessToken(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        return sharedPref.getString(ACCESS_TOKEN_TAG, null);
    }

    public static void setAccessToken(Context context, String mAccessToken) {
        SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        sharedPref.edit().putString(ACCESS_TOKEN_TAG, mAccessToken).commit();
    }

    public static String getActiveServer(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        return sharedPref.getString(ACTIVE_SERVER_TAG, null);
    }

    public static void setActiveServer(Context context, String activeServer) {
        SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        sharedPref.edit().putString(ACTIVE_SERVER_TAG, activeServer).commit();
    }

    public static String getActiveUser(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        return sharedPref.getString(ACTIVE_USER_TAG, null);
    }

    public static void setActiveUser(Context context, String activeUser) {
        SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        sharedPref.edit().putString(ACTIVE_USER_TAG, activeUser).commit();
    }

    public static void setDemoMode(Context context, String text) {
        SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        sharedPref.edit().putString(DEMO_TAG, text).commit();
    }

    public static boolean isDemoMode(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        String demo = sharedPref.getString(DEMO_TAG, null);
        if (LoginActivity.SERVER_DEMO.equalsIgnoreCase(demo)) {
            return true;
        }
        return false;
    }
    
    public static void setLastSync(Context context, String lastSync){
    	SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        sharedPref.edit().putString(LAST_SYNC, lastSync).commit();
    }
    
    public static String getLastSync(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        return sharedPref.getString(LAST_SYNC, null);
    }
    
    public static void setShowDirectionDialog(Context context, boolean shouldShow){
    	SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
    	sharedPref.edit().putBoolean(SHOULD_SHOW_DIRECTION_DIALOG, shouldShow).commit();
    }
    
    public static boolean shouldShowDirectionDialog(Context context){
    	SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(SHOULD_SHOW_DIRECTION_DIALOG, false);
    }
    
    public static void setCurrentProjectId(Context context, String projectId){
    	SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
    	sharedPref.edit().putString(CURRENT_PROJECT_ID, projectId).commit();
    }
    
    public static String getCurrentProjectId(Context context){
    	SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
    	return sharedPref.getString(CURRENT_PROJECT_ID, "");
    }
    
    public static void clearCurrentProjectId(Context context){
    	SharedPreferences sharedPref = context.getSharedPreferences(USER_INFO_ACCESS_TAG, Context.MODE_PRIVATE);
    	sharedPref.edit().remove(CURRENT_PROJECT_ID).commit();
    }
}
