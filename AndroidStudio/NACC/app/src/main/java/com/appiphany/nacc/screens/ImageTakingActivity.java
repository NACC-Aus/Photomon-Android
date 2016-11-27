package com.appiphany.nacc.screens;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.List;
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
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
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
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
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
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.LocationUtil;
import com.appiphany.nacc.utils.UIUtils;
import com.appiphany.nacc.utils.UncaughtExceptionHandler;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibrary;
import com.littlefluffytoys.littlefluffylocationlibrary.LocationLibraryConstants;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class ImageTakingActivity extends BaseActivity implements OnClickListener, Callback, PictureCallback, OnSeekBarChangeListener {
	private static final String ALPHA_KEY = "alpha_key";
    private FrameLayout mSurfaceViewLayout;
    private LinearLayout mCameraPanel;
    private ImageButton mTakingPictureBtn;
    private ImageButton mChangeCamBtn;
    private ImageButton mChangeFlashBtn;
    private SurfaceView mSurfaceView;
    private Button mGuideButton;
    
    private ImageView mGuideImageView;

    private SeekBar seekOpacityCamera;

    private RelativeLayout mRlConfigPanel;

    private ProgressDialog mProgressDialog;

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private DIRECTION mCurrentDirection = null;
    private boolean isPreviewing = false;
    private boolean isGuideOn = true;

    private int mCurrentCameraId = CameraInfo.CAMERA_FACING_BACK;
    private List<String> mSupportFlashModes;
    private int mCameraDegrees = 0;
    private String mGuidePhotoPath = null;
    private DIRECTION mGuidePhotoDirection = null;
    private SaveImageTask mTask;
    private CopyImageTask mTaskCopy;
    private LocationUtil mLocationUtil;

    private Site mBestSite;
    private UpdateSiteReceiver updateSiteReceiver;
    private boolean hasRegisterReceiver;
    private float currentAlpha;
    private static final int version = android.os.Build.VERSION.SDK_INT;
    private boolean hasSurface;
    
    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taking_picture_layout);

        mSurfaceViewLayout = (FrameLayout) findViewById(R.id.surface_view_layout);
        mCameraPanel = (LinearLayout) findViewById(R.id.taking_picture_layout);
        mTakingPictureBtn = (ImageButton) findViewById(R.id.taking_picture_btn);
        mChangeCamBtn = (ImageButton) findViewById(R.id.change_cam_btn);
        mChangeFlashBtn = (ImageButton) findViewById(R.id.flash_btn);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mGuideButton = (Button) findViewById(R.id.guide_btn);
        seekOpacityCamera = (SeekBar) findViewById(R.id.seekOpacityCamera);
        mRlConfigPanel = (RelativeLayout) findViewById(R.id.config_panel);

        mTakingPictureBtn = (ImageButton) findViewById(R.id.taking_picture_btn);
        mTakingPictureBtn.setOnClickListener(this);
        mGuideImageView = (ImageView) findViewById(R.id.guide_image_view);
        
        seekOpacityCamera.setIndeterminate(false);
        seekOpacityCamera.setOnSeekBarChangeListener(this);
        
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
            mCurrentCameraId = savedInstanceState.getInt("cameraId", CameraInfo.CAMERA_FACING_BACK);
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(android.R.style.Widget_DeviceDefault_Light_ProgressBar_Large);
        
        if(Config.shouldShowDirectionDialog(this)){
        	showChangeDirectionDialog();
        }        
        
        LocationLibrary.forceLocationUpdate(this);
        LocationLibrary.startAlarmAndListener(this);
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
						if(!mGuidePhotoPath.startsWith("http")){
							ImageLoader.getInstance().displayImage("file://" +  mGuidePhotoPath, mGuideImageView, GeneralUtil.getNewScaleOption(), new SimpleImageLoadingListener() {
								@Override
								public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

								}
							});	
						}else{
							ImageLoader.getInstance().displayImage(mGuidePhotoPath, mGuideImageView, GeneralUtil.getNewScaleOption(),new SimpleImageLoadingListener() {
								@Override
								public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

								}
							});
						}
					}
				}, 700);
                              
                              
                seekOpacityCamera.setVisibility(View.VISIBLE);
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB){
                	seekOpacityCamera.setMax(255);
           			mGuideImageView.setAlpha((int)currentAlpha);
           		}else{
           			seekOpacityCamera.setMax(10);
           			mGuideImageView.setAlpha(currentAlpha);
           		}
                
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

        mHolder = mSurfaceView.getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(this);

        mLocationUtil = new LocationUtil(this);

        if (mCurrentDirection != null) {
        	doDirectionChanged(mCurrentDirection);
        }
        
        registerLocationReceiver();
        
        isFlashOn = false;
        mChangeFlashBtn.setImageResource(R.drawable.ic_device_access_flash_off);
        changeFlashMode(false);
    }
    
	private void registerLocationReceiver() {
		if(!hasRegisterReceiver){
	    	updateSiteReceiver = new UpdateSiteReceiver(this);
	        IntentFilter intentFilter = new IntentFilter(LocationService.UPDATE_SITE_ACTION);
	        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
	        registerReceiver(updateSiteReceiver, intentFilter);
	        
	        final IntentFilter lftIntentFilter = new IntentFilter(LocationLibraryConstants.getLocationChangedPeriodicBroadcastAction());
	        registerReceiver(lftBroadcastReceiver, lftIntentFilter);
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

    private static final int SELECT_IMAGE = 0;

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
                        String[] filePathColumn = { MediaStore.Images.Media.DATA };

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
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop listen location change
        hideLoadingDialog();
    }

	private void unRegisterLocationReceiver() {
		if(hasRegisterReceiver){
        	unregisterReceiver(updateSiteReceiver);
        	unregisterReceiver(lftBroadcastReceiver);
        	hasRegisterReceiver = false;
        }
	}

    @Override
    protected void onStop() {
        super.onStop();
        mHolder.removeCallback(this);
        stopAndReleaseCamera();

        unRegisterLocationReceiver();
        mLocationUtil.stopLocationListener(locationListener);
    }

    @SuppressLint("NewApi")
	private void initCameraPanel() {
        int numOfCamera = 1;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
            numOfCamera = Camera.getNumberOfCameras();
        }

        if (numOfCamera <= 1) {
            mChangeCamBtn.setVisibility(View.GONE);
        } else {
            mChangeCamBtn.setVisibility(View.VISIBLE);
        }

    }

    @SuppressLint("NewApi")
	private void openCamera(int cameraId) {
        stopAndReleaseCamera();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
            mCamera = Camera.open(cameraId);
        } else {
            mCamera = Camera.open();
        }
        mCurrentCameraId = cameraId;
        setUpCamera();
    }

    private void stopAndReleaseCamera() {
        isPreviewing = false;
        if (mCamera != null) {
        	focus(false);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void startCameraPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
            isPreviewing = true;
        } else {
            isPreviewing = false;
        }
        
        focus(true);
    }
    
    private void stopCameraPreview(){
    	if(mCamera != null){
    		mCamera.stopPreview();
    	}
    	
    	isPreviewing = false;
    	focus(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //Handle config change myself
        super.onConfigurationChanged(newConfig);
        setUpCamera();
    }

    private void setUpCameraDegrees() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
        case Surface.ROTATION_0:
            mCameraDegrees = 90;
            break;
        case Surface.ROTATION_90:
            mCameraDegrees = 0;
            break;
        case Surface.ROTATION_180:
            mCameraDegrees = 270;
            break;
        case Surface.ROTATION_270:
            mCameraDegrees = 180;
            break;
        }
        if (mCamera != null) {
        	stopCameraPreview();
            mCamera.setDisplayOrientation(mCameraDegrees);
        }
    }

    @SuppressLint("InlinedApi")
	private void setUpCamera() {
        setUpCameraDegrees();
        Camera.Parameters camParameters = mCamera.getParameters();
        mSupportFlashModes = camParameters.getSupportedFlashModes();
        if (mSupportFlashModes == null || mSupportFlashModes.size() == 0) {
            mChangeFlashBtn.setVisibility(View.INVISIBLE);
        } else {
            if (mCurrentCameraId == CameraInfo.CAMERA_FACING_BACK) {
                mChangeFlashBtn.setVisibility(View.VISIBLE);
            } else {
                mChangeFlashBtn.setVisibility(View.INVISIBLE);
            }
            changeFlashMode(isFlashOn);
        }
        camParameters.setPictureFormat(ImageFormat.JPEG);
        camParameters.setJpegQuality(50);
        List<String> supportedFocusModes = camParameters.getSupportedFocusModes();
        String focusMode = "continuous-picture";
        if(version >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
        	focusMode = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        }
        if (supportedFocusModes != null) {
            if (supportedFocusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                camParameters.setFocusMode(focusMode);
            } else if (supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
                camParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
            }
        }
        List<Size> supportedSizes = camParameters.getSupportedPictureSizes();
        int chosenWidth = 0;
        int chosenHeight = 0;
        for (Size size : supportedSizes) {
            if (size.width > chosenWidth) {
                chosenWidth = size.width;
                chosenHeight = size.height;
            }
        }

        Ln.d("SELECTED PICTURE SIZE " + chosenWidth + " " + chosenHeight);
        camParameters.setPictureSize(chosenWidth, chosenHeight);
        Point screenSize = UIUtils.getScreenResolution(this);
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        
        Point previewSize = UIUtils.findBestPreviewSizeValue(camParameters, screenSize);

        if (previewSize != null) {
        	Ln.d("SELECTED PREVIEW SIZE " + previewSize.x + " " + previewSize.y);
            float aspectRatio = (float) previewSize.x / (float) previewSize.y;
            if (previewSize.x < previewSize.y) {
                aspectRatio = (float) previewSize.y / (float) previewSize.x;
            }
            camParameters.setPreviewSize(previewSize.x, previewSize.y);

            int screenWidth = outMetrics.widthPixels;
            int screenHeight = outMetrics.heightPixels;
            int surfaceWidth = 0;
            int surfaceHeight = 0;
            int panelHeight = 0;
            int panelWidth = 0;
            if (screenWidth > screenHeight) {
                //Landscape
                surfaceHeight = screenHeight;
                surfaceWidth = (int) (screenHeight * aspectRatio);
                panelHeight = screenHeight;
                panelWidth = screenWidth - surfaceWidth;

            } else {
                //potrait
                surfaceWidth = screenWidth;
                surfaceHeight = (int) (screenWidth * aspectRatio);
                panelHeight = screenHeight - surfaceHeight;
                panelWidth = screenWidth;
            }

            Ln.d("SURFACE SIZE " + surfaceWidth + " " + surfaceHeight + " " + panelWidth + " " + panelHeight);
            RelativeLayout.LayoutParams frameLp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(surfaceWidth, surfaceHeight);
            lp.gravity = Gravity.CENTER;
            if (panelWidth != 0 && panelHeight != 0) {
                RelativeLayout.LayoutParams panelLp = new RelativeLayout.LayoutParams(panelWidth, panelHeight);
                if (screenWidth > screenHeight) {
                    frameLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                    frameLp.addRule(RelativeLayout.LEFT_OF, mCameraPanel.getId());
                    panelLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                } else {
                    frameLp.addRule(RelativeLayout.ABOVE, mCameraPanel.getId());
                    panelLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                }
                mSurfaceViewLayout.setLayoutParams(frameLp);
                mCameraPanel.setLayoutParams(panelLp);
            }
        }

        mCamera.setParameters(camParameters);
    }
    
    @Override
    public void onClick(View v) {
    	if(!isPreviewing){
    		startCameraPreview();
    	}
    	
        if (mCamera != null && isPreviewing) {
            mCamera.takePicture(null, null, null, this);
        }
    }

    public void onChangeCam(View v) {
        if (mCurrentCameraId == CameraInfo.CAMERA_FACING_BACK) {
            openCamera(CameraInfo.CAMERA_FACING_FRONT);
            mChangeFlashBtn.setVisibility(View.INVISIBLE);
        } else {
            openCamera(CameraInfo.CAMERA_FACING_BACK);
            if (isFlashOn) {
                changeFlashMode(isFlashOn);
            }
        }
        if (mHolder != null) {
            try {
                mCamera.setPreviewDisplay(mHolder);
                startCameraPreview();
            } catch (IOException e) {
                mCamera.release();
                e.printStackTrace();
            }
        }
    }

    private void changeFlashMode(boolean isFlashOn) {
        if (mCamera != null) {        	
            Camera.Parameters camParameters = mCamera.getParameters();
            camParameters.setFlashMode(isFlashOn ? Parameters.FLASH_MODE_ON : Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(camParameters);                    
        }
    }

	private void focus(boolean isFocus) {
		if (mCamera == null) {
			return;
		}
		try {
			if (isFocus) {
				if (hasSurface) {

					mCamera.autoFocus(new AutoFocusCallback() {

						@Override
						public void onAutoFocus(boolean success, Camera camera) {

						}
					});

				}
			} else {
				mCamera.cancelAutoFocus();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
    private boolean isFlashOn = false;

    public void onChangeFlash(View v) {
        if (mSupportFlashModes != null && mSupportFlashModes.size() > 0) {
            isFlashOn = !isFlashOn;
            if (isFlashOn) {
                mChangeFlashBtn.setImageResource(R.drawable.ic_device_access_flash_on);
            } else {
                mChangeFlashBtn.setImageResource(R.drawable.ic_device_access_flash_off);
            }
            changeFlashMode(isFlashOn);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera == null) {
                openCamera(mCurrentCameraId);
            }
            mHolder = holder;
            mHolder.setFormat(PixelFormat.TRANSPARENT);
            mCamera.setPreviewDisplay(holder);
            if(!hasSurface){
            	hasSurface = true;
            	focus(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            mCamera.release();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        startCameraPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    	Ln.d("SURFACE DESTROYED");
        stopAndReleaseCamera();
        hasSurface = false;
        focus(false);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
    	Ln.d("call onPictureTaken");
        if (data != null) {
            stopCameraPreview();
            mTakingPictureBtn.setEnabled(false);
            if (mTask != null) {
                mTask.cancel(true);
            }

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
            if (path != null) {
                //showLoadingDialog();
                if (mCurrentCameraId == CameraInfo.CAMERA_FACING_FRONT
                        && (mCameraDegrees == 90 || mCameraDegrees == 270)) {
                    mCameraDegrees = mCameraDegrees + 180;
                }
                mTask = new SaveImageTask(this, path, photoId, mCameraDegrees);
                mTask.execute(data);

            } else {
                UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.storage_low_error, false).show();
            }
        } else {
            startCameraPreview();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("cameraId", mCurrentCameraId);
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
            showPreviewIntent.putExtra(BackgroundService.CAMERA_DEGREES, mCameraDegrees);
            showPreviewIntent.putExtra(BackgroundService.FROM_GALLERY, isSelectFromGallery);
            showPreviewIntent.putExtra(BackgroundService.BEST_SITE, mBestSite);
            startActivity(showPreviewIntent);
            
        } else {
            try {
            	 mCamera.setPreviewDisplay(mHolder);
                startCameraPreview();               
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }    

    private void showChangeDirectionDialog(){
    	mSurfaceView.setVisibility(View.INVISIBLE);
    	final Dialog dialog = new Dialog(this);
    	dialog.setCancelable(true);
    	dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle(R.string.msg_select_direction);
        dialog.setContentView(R.layout.dialog_change_direction);
        int screenwidth = getResources().getDisplayMetrics().widthPixels;
        int width = getResources().getDimensionPixelSize(R.dimen.pop_up_width);
        if(	width > screenwidth){
        	dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }else{
        	dialog.getWindow().setLayout(width, LayoutParams.WRAP_CONTENT);
        }
        dialog.getWindow().setGravity(Gravity.CENTER);
        Button btnNorth = (Button) dialog.findViewById(R.id.btnNorth);
        Button btnSouth = (Button) dialog.findViewById(R.id.btnSouth);
        Button btnEast = (Button) dialog.findViewById(R.id.btnEast);
        Button btnWest = (Button) dialog.findViewById(R.id.btnWest);        
        Button btnPoint = (Button)dialog.findViewById(R.id.btnPoint);
        
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

    			mSurfaceView.setVisibility(View.VISIBLE);  
    			doDirectionChanged(mCurrentDirection);
    			v.setSelected(true);
    			dialog.dismiss();
    			Config.setShowDirectionDialog(ImageTakingActivity.this, false);    
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

    private class SaveImageTask extends AsyncTask<byte[], Void, String> {
        private WeakReference<ImageTakingActivity> mContext;
        private String mFilename;
        private String mPath;
        private int mCameraDegree;
        private String mPhotoId;

        public SaveImageTask(ImageTakingActivity context, String path, String photoId, int cameraDegree) {
            mContext = new WeakReference<>(context);
            mFilename = photoId + ".jpg";
            mPath = path;
            mCameraDegree = cameraDegree;
            mPhotoId = photoId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setOrientation(mCameraDegree);
            startCameraPreview();
        }

        @Override
        protected String doInBackground(byte[]... params) {
            String result = null;
            if (!isCancelled()) {
                byte[] data = params[0];
                if (data != null) {
                    File file = new File(mPath, mFilename);
                    try {
                    	int degree = mCameraDegree % 360;
                        GeneralUtil.saveAndRotateBitmap(data, degree, file.getAbsolutePath(), GlobalState.getCurrentUserLocation());
                        result = file.getAbsolutePath();

                    } catch (Exception e) {
                        e.printStackTrace();
                        result = null;
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (mContext != null && mContext.get() != null) {
                ImageTakingActivity imagePreviewActivity = mContext.get();
                imagePreviewActivity.goToPreview(result, mPhotoId, false);
            }
        }
    }

    private void setOrientation(int cameraDegree) {
        int degree = cameraDegree % 360;
        Ln.d("screen orientation = "
                + getResources().getConfiguration().orientation + " degree = "
                + degree);
        switch (degree) {
        case 0:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            break;
        case 90:
            if (mCurrentCameraId == CameraInfo.CAMERA_FACING_BACK) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            }
            break;
        case 180:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            break;
        case 270:
            if (mCurrentCameraId == CameraInfo.CAMERA_FACING_BACK) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            break;
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
			if(intent == null || intent.getAction() == null || mContext == null && mContext.get() == null){
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
		
		CacheService mCacheService = CacheService.getInstance(
				context, CacheService.createDBNameFromUser(Config.getActiveServer(context), Config.getActiveUser(context)));
		GuidePhoto guidePhoto = mCacheService.getGuidePhotoBySiteId(mBestSite.getSiteId(), mCurrentDirection);
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
		
		isLoadingGuide = false;
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
			if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB){
				mGuideImageView.setAlpha(progress);
				currentAlpha = progress;
			}else{
				mGuideImageView.setAlpha(progress/10.0f);
				currentAlpha = progress/10.0f;
			}
		}
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}    	
	
	private final BroadcastReceiver lftBroadcastReceiver = new BroadcastReceiver() {		
		

		@Override
		public void onReceive(Context context, Intent intent) {
			// extract the location info in the broadcast
			Intent locationIntent = new Intent(ImageTakingActivity.this, LocationService.class);
			locationIntent.addCategory(LocationService.SERVICE_TAG);
			locationIntent.setAction(LocationService.LOCATION_CHANGED);
			locationIntent.putExtra(LocationService.LOCATION_DATA, intent.getSerializableExtra(LocationLibraryConstants.LOCATION_BROADCAST_EXTRA_LOCATIONINFO));
			startService(locationIntent);
		}
	};
}
