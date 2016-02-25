package com.appiphany.nacc.utils;

import android.app.Activity;

public class Waiter extends Thread {
	private long lastUsed;
	private long period;
	private boolean stop;
	private Activity mContext;

	public Waiter(long period, Activity context) {
		this.period = period;
		stop = false;
		this.mContext = context;
	}

	public void run() {
		long idle = 0;
		this.touch();
		do {
			idle = System.currentTimeMillis() - lastUsed;
//			Log.d(TAG, "Application is idle for " + idle + " ms");
			try {
				Thread.sleep(5000); // check every 5 seconds
			} catch (InterruptedException e) {
				Ln.d("Waiter interrupted!");
			}
			
			if (idle > period) {
				idle = 0;
				// do something here - e.g. call popup or so
//				Log.d(TAG, "===============================");
				mContext.finish();
			}
		} while (!stop);
		Ln.d("Finishing Waiter thread");
	}

	public synchronized void touch() {
		lastUsed = System.currentTimeMillis();
	}

	public synchronized void forceInterrupt() {
		this.interrupt();
	}

	// soft stopping of thread
	public synchronized void stopThread() {
		stop = true;
	}

	public synchronized void setPeriod(long period) {
		this.period = period;
	}

}