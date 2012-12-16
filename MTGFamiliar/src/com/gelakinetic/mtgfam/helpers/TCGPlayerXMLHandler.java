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

package com.gelakinetic.mtgfam.helpers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.Toast;

import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers.CardData;

public class TCGPlayerXMLHandler extends DefaultHandler {

	// ===========================================================
	// Fields
	// ===========================================================
	/*
	 * private boolean in_products = false; private boolean in_product = false;
	 * private boolean in_id = false;
	 */
	private boolean	in_hiprice	= false;
	private boolean	in_lowprice	= false;
	private boolean	in_avgprice	= false;
	private boolean	in_link			= false;

	public String		highprice;
	public String		avgprice;
	public String		lowprice;
	public String		link;

	// ===========================================================
	// Methods
	// ===========================================================
	@Override
	public void startDocument() throws SAXException {
		/*
		 * in_products = false; in_product = false; in_id = false;
		 */
		in_hiprice = false;
		in_lowprice = false;
		in_avgprice = false;
		in_link = false;
	}

	@Override
	public void endDocument() throws SAXException {
	}

	/**
	 * Gets be called on opening tags like: <tag> Can provide attribute(s), when
	 * xml was like: <tag attribute="attributeValue">
	 */
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		/*
		 * if (localName.equals("products")) { in_products = true; } else if
		 * (localName.equals("product")) { in_product = true; } else if
		 * (localName.equals("id")) { in_id = true; }
		 * 
		 * else
		 */if (localName.equals("hiprice")) {
			in_hiprice = true;
		}
		else if (localName.equals("lowprice")) {
			in_lowprice = true;
		}
		else if (localName.equals("avgprice")) {
			in_avgprice = true;
		}
		else if (localName.equals("link")) {
			in_link = true;
		}
	}

	/**
	 * Gets be called on closing tags like: </tag>
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		/*
		 * if (localName.equals("products")) { in_products = false; } else if
		 * (localName.equals("product")) { in_product = false; } else if
		 * (localName.equals("id")) { in_id = false; } else
		 */if (localName.equals("hiprice")) {
			in_hiprice = false;
		}
		else if (localName.equals("lowprice")) {
			in_lowprice = false;
		}
		else if (localName.equals("avgprice")) {
			in_avgprice = false;
		}
		else if (localName.equals("link")) {
			in_link = false;
		}
	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		String s = new String(ch, start, length);
		if (in_hiprice) {
			highprice = s;
		}
		if (in_lowprice) {
			lowprice = s;
		}
		if (in_avgprice) {
			avgprice = s;
		}
		if (in_link) {
			if (link == null) {
				link = s;
			}
			else {
				link += s;
			}
		}
	}
	
	public static class onFetchPriceCompleteListener {
		public void onFetchPriceSuccess(TCGPlayerXMLHandler XMLhandler) {
		}
		public void onFetchPriceFail(String error) {
		}
	}

	public static class FetchPriceTask extends AsyncTask<Void, Void, Void> {

		private String	error;
		private TCGPlayerXMLHandler				XMLhandler;
		private CardDbAdapter mDbHelper;
		private String setCode;
		private String number;
		private int rarity = 0;
		private int multiverseId;
		private String cardName;
		private onFetchPriceCompleteListener listener;
		private MainActivity mCtx;

		public FetchPriceTask(CardDbAdapter mDbHelper, CardData cd, MainActivity mCtx) {
			this.mDbHelper = mDbHelper;
			this.cardName = cd.name;
			this.setCode = cd.setCode;
			this.number = cd.cardNumber;
			try {
				this.multiverseId = this.mDbHelper.fetchMultiverseId(cd.name, cd.setCode);
			} catch (FamiliarDbException e) {
				this.multiverseId = -1;
			}
			this.mCtx = mCtx;
		}
		
		public FetchPriceTask(CardDbAdapter mDbHelper, String cardName, String setCode, String number, int multiverseId, MainActivity mCtx) {
			this.mDbHelper = mDbHelper;
			this.cardName = cardName;
			this.setCode = setCode;
			this.number = number;
			this.multiverseId = multiverseId;
			this.mCtx = mCtx;
		}
		
		public void setOnFetchPriceCompleteListener(onFetchPriceCompleteListener listener) {
			this.listener = listener;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			error = null;
			URL priceurl;
			try {
				XMLhandler = null;

				if(number == null) {
					Cursor c = mDbHelper.fetchCardByNameAndSet(cardName, setCode);
					number = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));
					rarity = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY));
					c.close();
				}
				
				String tcgname = mDbHelper.getTCGname(setCode);
				String tcgCardName;
				if (CardDbAdapter.isTransformable(number, setCode) && number.contains("b")) {
					tcgCardName = mDbHelper.getTransformName(setCode, number.replace("b", "a"));
				}
				else if (multiverseId!= -1 && mDbHelper.isSplitCard(multiverseId)) {
					tcgCardName = mDbHelper.getSplitName(multiverseId);
				}
				else {
					tcgCardName = cardName;
				}
				priceurl = new URL(CardDbAdapter.removeAccentMarks(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgname + "&p="
						+ tcgCardName).replace(" ", "%20").replace("Æ", "Ae")));

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
			} catch (FamiliarDbException e) {
				error = "FamiliarDbException";
				XMLhandler = null;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			
			if (XMLhandler != null && XMLhandler.highprice == null && error == null) {
				this.listener.onFetchPriceFail("Card Price Not Found");
				Toast.makeText(mCtx, "Card Price Not Found", Toast.LENGTH_SHORT).show();
			}
			else if (error == null) {
				this.listener.onFetchPriceSuccess(XMLhandler);
			}
			else {
				this.listener.onFetchPriceFail(error);
				if(error.equals("FamiliarDbException")) {
					mCtx.showDbErrorToast();
					mCtx.getSupportFragmentManager().popBackStack();
					return;
				}
				else {
					Toast.makeText(mCtx, error, Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
}