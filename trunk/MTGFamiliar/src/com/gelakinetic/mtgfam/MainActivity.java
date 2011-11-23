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

package com.gelakinetic.mtgfam;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements Runnable {
	private static final String	DB_PATH						= "/data/data/com.gelakinetic.mtgfam/databases/";
	private static final String	DB_NAME						= "data";
	private static final int		DBFROMAPK					= 0;
	private static final int		OTAPATCH					= 1;
	private static final int		APPLYINGPATCH			= 3;
	private static final int		DBFROMWEB					= 4;
//	private static final int		DATABASE_VERSION	= ;
	private static final int		EXCEPTION					= 99;
	private static final int		ABOUTDIALOG				= 0;
	private static final int		CHANGELOGDIALOG		= 1;
	private LinearLayout				search;
	private LinearLayout				life;
	private LinearLayout				rng;
	private LinearLayout				manapool;
	private Context							mCtx;
	private CardDbAdapter				mDbHelper;
	private ProgressDialog			dialog;
	private int									numCards, numCardsAdded = 0;
	private int									threadType;
	private String							patchname;
	private boolean							dialogReady;
	private SharedPreferences		preferences;
	private String							stacktrace;
	private LinearLayout				deckmanagement;
	private PackageInfo					pInfo;
	private LinearLayout				randomCard;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		mCtx = this;

		search = (LinearLayout) findViewById(R.id.cardsearch);
		life = (LinearLayout) findViewById(R.id.lifecounter);
		rng = (LinearLayout) findViewById(R.id.rng);
		manapool = (LinearLayout) findViewById(R.id.manapool);
		randomCard = (LinearLayout) findViewById(R.id.randomCard);
		deckmanagement = (LinearLayout) findViewById(R.id.deckManagement);

		search.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, SearchActivity.class);
				startActivity(i);
			}
		});

		life.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, LifeCounterActivity.class);
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

		deckmanagement.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, DeckManagementActivity.class);
				startActivity(i);
			}
		});

		randomCard.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, RandomCardActivity.class);
				startActivity(i);
			}
		});

		// Uncomment to get to deck management, currently under construction
		deckmanagement.setVisibility(View.GONE);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		File f = new File(DB_PATH, DB_NAME);
		int dbVersion = preferences.getInt("databaseVersion", -1);
		if (!f.exists() || dbVersion != CardDbAdapter.DATABASE_VERSION) {
			startThread(DBFROMAPK);
		}
		else {
			mDbHelper = new CardDbAdapter(this);
			mDbHelper.open();
			boolean autoupdate = preferences.getBoolean("autoupdate", true);
			if (autoupdate) {
				startThread(OTAPATCH);
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
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		if (id == ABOUTDIALOG) {
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
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {

			 case R.id.buildWebDB: startThread(DBFROMWEB); return true;
			 case R.id.refreshDB: startThread(DBFROMAPK); return true;

			case R.id.checkUpdate:
				// Set the last legality update time back to zero on a forced update
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("lastLegalityUpdate", 0);
				editor.commit();

				startThread(OTAPATCH);
				return true;
			case R.id.preferences:
				startActivity(new Intent().setClass(this, PreferencesActivity.class));
				return true;
			case R.id.aboutapp:
				showDialog(ABOUTDIALOG);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void startThread(int type) {
		if (type == DBFROMAPK) {
			dialog = new ProgressDialog(MainActivity.this);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setMessage("Decompressing database...");
			dialog.setCancelable(false);
			dialog.show();
			threadType = type;
			Thread thread = new Thread(this);
			thread.start();
		}
		else if (type == OTAPATCH) {
			// Only update the banning list if it hasn't been updated recently
			int curTime = (int) (new Date().getTime() * .001);
			int updatefrequency = Integer.valueOf(preferences.getString("updatefrequency", "3"));
			int lastLegalityUpdate = preferences.getInt("lastLegalityUpdate", 0); // should
																																						// be
																																						// global

			if ((curTime - lastLegalityUpdate) > (updatefrequency * 24 * 60 * 60)) {
				dialog = new ProgressDialog(MainActivity.this);
				dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				dialog.setMessage("Checking for Updates. Please wait...");
				dialog.setCancelable(false);
				dialog.show();

				threadType = type;
				Thread thread = new Thread(this);
				thread.start();
			}
		}
		else if (type == DBFROMWEB) {
			dialog = new ProgressDialog(MainActivity.this);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setMessage("Downloading and db from web. Please wait...");
			dialog.setCancelable(false);
			dialog.show();

			numCardsAdded = 0;
			threadType = type;
			Thread thread = new Thread(this);
			thread.start();
		}
	}

	public void run() {
		try {
			if (threadType == DBFROMAPK) {
				copyDB();
				handler.sendEmptyMessage(DBFROMAPK);
			}
			else if (threadType == OTAPATCH) {

				if (mDbHelper == null) {
					mDbHelper = new CardDbAdapter(this);
					mDbHelper.open();
				}
				ArrayList<String[]> patchInfo = JsonParser.readUpdateJsonStream(this);

				if (patchInfo != null) {
					try {
						parseLegality(new URL("https://sites.google.com/site/mtgfamiliar/manifests/legality.json"));
					}
					catch (MalformedURLException e1) {
					}
					for (int i = 0; i < patchInfo.size(); i++) {
						String[] set = patchInfo.get(i);
						if (!mDbHelper.doesSetExist(set[2])) {
							try {
								patchname = set[0];
								dialogReady = false;
								handler.sendEmptyMessage(APPLYINGPATCH);
								while (!dialogReady) {
									;// spin in this thread until the dialog is ready
								}
								parseJSON(new URL(set[1]));
							}
							catch (MalformedURLException e) {
							}
						}
					}
					parseTCGNames();
				}
				handler.sendEmptyMessage(OTAPATCH);
			}
			else if (threadType == DBFROMWEB) {
				try {
					if (mDbHelper == null) {
						mDbHelper = new CardDbAdapter(this);
						mDbHelper.open();
					}

					mDbHelper.dropCreateDB();

					SharedPreferences.Editor editor = preferences.edit();
					editor.putString("lastTCGNameUpdate", "");
					editor.putString("lastUpdate", "");
					editor.putString("date", "");
					editor.commit();

					parseJSON(new URL("https://sites.google.com/site/mtgfamiliar/patches/uptoISD.json.gzip"));
					parseLegality(new URL("https://sites.google.com/site/mtgfamiliar/manifests/legality.json"));
					parseTCGNames();
				}
				catch (MalformedURLException e) {
				}
				handler.sendEmptyMessage(DBFROMWEB);
			}
			threadType = -1;
		}
		catch (Exception e) {
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			stacktrace = result.toString();

			handler.sendEmptyMessage(EXCEPTION);
		}
	}

	private Handler	handler	= new Handler() {

														@Override
														public void handleMessage(Message msg) {
															switch (msg.what) {
																case DBFROMWEB:
																	if (dialog != null && dialog.isShowing()) {
																		try {
																			dialog.dismiss();
																		}
																		catch (Exception e) {
																			;
																		}
																	}
																	break;
																case DBFROMAPK:
																	mDbHelper = new CardDbAdapter(mCtx);
																	mDbHelper.open();
																	if (dialog != null && dialog.isShowing()) {
																		try {
																			dialog.dismiss();
																		}
																		catch (Exception e) {
																			;
																		}
																	}
																	startThread(OTAPATCH);
																	break;
																case OTAPATCH:
																	// If it successfully updated, update the
																	// timestamp
																	int curTime = (int) (new Date().getTime() * .001); // should
																																											// be
																																											// global?
																	SharedPreferences.Editor editor = preferences.edit();
																	editor.putInt("lastLegalityUpdate", curTime);
																	editor.commit();

																	if (dialog != null && dialog.isShowing()) {
																		try {
																			dialog.dismiss();
																		}
																		catch (Exception e) {
																			;
																		}
																	}
																	break;
																case APPLYINGPATCH:
																	numCardsAdded = 0;

																	if (dialog != null && dialog.isShowing()) {
																		try {
																			dialog.dismiss();
																		}
																		catch (Exception e) {
																			;
																		}
																	}
																	dialog = new ProgressDialog(MainActivity.this);
																	dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
																	dialog.setMessage("Adding " + patchname + ". Please wait...");
																	dialog.setCancelable(false);
																	dialog.show();

																	dialogReady = true;
																	break;
																case EXCEPTION:
																	if (dialog != null && dialog.isShowing()) {
																		try {
																			dialog.dismiss();
																		}
																		catch (Exception e) {
																			;
																		}
																	}

																	AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
																	builder.setMessage(stacktrace).setCancelable(true);
																	AlertDialog alert = builder.create();
																	alert.show();

																	stacktrace.toString();
															}
														}
													};

	private void parseJSON(URL cards) {
		try {
			GZIPInputStream gis = new GZIPInputStream(cards.openStream());
			JsonParser.readCardJsonStream(gis, this, mDbHelper);
		}
		catch (MalformedURLException e) {
			// Log.e("JSON error", e.toString());
		}
		catch (IOException e) {
			// Log.e("JSON error", e.toString());
		}
	}

	void parseLegality(URL legal) {
		try {
			InputStream in = new BufferedInputStream(legal.openStream());
			JsonParser.readLegalityJsonStream(in, mDbHelper, preferences);
		}
		catch (MalformedURLException e) {
			return;
		}
		catch (IOException e) {
			return;
		}
	}

	void parseTCGNames() {
		JsonParser.readTCGNameJsonStream(this, mDbHelper);
	}

	private void copyDB() {
		SharedPreferences.Editor editor = preferences.edit();

		if (mDbHelper != null) {
			mDbHelper.close();
		}
		try {
			File folder = new File(DB_PATH);
			if (!folder.exists()) {
				folder.mkdir();
			}
			File db = new File(folder, DB_NAME);
			if (db.exists()) {
				db.delete();
				editor.putString("lastUpdate", "");
				editor.putInt("databaseVersion", -1);
				editor.commit();
			}
			if (!db.exists()) {

				GZIPInputStream gis = new GZIPInputStream(getResources().openRawResource(R.raw.db));
				FileOutputStream fos = new FileOutputStream(db);

				byte[] buffer = new byte[1024];
				int length;
				while ((length = gis.read(buffer)) > 0) {
					fos.write(buffer, 0, length);
				}

				editor.putInt("databaseVersion", CardDbAdapter.DATABASE_VERSION);
				editor.commit();

				// Close the streams
				fos.flush();
				fos.close();
				gis.close();
			}
		}
		catch (NotFoundException e) {
		}
		catch (IOException e) {
		}
		catch (Exception e) {
		}
	}

	public void cardAdded() {
		numCardsAdded++;
		if (numCards != 0) {
			dialog.setProgress((100 * numCardsAdded) / numCards);
		}
		else {
			dialog.setProgress(numCardsAdded);
		}
	}

	public void setNumCards(int n) {
		numCards = n;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
