package com.appiphany.nacc.utils;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;

import com.appiphany.nacc.R;

public class LocationUtil {
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters
    private static final long MIN_TIME_BW_UPDATES = 5000; // 1 second
    private Activity mContext;
    private Location mLocation;
    private LocationManager locationMgr;
    public static boolean hasAskForGps;
    
    public LocationUtil(Activity context) {
        this.mContext = context;
        this.locationMgr = (LocationManager) context.getSystemService(Activity.LOCATION_SERVICE);
    }
    
    public Location getLocation(LocationListener mListener) {
        try {

            if (locationMgr == null) {
                locationMgr = (LocationManager) mContext.getSystemService(Activity.LOCATION_SERVICE);
            }

            // getting GPS status
            boolean isGPSEnabled = locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            boolean isNetworkEnabled = locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
                DialogUtil.showSettingsAlert(mContext, R.string.gps_setting_title, R.string.gps_setting_message,
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                return null;
            }

            if (!isGPSEnabled && !hasAskForGps) {
                DialogUtil.showGPSSettingsAlert(mContext, R.string.gps_setting_title,
                        R.string.gps_setting_confirm_message, Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                hasAskForGps = true;
            }

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationMgr.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, mListener);
                if (locationMgr != null) {
                    mLocation = locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }

            if (isNetworkEnabled) {
                if (mLocation == null) {
                    locationMgr.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, mListener);
                    if (locationMgr != null) {
                        mLocation = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mLocation;
    }

    public void stopLocationListener(LocationListener mListener) {
        if (locationMgr == null) {
            return;
        }

        locationMgr.removeUpdates(mListener);
        locationMgr = null;
    }

}
