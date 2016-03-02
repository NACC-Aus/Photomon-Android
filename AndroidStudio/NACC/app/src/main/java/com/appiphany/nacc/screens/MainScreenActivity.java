package com.appiphany.nacc.screens;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import roboguice.inject.InjectView;
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
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.appiphany.nacc.R;
import com.appiphany.nacc.events.UpdateProject;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Project;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.appiphany.nacc.services.PhotoAdapter;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.DialogUtil;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.UIUtils;
import com.appiphany.nacc.utils.UncaughtExceptionHandler;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibrary;

public class MainScreenActivity extends BaseActivity implements OnItemClickListener {
    @InjectView(R.id.list_photos)
    private ListView mListView;
    @InjectView(R.id.demo_view)
    private LinearLayout mDemoView;

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
    
    CacheService cacheService;

    private List<String> mGuidePhotoIds;
	private AlertDialog dialog1;
	private AlertDialog dialog2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_main_screen_layout);
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(GeneralUtil.getLogFilePath(this)));
        

        // just show demo view with demo mode
        if (Config.isDemoMode(this)) {
            mDemoView.setVisibility(View.VISIBLE);
        } else {
            mDemoView.setVisibility(View.GONE);
        }

        cacheService = new CacheService(this,
        		CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
        
        mListView.setOnItemClickListener(this);
        Intent intent = getIntent();
        Ln.d("[MainScreen] on create ");
        
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

        if(intent != null && intent.getBooleanExtra(DOWNLOAD_GUIDE_EXTRA, false) && !Config.isDemoMode(this)){
        }
        
        if (cacheService != null) {
            if (intent != null && intent.getBooleanExtra(FROM_LOGIN_EXTRA, false)) {
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
            mAdapter = new PhotoAdapter(this, R.layout.photo_item_layout, cacheService.getPhotos(Config.getCurrentProjectId(getActivityContext())),
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
        
        initActionBar();
        LocationLibrary.forceLocationUpdate(this);
        LocationLibrary.startAlarmAndListener(this);
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

    }

    @Override
    protected void onStop() {
        super.onStop();
        Ln.d("on stop");
        LocalBroadcastManager localMgr = LocalBroadcastManager.getInstance(this);
        localMgr.unregisterReceiver(mReceiver);
    }

    private void initActionBar() {
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
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(), android.R.layout.simple_list_item_1, items);
       
        getSupportActionBar()
                .setListNavigationCallbacks(adapter, new OnNavigationListener() {
					
					@Override
					public boolean onNavigationItemSelected(int itemPosition, long itemId) {
						Config.setCurrentProjectId(getActivityContext(), projects.get(itemPosition).getUid());
						refreshList();
						return false;
					}
				});
        
        if(currentIndex != -1){
        	Config.setCurrentProjectId(getActivityContext(), currentProjectId);
        	getSupportActionBar().setSelectedNavigationItem(currentIndex);
        }
    }
    
    private List<Project> getProjects(){
    	List<Project> ret = cacheService.getProjects();
    	return ret;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main_screen_menu, menu);   
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(3).setVisible(Config.isDemoMode(this));
        menu.getItem(4).setVisible(!Config.isDemoMode(this));
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
        case R.id.menu_take_picture:
            Intent startTakingPicture = new Intent(this, ImageTakingActivity.class);
            Config.setShowDirectionDialog(this, true);
            startActivity(startTakingPicture);
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
        	Intent siteIntent = new Intent(this, SiteManagementActivity.class);
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
        if (mAdapter != null) {
            mAdapter.setGuidePhotoId(mGuidePhotoIds);
            mAdapter.changeCursor(cacheService.getPhotos(Config.getCurrentProjectId(getActivityContext())));
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new PhotoAdapter(this, R.layout.photo_item_layout, cacheService.getPhotos(Config.getCurrentProjectId(getActivityContext())),
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
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static class UploadBroadcaseReceiver extends BroadcastReceiver {
        private WeakReference<MainScreenActivity> mContext;

        public UploadBroadcaseReceiver(MainScreenActivity context) {
            mContext = new WeakReference<MainScreenActivity>(context);
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
        startActivityForResult(showReviewPage, REQUEST_REVIEW);
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
}