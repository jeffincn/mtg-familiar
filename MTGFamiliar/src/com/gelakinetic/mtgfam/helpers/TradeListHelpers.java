package com.gelakinetic.mtgfam.helpers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;

import com.gelakinetic.mtgfam.activities.CardTradingActivity;
import com.gelakinetic.mtgfam.activities.CardViewActivity;
import com.gelakinetic.mtgfam.activities.WishlistActivity;

public class TradeListHelpers {

	public static final String card_not_found = "Card Not Found";
	public static final String mangled_url = "Mangled URL";
	public static final String database_busy = "Database Busy";
	public static final String card_dne = "Card Does Not Exist";
	public static final String fetch_failed = "Fetch Failed";
	public static final String number_of_invalid = "Number of Cards Invalid";
	public static final String price_invalid = "Price Invalid";

	private static final int LOW_PRICE = 0;
	// private static final int AVG_PRICE = 1;
	private static final int HIGH_PRICE = 2;

	public CardData FetchCardData(Context mCtx, CardData _data) {
		CardData data = _data;
		try {
			CardDbAdapter mDbHelper = new CardDbAdapter(mCtx);
			mDbHelper.openReadable();

			Cursor card;
			if (data.setCode == null || data.setCode.equals(""))
				card = mDbHelper.fetchCardByName(data.name,
						CardDbAdapter.allData);
			else
				card = mDbHelper.fetchCardByNameAndSet(data.name, data.setCode);

			if (card.moveToFirst()) {
				data.name = card.getString(card
						.getColumnIndex(CardDbAdapter.KEY_NAME));
				data.setCode = card.getString(card
						.getColumnIndex(CardDbAdapter.KEY_SET));
				data.tcgName = mDbHelper.getTCGname(data.setCode);
				data.type = card.getString(card
						.getColumnIndex(CardDbAdapter.KEY_TYPE));
				data.cost = card.getString(card
						.getColumnIndex(CardDbAdapter.KEY_MANACOST));
				data.ability = card.getString(card
						.getColumnIndex(CardDbAdapter.KEY_ABILITY));
				data.power = card.getString(card
						.getColumnIndex(CardDbAdapter.KEY_POWER));
				data.toughness = card.getString(card
						.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
				data.loyalty = card.getInt(card
						.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
				data.rarity = card.getInt(card
						.getColumnIndex(CardDbAdapter.KEY_RARITY));
				data.cardNumber = card.getString(card
						.getColumnIndex(CardDbAdapter.KEY_NUMBER));
			}
			card.deactivate();
			card.close();
		} catch (SQLiteException e) {
			data.message = card_not_found;
		} catch (IllegalStateException e) {
			data.message = database_busy;
		}
		return data;
	}

	private class PriceFetchProducer implements Runnable {
		private final LinkedBlockingQueue<FetchPriceTask> queue;
		private final ArrayList<CardData> lCardData;
		private final ArrayList<ArrayList<CardData>> cardSetWishlists;
		private final Object toNotify;
		private final int ps;
		private final CardTradingActivity cta;
		private final WishlistActivity wa;

		PriceFetchProducer(LinkedBlockingQueue<FetchPriceTask> _queue,
				ArrayList<CardData> _lCardData,
				ArrayList<ArrayList<CardData>> _cardSetWishlists, Object _toNotify,
				int _ps, CardTradingActivity _cta, WishlistActivity _wa) {
			queue = _queue;
			lCardData = _lCardData;
			cardSetWishlists = _cardSetWishlists;
			toNotify = _toNotify;
			ps = _ps;
			cta = _cta;
			wa = _wa;
		}

		public void run() {
			try {
				Log.d("TradeListHelpers", "Producer started.");
				while (!fetchComplete) {
					try{
						Log.d("TradeListHelpers", String.format("Putting into %d.",queue.size()));
						queue.put(produce());
						Log.d("TradeListHelpers", "Put.");
					} catch (IndexOutOfBoundsException e) { Thread.currentThread().interrupt(); }
				}
			} catch (InterruptedException ex) {
				Log.d("TradeListHelpers", "Producer ended due to exception.");
				Thread.currentThread().interrupt();
			}
		}

		FetchPriceTask produce() {
			if (lCardData != null) {
				for (CardData card : lCardData)
					if (!card.hasPrice() && ! card.isQueued) {
						card.isQueued = true;
						return new FetchPriceTask(card, toNotify, ps, cta, wa);
					}
			} else {
				for (ArrayList<CardData> cardWishist : cardSetWishlists)
					for (CardData card : cardWishist)
						if (!card.hasPrice() && ! card.isQueued) {
							card.isQueued = true;
							return new FetchPriceTask(card, toNotify, ps, cta, wa);
						}
			}
			fetchComplete = true;
			Log.d("TradeListHelpers", "Producer ended normally.");
			throw new IndexOutOfBoundsException();
		}
	}

	public volatile boolean fetchComplete = false;
	private class PriceFetchConsumer implements Runnable {
		private final LinkedBlockingQueue<FetchPriceTask> queue;

		PriceFetchConsumer(LinkedBlockingQueue<FetchPriceTask> q) {
			queue = q;
		}

		public void run() {
			try {
				Log.d("TradeListHelpers", "Consumer started.");
				while (!fetchComplete || !queue.isEmpty()) {
					Log.d("TradeListHelpers", String.format("Taking from %d.",queue.size()));
					queue.take().execute();
					Log.d("TradeListHelpers", "Took.");
				}
			} catch (InterruptedException ex) {
				Log.d("TradeListHelpers", "Consumer ended.");
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				e.toString();
			}
		}
	}

	public class FetchAllPrices extends QueuedAsyncTask<Void, Void, Integer> {
		private final ArrayList<CardData> lCardData;
		private final ArrayList<ArrayList<CardData>> cardSetWishlists;
		private final Object toNotify;
		private final int ps;
		private final CardTradingActivity cta;
		private final WishlistActivity wa;

		public FetchAllPrices(ArrayList<CardData> _lCardData,
				ArrayList<ArrayList<CardData>> _cardSetWishlists, Object _toNotify,
				int _ps, CardTradingActivity _cta, WishlistActivity _wa) {
			Log.d("TradeListHelpers", "Initializing fetch all prices.");
			lCardData = _lCardData;
			cardSetWishlists = _cardSetWishlists;
			toNotify = _toNotify;
			ps = _ps;
			cta = _cta;
			wa = _wa;
		}

		@Override
		protected void onPreExecute() {}
		@Override
		protected Integer doInBackground(Void... params) {
			Log.d("TradeListHelpers", "Starting fetch all prices.");
			LinkedBlockingQueue<FetchPriceTask> q = new LinkedBlockingQueue<FetchPriceTask>(
					10);

			if (lCardData != null) 
				for (CardData card : lCardData)
						card.isQueued = false;
			else 
				for (ArrayList<CardData> cardWishist : cardSetWishlists)
					for (CardData card : cardWishist)
							card.isQueued = false;
			
			PriceFetchProducer p = new PriceFetchProducer(q, lCardData,
					cardSetWishlists, toNotify, ps, cta, wa);
			PriceFetchConsumer c = new PriceFetchConsumer(q);
			new Thread(p).start();
			new Thread(c).start();
			Log.d("TradeListHelpers", "Finishing fetch all prices.");
			return 0;
		}
		@Override
		protected void onPostExecute(Integer result) {}
		
		@Override
		protected void onCancelled(){
			fetchComplete = true;
			Log.d("TradeListHelpers", "Cancelled.");
		}
	}
	
	public class FetchPriceTask extends QueuedAsyncTask<Void, Void, Integer> {
		CardData data;
		Object toNotify;
		String price = "";
		Context mCtx;
		private int priceSetting;
		private WishlistActivity wa;
		private CardTradingActivity cta;

		public FetchPriceTask(CardData _data, Object _toNotify, int ps,
				CardTradingActivity cta, WishlistActivity wa) {
			data = _data;
			toNotify = _toNotify;
			if (wa != null) {
				mCtx = (Context) wa;
			}
			if (cta != null) {
				mCtx = (Context) cta;
			}
			priceSetting = ps;
			this.cta = cta;
			this.wa = wa;
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Integer doInBackground(Void... params) {
			URL priceurl = null;

			try {
				String cardName = data.name;
				String cardNumber = data.cardNumber == null ? ""
						: data.cardNumber;
				String setCode = data.setCode == null ? "" : data.setCode;
				String tcgName = data.tcgName == null ? "" : data.tcgName;
				if (cardNumber == "" || setCode == "" || tcgName == "") {
					data = FetchCardData(mCtx, data);
					if (data.message == card_not_found
							|| data.message == database_busy) {
						price = data.message;
						return 1;
					}
				}

				if (cardNumber.contains("b")
						&& CardViewActivity.isTransformable(cardNumber,
								data.setCode)) {
					CardDbAdapter mDbHelper = new CardDbAdapter(mCtx);
					mDbHelper.openReadable();
					priceurl = new URL(new String(
							"http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s="
									+ tcgName
									+ "&p="
									+ mDbHelper.getTransformName(setCode,
											cardNumber.replace("b", "a")))
							.replace(" ", "%20").replace("Æ", "Ae"));
					mDbHelper.close();
				} else {
					priceurl = new URL(new String(
							"http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s="
									+ tcgName + "&p=" + cardName).replace(" ",
							"%20").replace("Æ", "Ae"));
				}
			} catch (MalformedURLException e) {
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
			} catch (NumberFormatException e) {
				// If this fails, it means price contains an error string, not a
				// number
				dPrice = 0;
				data.message = price;
			}
			data.price = (int) (dPrice * 100);

			if (wa != null) {
				wa.UpdateTotalPrices();
			} else if (cta != null) {
				cta.UpdateTotalPrices();
			}
			try {
				if (toNotify instanceof ArrayAdapter<?>)
					((ArrayAdapter<?>) toNotify).notifyDataSetChanged();
				else
					((BaseExpandableListAdapter) toNotify)
							.notifyDataSetChanged();
			} catch (Exception e) {
			}
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
			} catch (MalformedURLException e) {
				XMLhandler = null;
			} catch (IOException e) {
				XMLhandler = null;
			} catch (SAXException e) {
				XMLhandler = null;
			} catch (ParserConfigurationException e) {
				XMLhandler = null;
			}

			if (XMLhandler == null || XMLhandler.link == null) {
				return fetch_failed;
			} else {
				if (priceSetting == LOW_PRICE) {
					return XMLhandler.lowprice;
				} else if (priceSetting == HIGH_PRICE) {
					return XMLhandler.hiprice;
				} else {
					return XMLhandler.avgprice;
				}
			}
		}
	}

	public class CardData {

		public String name;
		public String cardNumber;
		public String tcgName;
		public String setCode;
		public int numberOf;
		public int price; // In cents
		public String message;
		public String type;
		public String cost;
		public String ability;
		public String power;
		public String toughness;
		public int loyalty;
		public int rarity;
		public boolean isQueued = false;

		public CardData(String name, String tcgName, String setCode,
				int numberOf, int price, String message, String number,
				String type, String cost, String ability, String p, String t,
				int loyalty, int rarity) {
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

		public CardData(String name, String tcgName, String setCode,
				int numberOf, int price, String message, String number) {
			this.name = name;
			this.cardNumber = number;
			this.setCode = setCode;
			this.tcgName = tcgName;
			this.numberOf = numberOf;
			this.price = price;
			this.message = message;
		}

		public CardData(String cardName, String cardSet, int numberOf) {
			this.name = cardName;
			this.numberOf = numberOf;
			this.setCode = cardSet;
		}

		public String getPriceString() {
			return "$" + String.valueOf(this.price / 100) + "."
					+ String.format("%02d", this.price % 100);
		}

		public boolean hasPrice() {
			return this.message == null || this.message.length() == 0;
		}

		public static final String delimiter = "%";

		public String toString() {
			return this.name + delimiter + this.setCode + delimiter
					+ this.numberOf + '\n';
		}

		public String toString(int side) {
			return side + delimiter + this.name + delimiter + this.setCode
					+ delimiter + this.numberOf + '\n';
		}
	}
}
