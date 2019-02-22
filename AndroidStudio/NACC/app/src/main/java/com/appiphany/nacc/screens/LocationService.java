package com.appiphany.nacc.screens;

import java.util.List;

import com.appiphany.nacc.events.UpdateSites;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.NetworkUtils;
import com.appiphany.nacc.utils.UIUtils;
import com.crashlytics.android.Crashlytics;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import de.greenrobot.event.EventBus;

public class LocationService extends Service{
	public static final String LOCATION_CHANGED = "location_changed";
	public static final String LOCATION_DATA = "location_data";
    public static final String REFRESH_SITE = "REFRESH_SITE";

	public static final String SERVICE_TAG = "LocationService";
	public static final String UPDATE_SITE_ACTION = "UpdateSite";
	private static final int CHECK_SETTING_INTERVAL = 15000;
	public LocationManager locationManager;
	private Context context;
	Intent intent;
	private volatile boolean isCheckingSetting;
	private UpdateSitesTask updateSitesTask;
	private boolean oldGPSEnabled;
	private boolean oldNetworkEnabled;
	
	@Override
	public void onCreate() {
		super.onCreate();
		intent = new Intent(UPDATE_SITE_ACTION);
		Ln.d("onCreate location service");
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Ln.d("onStartCommand location service");
        try {
            setupLocationListener();
            if (!isCheckingSetting) {
                isCheckingSetting = true;
                performOnBackgroundThread(new CheckForSettingChanged());
            }

            context = this;
            if (intent != null) {
                if(LOCATION_CHANGED.equals(intent.getAction())) {
                    onLocationChanged(intent);
                }else if(REFRESH_SITE.equals(intent.getAction())){
                    updateSite();
                }
            }

        }catch (Exception ex){
            ex.printStackTrace();
            Crashlytics.logException(ex);
        }

		return START_STICKY;
	}


	private void setupLocationListener() {
		if(locationManager == null){
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}
		
		// getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if(isGPSEnabled == oldGPSEnabled && isNetworkEnabled == oldNetworkEnabled){
        	return;
        }
        
        oldGPSEnabled = isGPSEnabled;
        oldNetworkEnabled = isNetworkEnabled;        
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		// handler.removeCallbacks(sendUpdatesToUI);
		super.onDestroy();
		Ln.v("Location service on deytroy");
		isCheckingSetting = false;
	}

	public static Thread performOnBackgroundThread(final Runnable runnable) {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				}catch (Exception ex){
					ex.printStackTrace();
				}
			}
		};
		
		t.start();
		return t;
	}
	
	Handler mainThreadhandler = new Handler(Looper.getMainLooper());
	
	private class CheckForSettingChanged implements Runnable{

		@Override
		public void run() {
			Ln.d("CheckForSettingChanged");
			try {
				while (isCheckingSetting) {
					try {
						Thread.sleep(CHECK_SETTING_INTERVAL);
						mainThreadhandler.post(updateSettingRunnable);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}
	
	Runnable updateSettingRunnable = new Runnable() {
		
		@Override
		public void run() {
			setupLocationListener();
		}
	};
	
	private class UpdateSitesTask implements Runnable{
		Location currentLocation;
		private volatile boolean isRunning;
		public UpdateSitesTask(Location currentLocation){
			this.currentLocation = currentLocation;
		}
		@Override
		public void run() {
			isRunning = true;
			List<Site> result = null;
			try {				
				if (Config.isDemoMode(context)) {
					CacheService mCacheService = new CacheService(
							context, CacheService.createDBNameFromUser(Config.getActiveServer(context), Config.getActiveUser(context)));
					result = mCacheService.getAllSite(Config.getCurrentProjectId(LocationService.this));
				}else{
					result = NetworkUtils.getAllSite(LocationService.this, Config.getCurrentProjectId(LocationService.this));
				}

			} catch (Exception e) {
				e.printStackTrace();
				result = null;
			}
			
			GlobalState.setSites(result);
			
			Site mBestSite = UIUtils.getBestSite(GlobalState.getSites(), currentLocation, context);
			GlobalState.setBestSite(mBestSite);

            EventBus.getDefault().post(new UpdateSites());
			Intent intent = new Intent();
			intent.setAction(UPDATE_SITE_ACTION);
			sendBroadcast(intent);
			Ln.d("has send broadcast update site");
			isRunning = false;
		}
		
	}
	
	public void onLocationChanged(Intent intent) {
		if(!intent.hasExtra(LOCATION_DATA)){
			return;
		}
		
		LocationInfo locInfo = (LocationInfo) intent.getSerializableExtra(LOCATION_DATA);
		
		Ln.i("Location changed");
		Location loc = new Location(locInfo.lastProvider);
		loc.setLatitude(locInfo.lastLat);
		loc.setLongitude(locInfo.lastLong);
		loc.setAccuracy(locInfo.lastAccuracy);
		GlobalState.setCurrentUserLocation(loc);
		
		if(null != updateSitesTask && updateSitesTask.isRunning){
			return;
		}
		
		updateSitesTask = new UpdateSitesTask(loc);
		performOnBackgroundThread(updateSitesTask);
	}

    public void updateSite(){
        if(null != updateSitesTask && updateSitesTask.isRunning || GlobalState.getCurrentUserLocation() == null){
            return;
        }

        updateSitesTask = new UpdateSitesTask(GlobalState.getCurrentUserLocation());
        performOnBackgroundThread(updateSitesTask);
    }
	
}
