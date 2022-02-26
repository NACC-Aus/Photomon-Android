package com.appiphany.nacc.screens;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.appiphany.nacc.events.UpdateProject;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Photo.DIRECTION;
import com.appiphany.nacc.model.Project;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.CacheService.UPLOAD_STATE;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class DownloadService extends IntentService {
	public static final String DOWNLOAD_GUIDE = "com.appiphany.nacc.DOWNLOAD_GUIDE";
	public static final String DOWNLOAD_GUIDE_FINISH_ACTION = "com.appiphany.nacc.DOWNLOAD_GUIDE_FINISH";
	public static final String DB_NAME = "com.appiphany.nacc.DB_NAME";
	public static final String SELECTED_SITE_ID = "com.appiphany.nacc.SELECTED_SITE_ID";
	public static final String UPDATE_PROJECT = "com.appiphany.nacc.UPDATE_PROJECT";

	private volatile boolean isDownloadingGuide;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	public DownloadService() {
		super(DownloadService.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			String action = intent.getAction();
			Ln.d("handle download guide with action = %s", action );
			if (DOWNLOAD_GUIDE.equals(action)) {
				downloadGuidePhotos(intent);
			}else if(UPDATE_PROJECT.equals(action)){
                updateProjects();
            }
		}
	}

	private void downloadGuidePhotos(Intent intent) {
		if (isDownloadingGuide) {
			return;
		}

		try {
			isDownloadingGuide = true;
			@SuppressWarnings("unchecked")
			ArrayList<Site> sites = (ArrayList<Site>) intent.getSerializableExtra(SELECTED_SITE_ID);
			
			if(sites != null && sites.size() > 0){
				List<Photo> result = NetworkUtils.getGuidePhotos(this, sites.get(0).getProjectId());
				
				//List<Site> networkSites = NetworkUtils.getAllSite(this);
				ArrayList<Site> selectedSite = new ArrayList<Site>();
				ArrayList<Site> unselectedSite = new ArrayList<Site>();
				for (Site site : sites) {
					if(site.isSelected()){
						selectedSite.add(site);
					}else{
						unselectedSite.add(site);
					}
				}
	
				removePhotoBySite(unselectedSite);
				addGuidesBySites(result, selectedSite);
				Intent sendingintent = new Intent(DOWNLOAD_GUIDE_FINISH_ACTION);
				LocalBroadcastManager.getInstance(this).sendBroadcast(sendingintent);
			}
			
		} catch (Exception e) {
			Ln.e(e);
		} finally {
			isDownloadingGuide = false;
			DownloadService.this.stopSelf();
		}
	}

	private void removePhotoBySite(ArrayList<Site> unselectedSite){
		if(unselectedSite.size() > 0){
			CacheService cacheService = new CacheService(this, CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
			for (Site site : unselectedSite) {
				cacheService.deleteGuidePhotosBySite(site.getSiteId());
				cacheService.deletePhotosBySite(site.getSiteId());
			}
		}
	}
	
	private void addGuidesBySites(List<Photo> result, List<Site> sites) {
		CacheService cacheService = new CacheService(this, CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
		if (result != null && result.size() > 0 && sites != null && sites.size() > 0) {
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
					cacheService.insertOrUpdateGuidePhoto(this, localPhoto.getPhotoID(),
							localPhoto.getPhotoPath(), DIRECTION.getDirection(localPhoto.getDirection()),
							localPhoto.getSiteId(), localPhoto.getProjectId());
					
					continue;
				}

				String photoId = CacheService.getNewPhotoID();
				photo.setPhotoID(photoId);
				photo.setUploadState(UPLOAD_STATE.DOWNLOAD);
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
					cacheService.insertOrUpdateGuidePhoto(this, photo.getPhotoID(), photo.getPhotoPath(), DIRECTION.getDirection(photo.getDirection()), photo.getSiteId(), site.getProjectId());
				}
			}
				
		}
	}

	private Site findSiteById(List<Site> sites, String siteId) {
		for (Site site : sites) {
			if (site.getSiteId().equalsIgnoreCase(siteId)) {
				return site;
			}
		}

		return null;
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

    private void updateProjects(){
        List<Project> projects = NetworkUtils.getProjects(Config.getActiveServer(this), Config.getAccessToken(this));
        if(projects.size() > 0){
            CacheService cacheService = new CacheService(this,
                    CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
            for (Project project : projects) {
				if(TextUtils.isEmpty(project.getName()) || TextUtils.isEmpty(project.getUid())){
					continue;
				}

                if(!cacheService.insertProject(project)){
                    cacheService.updateProject(project);
                }
            }
        }

        EventBus.getDefault().postSticky(new UpdateProject());
    }
}
