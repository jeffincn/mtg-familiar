package com.mtg.fam;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

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
	private static String			DB_INIT = "DB_INIT";
	
	
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
							startThread();
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
				startThread();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void cardAdded() {
		numCardsAdded++;
		dialog.setProgress((100 * numCardsAdded) / numCards);
	}

	public void startThread() {
		dialog = new ProgressDialog(main.this);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMessage("Downloading and Parsing Cards. Please wait...");
		dialog.setCancelable(false);
		dialog.show();

		numCardsAdded = 0;
		Thread thread = new Thread(this);
		thread.start();
	}

	public void run() {
		parseXML();
		handler.sendEmptyMessage(0);
	}

	private Handler	handler	= new Handler() {
														@Override
														public void handleMessage(Message msg) {
															dialog.dismiss();
															mPrefsEdit.putBoolean(DB_INIT, true);
															mPrefsEdit.commit();
														}
													};

	public void parseXML() {
		URL u = null;
		try {
			u = new URL("http://members.cox.net/aefeinstein/cards.xml");
			// For debugging, apoc only:
			// u = new URL("http://members.cox.net/aefeinstein/ap.xml");
		}
		catch (MalformedURLException e3) {
		}

		/* Get a SAXParser from the SAXPArserFactory. */
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = null;
		try {
			sp = spf.newSAXParser();
		}
		catch (ParserConfigurationException e2) {
		}
		catch (SAXException e2) {
		}

		/* Get the XMLReader of the SAXParser we created. */
		XMLReader xr = null;
		try {
			xr = sp.getXMLReader();
		}
		catch (SAXException e1) {
		}
		/* Create a new ContentHandler and apply it to the XML-Reader */
		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();
		mDbHelper.dropCreateDB();

		MtgXMLHandler XMLHandler = new MtgXMLHandler();
		xr.setContentHandler(XMLHandler);
		XMLHandler.setDb(mDbHelper);
		XMLHandler.setMain(this);

		/* Parse the xml-data from our URL. */
		try {
			xr.parse(new InputSource(u.openStream()));
		}
		catch (FileNotFoundException e) {
		}
		catch (IOException e) {
		}
		catch (SAXException e) {
		}
	}

	public void setNumCards(int n) {
		numCards = n;
	}
}
