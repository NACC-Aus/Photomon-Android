package com.appiphany.nacc.screens;

import java.lang.ref.WeakReference;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.appiphany.nacc.services.CursorFragmentPagerAdapter;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.Intents;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.UncaughtExceptionHandler;

public class ImageReviewActivity extends BaseActivity implements OnPageChangeListener {
    private static final int SHOW_NOTE_CODE = 222;
    private ViewPager mViewPager;
    private int mCurrentPhotoId;
    private CacheService cacheService;
    private PhotoReviewAdapter mAdapter;
    public static final String CURRENT_IMAGE_ID_EXTRA = "current_image_id_extra";
    private UploadListener mReceiver = new UploadListener(this);
    private MenuItem mReuploadMenu;
    private MenuItem mAddNoteMenu;
    private boolean isPhotoUploaded = true;
    private Toast mToast;
    private Site currentSite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_image_review_layout);
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(GeneralUtil.getLogFilePath(this)));
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
        Intent intent = getIntent();
        if (intent != null) {
            mCurrentPhotoId = intent.getIntExtra(CURRENT_IMAGE_ID_EXTRA, 0);
            currentSite = (Site) intent.getSerializableExtra(Intents.SELECTED_SITE);
        }

        cacheService = CacheService.getInstance(this,
                CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
        Cursor c = cacheService.getPhotos(Config.getCurrentProjectId(getActivityContext()), currentSite);
        mAdapter = new PhotoReviewAdapter(this, getSupportFragmentManager(), c);
        mViewPager = findViewById(R.id.image_view_pager);
        initActionBar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager localMgr = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter(BackgroundService.UPLOAD_FINISH_ACTION);
        intentFilter.addAction(BackgroundService.CONNECTED_ACTION);
        localMgr.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager localMgr = LocalBroadcastManager.getInstance(this);
        localMgr.unregisterReceiver(mReceiver);
    }

    @SuppressWarnings("ConstantConditions")
    private void initActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setTitle(R.string.saved_photos_title);
        setSupportProgressBarIndeterminate(true);
        setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reupload_menu, menu);
        mReuploadMenu = menu.findItem(R.id.reupload_menu);
        mAddNoteMenu = menu.findItem(R.id.note_menu);
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(mCurrentPhotoId);
        onPageSelected(mCurrentPhotoId);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (item.getItemId() == R.id.reupload_menu) {
            Fragment currentFragment = mAdapter.getFragment(mCurrentPhotoId);
            if (currentFragment != null && !currentFragment.isRemoving()) {
                Photo photoData = ((ImageReviewFragment) currentFragment).getPhotoData();
                if (photoData != null) {
                    cacheService.updateState(photoData.getPhotoID(), UPLOAD_STATE.UPLOADING);
                    updateActivityWithState(UPLOAD_STATE.UPLOADING);
                    notifyDataChanged();
                    Intent intentService = new Intent(this, BackgroundService.class);
                    intentService.setAction(BackgroundService.UPLOAD_ACTION);
                    intentService.putExtra(BackgroundService.PHOTO_DATA_EXTRA, photoData);
                    intentService.putExtra(BackgroundService.DB_NAME_EXTRA, cacheService.getMyDatabaseName());
                    startService(intentService);
                }
            }
        } else if (item.getItemId() == R.id.note_menu) {
        	if (!Config.isDemoMode(this) && !isPhotoUploaded) {
        		mToast.setText("Photo is not uploaded yet!");
        		mToast.show();
        		return false;
        	}
            Fragment currentFragment = mAdapter.getFragment(mCurrentPhotoId);
            Photo photoData = ((ImageReviewFragment) currentFragment).getPhotoData();
            String note = cacheService.getNoteForPhoto(photoData.getPhotoID());
            photoData.setNote(note);
            Intent noteIntent = new Intent(this, AddNoteActivity.class);
            noteIntent.setAction(AddNoteActivity.SHOW_NOTE);
            noteIntent.putExtra(BackgroundService.PHOTO_DATA_EXTRA, photoData);
            startActivityForResult(noteIntent, SHOW_NOTE_CODE);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SHOW_NOTE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Photo photo = (Photo) data.getSerializableExtra(BackgroundService.PHOTO_DATA_EXTRA);
                    if (photo != null) {
                        cacheService = CacheService.getInstance(
                                this,
                                CacheService.createDBNameFromUser(Config.getActiveServer(this),
                                        Config.getActiveUser(this)));
                        if (cacheService.isChangeNote(photo.getPhotoID(), photo.getNote())) {
                            cacheService.updateNoteForPhoto(photo.getPhotoID(), photo.getNote());
                            if (!Config.isDemoMode(this)) {
                                Intent intentService = new Intent(this, BackgroundService.class);
                                intentService.setAction(BackgroundService.UPLOAD_NOTE);
                                intentService.putExtra(BackgroundService.PHOTO_DATA_EXTRA, photo);
                                startService(intentService);
                            }
                        }
                    }
                }
            }
        }
    }

    private static class PhotoReviewAdapter extends CursorFragmentPagerAdapter {

        public PhotoReviewAdapter(Context context, FragmentManager fm,
                Cursor c) {
            super(context, fm, c);
        }

        @Override
        public Fragment getItem(Context context, Cursor cursor) {
            Fragment result = null;
            if (cursor != null) {
                String imageId = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_ID));
                String imageServerId = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_PHOTO_SERVER_ID));
                String imagePath = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_PHOTO_PATH));
                String imageName = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_NAME));
                UPLOAD_STATE uploadState = UPLOAD_STATE.valueOf(cursor.getInt(cursor
                        .getColumnIndex(CacheService.COLUMN_STATE)));
                String imageDirection = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_DIRECTION));
                long imageTime = cursor.getLong(cursor.getColumnIndex(CacheService.COLUMN_TAKEN_DATE));
                String siteId = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_SITE));
                String note = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_NOTE));
                Float opacity = cursor.getFloat(cursor.getColumnIndex(CacheService.COLUMN_OPACITY));
                String projectId = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_PROJECT));
                
                Photo photoModel = new Photo(imageId, imageServerId, imagePath, imageName, siteId, imageDirection,
                        uploadState,
                        new Date(imageTime), note, opacity, projectId);
                
                result = ImageReviewFragment.newInstance(photoModel, context);
            }
            return result;
        }
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {

    }

    @Override
    public void onPageSelected(int position) {
        mCurrentPhotoId = position;
        if (mAdapter != null) {
            ImageReviewFragment fragment = (ImageReviewFragment) mAdapter.getFragment(position);
            if (fragment != null && !fragment.isRemoving()) {
                fragment.updateGuideButton(cacheService.getGuidePhotoIds());
                fragment.updateFullscreen(isFullscreen);
                Bundle args = fragment.getArguments();
                if (args != null) {
                    Photo photoData = (Photo) args.getSerializable(ImageReviewFragment.PHOTO_MODEL_ARGS);
                    if (photoData != null) {
                        Ln.d("PAGE SELECTED " + photoData.getUploadState());
                        updateActivityWithState(photoData.getUploadState());
                    } else {
                        Ln.d("PAGE SELECTED NULL");
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    public void notifyDataChanged() {
        Cursor c = cacheService.getPhotos(Config.getCurrentProjectId(getActivityContext()));
        if (mAdapter != null && mAdapter.getCursor() != null) {
            mAdapter.getCursor().close();
        }
        mAdapter = new PhotoReviewAdapter(this, getSupportFragmentManager(), c);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(mCurrentPhotoId);
        onPageSelected(mCurrentPhotoId);
    }

    private boolean isFullscreen = false;

    public void notifyFullscreen(boolean isFullscreen) {
        this.isFullscreen = isFullscreen;
    }

    private void updateActivityWithState(UPLOAD_STATE state) {
        switch (state) {
        case NOT_UPLOAD:
            setSupportProgressBarIndeterminateVisibility(false);
            mReuploadMenu.setVisible(true);
            isPhotoUploaded = false;
            break;
        case UPLOADING:
            setSupportProgressBarIndeterminateVisibility(true);
            mReuploadMenu.setVisible(false);
            isPhotoUploaded = false;
            break;
        case UPLOADED:
        case DOWNLOAD:
            setSupportProgressBarIndeterminateVisibility(false);
            mReuploadMenu.setVisible(false);
            mAddNoteMenu.setVisible(true);
            isPhotoUploaded = true;
            break;
        default:
            break;
        }

        // don't show progress bar with demo mode
        if (Config.isDemoMode(this)) {
            setSupportProgressBarIndeterminateVisibility(false);
            mReuploadMenu.setVisible(false);
        }
    }

    private static class UploadListener extends BroadcastReceiver {
        private WeakReference<ImageReviewActivity> mContext;

        public UploadListener(ImageReviewActivity context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                if (intent.getAction().equals(BackgroundService.UPLOAD_FINISH_ACTION)) {
                    if (mContext != null && mContext.get() != null) {
                        mContext.get().notifyDataChanged();
                    }
                }
            }
        }

    }

}
