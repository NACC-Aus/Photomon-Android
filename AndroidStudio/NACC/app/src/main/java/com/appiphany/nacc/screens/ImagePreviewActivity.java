package com.appiphany.nacc.screens;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.DialogUtil;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.NetworkUtils;
import com.appiphany.nacc.utils.UIUtils;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibrary;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibraryConstants;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class ImagePreviewActivity extends BaseActivity implements OnClickListener {
    private static final int ADD_NOTE_CODE = 111;
    private static final int SHOW_DIALOG = 222;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 second

    private ImageView mPreviewView;
    private TextView mDirectionTextView;
    private TextView mDescriptionTextView;
    private RelativeLayout mInfoLayout;

    private Button mBtNote;

    private boolean isFullscreen = false;
    private String mFilename;
    private String mPhotoName;
    private String mPhotoId;
    private String mSiteId = null;
    private String mDirection = "North";
    private LocationManager locationMgr;
    private Location mUserLocation;
    private String mNoteString = "";

    private NetworkListener mReceiver = new NetworkListener(this);
    private GetSitesTask mTask;

    public static final String ORIENTATION_EXTRA = "orientation_extra";

    //  this variable use to check alert dialog is showed
    private boolean isShowed;

    private Site mBestSite;
    private boolean hasRegisterReceiver;
    private UpdateSiteReceiver updateSiteReceiver;
    private CacheService cacheService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setContentView(R.layout.activity_picture_preview_layout);

        mPreviewView = (ImageView) findViewById(R.id.image_preview_view);
        mDirectionTextView = (TextView) findViewById(R.id.image_name_text);
        mDescriptionTextView = (TextView) findViewById(R.id.image_description_text);
        mInfoLayout = (RelativeLayout) findViewById(R.id.image_info_panel);
        mBtNote = (Button) findViewById(R.id.add_note_btn);

        mPreviewView.setOnClickListener(this);
        mBtNote.setOnClickListener(this);

        LocationLibrary.forceLocationUpdate(this);
        LocationLibrary.startAlarmAndListener(this);

        Intent intent = getIntent();
        if (intent != null) {
            mFilename = intent.getStringExtra(BackgroundService.PHOTO_PATH_EXTRA);
            mPhotoId = intent.getStringExtra(BackgroundService.PHOTO_ID_EXTRA);
            mDirection = intent.getStringExtra(BackgroundService.DIRECTION_EXTRA);
            mBestSite = (Site) intent.getSerializableExtra(BackgroundService.BEST_SITE);
            mUserLocation = GlobalState.getCurrentUserLocation();

            mDirectionTextView.setText(mDirection);
            mDescriptionTextView.setText(UIUtils.getPhotoDate(new Date()));
            if (mFilename != null && mPhotoId != null) {
            	File file = new File(mFilename);
            	ImageLoader.getInstance().displayImage("file:///" + file.getAbsolutePath(),
						mPreviewView, GeneralUtil.getScaleDisplayOption());
            } else {
                finish();
            }
        }

        initActionBar();

        if(!Config.isDemoMode(this)) {
            mBestSite = GlobalState.getBestSite();

            if (mBestSite != null ) {
                getSupportActionBar().setTitle(mBestSite.getName());
                mPhotoName = mBestSite.getName();
                mSiteId = mBestSite.getSiteId();
            }
        }
    }

    private CacheService getCacheService(){
    	if(cacheService == null){
    		cacheService = CacheService.getInstance(this,
                    CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
    	}

    	return cacheService;
    }

    @SuppressWarnings("ConstantConditions")
    private void initActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("Selecting site...");
        setSupportProgressBarIndeterminate(true);
        setSupportProgressBarIndeterminateVisibility(false);
    }

    private void handleShowImageView() {
        if (mInfoLayout != null) {
            RelativeLayout.LayoutParams lp = (LayoutParams) mPreviewView.getLayoutParams();
            if (isFullscreen) {
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

                mInfoLayout.setVisibility(View.GONE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getSupportActionBar().hide();
            } else {
                mInfoLayout.setVisibility(View.VISIBLE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getSupportActionBar().show();
            }

            mPreviewView.setLayoutParams(lp);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter(BackgroundService.CONNECTED_ACTION);
        localBroadcastMgr.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unRegisterLocationReceiver();
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(this);
        localBroadcastMgr.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerLocationReceiver();

        if (Config.isDemoMode(this)) {
            if (getCacheService() != null) {
                List<Site> sites = getCacheService().getAllSite(Config.getCurrentProjectId(getActivityContext()));
                if (sites != null && sites.size() > 0) {
                    updateSiteName(sites);
                } else {
                	showDialogHandler.sendEmptyMessage(SHOW_DIALOG);
                	Ln.d("send message show dialog");
                }
            }
        } else {
            doGetSites();
        }
    }

    private DialogHandler showDialogHandler = new DialogHandler(this);

    private static class DialogHandler extends Handler{
    	private final WeakReference<ImagePreviewActivity> mService;

    	DialogHandler(ImagePreviewActivity service) {
            mService = new WeakReference<ImagePreviewActivity>(service);
        }

    	public void handleMessage(Message msg) {
    		ImagePreviewActivity service = mService.get();
            if (service == null) {
                 return;
            }

    		if (msg.what == SHOW_DIALOG) {
    			Ln.d("handle show dialog message");
    			if (!service.isFinishing()) {
    				CacheService cacheService = CacheService.getInstance(service,
    	                    CacheService.createDBNameFromUser(Config.getActiveServer(service), Config.getActiveUser(service)));
    	            if (cacheService != null) {
    	            	if(service.mUserLocation == null){
    	            		service.mUserLocation = GlobalState.getCurrentUserLocation();
    	            	}

    	            	if (service.mUserLocation == null) {
	    	            	if (!NetworkUtils.isNetworkOnline(service)) {
	    	            		DialogUtil.showSettingsAlert(service, R.string.wifi_setting_title,
	    	                            R.string.wifi_setting_message, Settings.ACTION_WIFI_SETTINGS);
	    	            	} else if (!NetworkUtils.isGPSAvailable(service)) {
	    	            		DialogUtil.showSettingsAlert(service, R.string.gps_setting_title,
	    	            				R.string.gps_setting_message, Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	    	            	}
    	            	} else {
    	            		service.showAddSiteDialog(service, cacheService, service.mUserLocation);
    	            	}
    	            }
    			}
    		}
    	};
    }

    Dialog dialog;
    // show dialog to create new site for local
    private void showAddSiteDialog(Activity activity, final CacheService cacheService, final Location location) {
    	Ln.d("call showAddSiteDialog");

    	if(dialog == null){
    		dialog = new Dialog(activity);
    	}

    	if(dialog.isShowing()){
    		return;
    	}

    	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    	UIUtils.showKeyBoard(ImagePreviewActivity.this);
        dialog.requestWindowFeature((int) Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.add_new_site);
        int width = getResources().getDimensionPixelSize(R.dimen.dialog_width);
        dialog.getWindow().setLayout(width, LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.CENTER);

        final EditText etSiteName = (EditText) dialog.findViewById(R.id.site_name_edittext);
        etSiteName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        Button btOk = (Button) dialog.findViewById(R.id.ok_button);
        btOk.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String siteName = etSiteName.getText().toString().trim();
                if (siteName.length() == 0) {
                    Toast.makeText(ImagePreviewActivity.this, R.string.input_site_name, Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    // check site name is exist?
                    if (cacheService.checkSiteNameExist(siteName)) {
                        Toast.makeText(ImagePreviewActivity.this, R.string.site_exist, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // save new site to database
                    if (location == null) {
                    	Toast.makeText(ImagePreviewActivity.this, "Waiting for get location!", Toast.LENGTH_SHORT).show();
                    	return;
                    }
                    double longitude;
                    double latitude;
                    // save for first site
                    if (mBestSite == null) {
                    	longitude = location.getLongitude();
                    	latitude = location.getLatitude();
                    } else {
                    	longitude = mBestSite.getLng();
                    	latitude = mBestSite.getLat();
                    }
                    Site site = new Site(UUID.randomUUID().toString(), siteName, latitude, longitude, Config.getCurrentProjectId(getActivityContext()));
                    if (cacheService.insertSite(site)) {
                    	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                        dialog.dismiss();
                        dialog = null;
                        UIUtils.hideKeyboard(ImagePreviewActivity.this, etSiteName);
                    } else {
                        UIUtils.buildAlertDialog(ImagePreviewActivity.this, R.string.dialog_title,
                                R.string.insert_site_fail, true);
                        return;
                    }

                    getSupportActionBar().setTitle(site.getName());
                    mPhotoName = site.getName();
                    mSiteId = site.getSiteId();
                }
            }
        });

        dialog.show();
    }

    private void doGetSites() {
        if (mSiteId == null) {
            setSupportProgressBarIndeterminateVisibility(true);
            updateSiteName(GlobalState.getSites());
            if (mUserLocation == null) {
            	mUserLocation = GlobalState.getCurrentUserLocation();

	        	if (mUserLocation == null) {
	        		 getLocation();
	        	}
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    public void getLocation() {
        try {
            locationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);

            // getting GPS status
            boolean isGPSEnabled = locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            boolean isNetworkEnabled = locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled && !isShowed) {
                isShowed = true;
                // no network provider is enabled
                DialogUtil.showSettingsAlert(this, R.string.gps_setting_title, R.string.gps_setting_message,
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                return;
            } else {
                isShowed = false;
                if (isNetworkEnabled) {
                    locationMgr.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                    if (locationMgr != null) {
                        mUserLocation = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (mUserLocation == null) {
                        locationMgr.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                        Ln.d("GPS Enabled");
                        if (locationMgr != null) {
                            mUserLocation = locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        }
                    }
                }

                if (GlobalState.getSites() != null)
                updateSiteName(GlobalState.getSites());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.preview_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private MenuItem clickedItem = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            discardChange();
            finish();
            break;
        case R.id.done_menu:
            goBackToMain(mFilename);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void discardChange() {
        if (mFilename != null) {
            File file = new File(mFilename);
            file.delete();
        }
    }

    @Override
    public void onBackPressed() {
        discardChange();
        super.onBackPressed();

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.image_preview_view) {
            isFullscreen = !isFullscreen;
            handleShowImageView();
        } else if (v.getId() == R.id.add_note_btn) {
            Intent addNoteIntent = new Intent(this, AddNoteActivity.class);
            addNoteIntent.setAction(AddNoteActivity.ADD_NOTE);
            addNoteIntent.putExtra(AddNoteActivity.NOTE, mNoteString);
            startActivityForResult(addNoteIntent, ADD_NOTE_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_NOTE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    mNoteString = data.getStringExtra(AddNoteActivity.NOTE);
                }
            }
        }
    }

    // save photo taken to database
    // set guide photo if isMakeGuide variable is true
    // back to main screen
    private void goBackToMain(String filename) {
        if (clickedItem != null) {
            clickedItem.setEnabled(true);
        }
        
        if(mPhotoName == null){
        	getLocation();
        	updateSiteName(GlobalState.getSites());
        }
        
        if (mPhotoName == null) {
            UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.no_photo_name_error, false).show();
            return;
        }
        if (filename != null) {
        	updateSiteName(GlobalState.getSites());
        	final int version = android.os.Build.VERSION.SDK_INT;
        	float opacity;
            if(version < android.os.Build.VERSION_CODES.HONEYCOMB){
            	opacity = 255;
            }else{
            	opacity = 10.0f;
            }
            
            Photo photoModel = new Photo(mPhotoId, "", filename, mPhotoName, mSiteId, mDirection,
                    UPLOAD_STATE.NOT_UPLOAD,
                    new Date(), mNoteString, opacity, Config.getCurrentProjectId(getActivityContext()));            
            CacheService cacheService = CacheService.getInstance(this,
                    CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
            if (cacheService.addNewPhoto(photoModel)) {
                Intent backToMainIntent = new Intent(this, MainScreenActivity.class);
                backToMainIntent.setAction(MainScreenActivity.REQUEST_UPLOAD);
                backToMainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                backToMainIntent.putExtra(BackgroundService.PHOTO_DATA_EXTRA, photoModel);
                startActivity(backToMainIntent);

            } else {
                UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.common_error, false).show();
            }

        }
    }
    
    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
        	Ln.d("[ImagePreviewAct] location change = " + location.getLatitude());
        	mUserLocation = location;
        }
    };

    private void updateSiteName(List<Site> sites) {
		if (sites == null && mBestSite == null) {
			if (mTask != null) {
				mTask.cancel(true);
			}

			mTask = new GetSitesTask(this);
			mTask.execute(Config.getActiveServer(this));
			return;
		}
		
        setSupportProgressBarIndeterminateVisibility(false);
        
    	if(mUserLocation == null){
    		mUserLocation = GlobalState.getCurrentUserLocation();
    	}
    	
        if (mUserLocation != null) {
        	mBestSite = GlobalState.getBestSite();
        	if (mBestSite == null) {
        		mBestSite = UIUtils.getBestSite(sites, mUserLocation, this);
        	}
        	            	
            if (mBestSite != null ) {
                getSupportActionBar().setTitle(mBestSite.getName());
                mPhotoName = mBestSite.getName();
                mSiteId = mBestSite.getSiteId();
            } 
        }
    }

    private static class GetSitesTask extends AsyncTask<String, Void, List<Site>> {
        private WeakReference<ImagePreviewActivity> mContext;

        public GetSitesTask(ImagePreviewActivity context) {
            mContext = new WeakReference<ImagePreviewActivity>(context);
        }

        @Override
        protected List<Site> doInBackground(String... params) {
            return NetworkUtils.getAllSite(mContext.get(), Config.getCurrentProjectId(mContext.get()));
        }

        @Override
        protected void onPostExecute(List<Site> result) {
            // store site to use for no wireless case
            GlobalState.setSites(result);

            if (mContext != null) {
                ImagePreviewActivity mActivity = mContext.get();
                if (mActivity != null) {
                    mActivity.updateSiteName(GlobalState.getSites());
                }
            }
        }
    }

    private static class NetworkListener extends BroadcastReceiver {
        private WeakReference<ImagePreviewActivity> mContext;

        public NetworkListener(ImagePreviewActivity context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction() != null && intent.getAction().equals(BackgroundService.CONNECTED_ACTION)) {
                    ImagePreviewActivity activity = mContext.get();
                    if (activity != null) {
                        activity.doGetSites();
                    }
                }
            }
        }
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
	private class UpdateSiteReceiver extends BroadcastReceiver {
		private WeakReference<ImagePreviewActivity> mContext;

		public UpdateSiteReceiver(ImagePreviewActivity context) {
			this.mContext = new WeakReference<>(context);
			Ln.d("create receiver");
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			Ln.d("ImagePreviewActivity UpdateSiteReceiver onReceive");
			if (intent == null || mContext == null || mContext.get() == null) {
				return;
			}

			// update site
			if (LocationService.UPDATE_SITE_ACTION.equalsIgnoreCase(intent
					.getAction())) {
				mBestSite = GlobalState.getBestSite();
				
				if (Config.isDemoMode(getActivityContext())) {
					if (getCacheService() != null) {
						List<Site> sites = getCacheService().getAllSite(Config.getCurrentProjectId(getActivityContext()));
						if (sites != null && sites.size() > 0) {
							updateSiteName(sites);
						} else {
							showDialogHandler.sendEmptyMessage(SHOW_DIALOG);
							Ln.d("send message show dialog");
						}
					}
				} else {
					updateSiteName(GlobalState.getSites());
				}
			}
		}
	}
	
	private void registerLocationReceiver() {
		if(!hasRegisterReceiver){	        
	        final IntentFilter lftIntentFilter = new IntentFilter(LocationLibraryConstants.getLocationChangedPeriodicBroadcastAction());
	        registerReceiver(lftBroadcastReceiver, lftIntentFilter);
	        
	    	updateSiteReceiver = new UpdateSiteReceiver(this);
	        IntentFilter intentFilter = new IntentFilter(LocationService.UPDATE_SITE_ACTION);
	        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
	        registerReceiver(updateSiteReceiver, intentFilter);
	        hasRegisterReceiver = true;
        }
	}
	
	private void unRegisterLocationReceiver() {
		if(hasRegisterReceiver){
        	unregisterReceiver(updateSiteReceiver);
        	unregisterReceiver(lftBroadcastReceiver);
        	hasRegisterReceiver = false;
        }
	}
	
	private final BroadcastReceiver lftBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// extract the location info in the broadcast
			Intent locationIntent = new Intent(ImagePreviewActivity.this, LocationService.class);
			locationIntent.addCategory(LocationService.SERVICE_TAG);
			locationIntent.setAction(LocationService.LOCATION_CHANGED);
			locationIntent.putExtra(LocationService.LOCATION_DATA, intent.getSerializableExtra(LocationLibraryConstants.LOCATION_BROADCAST_EXTRA_LOCATIONINFO));
			startService(locationIntent);
		}
	};
}
