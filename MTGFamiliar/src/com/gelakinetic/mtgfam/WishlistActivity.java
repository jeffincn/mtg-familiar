/**
Copyright 2012 

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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WishlistActivity extends FragmentActivity {
	private Context								mCtx;
	private final static int			DIALOG_UPDATE_CARD		= 1;
	private final static int			DIALOG_PRICE_SETTING	= 2;
	private int										positionForDialog;

	private AutoCompleteTextView	namefield;

	private Button								bAdd;
	private TextView							tradePrice;
	private ListView							lvWishlist;
	private TradeListAdapter			aaWishlist;
	private ArrayList<CardData>		lWishlist;

	private CardDbAdapter					mdbAdapter;
	private EditText							numberfield;

	private int										priceSetting;

	public static final String		card_not_found				= "Card Not Found";
	public static final String		mangled_url						= "Mangled URL";
	public static final String		database_busy					= "Database Busy";
	public static final String		card_dne							= "Card Does Not Exist";
	public static final String		fetch_failed					= "Fetch Failed";
	public static final String		number_of_invalid			= "Number of Cards Invalid";
	public static final String		price_invalid					= "Price Invalid";

	private static final int			LOW_PRICE							= 0;
	private static final int			AVG_PRICE							= 1;
	private static final int			HIGH_PRICE						= 2;

	private static final String		wishlistName				= "card.wishlist";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wishlist_activity);

		MenuFragmentCompat.init(this, R.menu.wishlist_menu, "wishlist_menu_fragment");

		mCtx = this;
		mdbAdapter = new CardDbAdapter(this).openReadable();

		namefield = (AutoCompleteTextView) findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(this, null));

		numberfield = (EditText) findViewById(R.id.numberInput);
		numberfield.setText("1");

		lWishlist = new ArrayList<CardData>();
		bAdd = (Button) findViewById(R.id.addCard);
		tradePrice = (TextView) findViewById(R.id.priceText);
		lvWishlist = (ListView) findViewById(R.id.wishlist);
		aaWishlist = new TradeListAdapter(mCtx, R.layout.wishlist_row, lWishlist);
		lvWishlist.setAdapter(aaWishlist);

		bAdd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().toString().length() > 0) {
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					int numberOf = Integer.parseInt(numberOfFromField);

					CardData data = new CardData(namefield.getText().toString(), "", "", numberOf, 0, "loading", "");

					lWishlist.add(0, data);
					aaWishlist.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(data, aaWishlist);
					loadPrice.execute();
					namefield.setText("");
					numberfield.setText("1");
				}
				else {
					Toast.makeText(getApplicationContext(), getString(R.string.type_card_first), Toast.LENGTH_SHORT).show();
				}
			}
		});

		lvWishlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		priceSetting = Integer.parseInt(prefs.getString("tradePrice", String.valueOf(AVG_PRICE)));

		//ensure the existence of the wishlist
		String[] files = fileList();
		Boolean wishlistExists = false;
		for (String fileName : files) {
			if (fileName.equals(wishlistName)) {
				wishlistExists = true;
			}
		}
		if (wishlistExists) {

			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(openFileInput(wishlistName)));
	
				lWishlist.clear();
	
				String line;
				String[] parts;
				while ((line = br.readLine()) != null) {
					parts = line.split(CardData.delimiter);
	
					String cardName = parts[0];
					String cardSet = parts[1];
					String tcgName = mdbAdapter.getTCGname(cardSet);
					int numberOf = Integer.parseInt(parts[2]);
	
					CardData cd = new CardData(cardName, tcgName, cardSet, numberOf, 0, "loading", Integer
							.toString(numberOf));
					lWishlist.add(0, cd);
					FetchPriceTask loadPrice = new FetchPriceTask(lWishlist.get(0), aaWishlist);
					loadPrice.execute();
				}
			}
			catch (NumberFormatException e) {
				Toast.makeText(getApplicationContext(), "NumberFormatException", Toast.LENGTH_LONG).show();
			}
			catch (IOException e) {
				Toast.makeText(getApplicationContext(), "IOException", Toast.LENGTH_LONG).show();
			}
			aaWishlist.notifyDataSetChanged();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		try {
			dismissDialog(DIALOG_UPDATE_CARD);
		}
		catch (IllegalArgumentException e) {
		}
		try {
			dismissDialog(DIALOG_PRICE_SETTING);
		}
		catch (IllegalArgumentException e) {
		}

		MyApp appState = ((MyApp) getApplicationContext());
		appState.setState(0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		if (c != null) {
//			c.deactivate();
//			c.close();
//		}
		if (mdbAdapter != null) {
			mdbAdapter.close();
		}
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder;
		switch (id) {
			case DIALOG_UPDATE_CARD: {
				final int position = positionForDialog;
				final ArrayList<CardData> lList = lWishlist;
				final TradeListAdapter aaList = aaWishlist;
				final int numberOfCards = lList.get(position).getNumberOf();
				final String priceOfCard = lList.get(position).getPriceString();

				View view = LayoutInflater.from(mCtx).inflate(R.layout.trader_card_click_dialog, null);
				Button removeAll = (Button) view.findViewById(R.id.traderDialogRemove);
				Button changeSet = (Button) view.findViewById(R.id.traderDialogChangeSet);
				Button cancelbtn = (Button) view.findViewById(R.id.traderDialogCancel);
				Button donebtn = (Button) view.findViewById(R.id.traderDialogDone);
				final EditText numberOf = (EditText) view.findViewById(R.id.traderDialogNumber);
				final EditText priceText = (EditText) view.findViewById(R.id.traderDialogPrice);

				builder = new AlertDialog.Builder(WishlistActivity.this);
				builder.setTitle(lList.get(position).getName()).setView(view);

				dialog = builder.create();

				String numberOfStr = String.valueOf(numberOfCards);
				numberOf.setText(numberOfStr);
				numberOf.setSelection(numberOfStr.length());

				String priceNumberStr = lList.get(position).hasPrice() ? priceOfCard.substring(1) : "";
				priceText.setText(priceNumberStr);
				priceText.setSelection(priceNumberStr.length());

				removeAll.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						lList.remove(position);
						aaList.notifyDataSetChanged();
						UpdateTotalPrices();
						removeDialog(DIALOG_UPDATE_CARD);
					}
				});

				changeSet.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						removeDialog(DIALOG_UPDATE_CARD);
						ChangeSet(position);
					}
				});

				cancelbtn.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						removeDialog(DIALOG_UPDATE_CARD);
					}
				});

				donebtn.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {

						// validate number of cards text
						if (numberOf.length() == 0) {
							Toast.makeText(getApplicationContext(), number_of_invalid, Toast.LENGTH_LONG).show();
							return;
						}

						// validate the price text
						String userInputPrice = priceText.getText().toString();
						double uIP;
						try {
							uIP = Double.parseDouble(userInputPrice);
							// Clear the message so the user's specified price will display
							lList.get(position).setMessage("");
						}
						catch (NumberFormatException e) {
							uIP = 0;
						}

						lList.get(position).setNumberOf(Integer.parseInt(numberOf.getEditableText().toString()));
						lList.get(position).setPrice((int) Math.round(uIP * 100));
						aaList.notifyDataSetChanged();
						UpdateTotalPrices();

						removeDialog(DIALOG_UPDATE_CARD);
					}
				});

				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						removeDialog(DIALOG_UPDATE_CARD);
					}
				});

				dialog.show();
				break;
			}
			case DIALOG_PRICE_SETTING: {
				builder = new AlertDialog.Builder(this);

				builder.setTitle("Price Options");
				builder.setSingleChoiceItems(new String[] { "Low", "Average", "High" }, priceSetting,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								priceSetting = which;
								dialog.dismiss();

								// Update ALL the prices!
								for (CardData data : lWishlist) {
									data.setMessage("loading");
									FetchPriceTask task = new FetchPriceTask(data, aaWishlist);
									task.execute();
								}
								aaWishlist.notifyDataSetChanged();

								// And also update the preference
								SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WishlistActivity.this)
										.edit();
								edit.putString("tradePrice", String.valueOf(priceSetting));
								edit.commit();

								removeDialog(DIALOG_PRICE_SETTING);
							}
						});

				dialog = builder.create();

				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						removeDialog(DIALOG_PRICE_SETTING);
					}
				});

				dialog.show();
				break;
			}
			default: {
				dialog = null;
			}
		}
		return dialog;
	}

	protected void ChangeSet(final int _position) {
		CardData data = lWishlist.get(_position);
		String name = data.getName();

		Cursor cards = mdbAdapter.fetchCardByName(name);
		Set<String> sets = new LinkedHashSet<String>();
		Set<String> setCodes = new LinkedHashSet<String>();
		while (!cards.isAfterLast()) {
			if (sets.add(mdbAdapter.getTCGname(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET))))) {
				setCodes.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET)));
			}
			cards.moveToNext();
		}
		cards.deactivate();
		cards.close();

		final String[] aSets = sets.toArray(new String[sets.size()]);
		final String[] aSetCodes = setCodes.toArray(new String[setCodes.size()]);
		AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
		builder.setTitle("Pick a Set");
		builder.setItems(aSets, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int item) {
				lWishlist.get(_position).setSetCode(aSetCodes[item]);
				lWishlist.get(_position).setTcgName(aSets[item]);
				lWishlist.get(_position).setMessage("loading");
				aaWishlist.notifyDataSetChanged();
				FetchPriceTask loadPrice = new FetchPriceTask(lWishlist.get(_position), aaWishlist);
				loadPrice.execute();
				return;
			}
		});
		builder.create().show();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Read values from the "savedInstanceState"-object and put them in your
		// textview
		namefield.setText(savedInstanceState.getString("nameBox"));
		tradePrice.setText(savedInstanceState.getString("totalPrice"));

		ArrayList<String> cardDataIn = savedInstanceState.getStringArrayList("wishlistCards");
		for (String card : cardDataIn) {
			String[] cardData = card.split("\\|");
			int numberOf = Integer.parseInt(cardData[6]);
			CardData data = new CardData(cardData[0], cardData[1], cardData[2], numberOf, Integer.parseInt(cardData[3]),
					cardData[4], cardData[5]);

			lWishlist.add(data);

			if (data.getMessage().equals("loading")) {
				FetchPriceTask loadPrice = new FetchPriceTask(data, aaWishlist);
				loadPrice.execute();
			}
		}
		aaWishlist.notifyDataSetChanged();
		UpdateTotalPrices();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("nameBox", namefield.getText().toString());
		outState.putString("totalPrice", (String) tradePrice.getText());

		ArrayList<String> cardDataOut = new ArrayList<String>();
		for (CardData data : lWishlist) {
			String cardData = String.format("%s|%s|%s|%s|%s|%s|%s", data.getName(), data.getTcgName(),
					data.getSetCode(), data.getPrice(), data.getMessage(), data.getNumber(), data.getNumberOf());
			cardDataOut.add(cardData);
		}
		outState.putStringArrayList("wishlistCards", cardDataOut);

		// Save the values you need from your textview into "outState"-object
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();
		try {
			FileOutputStream fos = openFileOutput(wishlistName, Context.MODE_PRIVATE);
			for (CardData cd : lWishlist) {
				fos.write(cd.toString().getBytes());
			}
			fos.close();
		}
		catch (FileNotFoundException e) {
			Toast.makeText(getApplicationContext(), "FileNotFoundException", Toast.LENGTH_LONG).show();
		}
		catch (IOException e) {
			Toast.makeText(getApplicationContext(), "IOException", Toast.LENGTH_LONG).show();
		}
	}
	
	private void UpdateTotalPrices() {
		int totalPrice = GetPricesFromTradeList(lWishlist);
		int color = PriceListHasBadValues(lWishlist) ? mCtx.getResources().getColor(R.color.red) : mCtx.getResources()
				.getColor(R.color.white);
		String sTotal = "$" + (totalPrice / 100) + "." + String.format("%02d", (totalPrice % 100));
		tradePrice.setText(sTotal);
		tradePrice.setTextColor(color);
	}

	private boolean PriceListHasBadValues(ArrayList<CardData> trade) {
		for (CardData data : trade) {
			if (!data.hasPrice()) {
				return true;
			}
		}
		return false;
	}

	private int GetPricesFromTradeList(ArrayList<CardData> _trade) {
		int totalPrice = 0;

		for (int i = 0; i < _trade.size(); i++) {// CardData data : _trade) {
			CardData data = _trade.get(i);
			if (data.hasPrice()) {
				totalPrice += data.getNumberOf() * data.getPrice();
			}
			else {
				String message = data.getMessage();

				// Remove the card from the list, unless it was just a fetch failed.
				// Otherwise, the card does not exist, or there is a database problem

				if (message.compareTo(card_not_found) == 0) {
					_trade.remove(data);
					i--;
					aaWishlist.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + card_not_found, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(mangled_url) == 0) {
					_trade.remove(data);
					i--;
					aaWishlist.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + mangled_url, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(database_busy) == 0) {
					_trade.remove(data);
					i--;
					aaWishlist.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + database_busy, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(card_dne) == 0) {
					_trade.remove(data);
					i--;
					aaWishlist.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + card_dne, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(fetch_failed) == 0) {

				}
			}
		}
		return totalPrice;
	}

	private class CardData {

		private String	name;
		private String	number;
		private String	tcgName;
		private String	setCode;
		private int			numberOf;
		private int			price;		// In cents
		private String	message;

		public CardData(String name, String tcgName, String setCode, int numberOf, int price, String message, String number) {
			this.name = name;
			this.number = number;
			this.setCode = setCode;
			if (setCode.length() > 0) {

			}
			this.tcgName = tcgName;
			this.numberOf = numberOf;
			this.price = price;
			this.message = message;
		}

		public String getName() {
			return this.name;
		}

		public String getNumber() {
			return this.number;
		}

		public void setNumber(String newNumber) {
			this.number = newNumber;
		}

		public String getTcgName() {
			return this.tcgName;
		}

		public void setTcgName(String newTcgName) {
			this.tcgName = newTcgName;
		}

		public String getSetCode() {
			return this.setCode;
		}

		public void setSetCode(String newSetCode) {
			this.setCode = newSetCode;
		}

		public int getNumberOf() {
			return this.numberOf;
		}

		public void setNumberOf(int newNumber) {
			this.numberOf = newNumber;
		}

		public int getPrice() {
			return this.price;
		}

		public String getPriceString() {
			return "$" + String.valueOf(this.price / 100) + "." + String.format("%02d", this.price % 100);
		}

		public void setPrice(int newPrice) {
			this.price = newPrice;
		}

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String newMessage) {
			this.message = newMessage;
		}

		public boolean hasPrice() {
			return this.message == null || this.message.length() == 0;
		}

		public static final String	delimiter	= "%";

		public String toString() {
			return this.getName() + delimiter + this.getSetCode() + delimiter + this.getNumberOf() + '\n';
		}

	}

	private class TradeListAdapter extends ArrayAdapter<CardData> {

		private int									layoutResourceId;
		private ArrayList<CardData>	items;
		private Context							mCtx;

		public TradeListAdapter(Context context, int textViewResourceId, ArrayList<CardData> items) {
			super(context, textViewResourceId, items);

			this.mCtx = context;
			this.layoutResourceId = textViewResourceId;
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inf = getLayoutInflater();
				v = inf.inflate(layoutResourceId, null);
			}
			CardData data = items.get(position);
			if (data != null) {
				TextView nameField = (TextView) v.findViewById(R.id.wishlistRowName);
				TextView setField = (TextView) v.findViewById(R.id.wishlistRowSet);
				TextView numberField = (TextView) v.findViewById(R.id.wishlistNumber);
				TextView priceField = (TextView) v.findViewById(R.id.wishlistRowPrice);

				nameField.setText(data.getName());
				setField.setText(data.getTcgName());
				numberField.setText(data.hasPrice() ? data.getNumberOf() + "x" : "");
				priceField.setText(data.hasPrice() ? data.getPriceString() : data.getMessage());

				if (data.hasPrice()) {
					priceField.setTextColor(mCtx.getResources().getColor(R.color.light_gray));
				}
				else {
					priceField.setTextColor(mCtx.getResources().getColor(R.color.red));
				}
			}
			return v;
		}
	}

//	private void fillData(Cursor c) {
//
//		ArrayList<String> fromList = new ArrayList<String>();
//		ArrayList<Integer> toList = new ArrayList<Integer>();
//		fromList.add(CardDbAdapter.KEY_NAME);
//		toList.add(R.id.cardname);
//		if (preferences.getBoolean("setPref", true)) {
//			fromList.add(CardDbAdapter.KEY_SET);
//			toList.add(R.id.cardset);
//		}
//		if (preferences.getBoolean("manacostPref", true)) {
//			fromList.add(CardDbAdapter.KEY_MANACOST);
//			toList.add(R.id.cardcost);
//		}
//		if (preferences.getBoolean("typePref", true)) {
//			fromList.add(CardDbAdapter.KEY_TYPE);
//			toList.add(R.id.cardtype);
//		}
//		if (preferences.getBoolean("abilityPref", true)) {
//			fromList.add(CardDbAdapter.KEY_ABILITY);
//			toList.add(R.id.cardability);
//		}
//		if (preferences.getBoolean("ptPref", true)) {
//			fromList.add(CardDbAdapter.KEY_POWER);
//			toList.add(R.id.cardp);
//			fromList.add(CardDbAdapter.KEY_TOUGHNESS);
//			toList.add(R.id.cardt);
//			fromList.add(CardDbAdapter.KEY_LOYALTY);
//			toList.add(R.id.cardt);
//		}
//		String[] from = new String[fromList.size()];
//		fromList.toArray(from);
//
//		int[] to = new int[toList.size()];
//		for (int i = 0; i < to.length; i++) {
//			to[i] = toList.get(i);
//		}
//
//		ResultListAdapter rla = new ResultListAdapter(this, R.layout.card_row, c, from, to, this.getResources());
//		lv.setAdapter(rla);
//	}
	
	private class FetchPriceTask extends AsyncTask<Void, Void, Integer> {
		CardData					data;
		TradeListAdapter	toNotify;
		String						price		= "";
		String						setCode	= "";
		String						tcgName	= "";

		public FetchPriceTask(CardData _data, TradeListAdapter _toNotify) {
			data = _data;
			toNotify = _toNotify;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			String cardName;
			String number;
			try {
				cardName = data.getName();
				number = data.getNumber();
				setCode = data.getSetCode();
				tcgName = data.getTcgName();
				if (number.equals("") || setCode.equals("") || tcgName.equals("")) {
					Cursor card;
					card = mdbAdapter.fetchCardByName(data.getName());
					if (card.moveToFirst()) {
						cardName = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NAME));
						if (data.getSetCode().equals("")) {
							setCode = card.getString(card.getColumnIndex(CardDbAdapter.KEY_SET));
							tcgName = mdbAdapter.getTCGname(setCode);

							data.setSetCode(setCode);
							data.setTcgName(tcgName);
						}

						number = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NUMBER));
						data.setNumber(number);

						card.deactivate();
						card.close();
					}
					else {
						price = card_dne;
						card.deactivate();
						card.close();
						return 1;
					}
				}
			}
			catch (SQLiteException e) {
				price = card_not_found;
				return 1;
			}
			catch (IllegalStateException e) {
				price = database_busy;
				return 1;
			}

			URL priceurl = null;

			try {
				if (number.contains("b") && CardViewActivity.isTransformable(number, data.setCode)) {
					priceurl = new URL(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgName + "&p="
							+ mdbAdapter.getTransformName(setCode, number.replace("b", "a"))).replace(" ", "%20").replace("Æ", "Ae"));
				}
				else {
					priceurl = new URL(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgName + "&p="
							+ cardName).replace(" ", "%20").replace("Æ", "Ae"));
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
			data.setTcgName(tcgName);
			data.setSetCode(setCode);
			double dPrice;
			try {
				dPrice = Double.parseDouble(price);
				data.setMessage("");
			}
			catch (NumberFormatException e) {
				// If this fails, it means price contains an error string, not a number
				dPrice = 0;
				data.setMessage(price);
			}
			data.setPrice((int) (dPrice * 100));

			UpdateTotalPrices();
			toNotify.notifyDataSetChanged();
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.wishlist_menu_clear:
				lWishlist.clear();
				aaWishlist.notifyDataSetChanged();
				UpdateTotalPrices();
				return true;
			case R.id.wishlist_menu_settings:
				showDialog(DIALOG_PRICE_SETTING);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
