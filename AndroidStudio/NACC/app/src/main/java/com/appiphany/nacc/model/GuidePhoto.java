package com.appiphany.nacc.model;

import android.database.Cursor;

public class GuidePhoto {
    public static final String TABLE_NAME = "guide_photo";
    public static final String GUIDE_ID = "guide_id";
    public static final String GUIDE_PHOTO_ID = "photo_id";
    public static final String GUIDE_PHOTO_PATH = "photo_path";
    public static final String GUIDE_PHOTO_DIRECTION = "photo_direction";
    public static final String SITE_ID = "site_id";
    public static final String PROJECT_ID = "project_id";

    private int id;
    private String photoId;
    private String photoPath;
    private String photoDirection;
    private String siteId;
    private String projectId;

    public GuidePhoto() {

    }

    public GuidePhoto(Cursor cur) {
        this.id = cur.getInt(cur.getColumnIndex(GUIDE_ID));
        this.photoId = cur.getString(cur.getColumnIndex(GUIDE_PHOTO_ID));
        this.photoPath = cur.getString(cur.getColumnIndex(GUIDE_PHOTO_PATH));
        this.photoDirection = cur.getString(cur.getColumnIndex(GUIDE_PHOTO_DIRECTION));
        this.siteId = cur.getString(cur.getColumnIndex(SITE_ID));
        this.projectId = cur.getString(cur.getColumnIndex(PROJECT_ID));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getPhotoDirection() {
        return photoDirection;
    }

    public void setPhotoDirection(String photoDirection) {
        this.photoDirection = photoDirection;
    }

	public String getSiteId() {
		return siteId;
	}

	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
}
