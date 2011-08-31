package com.mtg.fam;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
	private static final String DB_PATH = "/data/data/com.mtg.fam/databases/";
	private static final String DB_NAME = "data";
	private static final int	DBFROMAPK	= 0;
	private static final int OTAPATCH	= 1;
	private Button						search;
	private Button						life;
	private Button						rng;
	private Context						mCtx;
	private CardDbAdapter			mDbHelper;
	private ProgressDialog		dialog;
	private int								numCards, numCardsAdded = 0;
	private int	threadType;
	private Button	about;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mCtx = this;

		search = (Button) findViewById(R.id.cardsearch);
		life = (Button) findViewById(R.id.lifecounter);
		rng = (Button) findViewById(R.id.rng);
		about = (Button) findViewById(R.id.about);

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
		
		about.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(0);
			}
		});
		
		File f = new File(DB_PATH, DB_NAME);
		if(!f.exists()){
			startThread(DBFROMAPK);
		}
		else{
			//TODO check for OTA if DB already exists
		}
	}

	@Override
	protected void onStart(){
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
		if(mDbHelper != null){
			mDbHelper.close();
		}
	}

	@Override
	protected Dialog onCreateDialog( int id ) 
	{
		Context mContext = this;
		Dialog aboutDialog = new Dialog(mContext);
		aboutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		aboutDialog.setContentView(R.layout.aboutdialog);
		aboutDialog.setTitle("About " + getString(R.string.app_name));

		TextView text = (TextView) aboutDialog.findViewById(R.id.aboutfield);
		text.setAutoLinkMask(Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
		text.setText("This app is my gift to the MTG community."+'\n'+'\n'+
				"Please send questions, comments, concerns, and praise to mtg.familiar@gmail.com"+'\n'+'\n'+
				"Join the open source project at http://code.google.com/p/mtg-familiar"+'\n'+'\n'+
				"Thanks to chaudakh from MTG:Salvation for the wonderful Gatherer Extractor program."+'\n'+'\n'+
				"Thanks to zagaberoo from FNM for letting me bounce ideas off of."+'\n'+'\n'+
				"Special thanks to Richard Garfield and the rest of the folks at Wizards of the Coast!"+'\n'+'\n'+
				"They own and copyright all of this stuff, none of it is mine."+'\n'+'\n'+
				"-gelakinetic");
			
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
			case R.id.JSON:
				startThread(OTAPATCH);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void startThread(int type) {
		if(type == DBFROMAPK){
			dialog = new ProgressDialog(main.this);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setMessage("Decompressing database...");
			dialog.setCancelable(false);
			dialog.show();
			threadType = type;
			Thread thread = new Thread(this);
			thread.start();
		}
		else if(type == OTAPATCH){
			//	TODO grab the manifest file, check for differences
			//	If there are, spawn a dialog, parse the JSON into the db

			dialog = new ProgressDialog(main.this);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setMessage("Downloading and parsing an update. Please wait...");
			dialog.setCancelable(false);
			dialog.show();
			
			numCardsAdded = 0;
			threadType = type;
			Thread thread = new Thread(this);
			thread.start();
		}
	}

	public void run() {
		if(threadType == DBFROMAPK){
			copyDB();
			handler.sendEmptyMessage(DBFROMAPK);
		}
		else if(threadType == OTAPATCH){
			try {
				// TODO OTA currently refreshes entire DB from website
				
				mDbHelper = new CardDbAdapter(this);
				mDbHelper.open();
				mDbHelper.dropCreateDB();
				mDbHelper.close();

				parseJSON(new URL("http://members.cox.net/aefeinstein/cards.json.gzip"));
				parseLegality(new URL("http://members.cox.net/aefeinstein/legality.json"));
			}
			catch (MalformedURLException e) {
			}
			handler.sendEmptyMessage(OTAPATCH);
		}
		threadType = -1;
	}

	private Handler	handler	= new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
				case DBFROMAPK:
					dialog.dismiss();
					// TODO check for OTA update after initial add.
					// Do it here so the threads don't overlap
					// startThread(OTAPATCH);
					break;
				case OTAPATCH:
					dialog.dismiss();
					break;
			}
		}
	};

	private void parseJSON(URL cards) {
		try {
			mDbHelper = new CardDbAdapter(this);
			mDbHelper.open();
			GZIPInputStream gis = new GZIPInputStream(cards.openStream());
			JsonCardParser.readJsonStream(gis, this, mDbHelper); // auto sets encoding to UTF8
			mDbHelper.close();
		}
		catch (MalformedURLException e) {
			Log.e("JSON error", e.toString());
		}
		catch (IOException e) {
			Log.e("JSON error", e.toString());
		}
	}
	
	void parseLegality(URL legal){
		try{
			mDbHelper = new CardDbAdapter(this);
			mDbHelper.open();
			InputStream in = new BufferedInputStream(legal.openStream());
			JsonLegalityParser.readJsonStream(in, mDbHelper, getSharedPreferences("prefs", 0));
			mDbHelper.close();
		}
		catch (MalformedURLException e) {
			return;
		}
		catch (IOException e) {
			return;
		}
	}

	private void copyDB(){
		try {
			File folder = new File(DB_PATH);
			if(!folder.exists()){
				folder.mkdir();
			}
			File db = new File(folder, DB_NAME);
			if(db.exists()){
				db.delete();
			}
			if(!db.exists()){
				
				GZIPInputStream gis = new GZIPInputStream(getResources().openRawResource(R.raw.db));
				FileOutputStream fos = new FileOutputStream(db);

				byte[] buffer = new byte[1024];
				int length;
				int totalwritten=0;
				while ((length = gis.read(buffer))>0){
					fos.write(buffer, 0, length);
					totalwritten+=length;
				}

				//Close the streams
				fos.flush();
				fos.close();
				gis.close();
			}
		}
		catch (NotFoundException e) {
		}
		catch (IOException e) {
		}
		catch (Exception e){
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
	
	public void setNumCards(int n) {
		numCards = n;
	}
}