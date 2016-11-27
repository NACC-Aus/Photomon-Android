package com.appiphany.nacc.screens;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.appiphany.nacc.R;
import com.appiphany.nacc.utils.Config;
import com.appiphany.nacc.utils.Ln;
import com.zooz.android.lib.CheckoutActivity;

public class ZoozActivity extends BaseActivity implements OnClickListener {
	
	public static final int ZOOZ_ACTIVITY_ID = 111;
	private RadioButton mBt5;
	private RadioButton mBt10;
	private RadioButton mBt20;
	private RadioButton mBt50;
	private RadioButton mBt100;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_donation);
        mBt5 = (RadioButton) findViewById(R.id.bt5);
        mBt10 = (RadioButton) findViewById(R.id.bt10);
        mBt20 = (RadioButton) findViewById(R.id.bt20);
        mBt50 = (RadioButton) findViewById(R.id.bt50);
        mBt100 = (RadioButton) findViewById(R.id.bt100);

		initActionBar();
	}
	
	@SuppressWarnings("ConstantConditions")
    private void initActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.donation);
        setSupportProgressBarIndeterminate(true);
        setSupportProgressBarIndeterminateVisibility(false);

    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_donation, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.donate_menu) {
        	
        	RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radiogroup);
        	 
        	 
        	int checkedRadioButton = radioGroup.getCheckedRadioButtonId();
        	 
        	int amount = 0;
        	 
			switch (checkedRadioButton) {
			case R.id.bt5:
				amount = 5;
				break;
			case R.id.bt10:
				amount = 10;
				break;
			case R.id.bt20:
				amount = 20;
				break;
			case R.id.bt50:
				amount = 50;
				break;
			case R.id.bt100:
				amount = 100;
				break;
			}
        	
    		onCheckoutClick(amount);
        }
        
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onClick(View v) {
		
	}
	
	/**
	 * Initializes the ZooZ CheckoutActivity intent and starts the ZooZ CheckoutActivity 
	 *
	 */
	public void onCheckoutClick(int amount) {

		Intent intent = new Intent(this, CheckoutActivity.class);

		// send merchant credential, app_key as given in the registration
		intent.putExtra(CheckoutActivity.ZOOZ_APP_KEY, Config.ZOOZ_KEY);
		intent.putExtra(CheckoutActivity.ZOOZ_AMOUNT, amount);
		intent.putExtra(CheckoutActivity.ZOOZ_CURRENCY_CODE, "USD");
		intent.putExtra(CheckoutActivity.ZOOZ_IS_SANDBOX, true);

		
		// start ZooZCheckoutActivity and wait to the activity result.
		startActivityForResult(intent, ZOOZ_ACTIVITY_ID);
	}

	/**
	 * Parses the result returning from the ZooZ CheckoutActivity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == ZOOZ_ACTIVITY_ID) {

			switch (resultCode) {
			case Activity.RESULT_OK:
				Ln.d("Successfully paid. Your transaction id is: "
								+ data.getStringExtra(CheckoutActivity.ZOOZ_TRANSACTION_ID) + "\nDisplay ID: "
								+ data.getStringExtra(CheckoutActivity.ZOOZ_TRANSACTION_DISPLAY_ID));
				break;
			case Activity.RESULT_CANCELED:

				if (data != null)
					Ln.e("Error, cannot complete payment with ZooZ. "
									+ "Error code: "
									+ data.getIntExtra(
											CheckoutActivity.ZOOZ_ERROR_CODE, 0)
									+ "; Error Message: "
									+ data.getStringExtra(CheckoutActivity.ZOOZ_ERROR_MSG));
				break;

			default:
				break;
			}
		}
	}
}
