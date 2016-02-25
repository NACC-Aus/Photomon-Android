package com.appiphany.nacc.model;

import android.database.Cursor;

public class Project {
	public static final String TABLE_NAME = "project";
	public static final String ID = "project_id";
	public static final String NAME = "project_name";
	
	private String uid;
	private String name;
	
	public Project(){}
	
	public Project(Cursor cur){
		setUid(cur.getString(cur.getColumnIndex(ID)));
		setName(cur.getString(cur.getColumnIndex(NAME)));
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
}
