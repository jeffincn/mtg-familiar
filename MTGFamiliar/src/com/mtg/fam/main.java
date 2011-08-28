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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class main extends Activity implements Runnable {
	private static final String DB_PATH = "/data/data/com.mtg.fam/databases/";
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
		
		File f = new File(DB_PATH, "data");
		if(!f.exists()){
			startThread(DBFROMAPK);
		}
		else{
			startThread(OTAPATCH);
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

	/*
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.refreshDB:
				startThread("dbFromApk");
				return true;
			case R.id.JSON:
				startThread("JSON");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
*/
	
	public void cardAdded() {
		numCardsAdded++;
		if(numCards!=0){
			dialog.setProgress((100 * numCardsAdded) / numCards);
		}
		else{
			dialog.setProgress(numCardsAdded);
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
			
			/*
			dialog = new ProgressDialog(main.this);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setMessage(type + ": Downloading and Parsing Cards. Please wait...");
			dialog.setCancelable(false);
			dialog.show();
		*/
			
			try {
				parseJSON(new URL("www.cardstoparse.com"));
			}
			catch (MalformedURLException e) {
			}
			
			numCardsAdded = 0;
			threadType = type;
			Thread thread = new Thread(this);
			thread.start();
		}
	}

	public void run() {
		if(threadType == DBFROMAPK){
			copyDB();
			parseLegality(false);
			handler.sendEmptyMessage(DBFROMAPK);
		}
		else if(threadType == OTAPATCH){
			parseLegality(true);
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
					startThread(OTAPATCH);
					break;
				case OTAPATCH:
					break;
			}
		}
	};

	public void setNumCards(int n) {
		numCards = n;
	}

	private void parseJSON(URL cards) {
		try {
			mDbHelper = new CardDbAdapter(this);
			mDbHelper.open();
			mDbHelper.dropCreateDB();

			JsonCardParser jp = new JsonCardParser(this, mDbHelper);
			//URL cards = new URL("http://members.cox.net/aefeinstein/cards.json.gzip");
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

	private void copyDB(){
		try {
			File folder = new File(DB_PATH);
			if(!folder.exists()){
				folder.mkdir();
			}
			File db = new File(folder, "data");
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
	
	void parseLegality(boolean isCheck){
		try{
			mDbHelper = new CardDbAdapter(this);
			mDbHelper.open();
			URL legal = new URL("http://members.cox.net/aefeinstein/legality.json");
			InputStream in = new BufferedInputStream(legal.openStream());
			JsonLegalityParser.readJsonStream(in, mDbHelper, isCheck, getSharedPreferences("prefs", 0));
			mDbHelper.close();
		}
		catch (MalformedURLException e) {
			return;
		}
		catch (IOException e) {
			return;
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
				"Special thanks to Richard Garfield and the rest of the folks at Wizards of the Coast!"+'\n'+'\n'+
				"They own and copyright all of this stuff, none of it is mine."+'\n'+'\n'+
				"-gelakinetic");
			
		return aboutDialog;
		/*
		TextView tv = null;
		tv
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("About " + getString(R.string.app_name))
						.setMessage()
		       .setCancelable(false).
		       setNeutralButton("Thanks!", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
               dialog.cancel();
          }
      });

		AlertDialog alert = builder.create();
		return alert;
		*/
	}
}