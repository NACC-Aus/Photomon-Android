package com.appiphany.nacc.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.net.ConnectivityManagerCompat;
import android.util.Log;

import com.appiphany.nacc.screens.BackgroundService;
import com.appiphany.nacc.utils.Ln;
import com.appiphany.nacc.utils.UIUtils;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                    UIUtils.registerReminder(context);
                } else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    Ln.d("CONNECT CHANGE " + intent);
                    LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(context);
                    boolean hasNoConnectivity = intent
                            .getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                    if (!hasNoConnectivity) {
                        ConnectivityManager cm = (ConnectivityManager) context
                                .getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = ConnectivityManagerCompat.getNetworkInfoFromBroadcast(cm, intent);
                        if (networkInfo != null && networkInfo.isConnected()) {
                            Intent broadcastIntent = new Intent(BackgroundService.CONNECTED_ACTION);
                            localBroadcastMgr.sendBroadcast(broadcastIntent);
                        }
                    }
                }
            }
        }
    }

}
