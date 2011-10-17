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
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class CardViewActivity extends Activity implements Runnable {

	private static final int			PICLOAD		= 0;
	private static final int			PRICELOAD	= 1;
	protected static final int		TRANSFORM	= 7;
	protected static final String	NUMBER		= "number";
	protected static final String	SET				= "set";
	protected static final String	ISSINGLE	= "isSingle";
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
	private Cursor								formats		= null;
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
	private ImageGetter	imgGetter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.card_view_activity);

		imgGetter = ImageGetterHelper.GlyphGetter(getResources());
		
		Bundle extras = getIntent().getExtras();
		cardID = extras.getLong("id");

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();

		c = mDbHelper.fetchCard(cardID);
		c.moveToFirst();

		// http://magiccards.info/scans/en/mt/55.jpg
		cardName = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
		setCode = c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET));

		mtgi_code = mDbHelper.getCodeMtgi(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)));
		number = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));
		if(setCode.equals("PCP")){
			cardName = cardName.replace("�", "Ae");
			if(cardName.equalsIgnoreCase("tazeem")){
				cardName = "tazeem-release-promo";
				picurl = "http://magiccards.info/extras/plane/planechase/"+ cardName +".jpg";
			}
			if(cardName.equalsIgnoreCase("celestine reef")){
				cardName = "celestine-reef-pre-release-promo";
				picurl = "http://magiccards.info/extras/plane/planechase/"+ cardName +".jpg";
			}
			if(cardName.equalsIgnoreCase("horizon boughs")){
				cardName = "horizon-boughs-gateway-promo";
				picurl = "http://magiccards.info/extras/plane/planechase/"+ cardName +".jpg";
			}
			else{
				picurl = "http://magiccards.info/extras/plane/planechase/"+ cardName +".jpg";
				picurl = picurl.replace(" ","-");
			}
		}
		else if(setCode.equals("ARS")){
			picurl = "http://magiccards.info/extras/scheme/archenemy/"+ cardName +".jpg";
			picurl = picurl.replace(" ","-");
		}
		else{
			picurl = "http://magiccards.info/scans/en/" + mtgi_code + "/" + number + ".jpg";
		}
		picurl = picurl.toLowerCase();


		
		try {
			String tcgname = mDbHelper.getTCGname(setCode);
			if(number.contains("b")){
				priceurl = new URL(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgname + "&p="
						+ mDbHelper.getTransformName(setCode, number.replace("b", "a"))).replace(" ", "%20").replace("�", "Ae"));
			}
			else{
				priceurl = new URL(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgname + "&p="
						+ cardName).replace(" ", "%20").replace("�", "Ae"));
			}
		}
		catch (MalformedURLException e) {
			priceurl = null;
		}

		name = (TextView) findViewById(R.id.name);
		cost = (TextView) findViewById(R.id.cost);
		type = (TextView) findViewById(R.id.type);
		set = (TextView) findViewById(R.id.set);
		ability = (TextView) findViewById(R.id.ability);
		flavor = (TextView) findViewById(R.id.flavor);
		artist = (TextView) findViewById(R.id.artist);
		pt = (TextView) findViewById(R.id.pt);
		transform = (Button) findViewById(R.id.transformbutton);

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
			pt.setText(new Integer(loyalty).toString());
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

		if (number.contains("a") || number.contains("b")) {
			transform.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent();
					i.putExtra(SET, setCode);
					i.putExtra(NUMBER, number);
					setResult(TRANSFORM, i);
					finish();// transform!
					// Froyo+ only, disabled animations
					// overridePendingTransition(0, 0);
				}
			});
		}
		else {
			transform.setVisibility(View.GONE);
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
		if (formats != null) {
			formats.deactivate();
			formats.close();
		}
		if (c != null) {
			c.deactivate();
			c.close();
		}
		if (mDbHelper != null) {
			mDbHelper.close();
		}
		if (bmp != null) {
			bmp.recycle();
		}
	}

	Drawable drawable_from_url(String url, String src_name) throws java.net.MalformedURLException, java.io.IOException {
		return Drawable.createFromStream(((java.io.InputStream) new java.net.URL(url).getContent()), src_name);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.card_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.image:
				showDialog(0);
				return true;
			case R.id.price:
				showDialog(2);
				return true;
			case R.id.legality:
				showDialog(1);
				return true;
			case R.id.gatherer:
				String url = "http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + c.getInt(c.getColumnIndex(CardDbAdapter.KEY_MULTIVERSEID));
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(browserIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == 0) {
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.image_dialog);

			image = (ImageView) dialog.findViewById(R.id.cardimage);

			threadtype = PICLOAD;
			Thread thread = new Thread(this);
			thread.start();

			return dialog;
		}
		else if (id == 1) {
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.legality_dialog);

			formats = mDbHelper.fetchAllFormats();

			LegalListAdapter lla = new LegalListAdapter(this, R.layout.legal_row, formats, new String[] {
					CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_NAME }, new int[] { R.id.format, R.id.status }, cardID, mDbHelper, setCode);
			ListView lv = (ListView) dialog.findViewById(R.id.legallist);
			lv.setAdapter(lla);

			return dialog;
		}
		else if (id == 2) { // price
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.price_dialog);

			l = (TextView) dialog.findViewById(R.id.low);
			m = (TextView) dialog.findViewById(R.id.med);
			h = (TextView) dialog.findViewById(R.id.high);
			pricelink = (TextView) dialog.findViewById(R.id.pricelink);

			l.setText("Loading");
			m.setText("Loading");
			h.setText("Loading");
			pricelink.setText("");

			threadtype = PRICELOAD;
			Thread thread = new Thread(this);
			thread.start();

			return dialog;
		}
		return null;
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
					if(setCode.equals("PCP")){
						Matrix mat = new Matrix();
		        mat.setRotate(90);
		        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);
						float scale = (display.getWidth() - 20) / (float) d.getIntrinsicHeight();
						newWidth = Math.round(bmp.getWidth() * scale);
						newHeight = Math.round(bmp.getHeight() * scale);
					}
					else{
						float scale = (display.getWidth() - 20) / (float) d.getIntrinsicWidth();
						newWidth = Math.round(bmp.getWidth() * scale);
						newHeight = Math.round(bmp.getHeight() * scale);
					}
					bmp = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
					d = new BitmapDrawable(bmp);
				}
				catch (IOException e) {
					d = (BitmapDrawable) getResources().getDrawable(R.drawable.nonet);
				}
				catch (Exception e) {
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

	private Handler							handler	= new Handler() {
																				@Override
																				public void handleMessage(Message msg) {
																					switch (msg.what) {
																						case PICLOAD:
																							image.setImageDrawable(d);
																							break;
																						case PRICELOAD:
																							if (XMLhandler == null || XMLhandler.link == null) {
																								l.setText("No");
																								m.setText("Internet");
																								h.setText("Connection");
																							}
																							else {
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

	private TCGPlayerXMLHandler	XMLhandler;

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
		}
		catch (MalformedURLException e) {
			XMLhandler = null;
		}
		catch (IOException e) {
			XMLhandler = null;
		}
		catch (SAXException e) {
			XMLhandler = null;
		}
		catch (ParserConfigurationException e) {
			XMLhandler = null;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
