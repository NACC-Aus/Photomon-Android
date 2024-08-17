package com.appiphany.nacc.screens;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewConfiguration;

import androidx.multidex.MultiDex;

import com.appiphany.nacc.BuildConfig;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.Ln;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GlobalState extends Application {
    private static GlobalState instance;
    private static ThreadPoolExecutor mThreadPool;
    private static List<Site> sSites;
    private static BlockingQueue<Runnable> mQueues = new LinkedBlockingQueue<>();
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 15;
    private static final int KEEP_ALIVE_TIME = 1000;
    private static Site mBestSite;
    private static Location currentUserLocation;

    private Timer mActivityTransitionTimer;
    private TimerTask mActivityTransitionTimerTask;
    public boolean wasInBackground;
    private static final long MAX_ACTIVITY_TRANSITION_TIME_MS = 3000;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            @SuppressLint("SoonBlockedPrivateApi") Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            menuKeyField.setAccessible(true);
            menuKeyField.setBoolean(config, false);
        } catch (Exception ex) {
            // Ignore
        }

        initThreadPool();
        if(BuildConfig.DEBUG){
        	Ln.getConfig().setLoggingLevel(Log.VERBOSE);
        }else{
        	Ln.getConfig().setLoggingLevel(Log.ERROR);
        }
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
        if(sSites != null) {
            return Collections.unmodifiableList(new ArrayList<>(sSites));
        }

        return null;
    }

    public static List<Site> getProjectSites(String projectId) {
        List<Site> sites = getSites();
        if(sites == null || sites.isEmpty() || TextUtils.isEmpty(projectId)) {
            return null;
        }

        List<Site> result = new ArrayList<>();
        for (Site site: sites) {
            if(projectId.equals(site.getProjectId())) {
                result.add(site);
            }
        }

        return result;
    }

    public static Site getSite(String siteId){
        List<Site> sites = getSites();
        if(sites == null || TextUtils.isEmpty(siteId)) {
            return null;
        }

        for (Site site: sites) {
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
            GlobalState.sSites =  new ArrayList<>(mSites);
        }

    }
    
    public static void tearDown(){
    	 synchronized (mLock) {
             GlobalState.sSites = null;
             GlobalState.mBestSite = null;
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
