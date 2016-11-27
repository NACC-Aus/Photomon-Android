package com.appiphany.nacc.screens;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.appiphany.nacc.R;
import com.appiphany.nacc.model.Project;
import com.appiphany.nacc.services.CacheService;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.DialogUtil;
import com.appiphany.nacc.utils.NetworkUtils;
import com.appiphany.nacc.utils.UIUtils;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends BaseActivity implements OnClickListener {
    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private Button mSubmitButton;
    private Button mBtDemo;

    private ProgressDialog mProgressBar;
    private LoginTask mTask;
    private String mEmailText;
    private String mPasswordText;
    private String mServerText;
    private static final String EMAIL_TAG = "nacc-email-tag";
    private static final String PASSWORD_TAG = "nacc-password-tag";
    private static final String SERVER_TAG = "nacc-server-tag";
    private static final String USER_TAG = "nacc-user-info";
    private static final String ACCOUNT_TAG = "nacc-account-info";
    private static final String ACCOUNT_SERVER_TAG = "nacc-account-server-info";
    private static final String ACCOUNT_EMAIL_TAG = "nacc-account-email-info";
    private static final String ACCOUNT_PASS_TAG = "nacc-account-pass-info";
    public static final String SERVER_DEMO = "demo";
    private static final int CONECTION_TIMEOUT = 30000;
    private static final int RETRY_COUNT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_login);
        mUsernameEditText = (EditText) findViewById(R.id.email_input_view);
        mPasswordEditText = (EditText) findViewById(R.id.password_input_view);
        mSubmitButton = (Button) findViewById(R.id.login_button_view);
        mBtDemo = (Button) findViewById(R.id.demo_button_view);

        mSubmitButton.setOnClickListener(this);
        mBtDemo.setOnClickListener(this);
        mProgressBar = new ProgressDialog(this);
        mProgressBar.setMessage("Signing in...");
        mProgressBar.setCancelable(false);

        SharedPreferences sharedPref = getSharedPreferences(ACCOUNT_TAG, MODE_PRIVATE);
        mServerText = sharedPref.getString(ACCOUNT_SERVER_TAG, null);
        mEmailText = sharedPref.getString(ACCOUNT_EMAIL_TAG, null);
        mPasswordText = sharedPref.getString(ACCOUNT_PASS_TAG, null);

        Intent intent = getIntent();
        if (mServerText != null && mEmailText != null && mPasswordText != null && !Config.isDemoMode(this)) {
            if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_MAIN)) {
                showLoadingDialog();
                showEditTexts(false);
                if (mTask != null) {
                    mTask.cancel(true);
                }

                mTask = new LoginTask(this);
                mTask.execute(mServerText, mEmailText, mPasswordText);
            }

        } else {
            showEditTexts(true);
        }

        if (mServerText == null || mServerText.equalsIgnoreCase(SERVER_DEMO)) {
			// do something
        } else {
            mUsernameEditText.setText(mEmailText);
            mPasswordEditText.setText(mPasswordText);
        }
    }

    private void showEditTexts(boolean isDisplay) {
        if (isDisplay) {
            mUsernameEditText.setVisibility(View.VISIBLE);
            mPasswordEditText.setVisibility(View.VISIBLE);
            mSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mUsernameEditText.setVisibility(View.INVISIBLE);
            mPasswordEditText.setVisibility(View.INVISIBLE);
            mSubmitButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressBar != null) {
            mProgressBar.dismiss();
        }
    }

    private void showLoadingDialog() {
        if (mProgressBar != null) {
            mProgressBar.show();
        }
    }

    private void hideLoadingDialog() {
        if (mProgressBar != null) {
            mProgressBar.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSubmitButton) {
            mServerText = Config.DEFAULT_SERVER;
            mEmailText = mUsernameEditText.getText().toString();
            mPasswordText = mPasswordEditText.getText().toString();

            saveAccountToCache();

            if (NetworkUtils.isNetworkOnline(LoginActivity.this)) {
                if (UIUtils.isStringEmpty(mServerText)) {
                    mServerText = Config.DEFAULT_SERVER;
                }

                if (!mServerText.endsWith("/")) {
                    mServerText += "/";
                }

                try {
                    GenericUrl genericUrl = new GenericUrl(mServerText);
                } catch (Exception e) {
                    UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.server_required, false).show();
                    return;
                }

                if (UIUtils.isStringEmpty(mEmailText) || !UIUtils.isEmailValid(mEmailText)) {
                    UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.email_required, false).show();
                    return;
                }

                if (UIUtils.isStringEmpty(mPasswordText)) {
                    UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.password_required, false).show();
                    return;
                }

                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);

                if (mTask != null) {
                    mTask.cancel(true);
                }

                mTask = new LoginTask(this);
                mTask.execute(mServerText, mEmailText, mPasswordText);

                v.setEnabled(false);
                showLoadingDialog();
            } else {
                DialogUtil.showSettingsAlert(LoginActivity.this, R.string.wifi_setting_title,
                        R.string.wifi_setting_message, Settings.ACTION_WIFI_SETTINGS);
            }
        } else if (v == mBtDemo) {

            mServerText = SERVER_DEMO;
            mEmailText = SERVER_DEMO;
            mPasswordText = SERVER_DEMO;

            loginFinish(SERVER_DEMO);
        }
    }

    private void loginFinish(String result) {
        hideLoadingDialog();
        mSubmitButton.setEnabled(true);
        if("server_not_valid".equalsIgnoreCase(result)){
        	UIUtils.buildAlertDialog(this, R.string.server_url_title, R.string.msg_server_url_not_available, false).show();
            showEditTexts(true);
        }else if ("".equalsIgnoreCase(result)) {
            UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.login_connection_error, false).show();
            showEditTexts(true);
        } else if (result == null) {
            UIUtils.buildAlertDialog(this, R.string.dialog_title, R.string.login_error, false).show();
            showEditTexts(true);
        } else {
            saveLoginInfoToCache();
            Config.setAccessToken(this, result);
            Config.setActiveServer(this, mServerText);
            Config.setActiveUser(this, mEmailText);
            Config.setDemoMode(this, result);

            checkForPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA});
        }
    }

    @Override
    protected void executeTaskAfterPermission(String[] permissions) {
        goToMainScreen();
    }

    private void goToMainScreen() {
        if(!verifyPermissionGranted(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA})){
            return;
        }

        GlobalState.getInstance().initLocation();
        Intent showMainScreenIntent = new Intent(this, MainScreenActivity.class);
        showMainScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        showMainScreenIntent.putExtra(MainScreenActivity.FROM_LOGIN_EXTRA, true);
        showMainScreenIntent.putExtra(MainScreenActivity.DOWNLOAD_GUIDE_EXTRA, true);

        finish();
        startActivity(showMainScreenIntent);
        UIUtils.hideSoftKeyboard(this);
    }

    private static class LoginTask extends AsyncTask<String, Void, String> {
        private WeakReference<LoginActivity> mContext;

        LoginTask(LoginActivity context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... params) {
            String serverText = params[0];
            String emailText = params[1];
            String passwordText = params[2];
            
            if(!NetworkUtils.isServerAvailable(serverText)){
            	return "server_not_valid";
            }
            
            Map<String, String> data = new HashMap<String, String>();
            String message = null;
            data.put("email", emailText);
            data.put("password", passwordText);
            try {
                HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                UrlEncodedContent httpContent = new UrlEncodedContent(data); 
                HttpRequest httpRequest = httpTransport.createRequestFactory().buildPostRequest(
                        new GenericUrl(serverText + "sessions.json"), httpContent);
                httpRequest.setConnectTimeout(CONECTION_TIMEOUT);
                httpRequest.setNumberOfRetries(RETRY_COUNT);
                httpRequest.setRetryOnExecuteIOException(true);
                HttpResponse httpResponse = httpRequest.execute();
                if (httpResponse != null && httpResponse.getStatusCode() == 200) {
                    InputStream is = httpResponse.getContent();
                    InputStreamReader reader = new InputStreamReader(is);
                    JsonParser parser = new JsonParser();
                    JsonElement jsonElement = parser.parse(reader);
                    JsonObject resultObject = jsonElement.getAsJsonObject();
                    if (resultObject.has("AccessToken")) {
                    	String accessToken = resultObject.get("AccessToken").getAsString();   
                    	List<Project> projects = NetworkUtils.getProjects(serverText, accessToken);
                    	if(projects.size() > 0){
                    		CacheService cacheService = new CacheService(mContext.get(),
                            		CacheService.createDBNameFromUser(serverText, emailText));
                    		for (Project project : projects) {
                                if(TextUtils.isEmpty(project.getName()) || TextUtils.isEmpty(project.getUid())){
                                    continue;
                                }

                    			if(!cacheService.insertProject(project)){
                        			cacheService.updateProject(project);
                        		}
							}

                            if(TextUtils.isEmpty(Config.getCurrentProjectId(mContext.get()))){
                                Config.setCurrentProjectId(mContext.get(), projects.get(0).getUid());
                            }
                    	}
                    	
                        message = accessToken;
                    }
                }else{
                	return null;
                }

            } catch (Exception e) {
            	if (e instanceof IOException) {
                	if(e instanceof HttpResponseException){
                		message = null;
                	}else{
                		message = "";
                	}
                } else {
                    message = null;
                }

                e.printStackTrace();
            }

            return message;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (mContext != null) {
                LoginActivity loginActivity = mContext.get();
                if (loginActivity != null) {
                    loginActivity.loginFinish(result);
                }
            }
        }
    }

    private void saveLoginInfoToCache() {
        SharedPreferences sharedPreferences = getSharedPreferences(USER_TAG, Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putString(SERVER_TAG, mServerText);
        editor.putString(EMAIL_TAG, mEmailText);
        editor.putString(PASSWORD_TAG, mPasswordText);
        editor.apply();
    }

    private void saveAccountToCache() {
        SharedPreferences sharedPreferences = getSharedPreferences(ACCOUNT_TAG, Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putString(ACCOUNT_SERVER_TAG, mServerText);
        editor.putString(ACCOUNT_EMAIL_TAG, mEmailText);
        editor.putString(ACCOUNT_PASS_TAG, mPasswordText);
        editor.apply();
    }
}
