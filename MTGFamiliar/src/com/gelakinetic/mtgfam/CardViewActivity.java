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

package com.gelakinetic.mtgfam;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.MenuInflater;
import android.view.MenuItem;
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

// I don't like doing this, but we've gotta stick with the old clipboard
@SuppressWarnings("deprecation")
public class CardViewActivity extends FragmentActivity implements Runnable {

	private static final int			PICLOAD				= 0;
	private static final int			PRICELOAD			= 1;

	// Dont use 0, thats the default when the back key is pressed
	protected static final int		RANDOMLEFT		= 2;
	protected static final int		RANDOMRIGHT		= 3;
	protected static final int		QUITTOSEARCH	= 4;
	protected static final int		SWIPELEFT			= 5;
	protected static final int		SWIPERIGHT		= 6;

	protected static final String	NUMBER				= "number";
	protected static final String	SET						= "set";
	protected static final String	ISSINGLE			= "isSingle";
	private static final String		NO_INTERNET		= "no_internet";
	private static final int			GETLEGALITY		= 0;
	private static final int			GETPRICE			= 1;
	private static final int			GETIMAGE			= 2;
	private static final int			CHANGESET			= 3;
	private static final int			CARDRULINGS		= 4;
	private static final int			MAINPAGE			= 0;
	private static final int			DIALOG				= 1;
	private String								NO_ERROR			= "no_error";
	private CardDbAdapter					mDbHelper;
	private TextView							name;
	private TextView							cost;
	private TextView							type;
	private TextView							set;
	private TextView							ability;
	private TextView							pt;
	private TextView							flavor;
	private TextView							artist;
	private Cursor								c;
	private ImageView							image;
	private BitmapDrawable				d;
	private Bitmap								bmp;
	private long									cardID;
	private String								mtgi_code;
	private URL										priceurl;
	private String								picurl;
	private int										threadtype;
	private TextView							l;
	private TextView							m;
	private TextView							h;
	private TextView							pricelink;
	private Button								transform;
	private String								number;
	private String								setCode;
	private String								cardName;
	private ImageGetter						imgGetter;
	private TCGPlayerXMLHandler		XMLhandler;
	private String								priceErrType	= NO_ERROR;
	private SharedPreferences			preferences;
	private ImageView							cardpic;
	private int										loadTo;
	private boolean								failedLoad		= false;
	private Button								leftRandom;
	private Button								rightRandom;
	private String[]							legalities;
	private String[]							formats;
	private boolean								isRandom;
	private boolean								isSingle;
	private int									multiverseId;
	private boolean 							newImage;
	private boolean 							newPriceData;
	public ArrayList<Ruling>	ar;
	private Context	mCtx;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.card_view_activity);

		mCtx = this;
		
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

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		Bundle extras = getIntent().getExtras();
		cardID = extras.getLong("id");
		isRandom = extras.getBoolean(SearchActivity.RANDOM);
		isSingle = extras.getBoolean("IsSingle", false);

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();

		setInfoFromID(cardID);

		MenuFragmentCompat.init(this, R.menu.card_menu, "card_view_menu_fragment");
	}

	@Override
	protected void onResume() {
		super.onResume();
		MyApp appState = ((MyApp) getApplicationContext());
		if (appState.getState() == QUITTOSEARCH) {
			this.finish();
			return;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mDbHelper != null) {
			mDbHelper.close();
		}
		if (bmp != null) {
			bmp.recycle();
		}
	}

	private void setInfoFromID(long id) {
		c = mDbHelper.fetchCard(id, null);
		c.moveToFirst();

		// http://magiccards.info/scans/en/mt/55.jpg
		cardName = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
		setCode = c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET));

		mtgi_code = mDbHelper.getCodeMtgi(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)));
		number = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));
		if (setCode.equals("PCP")) {
			cardName = cardName.replace("Æ", "Ae");
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
			} else {
				picurl = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
				picurl = picurl.replace(" ", "-");
			}
		} else if (setCode.equals("ARS")) {
			picurl = "http://magiccards.info/extras/scheme/archenemy/" + cardName + ".jpg";
			picurl = picurl.replace(" ", "-");
		} else {
			picurl = "http://magiccards.info/scans/en/" + mtgi_code + "/" + number + ".jpg";
		}
		picurl = picurl.toLowerCase();

		try {
			String tcgname = mDbHelper.getTCGname(setCode);
			if (number.contains("b")) {
				priceurl = new URL(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgname + "&p="
						+ mDbHelper.getTransformName(setCode, number.replace("b", "a"))).replace(" ", "%20").replace("Æ", "Ae"));
			} else {
				priceurl = new URL(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgname + "&p="
						+ cardName).replace(" ", "%20").replace("Æ", "Ae"));
			}
		} catch (MalformedURLException e) {
			priceurl = null;
		}

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
		} else if (p != CardDbAdapter.NOONECARES && t != CardDbAdapter.NOONECARES) {

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
				} else {
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
				} else {
					spt += t;
				}
			}

			pt.setText(spt);
		} else {
			pt.setText("");
		}

		if (isTransformable(number, c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)))){
			transform.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (number.contains("a")) {
						number = number.replace("a", "b");
					} else if (number.contains("b")) {
						number = number.replace("b", "a");
					}
					long id = mDbHelper.getTransform(setCode, number);
					setInfoFromID(id);
				}
			});
			transform.setVisibility(View.VISIBLE);
		} else {
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
		} else {
			leftRandom.setVisibility(View.GONE);
			rightRandom.setVisibility(View.GONE);
		}

		if (preferences.getBoolean("picFirst", false)) {
			cardpic = (ImageView) findViewById(R.id.cardpic);
			loadTo = MAINPAGE;
			threadtype = PICLOAD;
			Thread thread = new Thread(this);
			thread.start();

			name.setVisibility(View.GONE);
			cost.setVisibility(View.GONE);
			type.setVisibility(View.GONE);
			set.setVisibility(View.GONE);
			ability.setVisibility(View.GONE);
			pt.setVisibility(View.GONE);
			flavor.setVisibility(View.GONE);
			artist.setVisibility(View.GONE);

			((FrameLayout) findViewById(R.id.frameLayout1)).setVisibility(View.GONE);
		} else {
			((ImageView) findViewById(R.id.cardpic)).setVisibility(View.GONE);
		}

		if (!isSingle && preferences.getBoolean("scrollresults", false)) {
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
		
		newImage = true;
		newPriceData = true;
	}

	public static boolean isTransformable(String number, String set) {
		if(number.contains("a") || number.contains("b")){
			if(set.compareTo("ISD")==0 || set.compareTo("DKA") == 0){
				return true;
			}
		}
		return false;
	}

	Drawable drawable_from_url(String url, String src_name) throws java.net.MalformedURLException, java.io.IOException {
		return Drawable.createFromStream(((java.io.InputStream) new java.net.URL(url).getContent()), src_name);
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
			showDialog(GETLEGALITY);
		}

	}

	private class FetchRulingsTask extends AsyncTask<String, Integer, Long> {

		private boolean error = false;
		
		@Override
		protected Long doInBackground(String... params) {

			URL url;
			InputStream is = null;
			DataInputStream dis;
			String line;

			ar = new ArrayList<Ruling>();
			
			try {
				url = new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + multiverseId);
				is = url.openStream(); // throws an IOException
				dis = new DataInputStream(new BufferedInputStream(is));

				String date = null, ruling;
				while ((line = dis.readLine()) != null) {
					if(line.contains("rulingDate") && line.contains("</td>")){
						date = line.split(">")[1].split("<")[0];
					}
					if(line.contains("rulingText") && line.contains("</td>")){
						ruling = line.split(">")[1].split("<")[0];
						Ruling r = new Ruling(date, ruling);
						ar.add(r);
					}
				}
			}
			catch (MalformedURLException mue) {
				//mue.printStackTrace();
				error = true;
			}
			catch (IOException ioe) {
				error = true;				
			}
			finally {
				try {
					if(is != null){
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
			progDialog.dismiss();
			if(!error){
				showDialog(CARDRULINGS);
			}
			else{
				Toast.makeText(mCtx, "No Internet Connection", Toast.LENGTH_SHORT).show();
			}
		}

	}

	private static class Ruling{
		public String date, ruling;
		public Ruling(String d, String r)
		{
			date = d;
			ruling = r;
		}
		
		public String toString(){
			return date + ": " + ruling;
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == GETIMAGE) {
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.image_dialog);

			image = (ImageView) dialog.findViewById(R.id.cardimage);
			loadTo = DIALOG;

			return dialog;
		} else if (id == GETLEGALITY) {
			if (formats == null)
				return null;

			Dialog dialog = new Dialog(this);
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
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.price_dialog);

			l = (TextView) dialog.findViewById(R.id.low);
			m = (TextView) dialog.findViewById(R.id.med);
			h = (TextView) dialog.findViewById(R.id.high);
			pricelink = (TextView) dialog.findViewById(R.id.pricelink);

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
				return builder.create();
			}
			catch (SQLException e) {
				//Should we do something here?
			}
		}
		else if(id == CARDRULINGS){
			
			if(ar.size() == 0)
			{
				Toast.makeText(this, "No Rulings For This Card", Toast.LENGTH_SHORT).show();
				return null;
			}
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Card Rulings");

			String message="";
			for(Ruling r : ar)
			{
				message += (r.toString()+'\n'+'\n');
			}
			
			builder.setMessage(message);
			return builder.create();
		}
		return null;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id) {
		case GETIMAGE:
			if(newImage) {
				image.setImageResource(R.drawable.loading);
				
				threadtype = PICLOAD;
				Thread thread = new Thread(this);
				thread.start();
				
				newImage = false;
			}
			break;
		case GETPRICE:
			if(newPriceData) {
				l.setText("Loading");
				m.setText("Loading");
				h.setText("Loading");
				pricelink.setText("");

				threadtype = PRICELOAD;
				Thread thread = new Thread(this);
				thread.start();
				
				newPriceData = false;
			}
		}
	}

	public void run() {
		switch (threadtype) {
		case (PICLOAD):
			try {
				URL u = new URL(picurl);
				d = new BitmapDrawable(u.openStream());
				bmp = d.getBitmap();

				Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				int newHeight;
				int newWidth;
				float scale;
				if (display.getWidth() < display.getHeight()) {
					scale = (display.getWidth() - 20) / (float) d.getIntrinsicWidth();
				} else {
					DisplayMetrics metrics = new DisplayMetrics();
					getWindowManager().getDefaultDisplay().getMetrics(metrics);
					int myHeight = display.getHeight() - 24;

					scale = (myHeight - 10) / (float) d.getIntrinsicHeight();
				}
				newWidth = Math.round(bmp.getWidth() * scale);
				newHeight = Math.round(bmp.getHeight() * scale);

				bmp = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
				d = new BitmapDrawable(bmp);
			} catch (IOException e) {
				d = (BitmapDrawable) getResources().getDrawable(R.drawable.nonet);
				failedLoad = true;
			} catch (Exception e) {
				d = (BitmapDrawable) getResources().getDrawable(R.drawable.nonet);
			}
			handler.sendEmptyMessage(PICLOAD);
			break;
		case (PRICELOAD):
			fetchPrices();
			handler.sendEmptyMessage(PRICELOAD);
			break;
		}
	}

	private Handler		handler	= new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PICLOAD:
				if (loadTo == MAINPAGE) {
					if (failedLoad) {
						cardpic.setVisibility(View.GONE);
						name.setVisibility(View.VISIBLE);
						cost.setVisibility(View.VISIBLE);
						type.setVisibility(View.VISIBLE);
						set.setVisibility(View.VISIBLE);
						ability.setVisibility(View.VISIBLE);
						pt.setVisibility(View.VISIBLE);
						flavor.setVisibility(View.VISIBLE);
						artist.setVisibility(View.VISIBLE);
					} else {
						cardpic.setImageDrawable(d);
					}
				} else if (loadTo == DIALOG) {
					image.setImageDrawable(d);
				}
				break;
			case PRICELOAD:
				if (priceErrType.equals(NO_INTERNET)) {
					l.setText("No");
					m.setText("Internet");
					h.setText("Connection");
				} else if (XMLhandler == null || XMLhandler.link == null) {
					l.setText("Price");
					m.setText("Fetch");
					h.setText("Failed");
				} else {
					l.setText("$" + XMLhandler.lowprice);
					m.setText("$" + XMLhandler.avgprice);
					h.setText("$" + XMLhandler.hiprice);

					pricelink.setMovementMethod(LinkMovementMethod.getInstance());
					pricelink.setText(Html.fromHtml("<a href=\"" + XMLhandler.link + "\">"
							+ getString(R.string.tcgplayerlink) + "</a>"));
				}
				break;
			}
		}
	};
	private TextView	copyView;
	private ProgressDialog	progDialog;

	void fetchPrices() {
		try {
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
		} catch (MalformedURLException e) {
			XMLhandler = null;
		} catch (IOException e) {
			priceErrType = NO_INTERNET;
			XMLhandler = null;
		} catch (SAXException e) {
			XMLhandler = null;
		} catch (ParserConfigurationException e) {
			XMLhandler = null;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		copyView = (TextView) v;

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.copy_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		switch (item.getItemId()) {
		case R.id.copy:
			String text = copyView.getText().toString();

			clipboard.setText(text);
			return true;

		case R.id.copyall:
			String cat = name.getText().toString() + '\n' + cost.getText().toString() + '\n' + type.getText().toString()
					+ '\n' + set.getText().toString() + '\n' + ability.getText().toString() + '\n' + flavor.getText().toString()
					+ '\n' + pt.getText().toString() + '\n' + artist.getText().toString();

			clipboard.setText(cat);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.image:
			showDialog(GETIMAGE);
			return true;
		case R.id.price:
			showDialog(GETPRICE);
			return true;
		case R.id.changeset:
			showDialog(CHANGESET);
			return true;
		case R.id.legality:
			new FetchLegalityTask().execute((String[]) null);
			return true;
		case R.id.gatherer:
			String url = "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + multiverseId;
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(browserIntent);
			return true;
		case R.id.cardrulings:
			progDialog = ProgressDialog.show(this, "", "Loading. Please wait...", true);
			progDialog.show();
			new FetchRulingsTask().execute((String[]) null);
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
}
