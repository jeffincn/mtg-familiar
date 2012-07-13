/**
Copyright 2011 Adam Feinstein

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
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MyApp;
import com.gelakinetic.mtgfam.helpers.TCGPlayerXMLHandler;

public class CardViewActivity extends FamiliarActivity {

	// Dont use 0, thats the default when the back key is pressed
	protected static final int				RANDOMLEFT		= 2;
	protected static final int				RANDOMRIGHT		= 3;
	protected static final int				QUITTOSEARCH	= 4;
	protected static final int				SWIPELEFT			= 5;
	protected static final int				SWIPERIGHT		= 6;

	// Dialogs
	private static final int					GETLEGALITY		= 0;
	private static final int					GETPRICE			= 1;
	private static final int					GETIMAGE			= 2;
	private static final int					CHANGESET			= 3;
	private static final int					CARDRULINGS		= 4;
	private static final int					BROKEN_IMAGE	= 5;

	// Where the card image is loaded to
	private static final int					MAINPAGE			= 0;
	private static final int					DIALOG				= 1;

	// Random useful things
	private ImageGetter								imgGetter;
	private TextView									copyView;

	// UI elements
	private TextView									name;
	private TextView									cost;
	private TextView									type;
	private TextView									set;
	private TextView									ability;
	private TextView									pt;
	private TextView									flavor;
	private TextView									artist;
	private Button										transform;
	private Button										leftRandom;
	private Button										rightRandom;
	private ImageView									cardpic;
	private ImageView									DialogImageView;

	// Stuff for AsyncTasks
	private BitmapDrawable						cardPicture;
	private String[]									legalities;
	private String[]									formats;
	private TCGPlayerXMLHandler				XMLhandler;
	public ArrayList<Ruling>					rulingsArrayList;
	private ProgressDialog						progDialog;
	AsyncTask<String, Integer, Long>	asyncTask;

	// Card info
	private long											cardID;
	private String										number;
	private String										setCode;
	private String										cardName;
	private String										mtgi_code;
	private int												multiverseId;

	// Preferences
	private int												loadTo;
	private boolean										isRandom;
	private boolean										isSingle;
	private boolean										scroll_results;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.card_view_activity);

		name = (TextView) findViewById(R.id.name);
		cost = (TextView) findViewById(R.id.cost);
		type = (TextView) findViewById(R.id.type);
		set = (TextView) findViewById(R.id.set);
		ability = (TextView) findViewById(R.id.ability);
		flavor = (TextView) findViewById(R.id.flavor);
		artist = (TextView) findViewById(R.id.artist);
		pt = (TextView) findViewById(R.id.pt);
		transform = (Button) findViewById(R.id.transformbutton);
		leftRandom = (Button) findViewById(R.id.randomLeft);
		rightRandom = (Button) findViewById(R.id.randomRight);

		imgGetter = ImageGetterHelper.GlyphGetter(getResources());

		registerForContextMenu(name);
		registerForContextMenu(cost);
		registerForContextMenu(type);
		registerForContextMenu(set);
		registerForContextMenu(ability);
		registerForContextMenu(pt);
		registerForContextMenu(flavor);
		registerForContextMenu(artist);

		Bundle extras = getIntent().getExtras();
		cardID = extras.getLong("id");
		isRandom = extras.getBoolean(SearchActivity.RANDOM);
		isSingle = extras.getBoolean("IsSingle", false);
		if (preferences.getBoolean("picFirst", false)) {
			loadTo = MAINPAGE;
		}
		else {
			loadTo = DIALOG;
		}
		scroll_results = preferences.getBoolean("scrollresults", false);

		progDialog = new ProgressDialog(this);
		progDialog.setTitle("");
		progDialog.setMessage("Loading. Please wait...");
		progDialog.setIndeterminate(true);
		progDialog.setCancelable(true);
		progDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface pd) {
				// TODO when the dialog is dismissed
				asyncTask.cancel(true);
			}
		});

		setInfoFromID(cardID);
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			dismissDialog(GETLEGALITY);
		}
		catch (IllegalArgumentException e) {
		}
		try {
			dismissDialog(GETPRICE);
		}
		catch (IllegalArgumentException e) {
		}
		try {
			dismissDialog(GETIMAGE);
		}
		catch (IllegalArgumentException e) {
		}
		try {
			dismissDialog(CHANGESET);
		}
		catch (IllegalArgumentException e) {
		}
		try {
			dismissDialog(CARDRULINGS);
		}
		catch (IllegalArgumentException e) {
		}
		try {
			dismissDialog(BROKEN_IMAGE);
		}
		catch (IllegalArgumentException e) {
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (progDialog.isShowing()) {
			progDialog.cancel();
		}
		if (asyncTask != null) {
			asyncTask.cancel(true);
		}
	}

	private void setInfoFromID(long id) {

		cardPicture = null;

		Cursor c = mDbHelper.fetchCard(id, null);
		c.moveToFirst();

		// http://magiccards.info/scans/en/mt/55.jpg
		cardName = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
		setCode = c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET));

		mtgi_code = mDbHelper.getCodeMtgi(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)));
		number = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));

		switch ((char) c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY))) {
			case 'C':
				set.setTextColor(this.getResources().getColor(R.color.common));
				break;
			case 'U':
				set.setTextColor(this.getResources().getColor(R.color.uncommon));
				break;
			case 'R':
				set.setTextColor(this.getResources().getColor(R.color.rare));
				break;
			case 'M':
				set.setTextColor(this.getResources().getColor(R.color.mythic));
				break;
			case 'T':
				set.setTextColor(this.getResources().getColor(R.color.timeshifted));
				break;
		}

		String sCost = c.getString(c.getColumnIndex(CardDbAdapter.KEY_MANACOST));
		sCost = sCost.replace("{", "<img src=\"").replace("}", "\"/>");

		CharSequence csCost = Html.fromHtml(sCost, imgGetter, null);

		c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));

		name.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME)));
		cost.setText(csCost);
		type.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_TYPE)));
		set.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)));

		String sAbility = c.getString(c.getColumnIndex(CardDbAdapter.KEY_ABILITY)).replace("{", "<img src=\"")
				.replace("}", "\"/>");
		CharSequence csAbility = Html.fromHtml(sAbility, imgGetter, null);
		ability.setText(csAbility);

		String sFlavor = c.getString(c.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
		CharSequence csFlavor = Html.fromHtml(sFlavor, imgGetter, null);
		flavor.setText(csFlavor);

		artist.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_ARTIST)));

		int loyalty = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
		float p = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_POWER));
		float t = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
		if (loyalty != CardDbAdapter.NOONECARES) {
			pt.setText(Integer.valueOf(loyalty).toString());
		}
		else if (p != CardDbAdapter.NOONECARES && t != CardDbAdapter.NOONECARES) {

			String spt = "";

			if (p == CardDbAdapter.STAR)
				spt += "*";
			else if (p == CardDbAdapter.ONEPLUSSTAR)
				spt += "1+*";
			else if (p == CardDbAdapter.TWOPLUSSTAR)
				spt += "2+*";
			else if (p == CardDbAdapter.SEVENMINUSSTAR)
				spt += "7-*";
			else if (p == CardDbAdapter.STARSQUARED)
				spt += "*^2";
			else {
				if (p == (int) p) {
					spt += (int) p;
				}
				else {
					spt += p;
				}
			}

			spt += "/";

			if (t == CardDbAdapter.STAR)
				spt += "*";
			else if (t == CardDbAdapter.ONEPLUSSTAR)
				spt += "1+*";
			else if (t == CardDbAdapter.TWOPLUSSTAR)
				spt += "2+*";
			else if (t == CardDbAdapter.SEVENMINUSSTAR)
				spt += "7-*";
			else if (t == CardDbAdapter.STARSQUARED)
				spt += "*^2";
			else {
				if (t == (int) t) {
					spt += (int) t;
				}
				else {
					spt += t;
				}
			}

			pt.setText(spt);
		}
		else {
			pt.setText("");
		}

		if (isTransformable(number, c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)))) {
			transform.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					cardPicture = null;
					if (number.contains("a")) {
						number = number.replace("a", "b");
					}
					else if (number.contains("b")) {
						number = number.replace("b", "a");
					}
					long id = mDbHelper.getTransform(setCode, number);
					setInfoFromID(id);
				}
			});
			transform.setVisibility(View.VISIBLE);
		}
		else {
			transform.setVisibility(View.GONE);
		}

		if (isRandom) {
			leftRandom.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent();
					setResult(RANDOMLEFT, i);
					finish();
				}
			});
			rightRandom.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent();
					setResult(RANDOMRIGHT, i);
					finish();
				}
			});
			leftRandom.setVisibility(View.VISIBLE);
			rightRandom.setVisibility(View.VISIBLE);
		}
		else {
			leftRandom.setVisibility(View.GONE);
			rightRandom.setVisibility(View.GONE);
		}

		if (loadTo == MAINPAGE) {
			cardpic = (ImageView) findViewById(R.id.cardpic);

			name.setVisibility(View.GONE);
			cost.setVisibility(View.GONE);
			type.setVisibility(View.GONE);
			set.setVisibility(View.GONE);
			ability.setVisibility(View.GONE);
			pt.setVisibility(View.GONE);
			flavor.setVisibility(View.GONE);
			artist.setVisibility(View.GONE);
			((FrameLayout) findViewById(R.id.frameLayout1)).setVisibility(View.GONE);

			progDialog.show();
			asyncTask = new FetchPictureTask();
			asyncTask.execute((String[]) null);
		}
		else {
			((ImageView) findViewById(R.id.cardpic)).setVisibility(View.GONE);
		}

		if (!isSingle && scroll_results) {
			leftRandom.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent();
					i.putExtra("lastID", cardID);
					setResult(SWIPELEFT, i);
					finish();
				}
			});
			rightRandom.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent();
					i.putExtra("lastID", cardID);
					setResult(SWIPERIGHT, i);
					finish();
				}
			});
			leftRandom.setVisibility(View.VISIBLE);
			rightRandom.setVisibility(View.VISIBLE);
		}

		multiverseId = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_MULTIVERSEID));

		c.close();
	}

	public static boolean isTransformable(String number, String setCode) {
		if (number.contains("a") || number.contains("b")) {
			if (setCode.compareTo("ISD") == 0 || setCode.compareTo("DKA") == 0) {
				return true;
			}
		}
		return false;
	}

	private class FetchLegalityTask extends AsyncTask<String, Integer, Long> {

		@Override
		protected Long doInBackground(String... params) {

			Cursor cFormats = mDbHelper.fetchAllFormats();
			formats = new String[cFormats.getCount()];
			legalities = new String[cFormats.getCount()];

			cFormats.moveToFirst();
			for (int i = 0; i < cFormats.getCount(); i++) {
				formats[i] = cFormats.getString(cFormats.getColumnIndex(CardDbAdapter.KEY_NAME));
				switch (mDbHelper.checkLegality(cardName, formats[i])) {
					case CardDbAdapter.LEGAL:
						legalities[i] = "Legal";
						break;
					case CardDbAdapter.RESTRICTED:
						legalities[i] = "Restricted";
						break;
					case CardDbAdapter.BANNED:
						legalities[i] = "Banned";
						break;
					default:
						legalities[i] = "Error";
						break;
				}
				cFormats.moveToNext();
			}

			cFormats.deactivate();
			cFormats.close();

			return null;
		}

		@Override
		protected void onPostExecute(Long result) {
			try {
				progDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
			}

			showDialog(GETLEGALITY);
		}

		@Override
		protected void onCancelled() {
			// TODO something when canceled?
		}

	}

	private class FetchPictureTask extends AsyncTask<String, Integer, Long> {

		private String	error;

		@Override
		protected Long doInBackground(String... params) {
			error = null;
			try {

				String picurl;
				if (setCode.equals("PP2")) {
					picurl = "http://magiccards.info/extras/plane/planechase-2012-edition/" + cardName + ".jpg";
					picurl = picurl.replace(" ", "-").replace("�", "Ae").replace("?", "").replace(",", "").replace("'", "")
							.replace("!", "");
				}
				else if (setCode.equals("PCP")) {
					if (cardName.equalsIgnoreCase("tazeem")) {
						cardName = "tazeem-release-promo";
						picurl = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
					}
					if (cardName.equalsIgnoreCase("celestine reef")) {
						cardName = "celestine-reef-pre-release-promo";
						picurl = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
					}
					if (cardName.equalsIgnoreCase("horizon boughs")) {
						cardName = "horizon-boughs-gateway-promo";
						picurl = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
					}
					else {
						picurl = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
					}
					picurl = picurl.replace(" ", "-").replace("�", "Ae").replace("?", "").replace(",", "").replace("'", "")
							.replace("!", "");
				}
				else if (setCode.equals("ARS")) {
					picurl = "http://magiccards.info/extras/scheme/archenemy/" + cardName + ".jpg";
					picurl = picurl.replace(" ", "-").replace("�", "Ae").replace("?", "").replace(",", "").replace("'", "")
							.replace("!", "");
				}
				else {
					picurl = "http://magiccards.info/scans/en/" + mtgi_code + "/" + number + ".jpg";
				}
				picurl = picurl.toLowerCase();

				URL u = new URL(picurl);
				cardPicture = new BitmapDrawable(u.openStream());
				Bitmap bmp = cardPicture.getBitmap();

				Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				int newHeight;
				int newWidth;
				float scale;
				if (display.getWidth() < display.getHeight()) {
					scale = (display.getWidth() - 20) / (float) cardPicture.getIntrinsicWidth();
				}
				else {
					scale = (display.getHeight() - 34) / (float) cardPicture.getIntrinsicHeight();
				}
				newWidth = Math.round(cardPicture.getIntrinsicWidth() * scale);
				newHeight = Math.round(cardPicture.getIntrinsicHeight() * scale);

				bmp = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
				cardPicture = new BitmapDrawable(mCtx.getResources(), bmp);

				// Throw this exception to test the dialog
				// throw(new NullPointerException("seriously"));
			}
			catch (FileNotFoundException e) {
				// internet works, image not found
				error = "Image Not Found";
			}
			catch (ConnectException e) {
				// no internet
				error = "No Internet Connection";
			}
			catch (UnknownHostException e) {
				// no internet
				error = "No Internet Connection";
			}
			catch (MalformedURLException e) {
				error = "MalformedURLException";
			}
			catch (IOException e) {
				error = "No Internet Connection";
			}
			catch (NullPointerException e) {
				error = "NPE: Image Not Found";
			}
			return null;
		}

		@Override
		protected void onPostExecute(Long result) {
			try {
				progDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
			}
			if (error == null) {
				if (loadTo == DIALOG) {
					removeDialog(GETIMAGE);
					showDialog(GETIMAGE);
				}
				else if (loadTo == MAINPAGE) {
					cardpic.setImageDrawable(cardPicture);
				}
			}
			else {
				if (error.equalsIgnoreCase("NPE: Image Not Found")) {
					showDialog(BROKEN_IMAGE);
				}
				else {
					Toast.makeText(mCtx, error, Toast.LENGTH_SHORT).show();
				}
				if (loadTo == MAINPAGE) {
					cardpic.setVisibility(View.GONE);
					name.setVisibility(View.VISIBLE);
					cost.setVisibility(View.VISIBLE);
					type.setVisibility(View.VISIBLE);
					set.setVisibility(View.VISIBLE);
					ability.setVisibility(View.VISIBLE);
					pt.setVisibility(View.VISIBLE);
					flavor.setVisibility(View.VISIBLE);
					artist.setVisibility(View.VISIBLE);
					((FrameLayout) findViewById(R.id.frameLayout1)).setVisibility(View.VISIBLE);
				}
			}
		}

		@Override
		protected void onCancelled() {
			// TODO something when canceled?
			if (loadTo == MAINPAGE) {
				cardpic.setVisibility(View.GONE);
				name.setVisibility(View.VISIBLE);
				cost.setVisibility(View.VISIBLE);
				type.setVisibility(View.VISIBLE);
				set.setVisibility(View.VISIBLE);
				ability.setVisibility(View.VISIBLE);
				pt.setVisibility(View.VISIBLE);
				flavor.setVisibility(View.VISIBLE);
				artist.setVisibility(View.VISIBLE);
				((FrameLayout) findViewById(R.id.frameLayout1)).setVisibility(View.VISIBLE);
			}
		}
	}

	private class FetchPriceTask extends AsyncTask<String, Integer, Long> {

		private String	error;

		@Override
		protected Long doInBackground(String... params) {
			error = null;
			URL priceurl;
			try {
				XMLhandler = null;

				String tcgname = mDbHelper.getTCGname(setCode);
				String tcgCardName;
				if (isTransformable(number, setCode) && number.contains("b")) {
					tcgCardName = mDbHelper.getTransformName(setCode, number.replace("b", "a"));
				}
				else if (mDbHelper.isSplitCard(multiverseId)) {
					tcgCardName = mDbHelper.getSplitName(multiverseId);
				}
				else {
					tcgCardName = cardName;
				}
				priceurl = new URL(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgname + "&p="
						+ tcgCardName).replace(" ", "%20").replace("�", "Ae"));

				// Get a SAXParser from the SAXPArserFactory.
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();

				// Get the XMLReader of the SAXParser we created.
				XMLReader xr = sp.getXMLReader();
				// Create a new ContentHandler and apply it to the XML-Reader
				XMLhandler = new TCGPlayerXMLHandler();
				xr.setContentHandler(XMLhandler);

				// Parse the xml-data from our URL.
				xr.parse(new InputSource(priceurl.openStream()));
				// Parsing has finished.
			}
			catch (FileNotFoundException e) {
				// internet works, price not found
				error = "Card Price Not Found";
			}
			catch (ConnectException e) {
				// no internet
				error = "No Internet Connection";
			}
			catch (MalformedURLException e) {
				error = "MalformedURLException";
				XMLhandler = null;
			}
			catch (IOException e) {
				error = "No Internet Connection";
				XMLhandler = null;
			}
			catch (SAXException e) {
				error = "SAXException";
				XMLhandler = null;
			}
			catch (ParserConfigurationException e) {
				error = "ParserConfigurationException";
				XMLhandler = null;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Long result) {
			try {
				progDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
			}

			if (XMLhandler != null && XMLhandler.hiprice == null && error == null) {
				Toast.makeText(mCtx, "Card Price Not Found", Toast.LENGTH_SHORT).show();
				return;
			}
			if (error == null) {
				removeDialog(GETPRICE);
				showDialog(GETPRICE);
			}
			else {
				Toast.makeText(mCtx, error, Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected void onCancelled() {
			// TODO something when canceled?
		}
	}

	private class FetchRulingsTask extends AsyncTask<String, Integer, Long> {

		private boolean	error	= false;

		@Override
		protected Long doInBackground(String... params) {

			URL url;
			InputStream is = null;
			DataInputStream dis;
			String line;

			rulingsArrayList = new ArrayList<Ruling>();

			try {
				url = new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + multiverseId);
				is = url.openStream(); // throws an IOException
				dis = new DataInputStream(new BufferedInputStream(is));

				String date = null, ruling;
				while ((line = dis.readLine()) != null) {
					if (line.contains("rulingDate") && line.contains("</td>")) {
						date = (line.replace("<autocard>", "").replace("</autocard>", "")).split(">")[1].split("<")[0];
					}
					if (line.contains("rulingText") && line.contains("</td>")) {
						ruling = (line.replace("<autocard>", "").replace("</autocard>", "")).split(">")[1].split("<")[0];
						Ruling r = new Ruling(date, ruling);
						rulingsArrayList.add(r);
					}
				}
			}
			catch (MalformedURLException mue) {
				// mue.printStackTrace();
				error = true;
			}
			catch (IOException ioe) {
				error = true;
			}
			finally {
				try {
					if (is != null) {
						is.close();
					}
				}
				catch (IOException ioe) {
					error = true;
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Long result) {
			try {
				progDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
			}

			if (!error) {
				showDialog(CARDRULINGS);
			}
			else {
				Toast.makeText(mCtx, "No Internet Connection", Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected void onCancelled() {
			// TODO something when canceled?
		}
	}

	private static class Ruling {
		public String	date, ruling;

		public Ruling(String d, String r) {
			date = d;
			ruling = r;
		}

		public String toString() {
			return date + ": " + ruling;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		if (id == GETIMAGE) {

			if (cardPicture == null) {
				return null;
			}

			dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.image_dialog);

			DialogImageView = (ImageView) dialog.findViewById(R.id.cardimage);
			DialogImageView.setImageDrawable(cardPicture);

			return dialog;
		}
		else if (id == BROKEN_IMAGE) {
			View dialogLayout = getLayoutInflater().inflate(R.layout.corruption_layout, null);
			TextView text = (TextView) dialogLayout.findViewById(R.id.corruption_message);
			text.setText(Html.fromHtml(getString(R.string.brokenImageString)));
			text.setMovementMethod(LinkMovementMethod.getInstance());

			dialog = new AlertDialog.Builder(this).setTitle(R.string.corruption_error_title).setView(dialogLayout)
					.setPositiveButton(android.R.string.ok, null).create();

			return dialog;
		}
		else if (id == GETLEGALITY) {
			if (formats == null) {
				return null;
			}

			dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.legality_dialog);

			// create the grid item mapping
			String[] from = new String[] { "format", "status" };
			int[] to = new int[] { R.id.format, R.id.status };

			// prepare the list of all records
			List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
			for (int i = 0; i < formats.length; i++) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put(from[0], formats[i]);
				map.put(from[1], legalities[i]);
				fillMaps.add(map);
			}

			SimpleAdapter adapter = new SimpleAdapter(this, fillMaps, R.layout.legal_row, from, to);
			ListView lv = (ListView) dialog.findViewById(R.id.legallist);
			lv.setAdapter(adapter);
			return dialog;
		}
		else if (id == GETPRICE) { // price

			if (XMLhandler == null) {
				return null;
			}

			dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.price_dialog);

			TextView l = (TextView) dialog.findViewById(R.id.low);
			TextView m = (TextView) dialog.findViewById(R.id.med);
			TextView h = (TextView) dialog.findViewById(R.id.high);
			TextView pricelink = (TextView) dialog.findViewById(R.id.pricelink);

			l.setText("$" + XMLhandler.lowprice);
			m.setText("$" + XMLhandler.avgprice);
			h.setText("$" + XMLhandler.hiprice);
			pricelink.setMovementMethod(LinkMovementMethod.getInstance());
			pricelink.setText(Html.fromHtml("<a href=\"" + XMLhandler.link + "\">" + getString(R.string.tcgplayerlink)
					+ "</a>"));
			return dialog;
		}
		else if (id == CHANGESET) {
			try {
				Cursor c = mDbHelper.fetchCardByName(cardName);
				Set<String> sets = new LinkedHashSet<String>();
				Set<Long> cardIds = new LinkedHashSet<Long>();
				while (!c.isAfterLast()) {
					if (sets.add(mDbHelper.getTCGname(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET))))) {
						cardIds.add(c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID)));
					}
					c.moveToNext();
				}
				c.deactivate();
				c.close();

				final String[] aSets = sets.toArray(new String[sets.size()]);
				final Long[] aIds = cardIds.toArray(new Long[cardIds.size()]);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Pick a Set");
				builder.setItems(aSets, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int item) {
						setInfoFromID(aIds[item]);
					}
				});
				dialog = builder.create();
				return dialog;
			}
			catch (SQLException e) {
				// Should we do something here?
			}
		}
		else if (id == CARDRULINGS) {

			if (rulingsArrayList == null) {
				return null;
			}

			dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.rulings_dialog);

			TextView textViewRules = (TextView) dialog.findViewById(R.id.rules);
			TextView textViewUrl = (TextView) dialog.findViewById(R.id.url);

			String message = "";
			if (rulingsArrayList.size() == 0) {
				message = "No rulings for this card";
			}
			else {
				for (Ruling r : rulingsArrayList) {
					message += (r.toString() + "<br><br>");
				}

				message = message.replace("{", "<img src=\"").replace("}", "\"/>");
			}
			CharSequence messageGlyph = Html.fromHtml(message, imgGetter, null);

			textViewRules.setText(messageGlyph);

			textViewUrl.setMovementMethod(LinkMovementMethod.getInstance());
			textViewUrl.setText(Html.fromHtml("<a href=http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid="
					+ multiverseId + ">Gatherer Page</a>"));

			return dialog;
		}
		return null;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		copyView = (TextView) v;

		android.view.MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.copy_menu, menu);
	}

	private static final boolean	useOldClipboard	= (android.os.Build.VERSION.SDK_INT < 11);

	@SuppressLint("NewApi")
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		// use a final static boolean for JIT compile-time culling of deprecated
		// calls for future-proofing
		// this is probably overkill because the old name space will likely be
		// retained for backwards compatibility
		// Scoped name space usage is poor practice, but direct references allow us
		// to target the correct SDK.
		String copyText = "";
		switch (item.getItemId()) {
			case R.id.copy:
				copyText = copyView.getText().toString();
				break;
			case R.id.copyall:
				copyText = name.getText().toString() + '\n' + cost.getText().toString() + '\n' + type.getText().toString()
						+ '\n' + set.getText().toString() + '\n' + ability.getText().toString() + '\n'
						+ flavor.getText().toString() + '\n' + pt.getText().toString() + '\n' + artist.getText().toString();
				break;
			default:
				return super.onContextItemSelected(item);
		}

		if (useOldClipboard) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(copyText);
			return true;
		}
		else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(copyText);
			return true;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.image:
				progDialog.show();
				asyncTask = new FetchPictureTask();
				asyncTask.execute((String[]) null);
				return true;
			case R.id.price:
				progDialog.show();
				asyncTask = new FetchPriceTask();
				asyncTask.execute((String[]) null);
				return true;
			case R.id.changeset:
				showDialog(CHANGESET);
				return true;
			case R.id.legality:
				progDialog.show();
				asyncTask = new FetchLegalityTask();
				asyncTask.execute((String[]) null);
				return true;
			case R.id.cardrulings:
				progDialog.show();
				asyncTask = new FetchRulingsTask();
				asyncTask.execute((String[]) null);
				return true;
			case R.id.quittosearch:
				MyApp appState = ((MyApp) getApplicationContext());
				appState.setState(QUITTOSEARCH);
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
			case GETIMAGE:
				if (DialogImageView != null) {
					DialogImageView.setImageDrawable(cardPicture);
				}
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.card_menu, menu);
		return true;
	}
}