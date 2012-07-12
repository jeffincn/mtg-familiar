/**
Copyright 2011 Adam Feinstein and April King.

This file is part of MTG Familiar.

MTG Familiar is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MTG Familiar is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.JsonParser;
import com.gelakinetic.mtgfam.helpers.RoundTimerService;
import com.gelakinetic.mtgfam.helpers.RulesParser;

public class MainActivity extends FamiliarActivity {
	private static final int ABOUTDIALOG = 0;
	private static final int CHANGELOGDIALOG = 1;
	private static final int DONATEDIALOG = 2;
	private static final int TTS_CHECK_CODE = 23;
	private Context mCtx;
	private CardDbAdapter mDbHelper;
	private SharedPreferences preferences;
	private PackageInfo pInfo;
	private TextView search;
	private TextView rules;
	private TextView rng;
	private TextView manapool;
	private TextView randomCard;
	private TextView nbplayerbutton;
	private TextView roundTimer;
	private TextView trader;
	private TextView wishlist;
	private Activity me;
	private long defaultLastRulesUpdate;
	private OTATask asyncTask;
	private ProgressDialog progDialogSpinner;
	private ProgressDialog progDialogHorizontal;
	private ProgressDialog progDialog;

	// private ProgressDialog dialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		mCtx = this;
		me = this;

		// NOTE: This needs to be updated with every release
		Calendar c = Calendar.getInstance();
		c.set(2012, Calendar.JUNE, 9, 0, 0, 0);
		defaultLastRulesUpdate = c.getTimeInMillis();

		search = (TextView) findViewById(R.id.cardsearch);
		rules = (TextView) findViewById(R.id.rules);
		rng = (TextView) findViewById(R.id.rng);
		manapool = (TextView) findViewById(R.id.manapool);
		randomCard = (TextView) findViewById(R.id.randomCard);
		nbplayerbutton = (TextView) findViewById(R.id.Nplayerlifecounter);
		roundTimer = (TextView) findViewById(R.id.roundTimer);
		trader = (TextView) findViewById(R.id.trade);
		wishlist = (TextView) findViewById(R.id.wishlist);

		nbplayerbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, NPlayerLifeActivity.class);
				startActivity(i);
			}
		});

		search.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, SearchActivity.class);
				startActivity(i);
			}
		});

		rules.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, RulesActivity.class);
				startActivity(i);
			}
		});

		rng.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, DiceActivity.class);
				startActivity(i);
			}
		});

		manapool.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, ManaPoolActivity.class);
				startActivity(i);
			}
		});

		randomCard.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, RandomCardActivity.class);
				startActivity(i);
			}
		});

		roundTimer.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, RoundTimerActivity.class);
				startActivity(i);
			}
		});

		// roundTimer.setVisibility(View.GONE);

		trader.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, CardTradingActivity.class);
				startActivity(i);
			}
		});

		wishlist.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, WishlistActivity.class);
				startActivity(i);
			}
		});

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getLong("lastRulesUpdate", 0) == 0) {
			SharedPreferences.Editor edit = preferences.edit();
			edit.putLong("lastRulesUpdate", defaultLastRulesUpdate);
			edit.commit();
		}

		progDialogSpinner = new ProgressDialog(this);
		progDialogSpinner.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progDialogSpinner.setIndeterminate(true);
		progDialogSpinner.setCancelable(false);
		progDialogSpinner.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface pd) {
				// TODO when the dialog is dismissed
				asyncTask.cancel(true);
			}
		});

		progDialogHorizontal = new ProgressDialog(this);
		progDialogHorizontal.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progDialogHorizontal.setIndeterminate(false);
		progDialogHorizontal.setCancelable(false);
		progDialogHorizontal.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface pd) {
				// TODO when the dialog is dismissed
				asyncTask.cancel(true);
			}
		});

		progDialog = progDialogSpinner;

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.openWritable();
		boolean autoupdate = preferences.getBoolean("autoupdate", true);
		if (autoupdate) {
			asyncTask = new OTATask();
			asyncTask.execute((Void[]) null);
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

		Intent i = new Intent(this, RoundTimerService.class);
		startService(i);

		try {
			Intent tts = new Intent();
			tts.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(tts, TTS_CHECK_CODE);
		}
		catch (ActivityNotFoundException anf) {
			showTtsWarningIfShould();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	// @Override
	// protected void onStart() {
	// super.onStart();
	// }

	// @Override
	// protected void onResume() {
	// super.onResume();
	// }

	// @Override
	// public void onPause() {
	// super.onPause();
	// }
	//
	// @Override
	// public void onStop() {
	// super.onStop();
	// }

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
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

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		if (id == DONATEDIALOG) {
			builder.setTitle("Donate to the Devs");
			builder.setNeutralButton("Thanks Anyway!", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});

			LayoutInflater inflater = getLayoutInflater();
			View dialoglayout = inflater.inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.dialog_layout_root));

			TextView text = (TextView) dialoglayout.findViewById(R.id.aboutfield);
			text.setText(Html.fromHtml(getString(R.string.donate_to_devs)));
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
		}
		else if (id == ABOUTDIALOG) {
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
			text.setText(Html.fromHtml(getString(R.string.about_this_app)));
			text.setMovementMethod(LinkMovementMethod.getInstance());

			builder.setView(dialoglayout);
		}
		else if (id == CHANGELOGDIALOG) {
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

			builder.setMessage(Html.fromHtml(getString(R.string.whatsnew)));
		}
		return builder.create();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		// case R.id.buildWebDB:
		// startThread(DBFROMWEB);
		// return true;
		// case R.id.refreshDB:
		// startThread(DBFROMAPK);
		// return true;

			case R.id.checkUpdate:
				// Set the last legality update time back to zero on a forced update
				SharedPreferences.Editor editor = preferences.edit();
				editor.putLong("lastLegalityUpdate", 0);
				editor.commit();

				asyncTask = new OTATask();
				asyncTask.execute((Void[]) null);
				return true;
			case R.id.preferences:
				startActivity(new Intent().setClass(this, PreferencesActivity.class));
				return true;
			case R.id.aboutapp:
				showDialog(ABOUTDIALOG);
				return true;
			case R.id.whatsnew:
				showDialog(CHANGELOGDIALOG);
				return true;
			case R.id.donate:
				showDialog(DONATEDIALOG);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public class OTATask extends AsyncTask<Void, String, Long> {

		@Override
		protected void onPreExecute() {
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

			// Only update the banning list if it hasn't been updated recently
			long curTime = new Date().getTime();
			int updatefrequency = Integer.valueOf(preferences.getString("updatefrequency", "3"));
			int lastLegalityUpdate = preferences.getInt("lastLegalityUpdate", 0);

			// days to ms
			if (((int) (curTime * .001) - lastLegalityUpdate) > (updatefrequency * 24 * 60 * 60)) {
				System.out.print("time to update");
			}
			else {
				// don't need to update
				System.out.print("no update needed");
				return null;
			}

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
