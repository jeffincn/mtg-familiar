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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.JsonParser;
import com.gelakinetic.mtgfam.helpers.MyApp;
import com.gelakinetic.mtgfam.helpers.RoundTimerService;
import com.gelakinetic.mtgfam.helpers.RulesParser;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingActivity;

public class FamiliarActivity extends SlidingActivity {

	private ProgressDialog									progDialogSpinner;
	private ProgressDialog									progDialogHorizontal;
	private ProgressDialog									progDialog;
	private long														defaultLastRulesUpdate;
	protected AsyncTask<Void, String, Long>	asyncTask;

	protected FamiliarActivity							me;
	protected static SharedPreferences			preferences;
	protected CardDbAdapter									mDbHelper;
	protected Context												mCtx;
	private PackageInfo	pInfo;

	private static final int	ABOUTDIALOG			= 100;
	private static final int	CHANGELOGDIALOG	= 101;
	private static final int	DONATEDIALOG		= 102;

	private static final int	TTS_CHECK_CODE	= 23;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayHomeAsUpEnabled(true);

		getSlidingMenu().setBehindOffsetRes(R.dimen.actionbar_home_width);
		getSlidingMenu().setBehindScrollScale(0.0f);
		getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		setBehindContentView(R.layout.sliding_menu);

		me = this;
		mCtx = this;

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.openWritable();

		Intent i = new Intent(this, RoundTimerService.class);
		startService(i);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// NOTE: This needs to be updated with every release
		Calendar c = Calendar.getInstance();
		c.set(2012, Calendar.JUNE, 9, 0, 0, 0);
		defaultLastRulesUpdate = c.getTimeInMillis();

		if (preferences.getLong("lastRulesUpdate", 0) == 0) {
			SharedPreferences.Editor edit = preferences.edit();
			edit.putLong("lastRulesUpdate", defaultLastRulesUpdate);
			edit.commit();
		}

		boolean autoupdate = preferences.getBoolean("autoupdate", true);
		if (autoupdate) {
			// Only update the banning list if it hasn't been updated recently
			long curTime = new Date().getTime();
			int updatefrequency = Integer.valueOf(preferences.getString("updatefrequency", "3"));
			int lastLegalityUpdate = preferences.getInt("lastLegalityUpdate", 0);
			// days to ms
			if (((int) (curTime * .001) - lastLegalityUpdate) > (updatefrequency * 24 * 60 * 60)) {
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
							finish();
						}
					}
				}

				if (update) {
					asyncTask = new OTATask();
					asyncTask.execute((Void[]) null);
				}
			}
		}

		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		}
		catch (NameNotFoundException e) {
			pInfo = null;
		}

		int lastVersion = preferences.getInt("lastVersion", 0);
		if (pInfo.versionCode != lastVersion) {
			showDialog(CHANGELOGDIALOG);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt("lastVersion", pInfo.versionCode);
			editor.commit();
		}

		try {
			Intent tts = new Intent();
			tts.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(tts, TTS_CHECK_CODE);
		}
		catch (ActivityNotFoundException anf) {
			showTtsWarningIfShould();
		}

		TextView search = (TextView) findViewById(R.id.cardsearch);
		TextView rules = (TextView) findViewById(R.id.rules);
		TextView rng = (TextView) findViewById(R.id.rng);
		TextView manapool = (TextView) findViewById(R.id.manapool);
		TextView randomCard = (TextView) findViewById(R.id.randomCard);
		TextView nbplayerbutton = (TextView) findViewById(R.id.Nplayerlifecounter);
		TextView roundTimer = (TextView) findViewById(R.id.roundTimer);
		TextView trader = (TextView) findViewById(R.id.trade);
		TextView wishlist = (TextView) findViewById(R.id.wishlist);

		TextView checkUpdate = (TextView) findViewById(R.id.checkUpdate);
		TextView preferencesButton = (TextView) findViewById(R.id.preferences);
		TextView donate = (TextView) findViewById(R.id.donate);
		TextView whatsnew = (TextView) findViewById(R.id.whatsnew);
		TextView aboutapp = (TextView) findViewById(R.id.aboutapp);

		nbplayerbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggle();
				Intent i = new Intent(mCtx, NPlayerLifeActivity.class);
				startActivity(i);
			}
		});

		search.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggle();
				Intent i = new Intent(mCtx, SearchActivity.class);
				startActivity(i);
			}
		});

		rules.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggle();
				Intent i = new Intent(mCtx, RulesActivity.class);
				startActivity(i);
			}
		});

		rng.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggle();
				Intent i = new Intent(mCtx, DiceActivity.class);
				startActivity(i);
			}
		});

		manapool.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggle();
				Intent i = new Intent(mCtx, ManaPoolActivity.class);
				startActivity(i);
			}
		});

		randomCard.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggle();
				Intent i = new Intent(mCtx, RandomCardActivity.class);
				startActivity(i);
			}
		});

		roundTimer.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggle();
				Intent i = new Intent(mCtx, RoundTimerActivity.class);
				startActivity(i);
			}
		});

		trader.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggle();
				Intent i = new Intent(mCtx, CardTradingActivity.class);
				startActivity(i);
			}
		});

		wishlist.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggle();
				Intent i = new Intent(mCtx, WishlistActivity.class);
				startActivity(i);
			}
		});

		checkUpdate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Set the last legality update time back to zero on a forced update
				SharedPreferences.Editor editor = preferences.edit();
				editor.putLong("lastLegalityUpdate", 0);
				editor.commit();

				asyncTask = new OTATask();
				asyncTask.execute((Void[]) null);
				toggle();
			}
		});
		preferencesButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent().setClass(mCtx, PreferencesActivity.class));
				toggle();
			}
		});
		donate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DONATEDIALOG);
				toggle();
			}
		});
		whatsnew.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(CHANGELOGDIALOG);
				toggle();
			}
		});
		aboutapp.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(ABOUTDIALOG);
				toggle();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
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

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				toggle();
				return true;
		}
		return super.onOptionsItemSelected(item);
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

			try {
				ArrayList<String[]> patchInfo = JsonParser.readUpdateJsonStream(preferences);

				URL legal = new URL("https://sites.google.com/site/mtgfamiliar/manifests/legality.json");
				InputStream in = new BufferedInputStream(legal.openStream());
				JsonParser.readLegalityJsonStream(in, mDbHelper, preferences);

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
			}
			catch (MalformedURLException e1) {
				// eat it
			}
			catch (IOException e) {
				// eat it
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

			MyApp appState = (MyApp) getApplicationContext();
			appState.setUpdating(false);
			appState.setUpdatingActivity(null);
		}

		@Override
		protected void onCancelled() {
			MyApp appState = (MyApp) getApplicationContext();
			appState.setUpdating(false);
			appState.setUpdatingActivity(null);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch(id) {
			case DONATEDIALOG: {
				builder.setTitle("Donate to the Devs");
				builder.setNeutralButton(R.string.dialog_thanks, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

				LayoutInflater inflater = getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.dialog_layout_root));

				TextView text = (TextView) dialoglayout.findViewById(R.id.aboutfield);
				text.setText(Html.fromHtml(getString(R.string.main_donate_text)));
				text.setMovementMethod(LinkMovementMethod.getInstance());

				text.setTextSize(15);

				ImageView paypal = (ImageView) dialoglayout.findViewById(R.id.imageview1);
				paypal.setImageResource(R.drawable.paypal);
				paypal.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
								.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=ALR4TSXWPPHUL"));

						startActivity(myIntent);
					}
				});
				((ImageView) dialoglayout.findViewById(R.id.imageview2)).setVisibility(View.GONE);

				builder.setView(dialoglayout);
				return builder.create();
			}
			case ABOUTDIALOG: {
				// You have to catch the exception because the package stuff is all
				// run-time
				if (pInfo != null) {
					builder.setTitle("About " + getString(R.string.app_name) + " " + pInfo.versionName);
				}
				else {
					builder.setTitle("About " + getString(R.string.app_name));
				}

				builder.setNeutralButton("Thanks!", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

				LayoutInflater inflater = getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.dialog_layout_root));

				TextView text = (TextView) dialoglayout.findViewById(R.id.aboutfield);
				text.setText(Html.fromHtml(getString(R.string.main_about_text)));
				text.setMovementMethod(LinkMovementMethod.getInstance());

				builder.setView(dialoglayout);
				return builder.create();
			}
			case CHANGELOGDIALOG: {
				if (pInfo != null) {
					builder.setTitle("What's New in Version " + pInfo.versionName);
				}
				else {
					builder.setTitle("What's New");
				}

				builder.setNeutralButton("Enjoy!", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

				builder.setMessage(Html.fromHtml(getString(R.string.main_whats_new_text)));
				return builder.create();
			}
			default: {
				return null;
			}
		}
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TTS_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// We have TTS, so flag it as such
				SharedPreferences.Editor edit = preferences.edit();
				edit.putBoolean("hasTts", true);
				edit.commit();
			}
			else {
				showTtsWarningIfShould();
			}
		}
	}

	private void showTtsWarningIfShould() {
		SharedPreferences.Editor edit = preferences.edit();
		boolean shouldShow = preferences.getBoolean("ttsShowDialog", true);

		if (shouldShow) {
			// So we don't display this dialog again and bother the user
			edit.putBoolean("ttsShowDialog", false);

			// Then display a dialog informing them of TTS
			AlertDialog dialog = new Builder(this)
			.setTitle("Text-to-Speech")
			.setMessage(
					"This application has text-to-speech capability for some of its features, but you don't "
							+ "seem to have it installed. If you want to install it, use the \"Install Text-to-Speech\" link "
							+ "in the settings menu.").setPositiveButton("OK", new OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									// Do nothing, just dismiss
								}
							}).create();

			dialog.show();
		}

		// Also, even if we aren't showing the dialog, set a boolean indicating
		// that we don't have TTS
		edit.putBoolean("has_tts", false);
		edit.commit();
	}

}
