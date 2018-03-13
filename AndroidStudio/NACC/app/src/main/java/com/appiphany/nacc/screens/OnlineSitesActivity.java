package com.appiphany.nacc.screens;

import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.utils.AbstractDataLoader;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.NetworkUtils;
import com.appiphany.nacc.utils.UIUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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

        if(dialog == null){
            dialog = new Dialog(this);
        }

        if(dialog.isShowing()){
            return;
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        UIUtils.showKeyBoard(this);
        dialog.requestWindowFeature((int) Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.add_new_site);
        int width = getResources().getDimensionPixelSize(R.dimen.dialog_width);
        dialog.getWindow().setLayout(width, RelativeLayout.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.CENTER);

        final EditText etSiteName = (EditText) dialog.findViewById(R.id.site_name_edittext);
        etSiteName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        Button btOk = (Button) dialog.findViewById(R.id.ok_button);
        btOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
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
                    dialog = null;
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    UIUtils.hideKeyboard(getActivityContext(), etSiteName);
                    addNewSite(siteName, String.valueOf(latitude), String.valueOf(longitude));
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
            String name = params[0];
            String lat = params[1];
            String lng = params[2];
            return NetworkUtils.addNewSite(weakReference.get(), currentProjectId, name, lat, lng);
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
