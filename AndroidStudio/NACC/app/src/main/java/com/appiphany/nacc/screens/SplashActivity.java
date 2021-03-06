
package com.appiphany.nacc.screens;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import com.appiphany.nacc.R;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
public class SplashActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
		Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_splash);
        Animation animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        animation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1500);
                            onInitializeEnd();
                            SplashActivity.this.finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        findViewById(R.id.ivSplash).startAnimation(animation);
    }

    public void onInitializeEnd() {
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
    }
}
