package com.mtg.fam;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class main extends Activity implements Runnable {
	private static final String	DB_PATH				= "/data/data/com.mtg.fam/databases/";
	private static final String	DB_NAME				= "data";
	private static final int		DBFROMAPK			= 0;
	private static final int		OTAPATCH			= 1;
	protected static final int	APPLYINGPATCH	= 3;
	private Button							search;
	private Button							life;
	private Button							rng;
	private Context							mCtx;
	private CardDbAdapter				mDbHelper;
	private ProgressDialog			dialog;
	private int									numCards, numCardsAdded = 0;
	private int									threadType;
	private String							patchname;
	private boolean							dialogReady;
	private SharedPreferences	preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mCtx = this;

		search = (Button) findViewById(R.id.cardsearch);
		life = (Button) findViewById(R.id.lifecounter);
		rng = (Button) findViewById(R.id.rng);

		search.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, search.class);
				startActivity(i);
			}
		});

		life.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, counter.class);
				startActivity(i);
			}
		});

		rng.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, rng.class);
				startActivity(i);
			}
		});
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		File f = new File(DB_PATH, DB_NAME);
		if (!f.exists()) {
			startThread(DBFROMAPK);
		}
		else {
			mDbHelper = new CardDbAdapter(this);
			mDbHelper.open();
			if(preferences.getBoolean("autoupdate",false)){
				startThread(OTAPATCH);
			}
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
		Context mContext = this;
		Dialog aboutDialog = new Dialog(mContext);
		aboutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		aboutDialog.setContentView(R.layout.aboutdialog);
		aboutDialog.setTitle("About " + getString(R.string.app_name));

		TextView text = (TextView) aboutDialog.findViewById(R.id.aboutfield);
		text.setAutoLinkMask(Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
		text.setText("This app is my gift to the MTG community." + '\n' + '\n'
				+ "Please send questions, comments, concerns, and praise to mtg.familiar@gmail.com" + '\n' + '\n'
				+ "Join the open source project at http://code.google.com/p/mtg-familiar" + '\n' + '\n'
				+ "Thanks to chaudakh from MTG:Salvation for the wonderful Gatherer Extractor program." + '\n' + '\n'
				+ "Thanks to zagaberoo from FNM for letting me bounce ideas off of." + '\n' + '\n'
				+ "Special thanks to Richard Garfield and the rest of the folks at Wizards of the Coast!" + '\n' + '\n'
				+ "They own and copyright all of this stuff, none of it is mine." + '\n' + '\n' + "-gelakinetic");

		return aboutDialog;
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
			case R.id.refreshDB:
				startThread(DBFROMAPK);
				return true;
			case R.id.checkUpdate:
				startThread(OTAPATCH);
				return true;
			case R.id.preferences:
				startActivity(new Intent().setClass(this, PreferencesActivity.class));
				return true;
			case R.id.aboutapp:
				showDialog(0);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void startThread(int type) {
		if (type == DBFROMAPK) {
			dialog = new ProgressDialog(main.this);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setMessage("Decompressing database...");
			dialog.setCancelable(false);
			dialog.show();
			threadType = type;
			Thread thread = new Thread(this);
			thread.start();
		}
		else if (type == OTAPATCH) {
			dialog = new ProgressDialog(main.this);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setMessage("Checking for Updates. Please wait...");
			dialog.setCancelable(false);
			dialog.show();

			threadType = type;
			Thread thread = new Thread(this);
			thread.start();
		}
	}

	public void run() {
		if (threadType == DBFROMAPK) {
			copyDB();
			handler.sendEmptyMessage(DBFROMAPK);
		}
		else if (threadType == OTAPATCH) {

			ArrayList<String[]> patchInfo = JsonUpdateParser.readJsonStream(this);
			if (patchInfo != null) {
				try {
					parseLegality(new URL("http://members.cox.net/aefeinstein/legality.json"));
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
			}
			handler.sendEmptyMessage(OTAPATCH);
		}
		threadType = -1;
	}

	private Handler	handler	= new Handler() {

														@Override
														public void handleMessage(Message msg) {
															switch (msg.what) {
																case DBFROMAPK:
																	mDbHelper = new CardDbAdapter(mCtx);
																	mDbHelper.open();
																	dialog.dismiss();
																	startThread(OTAPATCH);
																	break;
																case OTAPATCH:
																	dialog.dismiss();
																	break;
																case APPLYINGPATCH:
																	numCardsAdded = 0;

																	dialog.dismiss();
																	dialog = new ProgressDialog(main.this);
																	dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
																	dialog.setMessage("Adding " + patchname + ". Please wait...");
																	dialog.setCancelable(false);
																	dialog.show();

																	dialogReady = true;
																	break;
															}
														}
													};

	private void parseJSON(URL cards) {
		try {
			GZIPInputStream gis = new GZIPInputStream(cards.openStream());
			JsonCardParser.readJsonStream(gis, this, mDbHelper);
		}
		catch (MalformedURLException e) {
			Log.e("JSON error", e.toString());
		}
		catch (IOException e) {
			Log.e("JSON error", e.toString());
		}
	}

	void parseLegality(URL legal) {
		try {
			InputStream in = new BufferedInputStream(legal.openStream());
			JsonLegalityParser.readJsonStream(in, mDbHelper, getSharedPreferences("prefs", 0));
		}
		catch (MalformedURLException e) {
			return;
		}
		catch (IOException e) {
			return;
		}
	}

	private void copyDB() {
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
				SharedPreferences settings = this.getSharedPreferences("prefs", 0);
		    SharedPreferences.Editor editor = settings.edit();
		    editor.putString("lastUpdate", "");
		    editor.commit();
			}
			if (!db.exists()) {

				GZIPInputStream gis = new GZIPInputStream(getResources().openRawResource(R.raw.db));
				FileOutputStream fos = new FileOutputStream(db);

				byte[] buffer = new byte[1024];
				int length;
				int totalwritten = 0;
				while ((length = gis.read(buffer)) > 0) {
					fos.write(buffer, 0, length);
					totalwritten += length;
				}

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
}