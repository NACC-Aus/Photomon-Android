package com.appiphany.nacc.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.appiphany.nacc.screens.LocationService;
import com.appiphany.nacc.utils.Config;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibraryConstants;

public class LocationUpdateReceiver extends BroadcastReceiver {
    private long lastTimeUpdateLocation = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SystemClock.elapsedRealtime() - lastTimeUpdateLocation < Config.LOCATION_REFRESH_TIME) {
            onRefreshLocation(intent);
            return;
        }

        lastTimeUpdateLocation = SystemClock.elapsedRealtime();
        Intent locationIntent = new Intent(context, LocationService.class);
        locationIntent.addCategory(LocationService.SERVICE_TAG);
        locationIntent.setAction(LocationService.LOCATION_CHANGED);
        locationIntent.putExtra(LocationService.LOCATION_DATA, intent.getSerializableExtra(LocationLibraryConstants.LOCATION_BROADCAST_EXTRA_LOCATIONINFO));
        context.startService(locationIntent);
    }

    public void onRefreshLocation(Intent intent) {}
}
