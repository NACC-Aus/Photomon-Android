package com.appiphany.nacc.screens;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.DialogUtil;
import com.appiphany.nacc.utils.Intents;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.NetworkUtils;
import com.appiphany.nacc.utils.UIUtils;
import com.bumptech.glide.Glide;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class ImagePreviewActivity extends BaseActivity implements OnClickListener, SeekBar.OnSeekBarChangeListener {
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
    private ImageView mGuideImageView;

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
    private String mGuidePhotoPath;
    private SeekBar seekOpacityCamera;

    private NetworkListener mReceiver = new NetworkListener(this);
    private GetSitesTask mTask;

    public static final String ORIENTATION_EXTRA = "orientation_extra";

    //  this variable use to check alert dialog is showed
    private boolean isShowed;

    private Site mBestSite;
    private boolean hasRegisterReceiver;
    private UpdateSiteReceiver updateSiteReceiver;
    private CacheService cacheService;
    private Handler mHandler = new Handler();
    private float currentAlpha;
    private Site selectedSite;

    private boolean hasCancel;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setContentView(R.layout.activity_picture_preview_layout);
        setLayoutInsets(R.id.rootLayout);

        mPreviewView = findViewById(R.id.image_preview_view);
        mDirectionTextView = findViewById(R.id.image_name_text);
        mDescriptionTextView = findViewById(R.id.image_description_text);
        mInfoLayout = findViewById(R.id.image_info_panel);
        mBtNote = findViewById(R.id.add_note_btn);
        mGuideImageView = findViewById(R.id.guide_image_view);
        seekOpacityCamera = findViewById(R.id.seekOpacityCamera);

        mPreviewView.setOnClickListener(this);
        mBtNote.setOnClickListener(this);

        hasCancel = false;

        Intent intent = getIntent();
        if (intent != null) {
            mFilename = intent.getStringExtra(BackgroundService.PHOTO_PATH_EXTRA);
            mPhotoId = intent.getStringExtra(BackgroundService.PHOTO_ID_EXTRA);
            mDirection = intent.getStringExtra(BackgroundService.DIRECTION_EXTRA);
            mBestSite = (Site) intent.getSerializableExtra(BackgroundService.BEST_SITE);
            mUserLocation = GlobalState.getCurrentUserLocation();
            mGuidePhotoPath = intent.getStringExtra(BackgroundService.GUIDE_PHOTO);
            selectedSite = (Site) intent.getSerializableExtra(Intents.SELECTED_SITE);
            mDirectionTextView.setText(mDirection);
            mDescriptionTextView.setText(UIUtils.getPhotoDate(new Date()));
            if (mFilename != null && mPhotoId != null) {
                File file = new File(mFilename);
                Glide.with(this).asBitmap()
                        .load(file)
                        .into(mPreviewView);
            } else {
                finish();
            }
        }

        initActionBar();

        if (!Config.isDemoMode(this)) {
            mBestSite = GlobalState.getBestSite();
            if (selectedSite != null) {
                getSupportActionBar().setTitle(selectedSite.getName());
                mPhotoName = selectedSite.getName();
                mSiteId = selectedSite.getSiteId();
            } else if (mBestSite != null) {
                getSupportActionBar().setTitle(mBestSite.getName());
                mPhotoName = mBestSite.getName();
                mSiteId = mBestSite.getSiteId();
            }
        }

        seekOpacityCamera.setIndeterminate(false);
        seekOpacityCamera.setOnSeekBarChangeListener(this);

        seekOpacityCamera.setMax(10);
        seekOpacityCamera.setProgress(0);
        mGuideImageView.setAlpha(0f);
        currentAlpha = 0f;

        if (TextUtils.isEmpty(mGuidePhotoPath)) {
            seekOpacityCamera.setVisibility(View.INVISIBLE);
        } else {
            displayGuidePhoto(true);
        }
    }

    private CacheService getCacheService() {
        if (cacheService == null) {
            cacheService = CacheService.getInstance(this,
                    CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
        }

        return cacheService;
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
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

    @SuppressWarnings("deprecation")
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                mGuideImageView.setAlpha(progress);
                currentAlpha = progress;
            } else {
                mGuideImageView.setAlpha(progress / 10.0f);
                currentAlpha = progress / 10.0f;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private static class DialogHandler extends Handler {
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
                        if (service.mUserLocation == null) {
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
        }
    }

    Dialog dialog;

    // show dialog to create new site for local
    private void showAddSiteDialog(final Activity activity, final CacheService cacheService, final Location location) {
        Ln.d("call showAddSiteDialog");
        if (hasCancel || activity == null || activity.isFinishing()) {
            return;
        }

        View contentView = View.inflate(getActivityContext(), R.layout.add_new_site, null);
        final EditText etSiteName = contentView.findViewById(R.id.site_name_edittext);
        if (dialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivityContext());
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            builder.setCancelable(true);
            builder.setView(contentView);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    String siteName = etSiteName.getText().toString().trim();
                    if (siteName.length() == 0) {
                        Toast.makeText(ImagePreviewActivity.this, R.string.input_site_name, Toast.LENGTH_SHORT).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showAddSiteDialog(activity, cacheService, location);
                            }
                        }, 500);
                        return;
                    } else {
                        // check site name is exist?
                        if (Config.isDemoMode(getActivityContext()) && cacheService.checkSiteNameExist(siteName)) {
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

                        if (Config.isDemoMode(getActivityContext())) {
                            Site site = new Site(UUID.randomUUID().toString(), siteName, latitude, longitude, Config.getCurrentProjectId(getActivityContext()));
                            if (cacheService.insertSite(site)) {
                                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                                dialog.dismiss();
                                UIUtils.hideKeyboard(ImagePreviewActivity.this, etSiteName);
                            } else {
                                UIUtils.buildAlertDialog(ImagePreviewActivity.this, R.string.dialog_title,
                                        R.string.insert_site_fail, true);
                                return;
                            }

                            getSupportActionBar().setTitle(site.getName());
                            mPhotoName = site.getName();
                            mSiteId = site.getSiteId();
                        } else {
                            dialog.dismiss();
                            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                            UIUtils.hideKeyboard(ImagePreviewActivity.this, etSiteName);
                            addNewSite(siteName, String.valueOf(latitude), String.valueOf(longitude));
                        }
                    }

                    etSiteName.setText("");
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    UIUtils.hideKeyboard(getActivityContext(), etSiteName);
                    etSiteName.setText("");
                    hasCancel = true;
                }
            });

            dialog = builder.create();
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(true);
            int width = getResources().getDimensionPixelSize(R.dimen.dialog_width);
            dialog.getWindow().setLayout(width, RelativeLayout.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }

        if (dialog.isShowing()) {
            return;
        }

        UIUtils.showKeyBoard(ImagePreviewActivity.this);

        etSiteName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        dialog.show();
    }

    private void addNewSite(String name, String lat, String lng) {
        new AddSiteTask(this).execute(name, lat, lng);
    }

    private static class AddSiteTask extends AsyncTask<String, Void, Site> {
        private WeakReference<ImagePreviewActivity> weakReference;
        private String currentProjectId;

        public AddSiteTask(ImagePreviewActivity context) {
            this.weakReference = new WeakReference<>(context);
            currentProjectId = Config.getCurrentProjectId(context);
        }

        @Override
        protected Site doInBackground(String... params) {
            String name = params[0];
            String lat = params[1];
            String lng = params[2];
            return NetworkUtils.addNewSite(weakReference.get(), currentProjectId, name, lat, lng);
        }

        @Override
        protected void onPostExecute(Site site) {
            if (weakReference.get() == null || weakReference.get().getCacheService() == null) {
                return;
            }

            if (site == null) {
                Toast.makeText(weakReference.get(), R.string.msg_error_add_site, Toast.LENGTH_SHORT).show();
                return;
            }

            weakReference.get().reloadCurrentSite();
            if (!weakReference.get().getCacheService().insertSite(site)) {
                UIUtils.buildAlertDialog(weakReference.get(), R.string.dialog_title,
                        R.string.insert_site_fail, true);
                return;
            }

            weakReference.get().getSupportActionBar().setTitle(site.getName());
            weakReference.get().mPhotoName = site.getName();
            weakReference.get().mSiteId = site.getSiteId();
        }
    }

    private void reloadCurrentSite() {
        Intent locationIntent = new Intent(this, LocationService.class);
        locationIntent.addCategory(LocationService.SERVICE_TAG);
        locationIntent.setAction(LocationService.REFRESH_SITE);
        startService(locationIntent);
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
    private void goBackToMain(final String filename) {
        if (clickedItem != null) {
            clickedItem.setEnabled(true);
        }

        if (mPhotoName == null) {
            getLocation();
            updateSiteName(GlobalState.getSites());
        }

        if (mPhotoName == null) {
            UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.no_photo_name_error, false).show();
            return;
        }

        savePhoto(filename);
    }

    private void savePhoto(String filename) {
        if (filename != null) {
            updateSiteName(GlobalState.getSites());
            final int version = android.os.Build.VERSION.SDK_INT;
            float opacity;
            if (version < android.os.Build.VERSION_CODES.HONEYCOMB) {
                opacity = 255;
            } else {
                opacity = 10.0f;
            }

            String siteId;
            if (selectedSite != null && !TextUtils.isEmpty(selectedSite.getSiteId())) {
                siteId = selectedSite.getSiteId();
            } else {
                siteId = mSiteId;
            }

            Photo photoModel = new Photo(mPhotoId, "", filename, mPhotoName, siteId, mDirection,
                    UPLOAD_STATE.NOT_UPLOAD,
                    new Date(), mNoteString, opacity, Config.getCurrentProjectId(getActivityContext()));
            CacheService cacheService = CacheService.getInstance(this,
                    CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
            if (cacheService.addNewPhoto(photoModel)) {
                setResult(RESULT_OK);
                finish();

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

        if (sites != null && sites.isEmpty()) {
            CacheService cacheService = CacheService.getInstance(getActivityContext(),
                    CacheService.createDBNameFromUser(Config.getActiveServer(getActivityContext()), Config.getActiveUser(getActivityContext())));
            showAddSiteDialog(getActivityContext(), cacheService, mUserLocation);
            return;
        }

        setSupportProgressBarIndeterminateVisibility(false);

        if (mUserLocation == null) {
            mUserLocation = GlobalState.getCurrentUserLocation();
        }

        if (mUserLocation != null) {
            mBestSite = GlobalState.getBestSite();
            if (mBestSite == null) {
                mBestSite = UIUtils.getBestSite(sites, mUserLocation, this);
            }

            if (selectedSite != null) {
                getSupportActionBar().setTitle(selectedSite.getName());
                mPhotoName = selectedSite.getName();
                mSiteId = selectedSite.getSiteId();
            } else if (mBestSite != null) {
                getSupportActionBar().setTitle(mBestSite.getName());
                mPhotoName = mBestSite.getName();
                mSiteId = mBestSite.getSiteId();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void displayGuidePhoto(boolean show) {
        Ln.d("displayGuidePhoto");
        if (mGuidePhotoPath != null) {
            if (show) {
                mHandler.postDelayed(() -> {
                    if (!mGuidePhotoPath.startsWith("http")) {
                        Glide.with(ImagePreviewActivity.this).asBitmap()
                                .load("file://" + mGuidePhotoPath)
                                .into(mGuideImageView);
                    } else {
                        Glide.with(ImagePreviewActivity.this).asBitmap()
                                .load(mGuidePhotoPath)
                                .into(mGuideImageView);
                    }
                }, 700);


                seekOpacityCamera.setVisibility(View.VISIBLE);
                seekOpacityCamera.setMax(10);
                mGuideImageView.setAlpha(currentAlpha);

                mGuideImageView.setVisibility(View.VISIBLE);
            } else {
                mGuideImageView.setVisibility(View.INVISIBLE);
                seekOpacityCamera.setVisibility(View.INVISIBLE);
            }
        }
    }


    private static class GetSitesTask extends AsyncTask<String, Void, List<Site>> {
        private WeakReference<ImagePreviewActivity> mContext;

        public GetSitesTask(ImagePreviewActivity context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected List<Site> doInBackground(String... params) {
            if (mContext.get() == null) {
                return null;
            }

            return NetworkUtils.getAllSite(mContext.get(), Config.getCurrentProjectId(mContext.get()));
        }

        @Override
        protected void onPostExecute(List<Site> result) {
            // store site to use for no wireless case
            GlobalState.setSites(result);

            if (mContext.get() != null) {
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
        if (!hasRegisterReceiver) {
            updateSiteReceiver = new UpdateSiteReceiver(this);
            IntentFilter intentFilter = new IntentFilter(LocationService.UPDATE_SITE_ACTION);
            intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
                registerReceiver(updateSiteReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(updateSiteReceiver, intentFilter);
            }
            hasRegisterReceiver = true;
        }
    }

    private void unRegisterLocationReceiver() {
        if (hasRegisterReceiver) {
            unregisterReceiver(updateSiteReceiver);
            hasRegisterReceiver = false;
        }
    }
}
