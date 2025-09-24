package com.appiphany.nacc.screens;

import java.lang.reflect.Field;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.ZoomButtonsController;
import com.appiphany.nacc.R;
import com.appiphany.nacc.utils.Config;

public class DonateWebActivity extends BaseActivity {
	
	private WebView mWebView;
	private ProgressBar mProgressBar;
	
	@SuppressLint({ "NewApi", "SetJavaScriptEnabled" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_web);
		setLayoutInsets(R.id.rootLayout);
		initActionBar();
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		
		mWebView = (WebView) findViewById(R.id.webView);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.setVisibility(View.GONE);
		
		mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                // hide loading image
            	mProgressBar.setVisibility(View.GONE);
            	
            	//show webview
            	mWebView.setVisibility(View.VISIBLE);
            }
        });
		
		mWebView.loadUrl(Config.DONATE_URL);
		
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB){
			mWebView.getSettings().setDisplayZoomControls(false);
		} else {
			setZoomControlGone(mWebView);
		}
		
	}
	
	@SuppressWarnings("ConstantConditions")
    private void initActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setTitle(R.string.donate);
    }
	
	 @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        if (item.getItemId() == android.R.id.home) {
	            finish();
	        }
	        return super.onOptionsItemSelected(item);
	    }
	
	public void setZoomControlGone(View view){
	    Class<?> classType;
	    Field field;
	    try {
	        classType = WebView.class;
	        field = classType.getDeclaredField("mZoomButtonsController");
	        field.setAccessible(true);
	        ZoomButtonsController mZoomButtonsController = new ZoomButtonsController(view);
	        mZoomButtonsController.getZoomControls().setVisibility(View.GONE);
	        try {
	            field.set(view, mZoomButtonsController);
	        } catch (IllegalArgumentException e) {
	            e.printStackTrace();
	        } catch (IllegalAccessException e) {
	            e.printStackTrace();
	        }
	    } catch (SecurityException e) {
	        e.printStackTrace();
	    } catch (NoSuchFieldException e) {
	        e.printStackTrace();
	    }
	}
}
