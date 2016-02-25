package com.appiphany.nacc.screens;

import android.app.Activity;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;

public class BaseActivity extends SherlockActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	protected Activity getActivityContext(){
		return this;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(GlobalState.getInstance().wasInBackground()){
			wasInBackground();
		}

		GlobalState.getInstance().stopActivityTransitionTimer();
	}

	@Override
	protected void onPause() {
		super.onPause();
		GlobalState.getInstance().startActivityTransitionTimer();
	}

	protected void wasInBackground(){}
}
