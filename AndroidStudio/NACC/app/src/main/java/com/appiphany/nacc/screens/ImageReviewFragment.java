package com.appiphany.nacc.screens;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import roboguice.util.Ln;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Photo.DIRECTION;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.appiphany.nacc.utils.BitmapUtil;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.UIUtils;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ImageReviewFragment extends SherlockFragment implements OnClickListener, OnSeekBarChangeListener {
    private View mRootView;
    private ImageView mReviewImageView;
    private TextView mImageNameView;
    private TextView mImageDescView;
    private Button mGuideBtn;
    private Button mSaveToRollBtn;
    private Button mDeleteBtn;
    private Photo mPhoto;
    private static ProgressDialog mProgressBar;
    private RelativeLayout mInfoLayout;
    private boolean isGuidePhoto = false;
    private boolean isFullscreen = false;
    public static final String PHOTO_MODEL_ARGS = "photo-data";
    private CacheService cacheService;
    private static Context mContext;
    private SeekBar seekOpacity;
    private static final int version = android.os.Build.VERSION.SDK_INT;
    
    static ImageReviewFragment newInstance(Photo photo, Context context) {
        mContext = context;
        ImageReviewFragment f = new ImageReviewFragment();
        Bundle args = new Bundle();
        args.putSerializable(PHOTO_MODEL_ARGS, photo);
        f.setArguments(args);
        return f;
    }

    @SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.review_photo_item_layout, container, false);
        mInfoLayout = (RelativeLayout) mRootView.findViewById(R.id.info_panel);
        mReviewImageView = (ImageView) mRootView.findViewById(R.id.review_image_view);
        mImageNameView = (TextView) mRootView.findViewById(R.id.image_name_view);
        mImageDescView = (TextView) mRootView.findViewById(R.id.image_desc_view);
        mGuideBtn = (Button) mRootView.findViewById(R.id.make_guide_btn);
        mSaveToRollBtn = (Button) mRootView.findViewById(R.id.save_to_roll_btn);
        seekOpacity = (SeekBar) mRootView.findViewById(R.id.seekOpacity);
        mDeleteBtn = (Button) mRootView.findViewById(R.id.delete_btn);
        mProgressBar = new ProgressDialog(getSherlockActivity());
        mProgressBar.setIndeterminateDrawable(getResources().getDrawable(
                com.actionbarsherlock.R.drawable.abs__progress_medium_holo));
        mProgressBar.setMessage("Executing...");
        mProgressBar.setCancelable(false);
        mGuideBtn.setOnClickListener(this);
        mSaveToRollBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
        Bundle args = getArguments();
        if (args != null) {
            mPhoto = (Photo) args.getSerializable(PHOTO_MODEL_ARGS);            
            if (mPhoto != null) {
            	Ln.d("call get image");
            	if(mPhoto.getUploadState() != UPLOAD_STATE.DOWNLOAD){
            		mReviewImageView.setTag(mPhoto.getPhotoPath());
            		ImageLoader.getInstance().displayImage("file:///" + mPhoto.getPhotoPath(), mReviewImageView, GeneralUtil.getNewScaleOption());
            	}else{
            		Bitmap cacheBmp = ImageLoader.getInstance().getMemoryCache().get(mPhoto.getPhotoPath());
            		if(cacheBmp != null){
            			mReviewImageView.setImageBitmap(cacheBmp);
            		}else{
            			ImageLoader.getInstance().displayImage(mPhoto.getPhotoPath(), mReviewImageView, GeneralUtil.getScaleDisplayOption());
            		}
            	}
            	
                mImageNameView.setText(mPhoto.getPhotoName());
                mImageDescView.setText(mPhoto.getDirection() + "\n" + UIUtils.getPhotoDate(mPhoto.getTakenDate()));
                switch (mPhoto.getUploadState()) {
                case NOT_UPLOAD:
                    mImageNameView.setTextColor(Color.RED);
                    break;
                case UPLOADING:
                    mImageNameView.setTextColor(Color.WHITE);
                    break;
                case UPLOADED:
                case DOWNLOAD:
                    mImageNameView.setTextColor(Color.WHITE);
                    break;
                }

                // set text color to white with demo mode
                if (Config.isDemoMode(mContext)) {
                    mImageNameView.setTextColor(Color.WHITE);
                }
            }
        }
        cacheService = CacheService.getInstance(
                getSherlockActivity(),
                CacheService.createDBNameFromUser(Config.getActiveServer(this.getSherlockActivity()),
                        Config.getActiveUser(this.getSherlockActivity())));
        updateGuideButton(cacheService.getGuidePhotoIds());
        seekOpacity.setIndeterminate(false);
        
        if(version < android.os.Build.VERSION_CODES.HONEYCOMB){        	
        	seekOpacity.setMax(0);
        	seekOpacity.setMax(255);
        	seekOpacity.setProgress(0);
        	seekOpacity.setProgress((int)mPhoto.getOpacity());
        	mReviewImageView.setAlpha((int)mPhoto.getOpacity());
        }else{        	
        	seekOpacity.setMax(0);
        	seekOpacity.setMax(10);
        	seekOpacity.setProgress(0);
        	seekOpacity.setProgress((int)mPhoto.getOpacity());
        	mReviewImageView.setAlpha(mPhoto.getOpacity());
        	Ln.d("opacity = %d, id = %s", (int)mPhoto.getOpacity(), mPhoto.getPhotoID());
        }
        
        seekOpacity.setOnSeekBarChangeListener(this);
        return mRootView;
    }
    
    @Override
    public void onResume() {
        super.onResume();

        // hide progress dialog with demo mode
        if (Config.isDemoMode(mContext)) {
            hideLoadingDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    public void updateGuideButton(List<String> currentGuideId) {
        isGuidePhoto = false;
        if (currentGuideId != null && mPhoto != null) {
            if (currentGuideId.contains(mPhoto.getPhotoID())) {
                isGuidePhoto = true;
            }
        }
        if (!isGuidePhoto) {
            if (currentGuideId != null && currentGuideId.size() > 0 
            		&& cacheService.isExistGuidePhoto(mPhoto.getDirection(), mPhoto.getSiteId())) {
                mGuideBtn.setText(R.string.make_guide);
                mGuideBtn.setVisibility(View.INVISIBLE);
            } else {
                mGuideBtn.setVisibility(View.VISIBLE);
                mGuideBtn.setText(R.string.make_guide);
            }
        } else {
            mGuideBtn.setText(R.string.remove_guide);
        }
    }

    public void updateFullscreen(boolean isFullscreen) {
        this.isFullscreen = isFullscreen;
        handleShowImageView();
    }

    public Photo getPhotoData() {
        return mPhoto;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.make_guide_btn:
            handleGuideClick();
            break;
        case R.id.save_to_roll_btn:
            handleSaveToRollClick();
            break;
        case R.id.delete_btn:
            handleDeleteButtonClick();
            break;
        case R.id.review_image_view:
            isFullscreen = !isFullscreen;
            handleShowImageView();
            break;
        }
    }

    private void handleShowImageView() {
        if (mInfoLayout != null) {
            RelativeLayout.LayoutParams lp = (LayoutParams) mReviewImageView.getLayoutParams();
            if (isFullscreen) {
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

                mInfoLayout.setVisibility(View.INVISIBLE);
                getSherlockActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getSherlockActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getSherlockActivity().getSupportActionBar().hide();
            } else {
            	if(mPhoto.getUploadState() != UPLOAD_STATE.DOWNLOAD){
            		mReviewImageView.setTag(mPhoto.getPhotoPath());
            		ImageLoader.getInstance().displayImage("file://" + mPhoto.getPhotoPath(), mReviewImageView,
                        Config.FULL_OPTIONS);
            	}else{
            		Bitmap cacheBmp = ImageLoader.getInstance().getMemoryCache().get(mPhoto.getPhotoPath());
            		if(cacheBmp != null){
            			mReviewImageView.setImageBitmap(cacheBmp);
            		}else{
            			ImageLoader.getInstance().displayImage(mPhoto.getPhotoPath(), mReviewImageView, GeneralUtil.getScaleDisplayOption());
            		}
            	}
                mInfoLayout.setVisibility(View.VISIBLE);
                getSherlockActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getSherlockActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getSherlockActivity().getSupportActionBar().show();
            }
            mReviewImageView.setLayoutParams(lp);
            ((ImageReviewActivity) getSherlockActivity()).notifyFullscreen(isFullscreen);
        }

    }

    private void handleDeleteButtonClick() {
        if (isGuidePhoto) {
            UIUtils.buildAlertDialog(getSherlockActivity(), R.string.dialog_title, R.string.remove_delete_error, false)
                    .show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getSherlockActivity());
            builder.setTitle(R.string.dialog_title);
            builder.setMessage(R.string.remove_delete_confirm);
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
                    if (cacheService.deletePhoto(mPhoto.getPhotoID()) > 0) {
                        Cursor cur = cacheService.getPhotos(Config.getCurrentProjectId(getActivity()));
                        if (cur != null && cur.getCount() > 0) {
                        	ImageReviewActivity context = (ImageReviewActivity) getSherlockActivity(); 
                        	if (context != null) {                        		
                        		context.notifyDataChanged();
                        	}
                        } else {
                        	// finish activity if it has no more photos
                        	getSherlockActivity().finish();
                        }
                    }
                }
            });
            builder.show();
        }
    }


    private void handleSaveToRollClick() {
        SaveToGalleryTask mTask = new SaveToGalleryTask(this);
        mSaveToRollBtn.setEnabled(false);
        mTask.execute(mPhoto);
    }

    private void hideLoadingDialog() {
    	if (mProgressBar.isShowing()) {
    		mProgressBar.dismiss();
    	}
    }

    private void handleFinishSaved(boolean result) {
        mSaveToRollBtn.setEnabled(true);
        if (result) {
            UIUtils.buildAlertDialog(getSherlockActivity(), R.string.dialog_title,
                    R.string.saved_successfully,
                    false)
                    .show();
        } else {
            UIUtils.buildAlertDialog(getSherlockActivity(), R.string.dialog_title,
                    R.string.common_error, false)
                    .show();
        }
    }

    private void handleGuideClick() {
        if (isGuidePhoto) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getSherlockActivity());
            builder.setTitle(R.string.dialog_title);
            builder.setMessage(R.string.remove_guide_confirm);
            builder.setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setNeutralButton(R.string.ok_text, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isGuidePhoto = false;
                    mGuideBtn.setText(R.string.make_guide);
                    cacheService.deleteGuidePhoto(mPhoto.getPhotoID());
                    dialog.dismiss();

                }
            });
            builder.show();
        } else {
            cacheService.insertOrUpdateGuidePhoto(getSherlockActivity(), mPhoto.getPhotoID(), mPhoto.getPhotoPath(),
                    DIRECTION.getDirection(mPhoto.getDirection()), mPhoto.getSiteId(), Config.getCurrentProjectId(getActivity()));
            mGuideBtn.setText(R.string.remove_guide);
            isGuidePhoto = true;
        }
    }

    private static class SaveToGalleryTask extends AsyncTask<Photo, Void, Boolean> {
        private WeakReference<ImageReviewFragment> mContext;

        public SaveToGalleryTask(ImageReviewFragment context) {
            mContext = new WeakReference<ImageReviewFragment>(context);
        }

        @Override
        protected void onPreExecute() {
        	super.onPreExecute();
        	if (!mProgressBar.isShowing()) {
        		mProgressBar.show();
        	}
        }
        
        @Override
        protected Boolean doInBackground(Photo... params) {
            Photo mPhoto = params[0];
            boolean result = false;
            ImageReviewFragment context = null;
            if (mPhoto != null && mContext != null && mContext.get() != null) {
                try {
                    context = mContext.get();
                    Bitmap scaledBitmap = null;
                    if(mPhoto.getPhotoPath().startsWith("http")){
                    	scaledBitmap = ImageLoader.getInstance().loadImageSync(mPhoto.getPhotoPath());
                    }else {                    
                    	scaledBitmap = BitmapUtil.decodeFile(new File(mPhoto.getPhotoPath()), context.getSherlockActivity());
                    }

					String url = MediaStore.Images.Media.insertImage(context.getSherlockActivity().getContentResolver(), scaledBitmap,
                    		mPhoto.getPhotoID(),
                            mPhoto.getDirection() + "\n" + UIUtils.getPhotoDate(mPhoto.getTakenDate()));
                    
                    if(!mPhoto.getPhotoPath().startsWith("http")){
                    	scaledBitmap.recycle();
                    }

                    Ln.d("URL " + url);
                    result = url != null;

                } catch (OutOfMemoryError e) {
                	Ln.d("[ImageReviewFragment] OutOfMemoryError error = " + e.getMessage());
                	result = false;
                }
                catch (Exception e) {
                	Ln.d("[ImageReviewFragment] error = " + e.getMessage());
                	result = false;
				}
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            
            if (mProgressBar.isShowing()) {
        		mProgressBar.dismiss();
        	}
            
            ImageReviewFragment context = mContext.get();
            if (context != null) {
            	context.handleFinishSaved(result);
            }
            
            
        }

    }

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(fromUser){					
			if(version < android.os.Build.VERSION_CODES.HONEYCOMB){
				mReviewImageView.setAlpha(progress);
				cacheService.updateOpacityPhoto(mPhoto.getPhotoID(), progress);
			}else{
				mReviewImageView.setAlpha(progress/10.0f);
				cacheService.updateOpacityPhoto(mPhoto.getPhotoID(), progress);
			}
		}
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {		
	}

}
