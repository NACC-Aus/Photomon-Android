package com.appiphany.nacc.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Photo;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.services.GuideAdapter;
import com.appiphany.nacc.utils.AbstractDataLoader;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.NetworkUtils;

public class SitesActivity extends BaseFragmentActivity implements LoaderCallbacks<List<Site>>, OnItemClickListener {
	private static final int LOADER_ID = SitesActivity.class.hashCode();
	private GuideAdapter guideAdapter;
	private ListView lvSites;
	private ProgressBar progressLoadSites;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_site_list);
		initActionBar();
		getSupportLoaderManager().initLoader(LOADER_ID, null, this);
		lvSites = (ListView) findViewById(R.id.lvSites);
		progressLoadSites = (ProgressBar) findViewById(R.id.progressLoadSites);
		progressLoadSites.setVisibility(View.VISIBLE);
	}

	private void initActionBar() {
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayUseLogoEnabled(true);
		getSupportActionBar().setTitle(R.string.guides);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 getSupportMenuInflater().inflate(R.menu.sites_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;

		case R.id.mn_site_done:
			if (guideAdapter != null && guideAdapter.getDatasource() != null && guideAdapter.getDatasource().size() > 0) {
				Intent intentService = new Intent(this, DownloadService.class);
				intentService.setAction(DownloadService.DOWNLOAD_GUIDE);				
				intentService.putExtra(DownloadService.SELECTED_SITE_ID, new ArrayList<Site>(guideAdapter.getDatasource()));
				startService(intentService);
			}

			finish();
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	@Override
	public Loader<List<Site>> onCreateLoader(int arg0, Bundle arg1) {
		return new GetSiteLoader(this);
	}

	@Override
	public void onLoadFinished(Loader<List<Site>> arg0, List<Site> result) {
		progressLoadSites.setVisibility(View.INVISIBLE);
		if(result != null){			
			guideAdapter = new GuideAdapter(this, result);
			lvSites.setAdapter(guideAdapter);
			lvSites.setOnItemClickListener(this);
		}else{
			Toast.makeText(this, "There was an error when loading sites, please check network connection and try again later", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onLoaderReset(Loader<List<Site>> arg0) {
		if(guideAdapter != null){
			guideAdapter.clear();
		}
	}

	private static class GetSiteLoader extends AbstractDataLoader<List<Site>> {
		private Context context;
		public GetSiteLoader(Context context) {
			super(context);
			this.context = context;
		}

		@Override
		protected List<Site> buildList() {
			List<Site> sites = GlobalState.getSites();
			
			if (sites == null || sites.size() == 0) {
				sites = NetworkUtils.getAllSite(getContext(), Config.getCurrentProjectId(context));
				GlobalState.setSites(sites);				
			}
			
			if(sites != null && sites.size() > 0){
				// get network photo
				List<Photo> networkPhotos = NetworkUtils.getGuidePhotos(getContext(), Config.getCurrentProjectId(context));
				
				// check and Remove sites which don't have guide photo
				if(networkPhotos != null && networkPhotos.size() > 0){
					for (int i = sites.size() - 1; i >= 0; i--) {
						if(!checkSiteHasGuidePhoto(sites.get(i), networkPhotos)){
							sites.remove(i);
						}
					}
				}
				
				CacheService cacheService = new CacheService(getContext(), CacheService.createDBNameFromUser(Config.getActiveServer(getContext()), Config.getActiveUser(getContext())));
				List<Photo> photos = cacheService.getAllPhotos();
				
				if(photos != null && photos.size() > 0){
					for (Photo photo : photos) {
						if(photo.getPhotoPath().startsWith("http://") || photo.getPhotoPath().startsWith("https://")){
							for (Site site : sites) {
								if(photo.getSiteId().equals(site.getSiteId())){
									site.setDownloaded(true);
									site.setSelected(true);
								}
							}
						}
					}
				}
				
				Collections.sort(sites, new Comparator<Site>() {
			        @Override
			        public int compare(final Site object1, final Site object2) {
			            return object1.getName().compareTo(object2.getName());
			        }
			       } );
			}
			
			return sites;
		}
		
		private boolean checkSiteHasGuidePhoto(Site site, List<Photo> serverPhotos){
			for (Photo photo : serverPhotos) {
				if(site.getSiteId().equals(photo.getSiteId())){
					return true;
				}
			}
			
			return false;
		}
	}

	
	
	@Override
	public void onItemClick(AdapterView<?> adapter, View parent, int position, long id) {
		if(guideAdapter != null){
			guideAdapter.toggleCheck(position, parent);
		}		
	}
}
