package com.appiphany.nacc.screens;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.appiphany.nacc.R;
import com.appiphany.nacc.model.CacheItem;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.utils.AbstractDataLoader;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.NetworkUtils;
import com.appiphany.nacc.utils.UIUtils;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlineSitesActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<List<Site>> {
    private static final int LOADER_ID = OnlineSitesActivity.class.hashCode();
    private ListView lvSites;
    private List<Site> siteData;
    private ArrayAdapter<String> adapter;
    private Dialog dialog;
    private CacheService cacheService;
    private View progressLoadSites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sites_online);
        progressLoadSites = findViewById(R.id.progressLoadSites);
        initActionBar();
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        lvSites = (ListView) findViewById(R.id.listSites);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_site, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.add_menu) {
            showAddSiteDialog(GlobalState.getCurrentUserLocation());
        }

        return super.onOptionsItemSelected(item);
    }

    private void reloadSites() {
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @SuppressWarnings("ConstantConditions")
    private void initActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setTitle(R.string.site_title);
    }

    @Override
    public Loader<List<Site>> onCreateLoader(int id, Bundle args) {
        return new GetSiteLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<Site>> loader, List<Site> data) {
        progressLoadSites.setVisibility(View.GONE);
        if(data == null) {
            return;
        }

        siteData = data;
        List<String> siteNames = getSiteNameData();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, siteNames);
        lvSites.setAdapter(adapter);
    }

    private List<String> getSiteNameData() {
        List<String> result = new ArrayList<String>();
        if (siteData == null || siteData.size() == 0) {
            return result;
        }

        for (Site site : siteData) {
            result.add(site.getName());
        }

        return result;
    }

    @Override
    public void onLoaderReset(Loader<List<Site>> loader) {
        if(adapter != null){
            adapter.clear();
        }
    }

    private static class GetSiteLoader extends AbstractDataLoader<List<Site>> {
        private WeakReference<Context> context;

        GetSiteLoader(Context context) {
            super(context);
            this.context = new WeakReference<>(context);
        }

        @Override
        protected List<Site> buildList() {
            if(context.get() == null) {
                return new ArrayList<>();
            }

            return NetworkUtils.getAllSite(context.get(), Config.getCurrentProjectId(context.get()));
        }
    }

    private CacheService getCacheService(){
        if(cacheService == null){
            cacheService = CacheService.getInstance(this,
                    CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
        }

        return cacheService;
    }

    @SuppressWarnings("ConstantConditions")
    private void showAddSiteDialog(final Location location) {
        Ln.d("call showAddSiteDialog");

        View contentView = View.inflate(getActivityContext(), R.layout.add_new_site, null);
        final EditText etSiteName = (EditText) contentView.findViewById(R.id.site_name_edittext);
        if(dialog == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivityContext());
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            builder.setCancelable(true);
            builder.setView(contentView);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String siteName = etSiteName.getText().toString().trim();
                    if (siteName.length() == 0) {
                        Toast.makeText(getActivityContext(), R.string.input_site_name, Toast.LENGTH_SHORT).show();
                    } else {
                        // save new site to database
                        if (location == null) {
                            Toast.makeText(getActivityContext(), "Waiting for get location!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        double longitude;
                        double latitude;
                        Site mBestSite = GlobalState.getBestSite();
                        // save for first site
                        if (mBestSite == null) {
                            longitude = location.getLongitude();
                            latitude = location.getLatitude();
                        } else {
                            longitude = mBestSite.getLng();
                            latitude = mBestSite.getLat();
                        }

                        dialog.dismiss();
                        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                        UIUtils.hideSoftKeyboard(getActivityContext());
                        addNewSite(siteName, String.valueOf(latitude), String.valueOf(longitude));
                    }

                    etSiteName.setText("");
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    UIUtils.hideSoftKeyboard(getActivityContext());
                    etSiteName.setText("");
                }
            });

            dialog = builder.create();
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(true);
            int width = getResources().getDimensionPixelSize(R.dimen.dialog_width);
            dialog.getWindow().setLayout(width, RelativeLayout.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }

        if(dialog.isShowing()){
            return;
        }

        UIUtils.showKeyBoard(getActivityContext());
        etSiteName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        dialog.show();
    }

    private void addNewSite(String name, String lat, String lng){
        new AddSiteTask(this).execute(name, lat, lng);
    }

    private static class AddSiteTask extends AsyncTask<String, Void, Site> {
        private WeakReference<OnlineSitesActivity> weakReference;
        private String currentProjectId;

        AddSiteTask(OnlineSitesActivity context) {
            this.weakReference = new WeakReference<>(context);
            currentProjectId = Config.getCurrentProjectId(context);
        }

        @Override
        protected Site doInBackground(String... params) {
            if(weakReference.get() == null) {
                return null;
            }

            String name = params[0];
            String lat = params[1];
            String lng = params[2];

            Map<String, String> data = new HashMap<>();
            data.put("project_id", currentProjectId);
            data.put("name", name);
            data.put("latitude", lat);
            data.put("longitude", lng);
            CacheService cacheService = weakReference.get().getCacheService();
            CacheItem cacheItem = new CacheItem(CacheItem.TYPE_SITE, new Gson().toJson(data));
            cacheService.insertCache(cacheItem);

            Site site = NetworkUtils.addNewSite(weakReference.get(), currentProjectId, name, lat, lng);
            if(site != null) {
                cacheService.deleteCache(cacheItem);
            }

            return site;
        }

        @Override
        protected void onPostExecute(Site site) {
            if(weakReference.get() == null || weakReference.get().getCacheService() == null || site == null){
                return;
            }

            if (!weakReference.get().getCacheService().insertSite(site)) {
                UIUtils.buildAlertDialog(weakReference.get(), R.string.dialog_title,
                        R.string.insert_site_fail, true);
                return;
            }

            weakReference.get().reloadSites();
        }
    }
}
