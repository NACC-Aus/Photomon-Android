package com.appiphany.nacc.screens;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.TextUtils;

import com.appiphany.nacc.R;
import com.appiphany.nacc.model.CacheItem;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.MultipartFormHttpContent;
import com.appiphany.nacc.utils.NetworkUtils;
import com.appiphany.nacc.utils.UIUtils;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Background running service.
 * 
 */
public class BackgroundService extends IntentService {
    public static final String UPLOAD_FINISH_ACTION = "com.appiphany.nacc.UPLOAD_FINISH";
    public static final String UPLOAD_ACTION = "com.appiphany.nacc.UPLOAD_ACTION";
    public static final String REMINDER_ACTION = "com.appiphany.nacc.REMINDER_ACTION";
    public static final String CONNECTED_ACTION = "com.appiphany.nacc.REUPLOAD";
    public static final String UPLOAD_NOTE = "com.appiphany.nacc.UPLOAD_NOTE";
    public static final String MARK_GUIDE = "com.appiphany.nacc.MARK_GUIDE";
    public static final String REMOVE_GUIDE = "com.appiphany.nacc.REMOVE_GUIDE";
    public static final String PROCESS_CACHE = "com.appiphany.nacc.PROCESS_CACHE";
    
    public static final String DB_NAME_EXTRA = "db_name_extra";
    public static final String UPLOAD_STATE_EXTRA = "upload_state_extra";
    public static final String PHOTO_ID_EXTRA = "photo_id";
    public static final String SITE_ID_EXTRA = "site_id";
    public static final String PHOTO_CREATED_AT_EXTRA = "created_at";
    public static final String PHOTO_NOTE_EXTRA = "note";
    public static final String CAMERA_DEGREES = "camera_degrees";

    public static final int ERROR_NOTIFICATION = 0;
    public static final int REMINDER_NOTIFICATION = 1;
    /**
     * Extra for photo model.
     */
    public static final String PHOTO_DATA_EXTRA = "photo_data_extra";
    /**
     * Extra for photo path.
     */
    public static final String PHOTO_PATH_EXTRA = "photo_path_extra";

    public static final String CACHES_DATA_EXTRA = "caches_data_extra";

    public static final String DIRECTION_EXTRA = "direction_extra";
    private static final String SERVICE_NAME = "BackgroundService";

    private static final long MAX_RETRY_TIMES = 10;
    private static final long MAX_RETRY_TIME = 10 * 60 * 1000;
    public static final String FROM_GALLERY = "from_gallery";
    public static final String BEST_SITE = "best_site";
    public static final String GUIDE_PHOTO = "guide_photo";
    public static final String GUIDE_PHOTO_ALPHA = "guide_photo_alpha";

    private String mPhotoServerId;
    
    public BackgroundService() {
        super(SERVICE_NAME);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            
            Ln.d("ACTION " + action);
            if (action.equals(UPLOAD_ACTION)) {
                handleUploadIntent(intent);
            } else if (action.equals(REMINDER_ACTION)) {
                handleReminderIntent(intent);
            }

            if (action.equals(UPLOAD_NOTE)) {
                handleUploadNoteIntent(intent);
            }

            if (action.equals(PROCESS_CACHE)) {
                handleCache();
            }

            if(MARK_GUIDE.equals(action)) {
                Photo photoModel = (Photo) intent.getSerializableExtra(BackgroundService.PHOTO_DATA_EXTRA);
                if(!TextUtils.isEmpty(photoModel.getPhotoServerId())) {
                    doUpdateGuide(photoModel.getPhotoServerId(), photoModel.getProjectId(), true);
                }
            } else if (REMOVE_GUIDE.equals(action)) {
                Photo photoModel = (Photo) intent.getSerializableExtra(BackgroundService.PHOTO_DATA_EXTRA);
                if(!TextUtils.isEmpty(photoModel.getPhotoServerId())) {
                    doUpdateGuide(photoModel.getPhotoServerId(), photoModel.getProjectId(), false);
                }
            }
        }
    }
    
    private void handleReminderIntent(Intent intent) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        notificationBuilder.setContentText(getResources().getString(R.string.reminder_notifications));
        notificationBuilder.setContentTitle(getResources().getString(R.string.app_name));
        notificationBuilder.setWhen(System.currentTimeMillis());
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        Intent startActivityIntent = new Intent(this, MainScreenActivity.class);
        startActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, startActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setVibrate(Config.VIBRATE_PATTERN);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(REMINDER_NOTIFICATION);
        mNotificationManager.notify(0, notificationBuilder.build());
    }

    private void handleUploadIntent(Intent intent) {
        Photo photoModel = (Photo) intent.getSerializableExtra(BackgroundService.PHOTO_DATA_EXTRA);
        String imageDataPath = photoModel.getPhotoPath();
        String siteId = photoModel.getSiteId();
        String direction = photoModel.getDirection();
        String photoId = photoModel.getPhotoID();
        String note = photoModel.getNote();
        String takenDate = UIUtils.getPhotoDate(photoModel.getTakenDate());
        String projectId = photoModel.getProjectId();
        String dbName = intent.getStringExtra(BackgroundService.DB_NAME_EXTRA);
        int retryTimes = 0;
        if (doStartUpload(photoId, dbName)) {
            do {
                if (!doUploadPhotos(imageDataPath, siteId, direction, retryTimes, takenDate, note, projectId)) {
                    retryTimes++;
                    doUploadPhotos(imageDataPath, siteId, direction, retryTimes, takenDate, note, projectId);
                } else {
                    doUploadSuccess(photoId, dbName);
                    break;
                }
            } while (true);
        }
    }

    private void handleUploadNoteIntent(Intent intent) {
        Photo photoModel = (Photo) intent.getSerializableExtra(BackgroundService.PHOTO_DATA_EXTRA);
        doUploadPhotoNoteChange(photoModel.getPhotoServerId(), photoModel.getNote(), photoModel.getProjectId());
        long startTime = System.currentTimeMillis();
        long offset = 0;
        int retryTimes = 0;
        do {
            if (!doUploadPhotoNoteChange(photoModel.getPhotoServerId(), photoModel.getNote(), photoModel.getProjectId())) {
                offset = System.currentTimeMillis() - startTime;
                if (offset > MAX_RETRY_TIME || retryTimes > MAX_RETRY_TIMES) {
                    UIUtils.buildAlertDialog(BackgroundService.this, R.string.dialog_title, R.string.upload_note_fail,
                            true);
                    break;
                } else {
                    retryTimes++;
                    doUploadPhotoNoteChange(photoModel.getPhotoServerId(), photoModel.getNote(), photoModel.getProjectId());
                }
            } else {
                break;
            }
        } while (true);
    }

    private boolean doStartUpload(String photoId, String dbName) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent(UPLOAD_FINISH_ACTION);
        intent.putExtra(BackgroundService.PHOTO_ID_EXTRA, photoId);

        CacheService cacheService = new CacheService(this.getApplicationContext(), dbName);
        UPLOAD_STATE currentUploadState = cacheService.getState(photoId);
        if(currentUploadState == UPLOAD_STATE.DOWNLOAD){
        	return false;
        }
        
        if (currentUploadState == null || currentUploadState == UPLOAD_STATE.UPLOADED) {
            intent.putExtra(UPLOAD_STATE_EXTRA, UPLOAD_STATE.UPLOADED.getValue());
            localBroadcastManager.sendBroadcast(intent);
            return false;
        } else {
            cacheService.updateState(photoId, UPLOAD_STATE.UPLOADING);
            intent.putExtra(UPLOAD_STATE_EXTRA, UPLOAD_STATE.UPLOADING.getValue());
            localBroadcastManager.sendBroadcast(intent);
            return true;
        }
    }

    private void doUploadSuccess(String photoId, String dbName) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        Intent intent = new Intent(UPLOAD_FINISH_ACTION);
        intent.putExtra(BackgroundService.PHOTO_ID_EXTRA, photoId);
        intent.putExtra(UPLOAD_STATE_EXTRA, UPLOAD_STATE.UPLOADED.getValue());
        CacheService cacheService = new CacheService(this.getApplicationContext(), dbName);
        cacheService.updateState(photoId, UPLOAD_STATE.UPLOADED);
        cacheService.updatePhotoServerId(photoId, mPhotoServerId);

        Ln.d("doUploadSuccess");
        CacheItem cacheItem = cacheService.getCache(photoId);
        if(cacheItem != null) {
            Ln.d("doUploadSuccess has cache %s, type %d, \ndata %s",
                    cacheItem.getId(), cacheItem.getType(), cacheItem.getData());
            Photo photo = null;
            try{
                photo = new Gson().fromJson(cacheItem.getData(), Photo.class);
            }catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            if(photo != null && doUpdateGuide(mPhotoServerId, photo.getProjectId(), cacheItem.getType() == CacheItem.TYPE_MARK_GUIDE)){
                cacheService.deleteCache(photoId);
            }
        }

        localBroadcastManager.sendBroadcast(intent);
    }

    @SuppressWarnings("unused")
	private void doUploadFail(String photoId, String dbName) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        CacheService cacheService = new CacheService(this.getApplicationContext(), dbName);
        cacheService.updateState(photoId, UPLOAD_STATE.NOT_UPLOAD);

        Intent intent = new Intent(UPLOAD_FINISH_ACTION);
        intent.putExtra(BackgroundService.PHOTO_ID_EXTRA, photoId);
        intent.putExtra(UPLOAD_STATE_EXTRA, UPLOAD_STATE.NOT_UPLOAD.getValue());
        if (!localBroadcastManager.sendBroadcast(intent)) {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
            notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
            notificationBuilder.setContentText(getResources().getString(R.string.unable_to_upload));
            notificationBuilder.setContentTitle(getResources().getString(R.string.app_name));
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
            Intent startActivityIntent = new Intent(this, MainScreenActivity.class);
            startActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, startActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.setContentIntent(pendingIntent);
            notificationBuilder.setVibrate(Config.VIBRATE_PATTERN);
            notificationBuilder.setAutoCancel(true);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(ERROR_NOTIFICATION);
            mNotificationManager.notify(ERROR_NOTIFICATION, notificationBuilder.build());
        }

    }

    private boolean doUploadPhotos(String imageFilePath,
            String siteId,
            String direction,
            int retryTimes,
            String createdAt,
            String note, String projectId) {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        File imageFile = new File(imageFilePath);

        MultipartFormHttpContent httpContent = new MultipartFormHttpContent();
        httpContent.addParam("access_token", Config.getAccessToken(this));
        httpContent.addParam("site_id", siteId);
        httpContent.addParamWithoutEncode("direction", direction);
        httpContent.addParam("project_id", projectId);
        httpContent.addParam("image", "image.jpg", "image/jpg", imageFile);
        httpContent.addParamWithoutEncode("note", note);
        httpContent.addParamWithoutEncode("created_at", createdAt);
        httpContent.addParam("Content-Length", String.valueOf(imageFile.length()));
        Ln.d("retry upload image %s in %d times", imageFilePath, retryTimes);
        boolean result = false;
        try {
            Thread.sleep(1000);
            HttpRequest httpRequest = httpTransport.createRequestFactory().buildPostRequest(
                    new GenericUrl(Config.getActiveServer(this) + "photos.json"),
                    httpContent);
            httpRequest.setConnectTimeout(Config.HTTP_UPLOAD_IMAGE_TIMEOUT);
            httpRequest.setRetryOnExecuteIOException(false);
            httpRequest.setNumberOfRetries(0);
            HttpResponse httpResponse = httpRequest.execute();
            if (httpResponse.getStatusCode() == 200) {
                result = true;
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getContent(), "UTF-8"));
                StringBuilder builder = new StringBuilder();
                for (String line = null; (line = reader.readLine()) != null;) {
                    builder.append(line).append("\n");
                }
                JSONTokener tokener = new JSONTokener(builder.toString());
                JSONObject jsonResult = new JSONObject(tokener);
                mPhotoServerId = jsonResult.getString("ID");
                Ln.d("photo id from server = " + mPhotoServerId);
            }

            Ln.d("doUploadPhotos HTTP RESPONSE " + httpResponse.getStatusCode() + " \n" + httpResponse.parseAsString() + "\n\n");
            httpResponse.ignore();
        } catch (Throwable e) {
            e.printStackTrace();
            Ln.d("[BackgroundService] upload photo error = " + e.getMessage());
            result = false;
        }
        return result;
    }

    private boolean doUploadPhotoNoteChange(String photoId, String note, String projectId) {
    	Ln.d("upload note to server");
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        MultipartFormHttpContent httpContent = new MultipartFormHttpContent();
        httpContent.addParam("access_token", Config.getAccessToken(this));
        httpContent.addParamWithoutEncode("note", note);
        httpContent.addParamWithoutEncode("project_id", projectId);
        httpContent.addParamWithoutEncode("_method", "PUT");
        boolean result = false;
        try {
            HttpRequest httpRequest = httpTransport.createRequestFactory().buildPostRequest(
                    new GenericUrl(Config.getActiveServer(this) + "photos/" + photoId + ".json"),
                    httpContent);
            httpRequest.setConnectTimeout(Config.HTTP_CONNECT_TIMEOUT);
            httpRequest.setRetryOnExecuteIOException(false);
            httpRequest.setNumberOfRetries(0);
            HttpResponse httpResponse = httpRequest.execute();
            if (httpResponse.getStatusCode() == 200) {
                result = true;
            }

            Ln.d("HTTP RESPONSE " + httpResponse.getStatusCode() + " \n" + httpResponse.parseAsString() + "\n\n");
            httpResponse.ignore();
        } catch (Throwable e) {
        	Ln.d("[BackgroundService] error = " + e.getMessage());
            result = false;
        }
        return result;
    }

    private boolean doUpdateGuide(String photoId, String projectId, boolean isGuide) {
        Ln.d("doUpdateGuide %s", String.valueOf(isGuide) );
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        MultipartFormHttpContent httpContent = new MultipartFormHttpContent();
        httpContent.addParam("access_token", Config.getAccessToken(this));
        httpContent.addParamWithoutEncode("project_id", projectId);
        httpContent.addParamWithoutEncode("guide_photo", String.valueOf(isGuide));
        httpContent.addParamWithoutEncode("_method", "PUT");
        boolean result = false;
        try {
            HttpRequest httpRequest = httpTransport.createRequestFactory().buildPostRequest(
                    new GenericUrl(Config.getActiveServer(this) + "photos/" + photoId + ".json"),
                    httpContent);
            Ln.d("url %s & content \n", httpRequest.getUrl().toString());
            Ln.d(httpContent.getParamMaps());
            httpRequest.setConnectTimeout(Config.HTTP_CONNECT_TIMEOUT);
            httpRequest.setRetryOnExecuteIOException(false);
            httpRequest.setNumberOfRetries(0);
            HttpResponse httpResponse = httpRequest.execute();
            if (httpResponse.getStatusCode() == 200) {
                result = true;
            }

            Ln.d("doUpdateGuide HTTP RESPONSE " + httpResponse.getStatusCode() + " \n" + httpResponse.parseAsString() + "\n\n");
            httpResponse.ignore();
        } catch (Throwable e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    private void handleCache() {
        try {

            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            CacheService cacheService = CacheService.getInstance(this,
                    CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));

           ArrayList<CacheItem> cacheItems = cacheService.getCaches();
            Ln.d("handleCache %d", cacheItems.size());
            for (CacheItem item: cacheItems) {
                switch (item.getType()) {
                    case CacheItem.TYPE_SITE: {
                        Map<String, String> params = gson.fromJson(item.getData(), type);
                        Site site = NetworkUtils.addNewSite(this, params.get("project_id"),
                                params.get("name"), params.get("latitude"), params.get("longitude"));
                        if (site != null) {
                            cacheService.deleteCache(item);
                        }
                        break;
                    }
                }
            }
        }catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
