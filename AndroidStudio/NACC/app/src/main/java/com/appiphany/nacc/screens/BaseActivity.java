package com.appiphany.nacc.screens;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
	
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
