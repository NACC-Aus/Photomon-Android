package com.appiphany.nacc.screens;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.appiphany.nacc.R;
import com.appiphany.nacc.utils.UIUtils;

public class BaseActivity extends AppCompatActivity {
	private static final int REQUEST_PERMISSIONS = 232;
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

    protected void checkForPermission(final String[] permissions){
        boolean shouldCheckPermission = false;
        for (String item: permissions){
            if (ContextCompat.checkSelfPermission(getActivityContext(), item)
                    != PackageManager.PERMISSION_GRANTED) {
                shouldCheckPermission = true;
                break;
            }
        }

        if (shouldCheckPermission) {
            boolean shouldShowRequestPermissionRationale = false;
            for (String item: permissions){
                if (ActivityCompat.shouldShowRequestPermissionRationale
                        (this, item)){
                    shouldShowRequestPermissionRationale = true;
                    break;
                }
            }

            if (shouldShowRequestPermissionRationale) {
                AlertDialog dialog1 = UIUtils.showAlertDialog(this, R.string.dialog_title, R.string.msg_permission_required, false, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(getActivityContext(),
                                permissions,
                                REQUEST_PERMISSIONS);
                    }
                });
                dialog1.show();
            } else {
                ActivityCompat.requestPermissions(getActivityContext(),
                        permissions,
                        REQUEST_PERMISSIONS);
            }
        } else {
            executeTaskAfterPermission(permissions);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (verifyPermissionGranted(permissions)) {
                    executeTaskAfterPermission(permissions);
                }
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void executeTaskAfterPermission(String[] permissions){

    }

    protected boolean verifyPermissionGranted(String[] permissions){
        boolean hasPermission = true;
        for (String item: permissions){
            if (ContextCompat.checkSelfPermission(getActivityContext(), item)
                    != PackageManager.PERMISSION_GRANTED) {
                hasPermission = false;
                break;
            }
        }

        return hasPermission;
    }
}
