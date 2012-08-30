package com.gelakinetic.mtgfam.activities;

import java.util.Date;

import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.DbUpdaterService;
import com.gelakinetic.mtgfam.helpers.MyApp;
import com.gelakinetic.mtgfam.helpers.RoundTimerService;

public abstract class FamiliarActivity extends SherlockActivity {

	protected FamiliarActivity					me;
	protected static SharedPreferences	preferences;
	public CardDbAdapter								mDbHelper;
	protected Context										mCtx;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		me = this;
		mCtx = this;

		getSupportActionBar().setDisplayShowTitleEnabled(false);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		boolean autoupdate = preferences.getBoolean("autoupdate", true);
		if (autoupdate) {
			// Only update the banning list if it hasn't been updated recently
			long curTime = new Date().getTime();
			int updatefrequency = Integer.valueOf(preferences.getString("updatefrequency", "3"));
			int lastLegalityUpdate = preferences.getInt("lastLegalityUpdate", 0);
			// days to ms
			if (((curTime / 1000) - lastLegalityUpdate) > (updatefrequency * 24 * 60 * 60)) {
				// If we should be updating, check to see if we already are
				MyApp appState = (MyApp) getApplicationContext();
				boolean update;
				synchronized (this) {
					if (!appState.isUpdating()) {
						appState.setUpdating(true);
						appState.setUpdatingActivity(this);
						update = true;
					}
					else {
						update = false;
						if (appState.getUpdatingActivity() != this) {
							// finish();
						}
					}
				}

				if (update) {
					startService(new Intent(this, DbUpdaterService.class));
				}
			}
		}

		mDbHelper = new CardDbAdapter(this);
		try {
			mDbHelper.openWritable();
			// throw new android.database.sqlite.SQLiteDatabaseLockedException();
		}
		catch (SQLiteException e) {
			String name = e.getClass().getName();
			String parts[] = name.split("\\.");
			String msg = parts[parts.length - 1] + getString(R.string.error_couldnt_open_db_toast);
			Toast.makeText(mCtx, msg, Toast.LENGTH_LONG).show();
			this.finish();
			return;
		}

		updatingDisplay = false;
		timeShowing = false;

		timerHandler = new Handler();
		registerReceiver(endTimeReceiver, new IntentFilter(RoundTimerActivity.RESULT_FILTER));
		registerReceiver(startTimeReceiver, new IntentFilter(RoundTimerService.START_FILTER));
		registerReceiver(cancelTimeReceiver, new IntentFilter(RoundTimerService.CANCEL_FILTER));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
		unregisterReceiver(endTimeReceiver);
		unregisterReceiver(startTimeReceiver);
		unregisterReceiver(cancelTimeReceiver);
		timerHandler.removeCallbacks(timerUpdate);
	}

	// clear this in every activity. except not cardview
	@Override
	protected void onResume() {
		super.onResume();
		MyApp appState = ((MyApp) getApplicationContext());
		String classname = this.getClass().getCanonicalName();
		if (classname.equalsIgnoreCase("com.gelakinetic.mtgfam.activities.CardViewActivity")) {
			if (appState.getState() == CardViewActivity.QUITTOSEARCH) {
				this.finish();
				return;
			}
		}
		else {
			appState.setState(0);
		}

		Intent i = new Intent(RoundTimerService.REQUEST_FILTER);
		sendBroadcast(i);

		if (!updatingDisplay) {
			startUpdatingDisplay();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		updatingDisplay = false;
		timerHandler.removeCallbacks(timerUpdate);
	}

	/*
	 * Always add a virtual search key to the menu on the actionbar super.onCreateOptionsMenu should always be called from FamiliarActivities
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.name_search_hint).setIcon(R.drawable.menu_search).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_SEARCH);
					}
				}).start();
				return true;
			}
		}).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return true;
	}

	// Displays the timer in the actionbar

	private boolean						updatingDisplay;
	private long							endTime;
	protected Handler					timerHandler;
	private boolean						timeShowing;
	
	private Runnable					timerUpdate					= new Runnable() {
		@Override
		public void run() {
			displayTimeLeft();
			
			if (endTime > SystemClock.elapsedRealtime()) {
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				timeShowing = true;
				timerHandler.postDelayed(timerUpdate, 200);
			}
			else {
				timeShowing = false;
				getSupportActionBar().setDisplayShowTitleEnabled(false);
				timerHandler.removeCallbacks(timerUpdate);
			}
		}
	};
	
	private BroadcastReceiver	endTimeReceiver			= new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = intent.getLongExtra(RoundTimerService.EXTRA_END_TIME, SystemClock.elapsedRealtime());
			startUpdatingDisplay();
			timerHandler.postDelayed(timerUpdate, 200);
		}
	};
	
	private BroadcastReceiver	cancelTimeReceiver	= new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = 0;
			stopUpdatingDisplay();
		}
	};
	
	private BroadcastReceiver startTimeReceiver	= new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Intent i = new Intent(RoundTimerService.REQUEST_FILTER);
			sendBroadcast(i);
		}
	};
	
	private void startUpdatingDisplay() {
		updatingDisplay = true;
		displayTimeLeft();
		timerHandler.removeCallbacks(timerUpdate);
		timerHandler.postDelayed(timerUpdate, 200);
	}
	
	private void stopUpdatingDisplay() {
		updatingDisplay = false;
		timeShowing = false;
		displayTimeLeft();
		timerHandler.removeCallbacks(timerUpdate);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
	}

	private void displayTimeLeft() {
		long timeLeftMillis = endTime - SystemClock.elapsedRealtime();
		String timeLeftStr = "";

		if (timeLeftMillis <= 0) {
			timeLeftStr = "00:00:00";
		}
		else {
			long timeLeftInSecs = (timeLeftMillis / 1000);

			// This is a slight hack to handle the fact that it always rounds down. It
			// makes the clock look much nicer this way.
			timeLeftInSecs++;

			String hours = String.valueOf(timeLeftInSecs / (3600));
			String minutes = String.valueOf((timeLeftInSecs % 3600) / 60);
			String seconds = String.valueOf(timeLeftInSecs % 60);

			if (hours.length() == 1) {
				timeLeftStr += "0";
			}
			timeLeftStr += hours + ":";

			if (minutes.length() == 1) {
				timeLeftStr += "0";
			}
			timeLeftStr += minutes + ":";

			if (seconds.length() == 1) {
				timeLeftStr += "0";
			}
			timeLeftStr += seconds;
		}

		if (timeShowing) {
			getSupportActionBar().setTitle(timeLeftStr);
		}
	}
}
