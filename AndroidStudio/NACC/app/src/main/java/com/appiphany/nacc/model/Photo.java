package com.appiphany.nacc.model;

import java.io.Serializable;
import java.util.Date;

import android.database.Cursor;

import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.google.gson.annotations.SerializedName;

public class Photo implements Serializable {
    public static final String ID = "photoID";
    /**
     * 
     */
    private static final long serialVersionUID = -6960262224044307597L;   
    private String photoID;
    
    @SerializedName ("ID")
    private String photoServerId;
    
    private String photoName;
    
    @SerializedName ("ImageUrl")
    private String photoPath;
    
    @SerializedName ("SiteId")
    private String siteId;
    
    @SerializedName ("Direction")
    private String direction;
    
    private UPLOAD_STATE uploadState;
    
    @SerializedName ("CreatedAt")
    private Date takenDate;
    
    @SerializedName ("Note")
    private String note;
    
    private float opacity;

    @SerializedName ("ProjectId")
    private String projectId;
    
    public Photo(String photoId,
            String photoServerId,
            String photoPath,
            String photoName,
            String siteId,
            String direction,
            UPLOAD_STATE uploadState,
            Date takenDate,
            String note, Float opacity, String projectId) {
        setPhotoID(photoId);
        setPhotoServerId(photoServerId);
        setPhotoName(photoName);
        setPhotoPath(photoPath);
        setSiteId(siteId);
        setDirection(direction);
        setUploadState(uploadState);
        setTakenDate(takenDate);
        setNote(note);
        setOpacity(opacity);
        setProjectId(projectId);
    }
    
    public Photo(Cursor cur) {
    	this.photoID = cur.getString(cur.getColumnIndex(CacheService.COLUMN_ID));
    	this.photoPath = cur.getString(cur.getColumnIndex(CacheService.COLUMN_PHOTO_PATH));
    	this.photoName = cur.getString(cur.getColumnIndex(CacheService.COLUMN_NAME));
    	this.direction = cur.getString(cur.getColumnIndex(CacheService.COLUMN_DIRECTION));
    	this.photoServerId = cur.getString(cur.getColumnIndex(CacheService.COLUMN_PHOTO_SERVER_ID));
    	this.siteId = cur.getString(cur.getColumnIndex(CacheService.COLUMN_SITE));
    	this.note = cur.getString(cur.getColumnIndex(CacheService.COLUMN_NOTE));
    	long imageTime = cur.getLong(cur.getColumnIndex(CacheService.COLUMN_TAKEN_DATE));
    	this.takenDate = new Date(imageTime);
    	UPLOAD_STATE uploadStateVal = UPLOAD_STATE.valueOf(cur.getInt(cur.getColumnIndex(CacheService.COLUMN_STATE)));
    	this.uploadState = uploadStateVal;
    	this.opacity = cur.getFloat(cur.getColumnIndex(CacheService.COLUMN_OPACITY));
    	this.projectId = cur.getString(cur.getColumnIndex(CacheService.COLUMN_PROJECT));
    }

    public String getPhotoID() {
        return photoID;
    }

    public void setPhotoID(String photoID) {
        this.photoID = photoID;
    }

    public String getPhotoServerId() {
        return photoServerId;
    }

    public void setPhotoServerId(String photoServerId) {
        this.photoServerId = photoServerId;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public UPLOAD_STATE getUploadState() {
        return uploadState;
    }

    public void setUploadState(UPLOAD_STATE uploadState) {
        this.uploadState = uploadState;
    }

    public Date getTakenDate() {
        return takenDate;
    }

    public void setTakenDate(Date takenDate) {
        this.takenDate = takenDate;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public float getOpacity() {
		return opacity;
	}

	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public enum DIRECTION {
        NORTH("North"),
        SOUTH("South"),
        EAST("East"),
        WEST("West"),
        POINT("Photo Point");
        private String mValue;

        private DIRECTION(String value) {
            mValue = value;
        }

        public String getValue() {
            return mValue;
        }

        @Override
        public String toString() {
            return mValue;
        }

        public static DIRECTION getDirection(String input) {
            if (input != null) {
                for (DIRECTION direction : DIRECTION.values()) {
                    if (direction.getValue().toLowerCase().equals(input.toLowerCase())) {
                        return direction;
                    }
                }
            }
            return null;
        }
    }
}
