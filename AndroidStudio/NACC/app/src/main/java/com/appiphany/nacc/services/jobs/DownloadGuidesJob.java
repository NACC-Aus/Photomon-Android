package com.appiphany.nacc.services.jobs;

import android.content.Context;
import android.content.Intent;

import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Project;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.screens.GlobalState;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.GeneralUtil;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.NetworkUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import static com.appiphany.nacc.screens.DownloadService.DOWNLOAD_GUIDE_FINISH_ACTION;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DownloadGuidesJob extends Worker {
    static final String TAG = "DownloadGuidesJob";

    public DownloadGuidesJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private void addGuidesBySites(CacheService cacheService, Context context, List<Photo> result, List<Site> sites) {
        if (!GeneralUtil.isNullOrEmpty(result) && !GeneralUtil.isNullOrEmpty(sites)) {
            for (Photo photo : result) {
                Photo localPhoto = cacheService.getPhotoByPhotoServerId(photo.getPhotoServerId());
                // check if this photo already in database
                if (localPhoto != null) {
                    Ln.d("photo exist with path %s, direction = %s, siteId = %s ", photo.getPhotoPath(), photo.getDirection(), photo.getSiteId());
                    // if this site and direction don't have any guide photo, then make the image from server as guide
                    localPhoto.setNote(photo.getNote());
                    localPhoto.setPhotoPath(photo.getPhotoPath());
                    localPhoto.setProjectId(photo.getProjectId());
                    localPhoto.setSiteId(photo.getSiteId());
                    cacheService.updatePhoto(localPhoto);
                    cacheService.insertOrUpdateGuidePhoto(context, localPhoto.getPhotoID(),
                            localPhoto.getPhotoPath(), Photo.DIRECTION.getDirection(localPhoto.getDirection()),
                            localPhoto.getSiteId(), localPhoto.getProjectId());

                    continue;
                }

                String photoId = CacheService.getNewPhotoID();
                photo.setPhotoID(photoId);
                photo.setUploadState(CacheService.UPLOAD_STATE.DOWNLOAD);
                photo.setOpacity(getDefaultOpacity());


                Site site = findSiteById(sites, photo.getSiteId());
                if (site != null) {
                    photo.setPhotoName(site.getName());
                    photo.setProjectId(site.getProjectId());
                } else {
                    continue;
                }

                cacheService.addNewPhoto(photo);
                if(!cacheService.isExistGuidePhoto(photo.getDirection(), photo.getSiteId())){
                    cacheService.insertOrUpdateGuidePhoto(context, photo.getPhotoID(), photo.getPhotoPath(), Photo.DIRECTION.getDirection(photo.getDirection()), photo.getSiteId(), site.getProjectId());
                }
            }

        }
    }

    private float getDefaultOpacity(){
        // set default opacity
        final int version = android.os.Build.VERSION.SDK_INT;
        float opacity;
        if (version < android.os.Build.VERSION_CODES.HONEYCOMB) {
            opacity = 255;
        } else {
            opacity = 10.0f;
        }

        return opacity;
    }

    private Site findSiteById(List<Site> sites, String siteId) {
        for (Site site : sites) {
            if (site.getSiteId().equalsIgnoreCase(siteId)) {
                return site;
            }
        }

        return null;
    }

    public static void scheduleJob(Context context) {
        WorkRequest uploadWorkRequest =
                new OneTimeWorkRequest.Builder(DownloadGuidesJob.class)
                        .build();
        WorkManager
                .getInstance(context)
                .enqueue(uploadWorkRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = GlobalState.getInstance();
            CacheService cacheService = new CacheService(context, CacheService.createDBNameFromUser(Config.getActiveServer(context), Config.getActiveUser(context)));
            List<Project> projects = NetworkUtils.getProjects(Config.getActiveServer(context), Config.getAccessToken(context));
            for (Project project : projects) {
                List<Photo> photos = NetworkUtils.getGuidePhotos(context, project.getUid());
                if(GeneralUtil.isNullOrEmpty(photos)) {
                    continue;
                }

                List<Site> sites = NetworkUtils.getAllSite(context, project.getUid());
                if(GeneralUtil.isNullOrEmpty(sites)) {
                    continue;
                }

                addGuidesBySites(cacheService, context, photos, sites);
            }

            Intent sendingintent = new Intent(DOWNLOAD_GUIDE_FINISH_ACTION);
            LocalBroadcastManager.getInstance(context).sendBroadcast(sendingintent);
        }catch (Throwable throwable) {
            Ln.d(throwable);
            FirebaseCrashlytics.getInstance().recordException(throwable);
            return Result.failure();
        }

        return Result.success();
    }
}
