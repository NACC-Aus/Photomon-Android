package com.appiphany.nacc.screens;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewConfiguration;

import com.appiphany.nacc.BuildConfig;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.jobs.AppJobCreator;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.Ln;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.evernote.android.job.JobManager;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibrary;
import com.nostra13.universalimageloader.cache.disc.impl.LimitedAgeDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

import io.fabric.sdk.android.Fabric;

public class GlobalState extends Application {
    private static GlobalState instance;
    private static ThreadPoolExecutor mThreadPool;
    private static List<Site> sSites;
    private static BlockingQueue<Runnable> mQueues = new LinkedBlockingQueue<Runnable>();
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 15;
    private static final int KEEP_ALIVE_TIME = 1000;

    private static Context context;
    private static Site mBestSite;
    private static Location currentUserLocation;
    private static List<Photo> guidePhotos;

    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    public boolean wasInBackground;
    private static final long MAX_ACTIVITY_TRANSITION_TIME_MS = 3000;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(this, new Crashlytics.Builder().core(core).build());
        
        context = getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            try {
                ViewConfiguration config = ViewConfiguration.get(this);
                Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
                if (menuKeyField != null) {
                    menuKeyField.setAccessible(true);
                    menuKeyField.setBoolean(config, false);
                }
            } catch (Exception ex) {
                // Ignore
            }
        }

        JobManager.create(this).addJobCreator(new AppJobCreator());
        initLocation();
        initThreadPool();
        initImageLoader();
        if(BuildConfig.DEBUG){
        	Ln.getConfig().setLoggingLevel(Log.VERBOSE);
        }else{
        	Ln.getConfig().setLoggingLevel(Log.ERROR);
        }
    }
    
    public void initLocation(){
		try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

			LocationLibrary.showDebugOutput(GeneralUtil.isDebugMode());			
            LocationLibrary.initialiseLibrary(getBaseContext(), 3 * 60 * 1000, 5 * 60 * 1000, "com.appiphany.nacc");
        }
        catch (Exception ex) {
            Ln.d("UnsupportedOperationException thrown - the device doesn't have any location providers");
        }
	}
    
    public static Context getAppContext() {
        return context;
    }

    private void initImageLoader() {
        File cacheDir = StorageUtils.getCacheDirectory(getApplicationContext());
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(true)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565) // default
                .displayer(new SimpleBitmapDisplayer()).considerExifParams(true)
                .build();
        ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .memoryCacheExtraOptions(Config.CONFIG_WIDTH, Config.CONFIG_HEIGHT)
                .taskExecutor(getThreadPool())
                .taskExecutorForCachedImages(getThreadPool())
                .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024)) // default
                .diskCache(new LimitedAgeDiskCache(cacheDir, 3600 * 24 * 7))
                .diskCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
                .imageDownloader(new BaseImageDownloader(getApplicationContext())) // default
                .defaultDisplayImageOptions(options) // default
                .denyCacheImageMultipleSizesInMemory();
        
        if(GeneralUtil.isDebugMode()){
        	builder.writeDebugLogs();
        }
        
        ImageLoaderConfiguration config = builder.build();
        ImageLoader.getInstance().init(config);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        CacheService cacheService = new CacheService(this,
                CacheService.createDBNameFromUser(Config.getActiveServer(this.getApplicationContext()),
                        Config.getActiveUser(this.getApplicationContext())));
        cacheService.close();
    }

    private void initThreadPool() {
        setThreadPool(new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                mQueues));
    }

    public static ThreadPoolExecutor getThreadPool() {
        return mThreadPool;
    }

    public static void setThreadPool(ThreadPoolExecutor mThreadPool) {
        GlobalState.mThreadPool = mThreadPool;
    }

    public static List<Site> getSites() {
        return sSites;
    }

    public static List<Site> getProjectSites(String projectId) {
        if(sSites == null || sSites.isEmpty() || TextUtils.isEmpty(projectId)) {
            return null;
        }

        List<Site> result = new ArrayList<>();
        for (Site site: sSites) {
            if(projectId.equals(site.getProjectId())) {
                result.add(site);
            }
        }

        return result;
    }

    public static Site getSite(String siteId){
        if(sSites == null || TextUtils.isEmpty(siteId)) {
            return null;
        }

        for (Site site: sSites) {
            if(siteId.equals(site.getSiteId())) {
                return site;
            }
        }

        return null;
    }

    private static final Object mLock = new Object();

    public static void setSites(List<Site> mSites) {
    	if(mSites == null){
    		return;
    	}
    	
        synchronized (mLock) {
            GlobalState.sSites = mSites;
        }

    }
    
    public static void tearDown(){
    	 synchronized (mLock) {
             GlobalState.sSites = null;
             GlobalState.mBestSite = null;
             GlobalState.guidePhotos = null;
             GlobalState.currentUserLocation = null;
         }
    }
    
    public static Site getBestSite(){
    	return mBestSite;
    }
    
    public static void setBestSite(Site mBestSite){
    	synchronized (mLock) {
            GlobalState.mBestSite = mBestSite;
        }
    }

    public static void clearBestSite(){
        mBestSite = null;
    }

    public static void clearSites(){
        sSites = null;
    }

	public static Location getCurrentUserLocation() {
		return currentUserLocation;
	}

	public static void setCurrentUserLocation(Location currentUserLocation) {
		if(currentUserLocation == null){
			return;
		}
		
		synchronized (mLock) {
			GlobalState.currentUserLocation = currentUserLocation;
        }		
	}

	public static List<Photo> getGuidePhotos() {
		return guidePhotos;
	}

	public static boolean setGuidePhotos(List<Photo> guidePhotos) {
		if(guidePhotos != null && guidePhotos.size() == 0){
			return false;
		}
		
		synchronized (mLock) {
			GlobalState.guidePhotos = guidePhotos;
		}
		
		return true;
	}

    public void startActivityTransitionTimer() {
        mActivityTransitionTimer = new Timer();
        mActivityTransitionTimerTask = new TimerTask() {
            public void run() {
                wasInBackground = true;
            }
        };

        mActivityTransitionTimer.schedule(mActivityTransitionTimerTask,
                MAX_ACTIVITY_TRANSITION_TIME_MS);
    }

    public void stopActivityTransitionTimer() {
        if (mActivityTransitionTimerTask != null) {
            mActivityTransitionTimerTask.cancel();
        }

        if (mActivityTransitionTimer != null) {
            mActivityTransitionTimer.cancel();
        }

        wasInBackground = false;
    }

    public boolean wasInBackground(){
        return wasInBackground;
    }

    public static GlobalState getInstance(){
        return instance;
    }
}
