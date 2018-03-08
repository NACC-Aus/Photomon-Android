package com.appiphany.nacc.model;

import java.io.Serializable;

import android.database.Cursor;

import com.google.gson.annotations.SerializedName;

public class Site implements Serializable {   
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "site";
    public static final String SITE_ID = "site_id";
    public static final String SITE_NAME = "site_name";
    public static final String LATITUDE = "latitute";
    public static final String LONGIDUTE = "longitude";
    public static final String PROJECT_ID = "project_id";

    @SerializedName("ID")
    private String siteId;

    @SerializedName("Name")
    private String mName;

    @SerializedName("Latitude")
    private String latitude;
    @SerializedName("Longitude")
    private String longitude;

    private double mLat;
    private double mLng;
    private boolean isSelected;
    private boolean isDownloaded;

    @SerializedName("ProjectId")
    private String projectId;
    
    public Site(String siteId, String siteName, double lat, double lng, String projectId) {
        this.setSiteId(siteId);
        this.setName(siteName);
        this.setLat(lat);
        this.setLng(lng);
        setProjectId(projectId);
    }

    public Site(Cursor cur) {
        this.siteId = cur.getString(cur.getColumnIndex(SITE_ID));
        this.mName = cur.getString(cur.getColumnIndex(SITE_NAME));
        this.mLat = cur.getDouble(cur.getColumnIndex(LATITUDE));
        this.mLng = cur.getDouble(cur.getColumnIndex(LONGIDUTE));
        this.projectId = cur.getString(cur.getColumnIndex(PROJECT_ID));
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public double getLat() {
        return mLat;
    }

    public void setLat(double lat) {
        mLat = lat;
    }

    public double getLng() {
        return mLng;
    }

    public void setLng(double lng) {
        mLng = lng;
    }

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
	
	public void toggleSelected(){
		this.isSelected = !this.isSelected;
	}

	public boolean isDownloaded() {
		return isDownloaded;
	}

	public void setDownloaded(boolean isDownloaded) {
		this.isDownloaded = isDownloaded;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
}
