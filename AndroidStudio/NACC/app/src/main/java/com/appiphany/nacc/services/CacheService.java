package com.appiphany.nacc.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.appiphany.nacc.model.GuidePhoto;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Photo.DIRECTION;
import com.appiphany.nacc.model.Project;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.Ln;

public class CacheService extends SQLiteOpenHelper {
    private static final String TABLE_NAME_PHOTO = "photos_record";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PHOTO_SERVER_ID = "photo_server_id";
    public static final String COLUMN_NAME = "photo_name";
    public static final String COLUMN_PHOTO_PATH = "photo_path";
    public static final String COLUMN_TAKEN_DATE = "taken_date";
    public static final String COLUMN_DIRECTION = "direction";
    public static final String COLUMN_SITE = "siteId";
    public static final String COLUMN_STATE = "state";
    public static final String COLUMN_NOTE = "note";
    public static final String COLUMN_OPACITY = "opacity";
    public static final String COLUMN_PROJECT = "project_id";
    
    public static final int DATABASE_VERSION = 4;
    public static final String DATABASE_CREATE_PHOTO = "create table "
            + TABLE_NAME_PHOTO + "(" + COLUMN_ID + " string primary key, "
            + COLUMN_PHOTO_SERVER_ID + " text not null, "
            + COLUMN_PHOTO_PATH + " text not null, "
            + COLUMN_NAME + " text not null, "
            + COLUMN_SITE + " text not null, "
            + COLUMN_TAKEN_DATE + " long not null, "
            + COLUMN_DIRECTION + " text not null, "
            + COLUMN_NOTE + " text not null, "
            + COLUMN_STATE + " int not null, "
            + COLUMN_PROJECT + " text, "
            + COLUMN_OPACITY + " real not null "
            + ");";

    public static final String DATABASE_CREATE_SITE = "create table "
            + Site.TABLE_NAME + "("
            + Site.SITE_ID + " text primary key, "
            + Site.SITE_NAME + " text not null, "
            + Site.LATITUDE + " text not null, "
            + Site.PROJECT_ID + " text, "
            + Site.LONGIDUTE + " text not null "
            + ");";

    public static final String DATABASE_CREATE_GUIDE = "create table "
            + GuidePhoto.TABLE_NAME + "("
            + GuidePhoto.GUIDE_ID + " integer primary key autoincrement, "
            + GuidePhoto.GUIDE_PHOTO_ID + " text, "
            + GuidePhoto.GUIDE_PHOTO_PATH + " text, "
            + GuidePhoto.GUIDE_PHOTO_DIRECTION + " text, "
            + GuidePhoto.PROJECT_ID + " text, "
            + GuidePhoto.SITE_ID + " text "
            + ");";

    public static final String DATABASE_CREATE_PROJECT = "create table "
    		+ Project.TABLE_NAME + "("
    		+ Project.ID + " text primary key, "
    		+ Project.NAME + " text not null "
    				+ ")";
    
    public static final String DATABASE_DELETE = "drop table " + TABLE_NAME_PHOTO + ";";
    public static final String DATABASE_DELETE_SITE = "drop table " + Site.TABLE_NAME + ";";
    public static final String DATABASE_DELETE_GUIDE = "drop table " + GuidePhoto.TABLE_NAME + ";";
    public static final String DATABASE_DELETE_PROJECT = "drop table " + Project.TABLE_NAME + ";";
    
    private static String mMyDatabaseName;
    private static Context mContext;
    private static SQLiteDatabase database;
    private static SQLiteDatabase databaseDemo;

    public CacheService(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
        mMyDatabaseName = name;
        mContext = context;
        Ln.d("create cache service for db %s", name);
    }
    
    public SQLiteDatabase getDB() {
    	return getWritableDatabase();
    }
    
    private static SQLiteDatabase getDatabase() {
        if (Config.isDemoMode(mContext)) {
        	if (databaseDemo == null) {
        		databaseDemo = new CacheService(mContext, mMyDatabaseName).getDB();
            } else {
            	if (!databaseDemo.isOpen()) {
            		databaseDemo = new CacheService(mContext, mMyDatabaseName).getDB();
                }
            }
        	return databaseDemo;
        } 
        
	    if (database == null) {
	        	database = new CacheService(mContext, mMyDatabaseName).getDB();
        } else {
        	if (!database.isOpen()) {
        		database = new CacheService(mContext, mMyDatabaseName).getDB();
            }
        }
        
        return database;
    }

    public void cleanUp(){
    	tearDown();    	
    }
    
    public static void tearDown(){
    	if(database != null){
    		database.close();
    	}
    	
    	mMyDatabaseName = null;
    	_instance = null;
    	database = null;    	
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
    	Ln.d("CREATE DB WITH " + getMyDatabaseName());
        db.execSQL(DATABASE_CREATE_PHOTO);
        db.execSQL(DATABASE_CREATE_SITE);
        db.execSQL(DATABASE_CREATE_GUIDE);
        db.execSQL(DATABASE_CREATE_PROJECT);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            db.execSQL(DATABASE_DELETE);
            db.execSQL(DATABASE_DELETE_SITE);
            db.execSQL(DATABASE_DELETE_GUIDE);
            db.execSQL(DATABASE_DELETE_PROJECT);
        }
        db.execSQL(DATABASE_CREATE_PHOTO);

    }

    public boolean addNewPhoto(Photo photo) {
        return addNewPhoto(photo.getPhotoID(), photo.getPhotoServerId(), photo.getPhotoPath(), photo.getPhotoName(),
                photo.getDirection(),
                photo.getSiteId(),
                photo.getTakenDate(), photo.getUploadState(), photo.getNote(), photo.getOpacity(), photo.getProjectId());
    }

    public boolean addNewPhoto(String photoId,
            String photoServerId,
            String photoPath,
            String photoName,
            String direction,
            String siteId,
            Date takenDate,
            UPLOAD_STATE state,
            String note, float opacity, String projectId) {
        SQLiteDatabase db = getDatabase();
        ContentValues content = new ContentValues();
        content.put(COLUMN_ID, photoId);
        content.put(COLUMN_PHOTO_SERVER_ID, photoServerId);
        content.put(COLUMN_PHOTO_PATH, photoPath);
        content.put(COLUMN_NAME, photoName);
        content.put(COLUMN_DIRECTION, direction);
        content.put(COLUMN_SITE, siteId);
        content.put(COLUMN_TAKEN_DATE, takenDate == null? new Date().getTime(): takenDate.getTime());
        content.put(COLUMN_STATE, state.getValue());
        content.put(COLUMN_NOTE, note);
        content.put(COLUMN_OPACITY, opacity);
        content.put(COLUMN_PROJECT, projectId);
        long rowId = db.insert(TABLE_NAME_PHOTO, null, content);
        return rowId != -1;
    }

    public boolean updatePhoto(Photo photo) {
        SQLiteDatabase db = getDatabase();
        ContentValues content = new ContentValues();
        content.put(COLUMN_ID, photo.getPhotoID());
        content.put(COLUMN_PHOTO_SERVER_ID, photo.getPhotoServerId());
        content.put(COLUMN_PHOTO_PATH, photo.getPhotoPath());
        content.put(COLUMN_NAME, photo.getPhotoName());
        content.put(COLUMN_DIRECTION, photo.getDirection());
        content.put(COLUMN_SITE, photo.getSiteId());
        content.put(COLUMN_TAKEN_DATE, photo.getTakenDate() == null? new Date().getTime(): photo.getTakenDate().getTime());
        content.put(COLUMN_STATE, photo.getUploadState().getValue());
        content.put(COLUMN_NOTE, photo.getNote());
        content.put(COLUMN_OPACITY, photo.getOpacity());
        content.put(COLUMN_PROJECT, photo.getProjectId());
        long rowId = db.update(TABLE_NAME_PHOTO, content, COLUMN_ID + "= ?", new String[]{photo.getPhotoID()});
        return rowId > 0;
    }

    public void closeDatabase() {
        SQLiteDatabase db = getDatabase();
        if (db != null) {
            db.close();
        }
        
        db = null;
    }
    
    public boolean checkExistPhoto(String serverPhotoId){
    	return checkRecordExist(TABLE_NAME_PHOTO, new String[] {COLUMN_PHOTO_SERVER_ID}, new String[]{serverPhotoId});
    }
    
    private boolean checkRecordExist(String tableName, String[] keys, String [] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            sb.append(keys[i])
                .append("=\"")
                .append(values[i])
                .append("\" ");
            if (i<keys.length-1) sb.append("AND ");
        }

        Cursor cursor = database.query(tableName, null, sb.toString(), null, null, null, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }


    public int deletePhoto(String photoId) {
        SQLiteDatabase db = getDatabase();
        int rows = db.delete(TABLE_NAME_PHOTO, COLUMN_ID + " = \"" + photoId + "\"", null);
        return rows;
    }
    
    public int deleteSite(String siteId) {
        SQLiteDatabase db = getDatabase();
        int rows = db.delete(Site.TABLE_NAME, Site.SITE_ID + " = \"" + siteId + "\"", null);
        return rows;
    }
    
    public Site getSite(String siteId){
    	SQLiteDatabase db = getDatabase();
    	Cursor cur = db.query(Site.TABLE_NAME, null, Site.SITE_ID + " = \"" + siteId + "\"", null, null, null, null);
    	if(cur.moveToFirst()){    	
    		return new Site(cur);
    	}
    	
    	return null;
    }
    
    public int deletePhotosBySite(String siteId) {
        SQLiteDatabase db = getDatabase();
        int rows = db.delete(TABLE_NAME_PHOTO, COLUMN_SITE + " = \"" + siteId + "\"", null);
        return rows;
    }
    
    public int deleteGuidePhotosBySite(String siteId) {
        SQLiteDatabase db = getDatabase();
        int rows = db.delete(GuidePhoto.TABLE_NAME, GuidePhoto.SITE_ID + " = \"" + siteId + "\"", null);
        return rows;
    }
    
    public int updateSiteName(String siteId, String newName) {
        SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put(Site.SITE_NAME, newName);
        int rows = db.update(Site.TABLE_NAME, values, Site.SITE_ID + " = \"" + siteId + "\"", null);
        return rows;
    }
    
    public int updatePhotoNameBySiteId(String siteId, String newName){
    	 SQLiteDatabase db = getDatabase();
         ContentValues values = new ContentValues();
         values.put(COLUMN_NAME, newName);
         int rows = db.update(TABLE_NAME_PHOTO, values, COLUMN_SITE + " = \"" + siteId + "\"", null);
         return rows;
    }

    public int updateState(String photoId, UPLOAD_STATE state) {
        SQLiteDatabase db = getDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATE, state.getValue());
        
        int rows = db.update(TABLE_NAME_PHOTO, values, COLUMN_ID + " = \"" + photoId + "\"", null);
        return rows;
    }

    public int updatePhotoServerId(String photoId, String photoServerId) {
        SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHOTO_SERVER_ID, photoServerId);
        int rows = db.update(TABLE_NAME_PHOTO, values, COLUMN_ID + " = \"" + photoId + "\"", null);
        return rows;
    }

    public UPLOAD_STATE getState(String photoId) {
        int resultState = -1;
        SQLiteDatabase db = getDatabase();
        Cursor cursor = db.query(TABLE_NAME_PHOTO, null, COLUMN_ID + " = \"" + photoId + "\"", null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToPosition(0);
                resultState = cursor.getInt(cursor.getColumnIndex(COLUMN_STATE));
            }
        }
        
        if(cursor != null){
        	cursor.close();
        }

        return (resultState != -1) ? UPLOAD_STATE.valueOf(resultState) : null;

    }

    public Cursor getPhotos(String projectId) {
        SQLiteDatabase db = getDatabase();
        return db.query(TABLE_NAME_PHOTO, null, COLUMN_PROJECT + " = \"" + projectId + "\"", null, null, null, COLUMN_TAKEN_DATE + " desc");
    }
    
    public List<Photo> getAllPhotos(){
    	List<Photo> result = null;
    	SQLiteDatabase db = getDatabase();    	
        Cursor cursor = db.query(TABLE_NAME_PHOTO, null, null, null, null, null, COLUMN_TAKEN_DATE + " desc");
        if(cursor != null && cursor.getCount() > 0){
        	result = new ArrayList<Photo>();
        	for (int i = 0; i < cursor.getCount(); i++) {
        		if (cursor.moveToPosition(i)) {
        			Photo photo = new Photo(cursor);
        			result.add(photo);
        		}
        	}
        }
        
        if(cursor != null){
        	cursor.close();
        }
        
        return result;
    }

    public List<Photo> getNotUploadedPhotos() {
        SQLiteDatabase db = getDatabase();
        Cursor cursor = db.query(TABLE_NAME_PHOTO, null, COLUMN_STATE + " != \"" + UPLOAD_STATE.UPLOADED.getValue() + "\"",
                null,
                null, null, COLUMN_TAKEN_DATE + " desc");
        List<Photo> result = new ArrayList<Photo>();
        for (int i = 0; i < cursor.getCount(); i++) {
            if (cursor.moveToPosition(i)) {
                String photoPath = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_PHOTO_PATH));
                String photoId = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_ID));
                String photoServerId = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_PHOTO_SERVER_ID));
                String photoName = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_NAME));
                DIRECTION imageDirection = DIRECTION.getDirection(cursor.getString(cursor
                        .getColumnIndex(CacheService.COLUMN_DIRECTION)));
                long photoDate = cursor.getLong(cursor.getColumnIndex(CacheService.COLUMN_TAKEN_DATE));
                UPLOAD_STATE uploadStateVal = UPLOAD_STATE.valueOf(cursor.getInt(cursor
                        .getColumnIndex(CacheService.COLUMN_STATE)));
                String siteId = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_SITE));
                String note = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_NOTE));
                Float opacity = cursor.getFloat(cursor.getColumnIndex(CacheService.COLUMN_OPACITY));
                String projectId = cursor.getString(cursor.getColumnIndex(CacheService.COLUMN_PROJECT));
                
                Photo photo = new Photo(photoId, photoServerId, photoPath, photoName, siteId,
                        imageDirection.getValue(),
                        uploadStateVal, new Date(photoDate), note, opacity, projectId);
                
                result.add(photo);
            }
        }
        
        if(cursor != null){
        	cursor.close();
        }

        return result;
    }

    public enum UPLOAD_STATE {
        UPLOADED(1),
        UPLOADING(2),
        NOT_UPLOAD(3),
        DOWNLOAD(4);
        int mValue;

        UPLOAD_STATE(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static UPLOAD_STATE valueOf(int value) {
            switch (value) {
            case 1:
                return UPLOADED;
            case 2:
                return UPLOADING;
            case 3:
                return NOT_UPLOAD;
            case 4:
            	return DOWNLOAD;
            }
            return NOT_UPLOAD;
        }
    }

    public static String getNewPhotoID() {
        return UUID.randomUUID().toString();
    }

    public static String createDBNameFromUser(String server, String user) {
        if (server != null && user != null) {
            MessageDigest digest;
            String result = null;
            try {
                digest = MessageDigest.getInstance("md5");
                digest.update(server.getBytes());
                digest.update(user.getBytes());
                byte[] data = digest.digest();
                StringBuffer stringBuffer = new StringBuffer("");
                for (byte datum : data) {
                    stringBuffer.append(Integer.toString((datum & 0xff) + 0x100, 16).substring(1));
                }
                result = stringBuffer.toString() + ".db";
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return result;
        }
        return null;
    }

    private static CacheService _instance;
    private static Object _lock = new Object();

    public static CacheService getInstance(Context context, String dbName) {
    	Ln.d("get instance cache service for db %s", dbName);
        if (dbName != null
                && (_instance == null || _instance.getMyDatabaseName() == null || !_instance.getMyDatabaseName()
                        .equals(dbName))) {
            synchronized (_lock) {
                _instance = new CacheService(context, dbName);
            }

        }
        return _instance;
    }

    public String getMyDatabaseName() {
        return mMyDatabaseName;
    }

    public List<String> getAllSiteIdInGuide() {
    	SQLiteDatabase db = getDatabase();
        List<String> siteIds = new ArrayList<String>();
        Cursor cur = db.query(GuidePhoto.TABLE_NAME, null, null, null, null, null, null);
        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            while (!cur.isAfterLast()) {
                String siteId = cur.getString(cur.getColumnIndex(GuidePhoto.SITE_ID));
                siteIds.add(siteId);
                cur.moveToNext();
            }

        }
        
        if(cur != null){
        	cur.close();
        }

        return siteIds;
    }
    
    public void insertOrUpdateGuidePhoto(Context context, String photoId, String photoPath, DIRECTION photoDirection, String siteId, String projectId) {

        SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put(GuidePhoto.GUIDE_PHOTO_ID, photoId);
        values.put(GuidePhoto.GUIDE_PHOTO_PATH, photoPath);
        values.put(GuidePhoto.GUIDE_PHOTO_DIRECTION, photoDirection.getValue());
        values.put(GuidePhoto.SITE_ID, siteId);
        values.put(GuidePhoto.PROJECT_ID, projectId);

        // if photo direction exists in database then update photo id
        // else insert new record
        if (isExistGuidePhoto(photoDirection.getValue(), siteId)) {
            db.update(GuidePhoto.TABLE_NAME, values,
                    GuidePhoto.GUIDE_PHOTO_DIRECTION + " = '" + photoDirection.getValue() + "'", null);
        } else {
            db.insert(GuidePhoto.TABLE_NAME, null, values);
        }
    }
    
    public void insertGuidePhoto(Context context, String photoId, String photoPath, DIRECTION photoDirection, String siteId, String projectId){
    	SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put(GuidePhoto.GUIDE_PHOTO_ID, photoId);
        values.put(GuidePhoto.GUIDE_PHOTO_PATH, photoPath);
        values.put(GuidePhoto.GUIDE_PHOTO_DIRECTION, photoDirection.getValue());
        values.put(GuidePhoto.SITE_ID, siteId);
        values.put(GuidePhoto.PROJECT_ID, projectId);
        db.insert(GuidePhoto.TABLE_NAME, null, values);
    }

    public boolean deleteGuidePhoto(String photoId) {
        SQLiteDatabase db = getDatabase();
        int row = db.delete(GuidePhoto.TABLE_NAME, GuidePhoto.GUIDE_PHOTO_ID + " = '" + photoId + "'", null);
        return row > 0;
    }

    public List<String> getGuidePhotoIds() {
        SQLiteDatabase db = getDatabase();
        List<String> photoIds = new ArrayList<String>();
        Cursor cur = db.query(GuidePhoto.TABLE_NAME, null, null, null, null, null, null);
        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            while (!cur.isAfterLast()) {
                String photoId = cur.getString(cur.getColumnIndex(GuidePhoto.GUIDE_PHOTO_ID));
                photoIds.add(photoId);
                cur.moveToNext();
            }
        }
        
        if(cur != null){
        	cur.close();
        }

        return photoIds;
    }

    public boolean isExistGuidePhoto(String direction, String siteId) {
    	SQLiteDatabase db = getDatabase();
        boolean isExist = false;
        String whereClause = GuidePhoto.GUIDE_PHOTO_DIRECTION + " = '" + direction + "'" +
        				" AND " + GuidePhoto.SITE_ID + " = '" + siteId + "'";
        Cursor cur = db.query(GuidePhoto.TABLE_NAME, null, whereClause, null, null, null,
                null);
        if (cur != null && cur.getCount() > 0) {
            isExist = true;
        }
        
        if(cur != null){
        	cur.close();
        }

        return isExist;
    }
    
    public Photo getPhotoByPhotoServerId(String serverId){
		Photo ret = null;
		if (TextUtils.isEmpty(serverId)) {
			return null;
		}

		SQLiteDatabase db = getDatabase();
		Cursor cur = db.query(TABLE_NAME_PHOTO, null, COLUMN_PHOTO_SERVER_ID + " = '" + serverId + "'", null, null, null, null);
		if (cur != null && cur.getCount() > 0) {
			cur.moveToFirst();
			ret = new Photo(cur);
		}

		if (cur != null) {
			cur.close();
		}

		return ret;
    }

    public GuidePhoto getGuidePhotoByDirection(DIRECTION direction) {
        if (direction == null) {
            return null;
        }

        SQLiteDatabase db = getDatabase();
        GuidePhoto photo = null;
        Cursor cur = db.query(GuidePhoto.TABLE_NAME, null,
                GuidePhoto.GUIDE_PHOTO_DIRECTION + " = '" + direction.getValue() + "'", null, null, null, null);
        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            photo = new GuidePhoto(cur);
        }
        
        if(cur != null){
        	cur.close();
        }

        return photo;
    }
    
    public GuidePhoto getGuidePhotoBySiteId(String siteId, DIRECTION direction) {
        SQLiteDatabase db = getDatabase();
        GuidePhoto photo = null;
        Cursor cur = db.query(GuidePhoto.TABLE_NAME, null,
                GuidePhoto.SITE_ID + " = '" + siteId + "' AND " + GuidePhoto.GUIDE_PHOTO_DIRECTION + " = '" + direction.getValue() + "'", null, null, null, null);
        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            photo = new GuidePhoto(cur);
        }
        
        if(cur != null){
        	cur.close();
        }

        return photo;
    }

    public String getNoteForPhoto(String photoId) {
        SQLiteDatabase db = getDatabase();
        String note = "";

        Cursor cur = db.query(TABLE_NAME_PHOTO, null, COLUMN_ID + " = '" + photoId + "'", null, null, null, null);

        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            note = cur.getString(cur.getColumnIndex(COLUMN_NOTE));
        }
        
        if(cur != null){
        	cur.close();
        }

        return note;
    }

    public int updateNoteForPhoto(String photoId, String note) {
        SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE, note);
        int rows = db.update(TABLE_NAME_PHOTO, values, COLUMN_ID + " = \"" + photoId + "\"", null);
        return rows;
    }
    
    public int updateOpacityPhoto(String photoId, float opacity) {
        SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_OPACITY, opacity);
        int rows = db.update(TABLE_NAME_PHOTO, values, COLUMN_ID + " = \"" + photoId + "\"", null);
        return rows;
    }

    public boolean isChangeNote(String photoId, String note) {
        String oldNote = getNoteForPhoto(photoId);
        if (oldNote.equalsIgnoreCase(note)) {
            return false;
        }

        return true;
    }

    // =================== for site table ================ 
    public boolean insertSite(Site site) {
        SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put(Site.SITE_ID, site.getSiteId());
        values.put(Site.SITE_NAME, site.getName());
        values.put(Site.LATITUDE, site.getLat() + "");
        values.put(Site.LONGIDUTE, site.getLng() + "");
        values.put(Site.PROJECT_ID, site.getProjectId());

        Ln.d("insertSite lat = " + site.getLat() + " : " + site.getLng());

        long rowId = db.insert(Site.TABLE_NAME, null, values);
        return rowId != -1;
    }
    
    public List<Project> getProjects(){
    	SQLiteDatabase db = getDatabase();
    	List<Project> projects = new ArrayList<Project>();
    	Cursor cur = db.query(Project.TABLE_NAME, null, null, null, null, null, null);

        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            while (!cur.isAfterLast()) {
                Project project = new Project(cur);
                projects.add(project);
                cur.moveToNext();
            }
        }
        
        if(cur != null){
        	cur.close();
        }
        
    	return projects;
    }
    
    public boolean insertProject(Project project){    	
    	SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put(Project.ID, project.getUid());
        values.put(Project.NAME, project.getName());

        long rowId = db.insert(Project.TABLE_NAME, null, values);
        return rowId != -1;
    }
    
    public boolean updateProject(Project project){
    	SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put(Project.NAME, project.getName());

        long rowId = db.update(Project.TABLE_NAME, values, Project.ID + "= ?", new String[]{project.getUid()});
        return rowId > 0;
    }
    
    public int deleteProject() {
        SQLiteDatabase db = getDatabase();
        int rows = db.delete(Project.TABLE_NAME, null, null);
        return rows;
    }

    public List<Site> getAllSite(String projectId) {
        SQLiteDatabase db = getDatabase();
        List<Site> sites = new ArrayList<Site>();

        Cursor cur = db.query(Site.TABLE_NAME, null, Site.PROJECT_ID + " = '" + projectId + "'", null, null, null, null);

        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            while (!cur.isAfterLast()) {
                Site site = new Site(cur);
                sites.add(site);
                cur.moveToNext();
            }
        }
        
        if(cur != null){
        	cur.close();
        }

        return sites;
    }

    public boolean checkSiteNameExist(String siteName) {
        SQLiteDatabase db = getDatabase();
        Cursor cur = db.query(Site.TABLE_NAME, null, Site.SITE_NAME + " = '" + siteName + "'", null, null, null, null);

        if (cur != null && cur.getCount() > 0) {
            cur.close();
            return true;
        }

        return false;
    }

}
