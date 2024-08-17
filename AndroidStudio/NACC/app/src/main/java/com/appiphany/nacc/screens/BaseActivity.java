package com.appiphany.nacc.screens;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.appiphany.nacc.R;
import com.appiphany.nacc.utils.UIUtils;

public class BaseActivity extends AppCompatActivity {
	private static final int REQUEST_PERMISSIONS = 232;

    public static final String[] APP_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ?
            new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                    Manifest.permission.POST_NOTIFICATIONS,
            } :
            (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU ?
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.POST_NOTIFICATIONS,
                    } : new String[]
                    {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                    });
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
