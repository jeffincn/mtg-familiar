package com.gelakinetic.mtgfam.activities;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
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
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.JsonParser;
import com.gelakinetic.mtgfam.helpers.MyApp;
import com.gelakinetic.mtgfam.helpers.RulesParser;

public abstract class FamiliarActivity extends SherlockActivity {

	private ProgressDialog									progDialogSpinner;
	private ProgressDialog									progDialogHorizontal;
	private ProgressDialog									progDialog;
	private long														defaultLastRulesUpdate;
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

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.openWritable();

		// NOTE: This needs to be updated with every release
		Calendar c = Calendar.getInstance();
		c.set(2012, Calendar.JUNE, 9, 0, 0, 0);
		defaultLastRulesUpdate = c.getTimeInMillis();

		boolean autoupdate = preferences.getBoolean("autoupdate", true);
		if (autoupdate) {
			// Only update the banning list if it hasn't been updated recently
			long curTime = new Date().getTime();
			int updatefrequency = Integer.valueOf(preferences.getString("updatefrequency", "3"));
			int lastLegalityUpdate = preferences.getInt("lastLegalityUpdate", 0);
			// days to ms
			if (((int) (curTime * .001) - lastLegalityUpdate) > (updatefrequency * 24 * 60 * 60)) {
				asyncTask = new OTATask();
				asyncTask.execute((Void[]) null);
			}
		}
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
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.search_hint).setIcon(R.drawable.menu_search)
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

	public class OTATask extends AsyncTask<Void, String, Long> {

		@Override
		protected void onPreExecute() {

			progDialogSpinner = new ProgressDialog(me);
			progDialogSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progDialogSpinner.setIndeterminate(true);
			progDialogSpinner.setCancelable(false);
			progDialogSpinner.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface pd) {
					// TODO when the dialog is dismissed
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
				public void onCancel(DialogInterface pd) {
					// TODO when the dialog is dismissed
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

			progDialog.setMessage("Checking for Updates");
			progDialog.show();
		}

		@Override
		protected Long doInBackground(Void... params) {

			this.publishProgress(new String[] { "indeterminate", "Checking Legality" });

			ArrayList<String[]> patchInfo = JsonParser.readUpdateJsonStream(preferences);

			try {
				URL legal = new URL("https://sites.google.com/site/mtgfamiliar/manifests/legality.json");
				InputStream in = new BufferedInputStream(legal.openStream());
				JsonParser.readLegalityJsonStream(in, mDbHelper, preferences);
			}
			catch (MalformedURLException e1) {
			}
			catch (IOException e) {
			}

			this.publishProgress(new String[] { "determinate", "Checking for New Cards" });

			if (patchInfo != null) {

				for (int i = 0; i < patchInfo.size(); i++) {
					String[] set = patchInfo.get(i);
					if (!mDbHelper.doesSetExist(set[2])) {
						try {
							GZIPInputStream gis = new GZIPInputStream(new URL(set[1]).openStream());
							JsonParser.readCardJsonStream(gis, this, set[0], mDbHelper);
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

			this.publishProgress(new String[] { "determinate", "Checking for New Comprehensive Rules" });

			long lastRulesUpdate = preferences.getLong("lastRulesUpdate", defaultLastRulesUpdate);
			RulesParser rp = new RulesParser(new Date(lastRulesUpdate), mDbHelper, this);
			if (rp.needsToUpdate()) {
				if (rp.parseRules()) {
					// TODO - loadRulesAndGlossary() returns an error code; use it?
					rp.loadRulesAndGlossary();
				}
			}

			long curTime = new Date().getTime();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt("lastLegalityUpdate", (int) (curTime * .001));
			editor.putLong("lastRulesUpdate", curTime);
			editor.commit();

			return null;
		}

		public void publicPublishProgress(String... values) {
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
		}

		@Override
		protected void onCancelled() {
			// TODO something when canceled?
		}
	}
}
