package com.gelakinetic.mtgfam.helpers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;

import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.TradeFragment;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;

public class TradeListHelpers {

	public static final String	card_not_found		= "Card Not Found";
	public static final String	mangled_url				= "Mangled URL";
	public static final String	database_busy			= "Database Busy";
	public static final String	card_dne					= "Card Does Not Exist";
	public static final String	fetch_failed			= "Fetch Failed";
	public static final String	number_of_invalid	= "Number of Cards Invalid";
	public static final String	price_invalid			= "Price Invalid";

	private static final int		LOW_PRICE					= 0;
	// private static final int AVG_PRICE = 1;
	private static final int		HIGH_PRICE				= 2;

	public static CardData FetchCardData(Context mCtx, CardData _data) {
		CardData data = _data;
		try {
			CardDbAdapter mDbHelper = new CardDbAdapter(mCtx);
			mDbHelper.openReadable();

			Cursor card;
			if (data.setCode == null || data.setCode.equals(""))
				card = mDbHelper.fetchCardByName(data.name, CardDbAdapter.allData);
			else
				card = mDbHelper.fetchCardByNameAndSet(data.name, data.setCode);

			if (card.moveToFirst()) {
				data.name = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NAME));
				data.setCode = card.getString(card.getColumnIndex(CardDbAdapter.KEY_SET));
				data.tcgName = mDbHelper.getTCGname(data.setCode);
				data.type = card.getString(card.getColumnIndex(CardDbAdapter.KEY_TYPE));
				data.cost = card.getString(card.getColumnIndex(CardDbAdapter.KEY_MANACOST));
				data.ability = card.getString(card.getColumnIndex(CardDbAdapter.KEY_ABILITY));
				data.power = card.getString(card.getColumnIndex(CardDbAdapter.KEY_POWER));
				data.toughness = card.getString(card.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
				data.loyalty = card.getInt(card.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
				data.rarity = card.getInt(card.getColumnIndex(CardDbAdapter.KEY_RARITY));
				data.cardNumber = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NUMBER));
			}
			card.close();
		}
		catch (SQLiteException e) {
			data.message = card_not_found;
		}
		catch (IllegalStateException e) {
			data.message = database_busy;
		}
		return data;
	}

	public static final int MAX_SIMULTANEOUS_THREADS = 9; // could be 10, but leaves space for when one is winding down while the next is starting
	public static LinkedBlockingQueue<FetchPriceTask> pendingTasks = new LinkedBlockingQueue<FetchPriceTask>();
	public static ArrayBlockingQueue<FetchPriceTask> currentExecutingTasks = new ArrayBlockingQueue<FetchPriceTask>(MAX_SIMULTANEOUS_THREADS);
	
	public static void addTaskAndExecute(FetchPriceTask fpt){
		pendingTasks.add(fpt);
		executeIfAvailableSpace();
	}
	
	public static void executeIfAvailableSpace()
	{
		if(currentExecutingTasks.size() < MAX_SIMULTANEOUS_THREADS)
		{
			FetchPriceTask toExecute = pendingTasks.poll();
			if(toExecute != null){
				currentExecutingTasks.add(toExecute);
				toExecute.execute();
			}
		}
	}
	
	public static void cancelAllTasks(){
		pendingTasks.clear();
		for(FetchPriceTask fpa : currentExecutingTasks){
			fpa.cancel(true);
		}
	}
	
	public class FetchPriceTask extends AsyncTask<Void, Void, Integer> {
		CardData										data;
		Object											toNotify;
		String											price	= "";
		Context											mCtx;
		private int									priceSetting;
		private WishlistFragment		wf;
		private TradeFragment	cta;

		public FetchPriceTask(CardData _data, Object _toNotify, int ps, TradeFragment cta, WishlistFragment wf) {
			data = _data;
			toNotify = _toNotify;
			if (wf != null) {
				mCtx = wf.getActivity();
			}
			if (cta != null) {
				mCtx = (Context) cta.getActivity();
			}
			priceSetting = ps;
			this.cta = cta;
			this.wf = wf;
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Integer doInBackground(Void... params) {
			URL priceurl = null;

			try {
				String cardName = data.name;
				String cardNumber = data.cardNumber == null ? "" : data.cardNumber;
				String setCode = data.setCode == null ? "" : data.setCode;
				String tcgName = data.tcgName == null ? "" : data.tcgName;
				if (cardNumber == "" || setCode == "" || tcgName == "") {
					data = FetchCardData(mCtx, data);
					if (data.message == card_not_found || data.message == database_busy) {
						price = data.message;
						return 1;
					}
				}

				if (cardNumber.contains("b") && CardViewFragment.isTransformable(cardNumber, data.setCode)) {
					CardDbAdapter mDbHelper = new CardDbAdapter(mCtx);
					mDbHelper.openReadable();
					priceurl = new URL(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgName + "&p="
							+ mDbHelper.getTransformName(setCode, cardNumber.replace("b", "a"))).replace(" ", "%20").replace("Æ", "Ae"));
					mDbHelper.close();
				}
				else {
					priceurl = new URL(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgName + "&p=" + cardName).replace(" ", "%20")
							.replace("Æ", "Ae"));
				}
			}
			catch (MalformedURLException e) {
				priceurl = null;
				price = mangled_url;
				return 1;
			}

			price = fetchPrice(priceurl);

			if (price.equals(fetch_failed)) {
				return 1;
			}
			return 0;
		}

		// Look into progress bar "loading..." style updating

		@Override
		protected void onPostExecute(Integer result) {

			double dPrice;
			try {
				dPrice = Double.parseDouble(price);
				data.message = "";
			}
			catch (NumberFormatException e) {
				// If this fails, it means price contains an error string, not a number
				dPrice = 0;
				data.message = price;
			}
			data.price = (int) (dPrice * 100);

			if (wf != null) {
				wf.UpdateTotalPrices();
			}
			else if (cta != null) {
				cta.UpdateTotalPrices();
			}
			try {
				if (toNotify instanceof ArrayAdapter<?>)
					((ArrayAdapter<CardData>) toNotify).notifyDataSetChanged();
				else
					((BaseExpandableListAdapter) toNotify).notifyDataSetChanged();
			}
			catch (Exception e) {
			}
			
			if(!this.isCancelled()){
				// execute the next task
				executeIfAvailableSpace();
			}
			currentExecutingTasks.remove(this);
		}

		String fetchPrice(URL _priceURL) {
			TCGPlayerXMLHandler XMLhandler;
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
				xr.parse(new InputSource(_priceURL.openStream()));
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

			if (XMLhandler == null || XMLhandler.link == null) {
				return fetch_failed;
			}
			else {
				if (priceSetting == LOW_PRICE) {
					return XMLhandler.lowprice;
				}
				else if (priceSetting == HIGH_PRICE) {
					return XMLhandler.hiprice;
				}
				else {
					return XMLhandler.avgprice;
				}
			}
		}
	}

	public class CardData implements Cloneable {

		public String	name;
		public String	cardNumber;
		public String	tcgName;
		public String	setCode;
		public int		numberOf;
		public int		price;			// In cents
		public String	message;
		public String	type;
		public String	cost;
		public String	ability;
		public String	power;
		public String	toughness;
		public int		loyalty;
		public int		rarity;

		public CardData(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, String type, String cost,
				String ability, String p, String t, int loyalty, int rarity) {
			this.name = name;
			this.cardNumber = number;
			this.setCode = setCode;
			this.tcgName = tcgName;
			this.numberOf = numberOf;
			this.price = price;
			this.message = message;
			this.type = type;
			this.cost = cost;
			this.ability = ability;
			this.power = p;
			this.toughness = t;
			this.loyalty = loyalty;
			this.rarity = rarity;
		}

		public CardData(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, int rarity) {
			this.name = name;
			this.cardNumber = number;
			this.setCode = setCode;
			this.tcgName = tcgName;
			this.numberOf = numberOf;
			this.price = price;
			this.message = message;
			this.rarity = rarity;
		}

		public CardData(String cardName, String cardSet, int numberOf, String number, int rarity) {
			this.name = cardName;
			this.numberOf = numberOf;
			this.setCode = cardSet;
			this.cardNumber = number;
			this.rarity = rarity;
		}

		public String getPriceString() {
			return "$" + String.valueOf(this.price / 100) + "." + String.format("%02d", this.price % 100);
		}

		public boolean hasPrice() {
			return this.message == null || this.message.length() == 0;
		}

		public static final String	delimiter	= "%";

		public String toString() {
			return this.name + delimiter + this.setCode + delimiter + this.numberOf + delimiter + this.cardNumber + delimiter + this.rarity + '\n';
		}

		public String toString(int side) {
			return side + delimiter + this.name + delimiter + this.setCode + delimiter + this.numberOf + '\n';
		}

		public String toReadableString(boolean includeTcgName) {
			return String.valueOf(this.numberOf) + ' ' + this.name + (includeTcgName?" (" + this.tcgName + ')':"") + '\n';
		}

		public Object clone() { 
	        try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} 
		} 
	}
}
