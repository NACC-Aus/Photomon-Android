package com.appiphany.nacc.screens;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import com.appiphany.nacc.R;
import com.appiphany.nacc.utils.Config;

public class InfoActivity extends BaseActivity {
	private static final String USER_AGENT = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
	
    @SuppressLint("SetJavaScriptEnabled")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        setLayoutInsets(R.id.rootLayout);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);        
        final WebView info = (WebView) findViewById(R.id.webView);        
        WebSettings settings = info.getSettings();
        settings.setJavaScriptEnabled(true); 
        settings.setUserAgentString(USER_AGENT);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDefaultTextEncodingName("utf-8");
        info.setVisibility(View.GONE);		
        info.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // hide loading image
            	progressBar.setVisibility(View.GONE);            	
            	//show webview
            	info.setVisibility(View.VISIBLE);
            }
        });
        
        info.loadUrl(Config.INFO_URL);
        initActionBar();
    }

    @SuppressWarnings("ConstantConditions")
    private void initActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setTitle(R.string.info_text);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
