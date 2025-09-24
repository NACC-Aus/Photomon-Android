package com.appiphany.nacc.screens;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.appiphany.nacc.R;
import com.appiphany.nacc.model.GuidePhoto;
import com.appiphany.nacc.model.Photo.DIRECTION;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.Intents;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.LocationUtil;
import com.appiphany.nacc.utils.UIUtils;
import com.bumptech.glide.Glide;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class ImageTakingActivity extends BaseActivity implements OnClickListener, OnSeekBarChangeListener {
	private static final String ALPHA_KEY = "alpha_key";
    private static final int SELECT_IMAGE = 0;
    private static final int PREVIEW_IMAGE = 1;
    private FrameLayout mSurfaceViewLayout;
    private LinearLayout mCameraPanel;
    private ImageButton mTakingPictureBtn;
    private ImageButton mChangeCamBtn;
    private ImageButton mChangeFlashBtn;
    private Button mGuideButton;
    
    private ImageView mGuideImageView;

    private SeekBar seekOpacityCamera;

    private RelativeLayout mRlConfigPanel;

    private ProgressDialog mProgressDialog;

    private DIRECTION mCurrentDirection = null;
    private boolean isGuideOn = true;

    private String mGuidePhotoPath = null;
    private DIRECTION mGuidePhotoDirection = null;
    private CopyImageTask mTaskCopy;
    private LocationUtil mLocationUtil;

    private Site mBestSite;
    private UpdateSiteReceiver updateSiteReceiver;
    private boolean hasRegisterReceiver;
    private float currentAlpha;
    private static final int version = android.os.Build.VERSION.SDK_INT;
    private boolean hasSurface;

    private Map<DIRECTION, GuidePhoto> allGuidePhotos = new HashMap<>();

    private CameraView cameraView;
    private Site selectedSite;

    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taking_picture_layout);
        setLayoutInsets(R.id.rootLayout);

        cameraView = findViewById(R.id.camera);
        mSurfaceViewLayout = findViewById(R.id.surface_view_layout);
        mCameraPanel = findViewById(R.id.taking_picture_layout);
        mTakingPictureBtn = findViewById(R.id.taking_picture_btn);
        mChangeCamBtn = findViewById(R.id.change_cam_btn);
        mChangeFlashBtn = findViewById(R.id.flash_btn);
        mGuideButton = findViewById(R.id.guide_btn);
        seekOpacityCamera = findViewById(R.id.seekOpacityCamera);
        mRlConfigPanel = findViewById(R.id.config_panel);

        mTakingPictureBtn = findViewById(R.id.taking_picture_btn);
        mTakingPictureBtn.setOnClickListener(this);
        mGuideImageView = findViewById(R.id.guide_image_view);
        
        seekOpacityCamera.setIndeterminate(false);
        seekOpacityCamera.setOnSeekBarChangeListener(this);

        if(getIntent().hasExtra(Intents.SELECTED_SITE)) {
            selectedSite = (Site) getIntent().getSerializableExtra(Intents.SELECTED_SITE);
        }
        currentAlpha = savedInstanceState != null ? savedInstanceState.getFloat(ALPHA_KEY, 0.0f) : 0.0f;
        
        if(currentAlpha == 0){
	        if(version < android.os.Build.VERSION_CODES.HONEYCOMB){
	        	seekOpacityCamera.setMax(255);
	        	seekOpacityCamera.setProgress(120);
	        	mGuideImageView.setAlpha(120);
	        	currentAlpha = 120;
	        }else{        	
	        	seekOpacityCamera.setMax(10);
	        	seekOpacityCamera.setProgress(5);
	        	mGuideImageView.setAlpha(0.5f);
	        	currentAlpha = 0.5f;
	        }
        }else{
        	if(version < android.os.Build.VERSION_CODES.HONEYCOMB){
	        	seekOpacityCamera.setMax(255);
	        	seekOpacityCamera.setProgress(120);
	        	mGuideImageView.setAlpha((int)currentAlpha);
	        }else{        	
	        	seekOpacityCamera.setMax(10);
	        	seekOpacityCamera.setProgress(5);
	        	mGuideImageView.setAlpha(currentAlpha);
	        }
        }
        
        hasSurface = false;
        initCameraPanel();

        if (savedInstanceState != null) {
            mCurrentDirection = DIRECTION.getDirection(savedInstanceState.getString("direction"));            
            isFlashOn = savedInstanceState.getBoolean("isFlashOn", false);
            isGuideOn = savedInstanceState.getBoolean("isGuideOn", false);
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(android.R.style.Widget_DeviceDefault_Light_ProgressBar_Large);

        getAllGuidePhotos(this);

        if(Config.shouldShowDirectionDialog(this)){
        	showChangeDirectionDialog();
        }        

        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM); // Pinch to zoom!
        cameraView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS);

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                ImageTakingActivity.this.onPictureTaken(result.getData());
            }
        });
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
    }

    private void doDirectionChangedToValue(DIRECTION value) {
    	 doDirectionChanged(value);
    }

    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void displayGuidePhoto(boolean show) {
    	Ln.d("displayGuidePhoto");
        if (mGuidePhotoPath != null) {
            if (show) {
                int orientationImage = 0;
                ExifInterface exif;
                try {
                    exif = new ExifInterface(mGuidePhotoPath);
                    orientationImage = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
				} catch (IOException e) {
                    e.printStackTrace();
                }

                final int guideWidth;
                final int guideHeight;
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    guideWidth = mSurfaceViewLayout.getMeasuredWidth();
                    guideHeight = mSurfaceViewLayout.getMeasuredHeight();
                } else {
                    guideWidth = mRlConfigPanel.getMeasuredWidth();
                    guideHeight = mRlConfigPanel.getMeasuredHeight();
                }
                
                mHandler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
					    if (!getActivityContext().isDestroyed()) {
                            if(!mGuidePhotoPath.startsWith("http")){
                                Glide.with(getActivityContext()).load(new File(mGuidePhotoPath)).into(mGuideImageView);
                            }else{
                                Glide.with(getActivityContext()).load(mGuidePhotoPath).into(mGuideImageView);
                            }
                        }
					}
				}, 700);
                              
                              
                seekOpacityCamera.setVisibility(View.VISIBLE);
                seekOpacityCamera.setMax(10);
                mGuideImageView.setAlpha(currentAlpha);

                FrameLayout.LayoutParams params = new LayoutParams(guideWidth, guideHeight, Gravity.CENTER);
                mGuideImageView.setLayoutParams(params);
                mGuideButton.setText(R.string.guide_on);
                mGuideImageView.setVisibility(View.VISIBLE);
            } else {
                mGuideButton.setText(R.string.guide_off);
                mGuideImageView.setVisibility(View.INVISIBLE);
                seekOpacityCamera.setVisibility(View.INVISIBLE);
            }
        }
    }

    Handler mHandler = new Handler();
    
    @SuppressWarnings("deprecation")
	@Override
    protected void onResume() {
        super.onResume();
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);


        mLocationUtil = new LocationUtil(this);

        if (mCurrentDirection != null) {
        	doDirectionChanged(mCurrentDirection);
        }
        
        registerLocationReceiver();

        cameraView.open();
        isFlashOn = false;
        mChangeFlashBtn.setImageResource(R.drawable.ic_device_access_flash_off);
        changeFlashMode(false);
    }
    
	private void registerLocationReceiver() {
		if(!hasRegisterReceiver){
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

    public void doGuideChanged(View v) {
        if (mGuidePhotoDirection == null || (mCurrentDirection == mGuidePhotoDirection)) {
            isGuideOn = !isGuideOn;
            displayGuidePhoto(isGuideOn);

            getGuidePhotoBySites();
        }
    }

    public void doDirectionChanged(DIRECTION value) {
        //resetDirections
    	mCurrentDirection = value;

        // always set "guide on" when change direction
        isGuideOn = true;
        
        mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				getAndShowGuidePhoto(ImageTakingActivity.this);
			}
		}, 300);
        
        getGuidePhotoBySites();

    }

    public void onGoToLibrary(View v) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);

        startActivityForResult(i, SELECT_IMAGE);
    }

    private void showLoadingDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    private void hideLoadingDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_IMAGE) {
                if (data != null) {
                    String picturePath = null;
                    if (ContentResolver.SCHEME_CONTENT.equals(data.getData().getScheme())) {
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        Cursor cursor = getContentResolver().query(data.getData(),
                                filePathColumn, null, null, null);
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        picturePath = cursor.getString(columnIndex);
                        cursor.close();
                    } else if (ContentResolver.SCHEME_FILE.equals(data.getData().getScheme())) {
                        picturePath = data.getDataString();
                    }
                    if (picturePath != null) {
                        showLoadingDialog();
                        mTakingPictureBtn.setEnabled(false);
                        if (mTaskCopy != null) {
                            mTaskCopy.cancel(true);
                        }
                        String photoId = CacheService.getNewPhotoID();
                        String path = getExternalCacheDir().getAbsolutePath();
                        mTaskCopy = new CopyImageTask(this, path, photoId);
                        mTaskCopy.execute(picturePath);
                    }

                }
            } else if (requestCode == PREVIEW_IMAGE) {
                setResult(RESULT_OK);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.close();
        // stop listen location change
        hideLoadingDialog();
    }

	private void unRegisterLocationReceiver() {
		if(hasRegisterReceiver){
        	unregisterReceiver(updateSiteReceiver);
        	hasRegisterReceiver = false;
        }
	}

    @Override
    protected void onStop() {
        super.onStop();

        unRegisterLocationReceiver();
        mLocationUtil.stopLocationListener(locationListener);
    }

	private void initCameraPanel() {
        int numOfCamera = Camera.getNumberOfCameras();

        if (numOfCamera <= 1) {
            mChangeCamBtn.setVisibility(View.GONE);
        } else {
            mChangeCamBtn.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //Handle config change myself
        super.onConfigurationChanged(newConfig);
    }


    @SuppressLint("InlinedApi")

    @Override
    public void onClick(View v) {
        cameraView.takePicture();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.destroy();
    }

    public void onChangeCam(View v) {
        cameraView.toggleFacing();
    }

    private void changeFlashMode(boolean isFlashOn) {
        if(isFlashOn) {
            cameraView.setFlash(Flash.ON);
        } else{
            cameraView.setFlash(Flash.OFF);
        }
    }
	
    private boolean isFlashOn = false;

    public void onChangeFlash(View v) {
        isFlashOn = !isFlashOn;
        if (isFlashOn) {
            mChangeFlashBtn.setImageResource(R.drawable.ic_device_access_flash_on);
        } else {
            mChangeFlashBtn.setImageResource(R.drawable.ic_device_access_flash_off);
        }
        changeFlashMode(isFlashOn);
    }


    public void onPictureTaken(byte[] picture) {
    	Ln.d("call onPictureTaken");
        mTakingPictureBtn.setEnabled(false);
        try {
            String photoId = CacheService.getNewPhotoID();
            String path = null;
            long availabelInternalMem = getAvailableInternalMemorySize();
            if (availabelInternalMem < 1024 * 1024 * 2) {
                availabelInternalMem = getAvailableExternalMemorySize();
                if (availabelInternalMem == -1 || availabelInternalMem < 1024 * 1024 * 2) {
                    path = null;
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    if (dir != null) {
                        path = dir.getAbsolutePath();
                    }
                }
            } else {
                File dir = getDir("photos", MODE_PRIVATE);
                if (dir != null) {
                    path = dir.getAbsolutePath();
                    Ln.d("[ImageTakingActivity] path = " + path);
                }
            }



            if(path != null) {
                File file = new File(path, photoId + ".jpg");
                try {
                    GeneralUtil.saveBitmap(picture, file.getAbsolutePath());
                    goToPreview(file.getAbsolutePath(), photoId, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                UIUtils.buildAlertDialog(getActivityContext(), R.string.dialog_title, R.string.storage_low_error, false).show();
            }
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isFlashOn", isFlashOn);
        if(mCurrentDirection != null){
        	outState.putString("direction", mCurrentDirection.getValue());
        }
        outState.putBoolean("isGuideOn", isGuideOn);
        outState.putFloat(ALPHA_KEY, currentAlpha);
        super.onSaveInstanceState(outState);

    }

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    public static long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return availableBlocks * blockSize;
        } else {
            return -1;
        }
    }

    private void goToPreview(final String filename, final String photoId, final boolean isSelectFromGallery) {
        mTakingPictureBtn.setEnabled(true);
        mBestSite = GlobalState.getBestSite();
        if (filename != null) {
        	Intent showPreviewIntent = new Intent(ImageTakingActivity.this, ImagePreviewActivity.class);
            showPreviewIntent.putExtra(BackgroundService.PHOTO_PATH_EXTRA, filename);
            showPreviewIntent.putExtra(BackgroundService.DIRECTION_EXTRA, mCurrentDirection.getValue());
            showPreviewIntent.putExtra(BackgroundService.PHOTO_ID_EXTRA, photoId);
            showPreviewIntent.putExtra(BackgroundService.FROM_GALLERY, isSelectFromGallery);
            showPreviewIntent.putExtra(BackgroundService.BEST_SITE, mBestSite);
            showPreviewIntent.putExtra(Intents.SELECTED_SITE, selectedSite);
            showPreviewIntent.putExtra(BackgroundService.GUIDE_PHOTO, mGuidePhotoPath);
            showPreviewIntent.putExtra(BackgroundService.GUIDE_PHOTO_ALPHA, currentAlpha);
            startActivityForResult(showPreviewIntent, PREVIEW_IMAGE);
            
        }
    }    

    private void showChangeDirectionDialog(){
    	cameraView.setVisibility(View.INVISIBLE);
    	final Dialog dialog = new Dialog(this);
    	dialog.setCancelable(true);
    	dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle(R.string.msg_select_direction);
        dialog.setContentView(R.layout.dialog_change_direction);
        int screenwidth = getResources().getDisplayMetrics().widthPixels;
        int width = getResources().getDimensionPixelSize(R.dimen.pop_up_width);
        if(	width > screenwidth){
        	dialog.getWindow().setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }else{
        	dialog.getWindow().setLayout(width, LayoutParams.WRAP_CONTENT);
        }

        dialog.getWindow().setGravity(Gravity.CENTER);
        Button btnNorth = dialog.findViewById(R.id.btnNorth);
        Button btnSouth = dialog.findViewById(R.id.btnSouth);
        Button btnEast = dialog.findViewById(R.id.btnEast);
        Button btnWest = dialog.findViewById(R.id.btnWest);
        Button btnPoint = dialog.findViewById(R.id.btnPoint);

        int guideBackground = ContextCompat.getColor(this,
                R.color.guide_dark_color_bgr);
        int guideText = ContextCompat.getColor(this,
                R.color.white);

        if(allGuidePhotos.get(DIRECTION.NORTH) != null){
            btnNorth.getBackground().setColorFilter(guideBackground, PorterDuff.Mode.MULTIPLY);
            btnNorth.setTextColor(guideText);
        }

        if(allGuidePhotos.get(DIRECTION.SOUTH) != null){
            btnSouth.getBackground().setColorFilter(guideBackground, PorterDuff.Mode.MULTIPLY);
            btnSouth.setTextColor(guideText);
        }

        if(allGuidePhotos.get(DIRECTION.EAST) != null){
            btnEast.getBackground().setColorFilter(guideBackground, PorterDuff.Mode.MULTIPLY);
            btnEast.setTextColor(guideText);
        }

        if(allGuidePhotos.get(DIRECTION.WEST) != null){
            btnWest.getBackground().setColorFilter(guideBackground, PorterDuff.Mode.MULTIPLY);
            btnWest.setTextColor(guideText);
        }

        if(allGuidePhotos.get(DIRECTION.POINT) != null){
            btnPoint.getBackground().setColorFilter(guideBackground, PorterDuff.Mode.MULTIPLY);
            btnPoint.setTextColor(guideText);
        }

        OnClickListener directionClickListener = new OnClickListener() {
    		
    		@Override
    		public void onClick(View v) {
    			switch (v.getId()) {
    			case R.id.btnNorth:
    				mCurrentDirection = DIRECTION.NORTH;
    				break;
    			case R.id.btnSouth:
    				mCurrentDirection = DIRECTION.SOUTH;
    				break;
    			case R.id.btnEast:
    				mCurrentDirection = DIRECTION.EAST;
    				break;
    			case R.id.btnWest:
    				mCurrentDirection = DIRECTION.WEST;
    				break;
    			case R.id.btnPoint:
    				mCurrentDirection = DIRECTION.POINT;
    				break;
    			}			

    			cameraView.setVisibility(View.VISIBLE);
    			doDirectionChanged(mCurrentDirection);
    			v.setSelected(true);
    			dialog.dismiss();
    			Config.setShowDirectionDialog(ImageTakingActivity.this, false);
                showOutOfRangePoint();
    		}
    	};
    	
    	dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				ImageTakingActivity.this.finish();				
			}
		});
    	
        btnNorth.setOnClickListener(directionClickListener);
        btnSouth.setOnClickListener(directionClickListener);
        btnEast.setOnClickListener(directionClickListener);
        btnWest.setOnClickListener(directionClickListener);
        btnPoint.setOnClickListener(directionClickListener);
        dialog.show();
    }

    private void showOutOfRangePoint() {
        Site site = selectedSite;
        if(site == null) {
            site = GlobalState.getBestSite();
        }

        if(site != null && GlobalState.getCurrentUserLocation() != null) {
            float distance = LocationUtil.distanceBetween(site.getLat(), site.getLng(),
                    GlobalState.getCurrentUserLocation().getLatitude(), GlobalState.getCurrentUserLocation().getLongitude());
            if (distance > Config.LOCATION_NEAREST_DISTANCE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivityContext());
                builder.setTitle(null).setMessage(getString(R.string.msg_photo_out_of_range, (int)distance));
                builder.setCancelable(true);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                });

                builder.create().show();
            }
        }
    }

    private class CopyImageTask extends AsyncTask<String, Void, String> {
        private WeakReference<ImageTakingActivity> mContext;
        private String mFilename;
        private String mPath;
        private String mPhotoId;

        public CopyImageTask(ImageTakingActivity context, String path, String photoId) {
            mContext = new WeakReference<ImageTakingActivity>(context);
            mFilename = photoId + ".jpg";
            mPath = path;
            mPhotoId = photoId;
        }

        @Override
        protected String doInBackground(String... params) {
            String result = null;
            if (!isCancelled()) {
                String existingPath = params[0];
                File existingFile = new File(existingPath);
                if (existingFile.exists()) {
                    File file = new File(mPath, mFilename);
                    FileChannel fos;
                    FileChannel fin;
                    try {
                        fin = (new FileInputStream(existingFile)).getChannel();
                        fos = (new FileOutputStream(file)).getChannel();
                        fos.transferFrom(fin, 0, fin.size());
                        fin.close();
                        fos.close();
                        result = mPath + "/" + mFilename;
                    } catch (IOException e) {
                        e.printStackTrace();
                        result = null;
                    }
                } else {
                	Ln.d("SOURCE FILE NOT EXIST");
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (mContext != null && mContext.get() != null) {
                ImageTakingActivity imagePreviewActivity = mContext.get();
                imagePreviewActivity.goToPreview(result, mPhotoId, true);
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
        	Ln.d("[ImageTakingAct] location change = " + location.getLatitude());
            getGuidePhotoBySites();
        }
    };

    private void getGuidePhotoBySites() {
    	mLocationUtil.getLocation(locationListener);
    }
    
    private class UpdateSiteReceiver extends BroadcastReceiver{
    	private WeakReference<ImageTakingActivity> mContext;
    	
    	public UpdateSiteReceiver(ImageTakingActivity context){
    		this.mContext = new WeakReference<>(context);
    		Ln.d("create receiver");
    	}
    	
		@Override
		public void onReceive(Context context, Intent intent) {
			Ln.d("UpdateSiteReceiver onReceive");
			if(intent == null || intent.getAction() == null || mContext == null || mContext.get() == null){
				return;
			}
			
			Ln.d("action = " + intent.getAction());
			// update site
			if(LocationService.UPDATE_SITE_ACTION.equalsIgnoreCase(intent.getAction())){
				if(mCurrentDirection != null){
				getAndShowGuidePhoto(mContext.get());
				}
			}
		}		
    }  
    
    private volatile boolean isLoadingGuide;
    
    private void getAndShowGuidePhoto(Context context) {
    	if(isLoadingGuide){
    		return;
    	}
    	
    	isLoadingGuide = true;
    	
    	if(mCurrentDirection == null){
    		Ln.d("current direction is null");
    		isLoadingGuide = false;
    		showNoGuide();
    		return;
    	}  
    	
    	Ln.d("getAndShowGuidePhoto ");
    	Site site = selectedSite;
    	if(site == null) {
            mBestSite = GlobalState.getBestSite();
            if (mBestSite == null) {
                // try get best site again
                Ln.d("best site is null, try get again");
                mBestSite = UIUtils.getBestSite(GlobalState.getSites(), mLocationUtil.getLocation(locationListener), context);
            }

            if (mBestSite == null) {
                Ln.d("best site still null");
                showNoGuide();
                isLoadingGuide = false;
                return;
            }

            site = mBestSite;
        }
		
		CacheService mCacheService = CacheService.getInstance(
				context, CacheService.createDBNameFromUser(Config.getActiveServer(context), Config.getActiveUser(context)));
		GuidePhoto guidePhoto = mCacheService.getGuidePhotoBySiteId(site.getSiteId(), mCurrentDirection);
		if (guidePhoto != null) {
		    mGuidePhotoPath = guidePhoto.getPhotoPath();
		    mGuidePhotoDirection = DIRECTION.getDirection(guidePhoto.getPhotoDirection());
		} else {
			Ln.d("guide photo is null");
			showNoGuide();
		    isLoadingGuide = false;
		    return;
		}

		if (mGuidePhotoDirection != null) {
		    if (mGuidePhotoDirection != mCurrentDirection) {
		    	Ln.d("guide photo don't match direction");
		    	showNoGuide();
		    } else {
		        displayGuidePhoto(isGuideOn);
		    }
		}

        if(allGuidePhotos.isEmpty()){
            getAllGuidePhotos(context);
        }

		isLoadingGuide = false;
	}

    private void getAllGuidePhotos(Context context){
        Site site = selectedSite != null? selectedSite : GlobalState.getBestSite();
        if (site != null) {
            CacheService mCacheService = CacheService.getInstance(
                    context, CacheService.createDBNameFromUser(Config.getActiveServer(context), Config.getActiveUser(context)));

            for (DIRECTION dir : DIRECTION.values()) {
                GuidePhoto guidePhoto = mCacheService.getGuidePhotoBySiteId(site.getSiteId(), dir);
                allGuidePhotos.put(dir, guidePhoto);
            }
        }
    }

    private void showNoGuide(){
    	Ln.d("show no guide");
    	displayGuidePhoto(false);
	    mGuideButton.setText(R.string.no_guide);
	    seekOpacityCamera.setVisibility(View.INVISIBLE);
    }
    
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(fromUser){
            mGuideImageView.setAlpha(progress/10.0f);
            currentAlpha = progress/10.0f;
        }
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}    	
}
