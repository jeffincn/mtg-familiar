package com.gelakinetic.mtgfam.activities;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.BuildDate;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.DbUpdaterService;
import com.gelakinetic.mtgfam.helpers.JsonParser;
import com.gelakinetic.mtgfam.helpers.MyApp;
import com.gelakinetic.mtgfam.helpers.RulesParser;

public abstract class FamiliarActivity extends SherlockActivity {

	private ProgressDialog									progDialogSpinner;
	private ProgressDialog									progDialogHorizontal;
	private ProgressDialog									progDialog;
	protected AsyncTask<Void, String, Long>	asyncTask;

	protected FamiliarActivity							me;
	protected static SharedPreferences			preferences;
	protected CardDbAdapter									mDbHelper;
	protected Context												mCtx;

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
				//If we should be updating, check to see if we already are
				MyApp appState = (MyApp)getApplicationContext();
				boolean update;
				synchronized(this) {
					if(!appState.isUpdating()) {
						appState.setUpdating(true);
						appState.setUpdatingActivity(this);
						update = true;
					}
					else {
						update = false;
						if(appState.getUpdatingActivity() != this) {
							//finish();
						}
					}
				}
				
				if(update) {
                    startService(new Intent(this, DbUpdaterService.class));
					//asyncTask = new OTATask();
					//asyncTask.execute((Void[]) null);
				}
			}
		}

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.openWritable();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	/*
	 * Always add a virtual search key to the menu on the actionbar
	 * super.onCreateOptionsMenu should always be called from FamiliarActivities
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.name_search_hint).setIcon(R.drawable.menu_search)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
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
	}

	public class OTATask extends AsyncTask<Void, String, Long>
            implements JsonParser.CardProgressReporter, RulesParser.ProgressReporter {
		
		@Override
		protected void onPreExecute() {

			progDialogSpinner = new ProgressDialog(me);
			progDialogSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progDialogSpinner.setIndeterminate(true);
			progDialogSpinner.setCancelable(false);
			progDialogSpinner.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface pd) {
					asyncTask.cancel(true);
				}
			});
			progDialogSpinner.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_SEARCH) {
						return true;
					}
					return false;
				}
			});

			progDialogHorizontal = new ProgressDialog(me);
			progDialogHorizontal.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progDialogHorizontal.setIndeterminate(false);
			progDialogHorizontal.setCancelable(false);
			progDialogHorizontal.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface pd) {
					asyncTask.cancel(true);
				}
			});
			progDialogHorizontal.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_SEARCH) {
						return true;
					}
					return false;
				}
			});
			progDialog = progDialogSpinner;

			// lock the rotation
			int currentOrientation = me.getResources().getConfiguration().orientation;
			if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				me.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
			else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
				me.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			else {
				me.setRequestedOrientation(me.getRequestedOrientation());
			}

			progDialog.setMessage(mCtx.getString(R.string.update_updates));
			progDialog.show();
		}

		@Override
		protected Long doInBackground(Void... params) {
			
			this.publishProgress(new String[] { "indeterminate", mCtx.getString(R.string.update_legality) });

			try {				
				ArrayList<String[]> patchInfo = JsonParser.readUpdateJsonStream(preferences);

				URL legal = new URL("https://sites.google.com/site/mtgfamiliar/manifests/legality.json");
				InputStream in = new BufferedInputStream(legal.openStream());
				JsonParser.readLegalityJsonStream(in, mDbHelper, preferences);

				this.publishProgress(new String[] { "indeterminate", mCtx.getString(R.string.update_cards) });

				if (patchInfo != null) {
					
					for (int i = 0; i < patchInfo.size(); i++) {
						String[] set = patchInfo.get(i);
						if (!mDbHelper.doesSetExist(set[2])) {
							try {
								GZIPInputStream gis = new GZIPInputStream(new URL(set[1]).openStream());
								JsonParser.readCardJsonStream(gis, this, set[0], mDbHelper, mCtx);
							}
							catch (MalformedURLException e) {
								// Log.e("JSON error", e.toString());
							}
							catch (IOException e) {
								// Log.e("JSON error", e.toString());
							}
						}
					}
					JsonParser.readTCGNameJsonStream(preferences, mDbHelper);
				}
			}
			catch (MalformedURLException e1) {
				// eat it
			}
			catch (IOException e) {
				// eat it
			}

			this.publishProgress(new String[] { "indeterminate", mCtx.getString(R.string.update_rules) });

			long lastRulesUpdate = preferences.getLong("lastRulesUpdate", BuildDate.get(mCtx).getTime());
			RulesParser rp = new RulesParser(new Date(lastRulesUpdate), mDbHelper, mCtx, this);
			boolean newRulesParsed = false;
			if (rp.needsToUpdate()) {
				if (rp.parseRules()) {
					int code = rp.loadRulesAndGlossary();
					
					//Only save the timestamp of this if the update was 100% successful; if
					//something went screwy, we should let them know and try again next update.
					if(code == RulesParser.SUCCESS) {
						newRulesParsed = true;	
					}
					else {
						//TODO - We should indicate failure here somehow (toasts don't work in the async task)
					}
				}
			}

			long curTime = new Date().getTime();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt("lastLegalityUpdate", (int)(curTime / 1000));
			if(newRulesParsed) {
				editor.putLong("lastRulesUpdate", curTime);
			}
			editor.commit();

			return null;
		}

        @Override
		public void reportRulesProgress(String... values) {
			this.publishProgress(values);
        }
        @Override
		public void reportJsonCardProgress(String... values) {
			this.publishProgress(values);
        }

		@Override
		protected void onProgressUpdate(String... values) {
			if (values.length == 2) {
				progDialog.dismiss();
				if (values[0].equalsIgnoreCase("indeterminate")) {
					progDialog = progDialogSpinner;
				}
				else {
					progDialog = progDialogHorizontal;
				}
				progDialog.setMessage(values[1]);
				progDialog.show();
			}
			else if (values.length == 3) {
				if (values[0] != null) {
					progDialog.setTitle(values[0]);
				}
				if (values[1] != null) {
					progDialog.setMessage(values[1]);
				}
				if (values[2] != null) {
					try {
						int i = Integer.parseInt(values[2]);
						progDialog.setProgress(i);
					}
					catch (NumberFormatException e) {
					}
				}
			}
			else {
				progDialog.setMessage("PROBLEM");
			}
		}

		@Override
		protected void onPostExecute(Long result) {
			me.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
			try {
				progDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
			}
			
			MyApp appState = (MyApp)getApplicationContext();
			appState.setUpdating(false);
			appState.setUpdatingActivity(null);
		}

		@Override
		protected void onCancelled() {			
			MyApp appState = (MyApp)getApplicationContext();
			appState.setUpdating(false);
			appState.setUpdatingActivity(null);
		}
	}
}
