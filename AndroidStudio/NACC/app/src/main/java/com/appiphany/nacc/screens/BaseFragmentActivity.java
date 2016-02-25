package com.appiphany.nacc.screens;

import android.app.Activity;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BaseFragmentActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	protected Activity getActivityContext(){
		return this;
	}
}
