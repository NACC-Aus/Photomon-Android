package com.appiphany.nacc.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import roboguice.util.Ln;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Site;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.utils.Config;

public class SiteManagementActivity extends SherlockListActivity implements OnItemClickListener {
	private ListView lvSites;
	private ActionMode mMode;
	private List<Site> siteData;
	private List<String> siteNames;
	private boolean isEditMode;
	private ArrayAdapter<String> adapter;
	private CacheService cacheService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manage_sites);
		mMode = null;
		lvSites = getListView();
		lvSites.setItemsCanFocus(false);
		lvSites.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		lvSites.setOnItemClickListener(this);

		cacheService = new CacheService(this, CacheService.createDBNameFromUser(Config.getActiveServer(this), Config.getActiveUser(this)));
		if (cacheService != null && cacheService.getAllSite(Config.getCurrentProjectId(this)).size() > 0) {
			siteData = cacheService.getAllSite(Config.getCurrentProjectId(this));
			siteNames = getSiteNameData();

			adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, siteNames);
			setListAdapter(adapter);
		}

		initActionBar();

	}

	private void initActionBar() {
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayUseLogoEnabled(true);
		getSupportActionBar().setTitle(R.string.site_title);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
		}

		return super.onOptionsItemSelected(item);
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

	private void removeSiteData(List<Integer> indices) {
		if (indices.size() == 0) {
			return;
		}

		// keep the old index while remove item
		Collections.sort(indices, Collections.reverseOrder());
		System.out.println(indices);
		for (Integer position : indices) {
			Ln.d("delete item %d", position);
			
			if (cacheService != null) {
				String sideId = siteData.get(position).getSiteId();
				cacheService.deletePhotosBySite(sideId);
				cacheService.deleteGuidePhotosBySite(sideId);
				cacheService.deleteSite(sideId);
				clearSitesOnMemory(sideId);
			}

			adapter.remove(siteNames.get(position));
			siteData.remove(position);
		}

		lvSites.setAdapter(adapter);
	}

	private void clearSitesOnMemory(String sideId){		
		if(GlobalState.getBestSite() != null && sideId.equals(GlobalState.getBestSite().getSiteId())){
			GlobalState.setBestSite(null);
		}
		
		if(GlobalState.getSites() != null){
			for (int i = GlobalState.getSites().size() - 1; i >= 0; i --) {
				if(sideId.equals(GlobalState.getSites().get(i).getSiteId())){
					GlobalState.getSites().remove(i);
				}
			}
		}
	}
	
	private void changeSiteName(int position, String newName) {
		if (cacheService != null) {
			String sideId = siteData.get(position).getSiteId();
			if (!TextUtils.isEmpty(sideId)) {
				cacheService.updateSiteName(sideId, newName);
				cacheService.updatePhotoNameBySiteId(sideId, newName);
				if (GlobalState.getBestSite() != null && sideId.equals(GlobalState.getBestSite().getSiteId())) {
					GlobalState.getBestSite().setName(newName);
				}
			}
		}

		siteNames.set(position, newName);
		siteData.get(position).setName(newName);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		SparseBooleanArray checked = lvSites.getCheckedItemPositions();
		boolean hasCheckedElement = false;
		int checkCount = 0;
		for (int i = 0; i < checked.size(); i++) {
			if (hasCheckedElement == false) {
				hasCheckedElement = checked.valueAt(i);
			}

			Ln.d("item %d has value = %b", i, checked.valueAt(i));
			if (checked.valueAt(i)) {
				checkCount++;
			}
		}

		isEditMode = !(checkCount > 1);

		if (hasCheckedElement) {
			if (mMode == null) {
				mMode = startActionMode(new ModeCallback());
			} else {
				mMode.invalidate();
			}
		} else {
			if (mMode != null) {
				mMode.finish();
			}
		}
	}

	private final class ModeCallback implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Create the menu from the xml file
			MenuInflater inflater = getSupportMenuInflater();
			inflater.inflate(R.menu.manage_sites_menu, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			if (isEditMode) {
				menu.getItem(0).setVisible(true);
			} else {
				menu.getItem(0).setVisible(false);
			}

			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// Destroying action mode, let's unselect all items
			for (int i = 0; i < lvSites.getAdapter().getCount(); i++)
				lvSites.setItemChecked(i, false);

			if (mode == mMode) {
				mMode = null;
			}
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			SparseBooleanArray checked = lvSites.getCheckedItemPositions();
			switch (item.getItemId()) {
			case R.id.mn_site_delete:
				List<Integer> indices = new ArrayList<Integer>();

				for (int i = checked.size() - 1; i >= 0; i--) {
					if (checked.valueAt(i)) {
						int position = checked.keyAt(i);
						indices.add(position);
					}
				}

				removeSiteData(indices);
				break;
			case R.id.mn_site_edit:
				for (int i = 0; i < checked.size(); i++) {
					if (checked.valueAt(i)) {
						int position = checked.keyAt(i);
						showEditNameDialog(position);
					}
				}
				break;
			default:
				break;
			}

			mode.finish();
			return true;
		}
	};

	private void showEditNameDialog(final int position) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Title");
		alert.setMessage("Message");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);
		input.setText(siteData.get(position).getName());
		input.setSelection(input.getText().length());
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				changeSiteName(position, value);
				dialog.dismiss();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});

		alert.show();
	}
}
