package com.appiphany.nacc.screens;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.appiphany.nacc.R;
import com.appiphany.nacc.events.UpdateProject;
import com.appiphany.nacc.model.CacheItem;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Project;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.appiphany.nacc.services.PhotoAdapter;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.DialogUtil;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.Intents;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainScreenActivity extends BaseActivity implements OnItemClickListener, View.OnClickListener {
    private ListView mListView;
    private LinearLayout mDemoView;
    private FloatingActionButton mFABAddNew;

    private PhotoAdapter mAdapter;
    private UploadBroadcaseReceiver mReceiver = new UploadBroadcaseReceiver(this);

    public static final String REQUEST_UPLOAD = "com.appiphany.nacc.REQUEST_UPLOAD";
    public static final String ACTION_LOGOUT = "com.appiphany.nacc.ACTION_LOGOUT";
    public static final String FROM_LOGIN_EXTRA = "from_login_extra";
    public static final String DOWNLOAD_GUIDE_EXTRA = "download_guide";

    private static final int REQUEST_REVIEW = 1;
    private static final int REQUEST_REMINDER = 2;
    private static final int REQUEST_MANAGE_SITE = 3;
    private static final int REQUEST_SEND_EMAIL = 4;
    private static final int REQUEST_TAKING_PHOTO = 5;
    
    CacheService cacheService;

    private List<String> mGuidePhotoIds;
	private AlertDialog dialog1;
	private AlertDialog dialog2;
    private boolean firstLoading;
    private Site currentSite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_main_screen_layout);
        setLayoutInsets(R.id.rootLayout);

        firstLoading = true;
        mListView = findViewById(R.id.list_photos);
        mDemoView = findViewById(R.id.demo_view);
        mFABAddNew = findViewById(R.id.fabAddNew);

        // just show demo view with demo mode
        if (Config.isDemoMode(this)) {
            mDemoView.setVisibility(View.VISIBLE);
        } else {
            mDemoView.setVisibility(View.GONE);
        }

        cacheService = new CacheService(this,
        		CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
        
        mListView.setOnItemClickListener(this);
        loadData();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mListView.setNestedScrollingEnabled(true);
        }

        mFABAddNew.setOnClickListener(this);
        initActionBar();
    }

    private void loadData(){
        Intent intent = getIntent();
        Ln.d("[MainScreen] on create ");

        currentSite = (Site) intent.getSerializableExtra(Intents.SELECTED_SITE);
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(REQUEST_UPLOAD) && !Config.isDemoMode(this) && intent.hasExtra(BackgroundService.PHOTO_DATA_EXTRA)) {
                Intent intentService = new Intent(this, BackgroundService.class);
                Photo photoData = (Photo) intent.getSerializableExtra(BackgroundService.PHOTO_DATA_EXTRA);
                cacheService.updateState(photoData.getPhotoID(), UPLOAD_STATE.UPLOADING);
                intentService.setAction(BackgroundService.UPLOAD_ACTION);
                intentService.putExtras(intent);
                if (cacheService != null) {
                    intentService.putExtra(BackgroundService.DB_NAME_EXTRA, cacheService.getMyDatabaseName());
                }
                startService(intentService);
            }

            mGuidePhotoIds = cacheService.getGuidePhotoIds();
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
            mGuidePhotoIds = cacheService.getGuidePhotoIds();
            Cursor cursor = cacheService.getPhotos(Config.getCurrentProjectId(getActivityContext()), currentSite);
            mAdapter = new PhotoAdapter(this, R.layout.photo_item_layout, cursor,
                    new String[] { CacheService.COLUMN_ID }, new int[] { R.id.photo_text_view },
                    0, mGuidePhotoIds);
            mListView.setAdapter(mAdapter);
            if (!Config.isDemoMode(this)) {
                reUpload();
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.common_error);
            builder.setNeutralButton(R.string.ok_text, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            builder.show();
        }
    }

    private void reloadGuidePhoto(){
    	mGuidePhotoIds = cacheService.getGuidePhotoIds();
    }
    
    private DialogInterface.OnClickListener dialogListener = new OnClickListener() {
		
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
            FirebaseCrashlytics.getInstance().recordException(throwable);
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
        IntentFilter networkFilter = new IntentFilter();
        networkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Ln.d("on stop");
        LocalBroadcastManager localMgr = LocalBroadcastManager.getInstance(this);
        localMgr.unregisterReceiver(mReceiver);
    }


    @SuppressWarnings("ConstantConditions")
    private void initActionBar() {
        getSupportActionBar().setIcon(R.drawable.actionbar_logo_with_space);
    	getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        if(currentSite != null && !TextUtils.isEmpty(currentSite.getName())) {
            getSupportActionBar().setTitle(currentSite.getName());
            return;
        }

        final List<Project> projects = getProjects();
        String projectId = Config.getCurrentProjectId(getActivityContext());
        if(GeneralUtil.isNullOrEmpty(projects) || Config.isDemoMode(getActivityContext()) || TextUtils.isEmpty(projectId)){
        	return;
        }

        for (Project project: projects) {
            if(projectId.equals(project.getUid())) {
                getSupportActionBar().setTitle(project.getName());
                break;
            }
        }
    }

    private void reloadCurrentSite() {
        Intent locationIntent = new Intent(MainScreenActivity.this, LocationService.class);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
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
                4, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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
                cacheService.updateState(photoData.getPhotoID(), UPLOAD_STATE.UPLOADING);
                intentService.setAction(BackgroundService.UPLOAD_ACTION);
                intentService.putExtras(intent);
                if (cacheService != null) {
                    intentService.putExtra(BackgroundService.DB_NAME_EXTRA, cacheService.getMyDatabaseName());
                }
                startService(intentService);
            }

            mGuidePhotoIds = cacheService.getGuidePhotoIds();
            refreshList();
        }
    }

    private void refreshList() {
        Cursor cursor = cacheService.getPhotos(Config.getCurrentProjectId(getActivityContext()), currentSite);
        if (mAdapter != null) {
            mAdapter.setGuidePhotoId(mGuidePhotoIds);
            mAdapter.changeCursor(cursor);
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new PhotoAdapter(this, R.layout.photo_item_layout, cursor,
                    new String[] { CacheService.COLUMN_ID }, null,
                    0, mGuidePhotoIds);
            mListView.setAdapter(mAdapter);
        }
    }

    private void handleDownload(UPLOAD_STATE uploadState, String photoId) {
        mGuidePhotoIds = cacheService.getGuidePhotoIds();
        if (mAdapter != null) {
            mAdapter.setGuidePhotoId(mGuidePhotoIds);
        }
        if (uploadState == UPLOAD_STATE.NOT_UPLOAD) {
        	if(!Config.isDemoMode(this)){
        		UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.connection_error, false).show();
        	}
        }
        refreshList();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_REVIEW) {
            mGuidePhotoIds = cacheService.getGuidePhotoIds();
            refreshList();
        } else if (requestCode == REQUEST_REMINDER) {
            UIUtils.registerReminder(this);

        }else if(requestCode == REQUEST_MANAGE_SITE){
        	refreshList();
        }else if(requestCode == REQUEST_SEND_EMAIL){
        	GeneralUtil.deleteLogFile(this);
        } else if(requestCode == REQUEST_TAKING_PHOTO && resultCode == RESULT_OK){
            setResult(RESULT_OK);
            loadData();
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        if(view == mFABAddNew){
            Intent startTakingPicture = new Intent(this, ImageTakingActivity.class);
            Config.setShowDirectionDialog(this, true);
            startTakingPicture.putExtra(Intents.SELECTED_SITE, currentSite);
            startActivityForResult(startTakingPicture, REQUEST_TAKING_PHOTO);
        }
    }

    private static class UploadBroadcaseReceiver extends BroadcastReceiver {
        private WeakReference<MainScreenActivity> mContext;

        public UploadBroadcaseReceiver(MainScreenActivity context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction() != null) {
                    if (intent.getAction().equals(BackgroundService.UPLOAD_FINISH_ACTION)) {
                        Ln.d("FINISH UPLOAD");
                        String photoId = intent.getStringExtra(BackgroundService.PHOTO_ID_EXTRA);
                        UPLOAD_STATE uploadState = UPLOAD_STATE.valueOf(intent.getIntExtra(
                                BackgroundService.UPLOAD_STATE_EXTRA, 0));
                        MainScreenActivity mainScreenActivity = mContext.get();
                        if (mainScreenActivity != null) {
                            mainScreenActivity.handleDownload(uploadState, photoId);
                        }
                    } else if (intent.getAction().equals(BackgroundService.CONNECTED_ACTION)) {
                        MainScreenActivity mainScreenActivity = mContext.get();
                        if (mainScreenActivity != null) {
                            mainScreenActivity.reUpload();
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent showReviewPage = new Intent(this, ImageReviewActivity.class);
        showReviewPage.putExtra(ImageReviewActivity.CURRENT_IMAGE_ID_EXTRA, position);
        showReviewPage.putExtra(Intents.SELECTED_SITE, currentSite);
        startActivityForResult(showReviewPage, REQUEST_REVIEW);
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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent locationIntent = new Intent(getActivityContext(), LocationService.class);
                    locationIntent.addCategory(LocationService.SERVICE_TAG);
                    startService(locationIntent);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    FirebaseCrashlytics.getInstance().recordException(throwable);
                }
            }
        }, 200);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UpdateProject event){
        initActionBar();
        EventBus.getDefault().removeStickyEvent(event);
    }
}
