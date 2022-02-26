package com.appiphany.nacc.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Photo.DIRECTION;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.UIUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.Date;
import java.util.List;

public class PhotoAdapter extends SimpleCursorAdapter {
    private Context mContext;
    private List<String> mGuidePhotoIds;
    public PhotoAdapter(Context context,
            int layout,
            Cursor c,
            String[] from,
            int[] to,
            int flags,
            List<String> guidePhotoId) {
        super(context, layout, c, from, to, flags);
        mContext = context;
        mGuidePhotoIds = guidePhotoId;
    }

    public void setGuidePhotoId(List<String> guidePhotoIds) {
        mGuidePhotoIds = guidePhotoIds;
        this.notifyDataSetChanged();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return View.inflate(context, R.layout.photo_item_layout, null);
    }

    @SuppressLint("Range")
    @Override
    public void bindView(View convertView, Context arg1, Cursor cursor) {
        TextView imageNameTextView = convertView.findViewById(R.id.photo_text_view);
        TextView imageDetailView = convertView.findViewById(R.id.photo_detail_view);
        ImageView thumbnailView = convertView.findViewById(R.id.image_thumbnail_view);
        final ProgressBar progressBar = convertView.findViewById(R.id.photo_upload_progress_view);
        String photoPath = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_PHOTO_PATH));
        String photoId = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_ID));
        String photoName = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_NAME));
        DIRECTION imageDirection = DIRECTION.getDirection(cursor.getString(cursor
                .getColumnIndex(CacheService.COLUMN_DIRECTION)));
        long photoDate = cursor.getLong(cursor.getColumnIndex(CacheService.COLUMN_TAKEN_DATE));
        UPLOAD_STATE uploadStateVal = UPLOAD_STATE.valueOf(cursor.getInt(cursor
                .getColumnIndex(CacheService.COLUMN_STATE)));
        if (uploadStateVal == UPLOAD_STATE.UPLOADING) {
            progressBar.setVisibility(View.VISIBLE);
            imageNameTextView.setTextColor(Color.BLACK);
        } else if (uploadStateVal == UPLOAD_STATE.UPLOADED) {
            progressBar.setVisibility(View.GONE);
            imageNameTextView.setTextColor(Color.BLACK);
        } else if (uploadStateVal == UPLOAD_STATE.NOT_UPLOAD) {
            progressBar.setVisibility(View.GONE);
            imageNameTextView.setTextColor(Color.RED);
        } else if (uploadStateVal == UPLOAD_STATE.DOWNLOAD) {
            progressBar.setVisibility(View.VISIBLE);
            imageNameTextView.setTextColor(Color.BLACK);
        }
        
        // don't show progress bar with demo mode
        if (Config.isDemoMode(mContext)) {
            progressBar.setVisibility(View.GONE);
            imageNameTextView.setTextColor(Color.BLACK);
        }

        if (mGuidePhotoIds.size() > 0 && mGuidePhotoIds.contains(photoId)) {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.guide_color_bgr));
        } else {
            convertView.setBackgroundColor(Color.WHITE);
        }

        if (uploadStateVal == UPLOAD_STATE.DOWNLOAD) {
            RequestListener<Drawable> listener = new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    progressBar.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    progressBar.setVisibility(View.GONE);
                    return false;
                }
            };

            if(photoPath.startsWith("http")) {
                Glide.with(mContext).load(photoPath).listener(listener).into(thumbnailView);
            }else{
                Glide.with(mContext).load(new File(photoPath)).listener(listener).into(thumbnailView);
            }
        }else{
            if(photoPath.startsWith("http")){
                Glide.with(mContext).load(photoPath).into(thumbnailView);
            }else {
                Glide.with(mContext).load(new File(photoPath)).into(thumbnailView);
            }
        }

        imageNameTextView.setText(photoName);
        convertView.setTag(photoId);
        imageDetailView.setText(imageDirection + " " + UIUtils.getPhotoDate(new Date(photoDate)));
    }
}