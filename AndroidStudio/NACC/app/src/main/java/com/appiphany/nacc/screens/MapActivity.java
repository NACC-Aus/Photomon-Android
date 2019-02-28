package com.appiphany.nacc.screens;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appiphany.nacc.R;
import com.appiphany.nacc.events.UpdateProject;
import com.appiphany.nacc.events.UpdateSites;
import com.appiphany.nacc.model.CacheItem;
import com.appiphany.nacc.model.GuidePhoto;
import com.appiphany.nacc.model.MarkerInfo;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Project;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.jobs.DownloadGuidesJob;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.DialogUtil;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.LocationUtil;
import com.appiphany.nacc.utils.UIUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibrary;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibraryConstants;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class MapActivity extends BaseActivity implements View.OnClickListener, OnMapReadyCallback {
    private LinearLayout mDemoView;
    private ImageView imgLocation;

    private MapActivity.UploadBroadcaseReceiver mReceiver = new MapActivity.UploadBroadcaseReceiver(this);

    public static final String REQUEST_UPLOAD = "com.appiphany.nacc.REQUEST_UPLOAD";
    public static final String ACTION_LOGOUT = "com.appiphany.nacc.ACTION_LOGOUT";
    public static final String FROM_LOGIN_EXTRA = "from_login_extra";
    public static final String DOWNLOAD_GUIDE_EXTRA = "download_guide";

    private static final int REQUEST_REVIEW = 1;
    private static final int REQUEST_REMINDER = 2;
    private static final int REQUEST_MANAGE_SITE = 3;
    private static final int REQUEST_SEND_EMAIL = 4;

    private CacheService cacheService;
    private AlertDialog dialog1;
    private AlertDialog dialog2;
    private boolean firstLoading;
    private GoogleMap map;
    private Map<Marker, Object> markers = new HashMap<>();
    private boolean canDrawMarkers = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_map);

        firstLoading = true;
        mDemoView = findViewById(R.id.demo_view);
        imgLocation = findViewById(R.id.imgLocation);
        imgLocation.setOnClickListener(this);

        // just show demo view with demo mode
        if (Config.isDemoMode(this)) {
            mDemoView.setVisibility(View.VISIBLE);
        } else {
            mDemoView.setVisibility(View.GONE);
        }

        cacheService = new CacheService(this,
                CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));

        Intent intent = getIntent();
        Ln.d("[MainScreen] on create ");

        String action = intent.getAction();
        if (action != null) {
            if (action.equals(REQUEST_UPLOAD) && !Config.isDemoMode(this)) {
                Intent intentService = new Intent(this, BackgroundService.class);
                Photo photoData = (Photo) intent.getSerializableExtra(BackgroundService.PHOTO_DATA_EXTRA);
                cacheService.updateState(photoData.getPhotoID(), CacheService.UPLOAD_STATE.UPLOADING);
                intentService.setAction(BackgroundService.UPLOAD_ACTION);
                intentService.putExtras(intent);
                if (cacheService != null) {
                    intentService.putExtra(BackgroundService.DB_NAME_EXTRA, cacheService.getMyDatabaseName());
                }
                startService(intentService);
            }

            refreshList();
        }

        if(intent.getBooleanExtra(DOWNLOAD_GUIDE_EXTRA, false) && !Config.isDemoMode(this)){
        }

        if (cacheService != null) {
            if (intent.getBooleanExtra(FROM_LOGIN_EXTRA, false)) {
                if (dialog1 == null) {
                    dialog1 = UIUtils.showAlertDialog(this, R.string.dialog_title, R.string.main_screen_alert, false, dialogListener);
                    dialog1.show();
                } else {
                    dialog1 .dismiss();
                }
                if (dialog2 == null) {
                    dialog2 = UIUtils.showAlertDialog(this, R.string.dialog_title, R.string.main_screen_alert_2, false, dialogListener);
                } else {
                    dialog2.dismiss();
                }

            }
            if (!Config.isDemoMode(this)) {
                reUpload();
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.common_error);
            builder.setNeutralButton(R.string.ok_text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            builder.show();
        }

        initActionBar();
        LocationLibrary.forceLocationUpdate(this);
        LocationLibrary.startAlarmAndListener(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!Config.isDemoMode(this)) {
            DownloadGuidesJob.scheduleJob();
        }
    }

    private void reloadGuidePhoto(){
    }

    private DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (dialog1.isShowing()) {
                dialog1.dismiss();
                dialog2.show();
                return;
            }

            if (dialog2.isShowing()) {
                dialog2.dismiss();
            }

        }
    };
    /**
     * get list photos not upload yet and send to background service to upload
     */
    private void reUpload() {
        try {
            List<Photo> notUploadedPhotos = cacheService.getNotUploadedPhotos();
            if (notUploadedPhotos != null && notUploadedPhotos.size() > 0) {
                for (Photo photo : notUploadedPhotos) {
                    Intent intentService = new Intent(this, BackgroundService.class);
                    intentService.setAction(BackgroundService.UPLOAD_ACTION);
                    intentService.putExtra(BackgroundService.PHOTO_DATA_EXTRA, photo);
                    intentService.putExtra(BackgroundService.DB_NAME_EXTRA, cacheService.getMyDatabaseName());
                    startService(intentService);

                }
            }

            ArrayList<CacheItem> cacheItems = cacheService.getCaches();
            if(!cacheItems.isEmpty()) {
                Intent intentService = new Intent(this, BackgroundService.class);
                intentService.setAction(BackgroundService.PROCESS_CACHE);
                startService(intentService);
            }

        }catch (Throwable throwable) {
            Crashlytics.logException(throwable);
            throwable.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start the location service
        startLocationService();

        LocalBroadcastManager localMgr = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter(BackgroundService.UPLOAD_FINISH_ACTION);
        intentFilter.addAction(BackgroundService.CONNECTED_ACTION);
        intentFilter.addAction(DownloadService.DOWNLOAD_GUIDE_FINISH_ACTION);
        localMgr.registerReceiver(mReceiver, intentFilter);

        final IntentFilter lftIntentFilter = new IntentFilter(LocationLibraryConstants.getLocationChangedPeriodicBroadcastAction());
        registerReceiver(lftBroadcastReceiver, lftIntentFilter);

        IntentFilter networkFilter = new IntentFilter();
        networkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Ln.d("on stop");
        LocalBroadcastManager localMgr = LocalBroadcastManager.getInstance(this);
        localMgr.unregisterReceiver(mReceiver);
        unregisterReceiver(lftBroadcastReceiver);
    }


    @SuppressWarnings("ConstantConditions")
    private void initActionBar() {
        getSupportActionBar().setIcon(R.drawable.actionbar_logo_with_space);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        final List<Project> projects = getProjects();

        if(projects == null || projects.size() == 0 || Config.isDemoMode(getActivityContext())){
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            return;
        }

        if(projects.size() == 1){
            getSupportActionBar().setTitle(projects.get(0).getName());
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            return;
        }

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);


        final List<String> items = new ArrayList<String>();
        String currentProjectId = Config.getCurrentProjectId(getActivityContext());
        int currentIndex = -1;
        for (Project project : projects) {
            items.add(project.getName());
            if(currentProjectId.equals(project.getUid())){
                currentIndex = projects.indexOf(project);
            }
        }


//        ArrayAdapter<String> adapter = new ArrayAdapter<>(getSupportActionBar().getThemedContext(),
//                android.R.layout.simple_list_item_1, items);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MapActivity.this
                , R.layout.action_bar_dropdown_item,
                android.R.id.text1, items){
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.action_bar_spinner_item, null);
                }

                TextView textView = convertView.findViewById(android.R.id.text1);
                textView.setText(items.get(getSupportActionBar().getSelectedNavigationIndex()));
                return convertView;
            }
        };

        getSupportActionBar()
                .setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {

                    @Override
                    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                        canDrawMarkers = true;
                        adapter.notifyDataSetChanged();
                        Config.setCurrentProjectId(getActivityContext(), projects.get(itemPosition).getUid());
                        refreshList();
                        if(!firstLoading) {
                            GlobalState.clearBestSite();
                            GlobalState.clearSites();
                        }

                        firstLoading = false;
                        reloadCurrentSite();
                        return false;
                    }
                });

        if(currentIndex != -1){
            Config.setCurrentProjectId(getActivityContext(), currentProjectId);
            getSupportActionBar().setSelectedNavigationItem(currentIndex);
        }
    }

    private void reloadCurrentSite() {
        Intent locationIntent = new Intent(MapActivity.this, LocationService.class);
        locationIntent.addCategory(LocationService.SERVICE_TAG);
        locationIntent.setAction(LocationService.REFRESH_SITE);
        startService(locationIntent);
    }

    private List<Project> getProjects(){
        final List<Project> ret = cacheService.getProjects();
        Collections.sort(ret, new Comparator<Project>() {
            @Override
            public int compare(Project o1, Project o2) {
                if(o1.getName() == null && o2.getName() == null) {
                    return 0;
                }

                if(o1.getName() == null) {
                    return  -1;
                }

                if(o2.getName() == null) {
                    return 1;
                }

                return o1.getName().compareTo(o2.getName());
            }
        });
        return ret;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_screen_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(6).setVisible(!Config.isDemoMode(this));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_title);
                builder.setMessage(R.string.logout_confirmed);
                builder.setCancelable(true);
                builder.setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setNeutralButton(R.string.ok_text, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        doLogout();
                    }
                });
                builder.show();

                break;
            case R.id.menu_reminder:
                Intent showReminder = new Intent(this, ReminderActivity.class);
                startActivityForResult(showReminder, REQUEST_REMINDER);
                break;
            case R.id.menu_info:
                Intent infoIntent = new Intent(this, InfoActivity.class);
                startActivity(infoIntent);
                break;
            case R.id.menu_donation:
                Intent zoozIntent = new Intent(this, DonateWebActivity.class);
                startActivity(zoozIntent);
                break;
            case R.id.menu_manage_sites:
                Intent siteIntent;
                if(Config.isDemoMode(this)) {
                    siteIntent = new Intent(this, SiteManagementActivity.class);
                }else{
                    siteIntent = new Intent(this, OnlineSitesActivity.class);
                }

                startActivityForResult(siteIntent, REQUEST_MANAGE_SITE);
                break;
            case R.id.menu_guides:
                Intent guide = new Intent(this, SitesActivity.class);
                startActivity(guide);
                break;
            case  R.id.menu_gallery:
                Intent gallery = new Intent(this, MainScreenActivity.class);
                startActivity(gallery);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("InlinedApi")
    private void doLogout() {
        // stop location service
        stopLocationService();
        stopDownloadGuideService();
        if(cacheService != null){
            cacheService.closeDatabase();
            cacheService.cleanUp();
            cacheService = null;
        }

        CacheService.tearDown();
        Intent startLoginActivityIntent = new Intent(this, LoginActivity.class);
        startLoginActivityIntent.setAction(ACTION_LOGOUT);
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB ){
            startLoginActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }else{
            startLoginActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        Config.setAccessToken(this, null);
        Config.setActiveServer(this, null);
        Config.setActiveUser(this, null);
        Config.clearCurrentProjectId(this);
        GlobalState.tearDown();
        startActivity(startLoginActivityIntent);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, BackgroundService.class);
        alarmIntent.setAction(BackgroundService.REMINDER_ACTION);
        PendingIntent alarmPendingIntent = PendingIntent.getService(this,
                4, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmMgr.cancel(alarmPendingIntent);
        finish();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Ln.d("[MainScreen] new intent ");
        if (dialog1.isShowing()) {
            dialog1.dismiss();
        }
        if (dialog2.isShowing()) {
            dialog2.dismiss();
        }

        String action = intent.getAction();
        if (action != null) {
            if (action.equals(REQUEST_UPLOAD) && !Config.isDemoMode(this)) {
                Intent intentService = new Intent(this, BackgroundService.class);
                Photo photoData = (Photo) intent.getSerializableExtra(BackgroundService.PHOTO_DATA_EXTRA);
                cacheService.updateState(photoData.getPhotoID(), CacheService.UPLOAD_STATE.UPLOADING);
                intentService.setAction(BackgroundService.UPLOAD_ACTION);
                intentService.putExtras(intent);
                if (cacheService != null) {
                    intentService.putExtra(BackgroundService.DB_NAME_EXTRA, cacheService.getMyDatabaseName());
                }
                startService(intentService);
            }

            refreshList();
        }
    }

    private void refreshList() {
        drawPhotoMarkers();
    }

    private void handleDownload(CacheService.UPLOAD_STATE uploadState, String photoId) {
        if (uploadState == CacheService.UPLOAD_STATE.NOT_UPLOAD) {
            if(!Config.isDemoMode(this)){
                UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.connection_error, false).show();
            }
        }
        refreshList();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_REVIEW) {
            refreshList();
        } else if (requestCode == REQUEST_REMINDER) {
            UIUtils.registerReminder(this);

        }else if(requestCode == REQUEST_MANAGE_SITE){
            refreshList();
        }else if(requestCode == REQUEST_SEND_EMAIL){
            GeneralUtil.deleteLogFile(this);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        if (view == imgLocation) {
            goToMyLocation();
        }
    }

    private void drawPhotoMarkers() {
        String projectId = Config.getCurrentProjectId(getActivityContext());
        List<Site> sites = GlobalState.getProjectSites(projectId);
        if (map != null && sites != null && !sites.isEmpty()) {
            map.clear();
            markers.clear();
            canDrawMarkers = false;
            for (Site site: sites) {
                LatLng latlng = new LatLng(site.getLat(), site.getLng());
                Marker marker = map.addMarker(new MarkerOptions().position(latlng));
                GuidePhoto guidePhoto = cacheService.getGuidePhotoByDirection(projectId, site.getSiteId(), Photo.DIRECTION.POINT);
                MarkerInfo info = new MarkerInfo();
                if(guidePhoto != null) {
                    info.setImageUrl(guidePhoto.getPhotoPath());
                    markers.put(marker,guidePhoto);
                } else {
                    List<Photo> photos = cacheService.getPhotosList(projectId, site.getSiteId());
                    if (!photos.isEmpty()) {
                        info.setImageUrl(photos.get(0).getPhotoPath());
                        markers.put(marker,photos.get(0));
                    }
                }

                info.setSiteId(site.getSiteId());

                marker.setTag(info);
            }

            drawCurrentLocation();

            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    String siteId = null;
                    if(marker.getTag() instanceof MarkerInfo) {
                        MarkerInfo info = (MarkerInfo) marker.getTag();
                        siteId = info.getSiteId();
                    }

                    Intent intent = new Intent(getActivityContext(), MainScreenActivity.class);
                    intent.putExtra(MainScreenActivity.SITE_ID, siteId);
                    startActivity(intent);
                }
            });
        } else if (map != null) {
            map.clear();
            drawCurrentLocation();
        }
    }

    private void drawCurrentLocation() {
        if (GlobalState.getCurrentUserLocation() != null) {
            LatLng location = new LatLng(GlobalState.getCurrentUserLocation().getLatitude(), GlobalState.getCurrentUserLocation().getLongitude());
            Canvas canvas = new Canvas();
            Drawable circleDrawable = ContextCompat.getDrawable(this, R.drawable.shape_current_location_circle);
            Bitmap bitmap = Bitmap.createBitmap(circleDrawable.getIntrinsicWidth(), circleDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmap);
            circleDrawable.setBounds(0, 0, circleDrawable.getIntrinsicWidth(), circleDrawable.getIntrinsicHeight());
            circleDrawable.draw(canvas);
            map.addMarker(new MarkerOptions().position(location)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
            double zoomLevel = LocationUtil.getZoomForMetersWide(this, Config.MAP_ZOOM_DISTANCE, location.latitude);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, (float) zoomLevel));
        }
    }

    private void goToMyLocation(){
        if(GlobalState.getCurrentUserLocation() != null) {
            LatLng latLng = new LatLng(GlobalState.getCurrentUserLocation().getLatitude(), GlobalState.getCurrentUserLocation().getLongitude());
            double zoomLevel = LocationUtil.getZoomForMetersWide(this, Config.MAP_ZOOM_DISTANCE, latLng.latitude);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float) zoomLevel);
            map.animateCamera(cameraUpdate);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setInfoWindowAdapter(new MapActivity.MapInfoWindow(this));
    }

    private static class MapInfoWindow implements GoogleMap.InfoWindowAdapter {
        private final Map<Marker, Bitmap> images = new HashMap<>();
        private final Map<Marker, Target<Bitmap>> targets = new HashMap<>();
        private final View view;
        private final ImageView imgPhoto;
        private WeakReference<Context> context;
        private int size;

        MapInfoWindow(Context context) {
            this.context = new WeakReference<>(context);
            size = (int) context.getResources().getDimension(R.dimen.marker_thumb);
            view = View.inflate(context, R.layout.item_map_info, null);
            imgPhoto = view.findViewById(R.id.imgPhoto);
        }

        public void showInfoWindow(Marker marker) {
            if (context.get() != null) {
                Glide.with(context.get()).clear(targets.get(marker)); // will do images.remove(marker) too
            }

            marker.showInfoWindow(); // indirectly calls getInfoContents which will return null and start Glide load
        }

        public void remove(Marker marker) {
            images.remove(marker);
            if (context.get() != null) {
                Glide.with(context.get()).clear(targets.remove(marker));
            }

            marker.remove();
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        private Target<Bitmap> getTarget(Marker marker) {
            Target<Bitmap> target = targets.get(marker);
            if (target == null) {
                target = new InfoTarget(marker);
                targets.put(marker, target); // missing in original (fixed 2018 August)
            }
            return target;
        }

        private class InfoTarget extends SimpleTarget<Bitmap> {
            Marker marker;
            InfoTarget(Marker marker) {
                super(size, size); // otherwise Glide will load original sized bitmap which is huge
                this.marker = marker;
            }

            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                images.put(marker, resource);
                marker.showInfoWindow();
            }

            @Override // Target
            public void onLoadCleared(Drawable placeholder) {
                images.remove(marker); // clean up previous image, it became invalid
                // don't call marker.showInfoWindow() to update because this is most likely called from Glide.into()
            }
        }

        @Override
        public View getInfoContents(final Marker marker) {
            if (marker.getTag() instanceof MarkerInfo && !TextUtils.isEmpty(((MarkerInfo) marker.getTag()).getImageUrl())) {
                Bitmap image = images.get(marker);
                MarkerInfo info = (MarkerInfo) marker.getTag();
                String photoPath = info.getImageUrl();
                if (image == null) {
                    RequestOptions options = new RequestOptions()
                            .placeholder(R.drawable.ic_launcher)
                            .override(size, size).centerCrop();
                    if(photoPath.startsWith("http")) {
                        Glide.with(context.get()).asBitmap().load(photoPath).apply(options).into(getTarget(marker));
                    } else {
                        Glide.with(context.get()).asBitmap().load(new File(photoPath)).apply(options).into(getTarget(marker));
                    }

                    return view;
                } else {
                    imgPhoto.setImageBitmap(image);
                    return view;
                }
            } else {
                imgPhoto.setImageResource(R.drawable.ic_launcher);
            }

            return view;
        }
    }

    private static class UploadBroadcaseReceiver extends BroadcastReceiver {
        private WeakReference<MapActivity> mContext;

        public UploadBroadcaseReceiver(MapActivity context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction() != null) {
                    if (intent.getAction().equals(BackgroundService.UPLOAD_FINISH_ACTION)) {
                        Ln.d("FINISH UPLOAD");
                        String photoId = intent.getStringExtra(BackgroundService.PHOTO_ID_EXTRA);
                        CacheService.UPLOAD_STATE uploadState = CacheService.UPLOAD_STATE.valueOf(intent.getIntExtra(
                                BackgroundService.UPLOAD_STATE_EXTRA, 0));
                        MapActivity MapActivity = mContext.get();
                        if (MapActivity != null) {
                            MapActivity.handleDownload(uploadState, photoId);
                        }
                    } else if (intent.getAction().equals(BackgroundService.CONNECTED_ACTION)) {
                        MapActivity MapActivity = mContext.get();
                        if (MapActivity != null) {
                            MapActivity.reUpload();
                        }
                    } else if (intent.getAction().equals(DownloadService.DOWNLOAD_GUIDE_FINISH_ACTION) && mContext != null && mContext.get() != null) {
                        mContext.get().reloadGuidePhoto();
                        mContext.get().refreshList();
                    }
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showDialogConfirmExit();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showDialogConfirmExit() {
        DialogUtil.createConfirmDialog(this, confirmExitListenner, R.string.confirm_exit);
    }

    DialogInterface.OnClickListener confirmExitListenner = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            stopLocationService();
            moveTaskToBack(true);
        }
    };

    private void stopLocationService() {
        Intent locationIntent = new Intent(this, LocationService.class);
        locationIntent.addCategory(LocationService.SERVICE_TAG);
        stopService(locationIntent);
    }

    private void startLocationService() {
        Intent locationIntent = new Intent(this, LocationService.class);
        locationIntent.addCategory(LocationService.SERVICE_TAG);
        startService(locationIntent);
    }

    private void stopDownloadGuideService(){
        Intent locationIntent = new Intent(this, DownloadService.class);
        stopService(locationIntent);
    }

    @Override
    protected void onDestroy() {
        stopLocationService();
        stopDownloadGuideService();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        EventBus.getDefault().register(this);
        super.onResume();
    }

    @Override
    protected void wasInBackground() {
        // update projects
        Intent intentService = new Intent(this, DownloadService.class);
        intentService.setAction(DownloadService.UPDATE_PROJECT);
        startService(intentService);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(UpdateProject event){
        initActionBar();
        EventBus.getDefault().removeStickyEvent(event);
    }

    public void onEventMainThread(UpdateSites event) {
        if(canDrawMarkers) {
            drawPhotoMarkers();
        }
    }

    private final BroadcastReceiver lftBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // extract the location info in the broadcast
            Intent locationIntent = new Intent(getActivityContext(), LocationService.class);
            locationIntent.addCategory(LocationService.SERVICE_TAG);
            locationIntent.setAction(LocationService.LOCATION_CHANGED);
            locationIntent.putExtra(LocationService.LOCATION_DATA, intent.getSerializableExtra(LocationLibraryConstants.LOCATION_BROADCAST_EXTRA_LOCATIONINFO));
            startService(locationIntent);
        }
    };
}
