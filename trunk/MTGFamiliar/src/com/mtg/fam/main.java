package com.mtg.fam;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class main extends Activity implements Runnable {
	private Button						search;
	private Button						life;
	private Button						rng;
	private Context						mCtx;
	private CardDbAdapter			mDbHelper;
	private ProgressDialog		dialog;
	private int								numCards, numCardsAdded = 0;
	private SharedPreferences	mPrefs;
	private Editor						mPrefsEdit;
	private TextView	timer;
	private static String			DB_INIT	= "DB_INIT";
	
	private long	start, stop;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mCtx = this;

		mPrefs = getPreferences(MODE_PRIVATE);
		mPrefsEdit = mPrefs.edit();

		search = (Button) findViewById(R.id.cardsearch);
		life = (Button) findViewById(R.id.lifecounter);
		rng = (Button) findViewById(R.id.rng);

		timer = (TextView)findViewById(R.id.title);
		
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

		if (!mPrefs.getBoolean(DB_INIT, false)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.dbdownloadmsg)).setCancelable(false)
					.setPositiveButton(getString(R.string.dlnow), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							startThread("XML");
							dialog.cancel();
						}
					}).setNegativeButton(getString(R.string.dllater), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
		}
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
		if(mDbHelper != null){
			mDbHelper.close();
		}
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
				start = System.currentTimeMillis();
				startThread("XML");
				return true;
			case R.id.JSON:
				start = System.currentTimeMillis();
				startThread("JSON");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void cardAdded() {
		numCardsAdded++;
		if(numCards!=0){
			dialog.setProgress((100 * numCardsAdded) / numCards);
		}
		else{
			dialog.setProgress(numCardsAdded);
		}
	}

	public void startThread(String type) {
		dialog = new ProgressDialog(main.this);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMessage(type + ": Downloading and Parsing Cards. Please wait...");
		dialog.setCancelable(false);
		dialog.show();
		
		numCardsAdded = 0;
		Thread thread = new Thread(this);
		thread.start();
	}

	public void run() {
		parseJSON();
		handler.sendEmptyMessage(0);
	}

	private Handler	handler	= new Handler() {
														@Override
														public void handleMessage(Message msg) {
															dialog.dismiss();
															stop = System.currentTimeMillis();
															timer.setText((stop-start)/1000.0f + " seconds");
															mPrefsEdit.putBoolean(DB_INIT, true);
															mPrefsEdit.commit();
														}
													};

	public void setNumCards(int n) {
		numCards = n;
	}

	private void parseJSON() {
		try {
			mDbHelper = new CardDbAdapter(this);
			mDbHelper.open();
			mDbHelper.dropCreateDB();
			
			JSONparser jp = new JSONparser(this, mDbHelper);
			URL cards = new URL("http://members.cox.net/aefeinstein/cards.json.gzip");
//			URL cards = new URL("http://members.cox.net/aefeinstein/ap.json.gzip");
			GZIPInputStream gis = new GZIPInputStream(cards.openStream());
			jp.readJsonStream(gis);
			
			mDbHelper.close();
		}
		catch (MalformedURLException e) {
			Log.e("JSON error", e.toString());
		}
		catch (IOException e) {
			Log.e("JSON error", e.toString());
		}
	}
}