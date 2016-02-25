package com.appiphany.nacc.screens;

import java.util.Date;

import android.os.Bundle;
import android.preference.CheckBoxPreference;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.appiphany.nacc.R;
import com.appiphany.nacc.ui.controls.DatePreference;
import com.appiphany.nacc.ui.controls.TimePreference;

public class ReminderActivity extends SherlockPreferenceActivity {
    private DatePreference mDatePreference;
    private TimePreference mTimePreference;
    private CheckBoxPreference mCheckboxPreference;

    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_reminder_layout);
        mCheckboxPreference = (CheckBoxPreference) findPreference("reminder_enable");
        mDatePreference = (DatePreference) findPreference("reminder_date");
        mTimePreference = (TimePreference) findPreference("reminder_time");
        if (!mCheckboxPreference.isChecked()) {
            mDatePreference.setDate(DatePreference.formatter().format(new Date()));
            mTimePreference.setTime(TimePreference.formatter().format(new Date()));
        }
        initActionBar();
    }

    private void initActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setTitle(R.string.reminder_title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.preview_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_OK);
            finish();
        } else if (item.getItemId() == R.id.done_menu) {
            setResult(RESULT_OK);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

}
