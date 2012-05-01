/**
Copyright 2012 Jonathan Bettger

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
import java.util.ArrayList;

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
import android.text.Editable;
import android.text.TextWatcher;
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

public class CardTradingActivity extends FragmentActivity {
	private Context															mCtx;
	private final static int DIALOG_UPDATE_CARD					= 1;
	private final static int DIALOG_PRICE_SETTING = 2;
	private String sideForDialog;
	private int positionForDialog;
	
	private AutoCompleteTextView								namefield;

	private Button															bAddTradeLeft;
	private TextView														tradePriceLeft;
	private ListView														lvTradeLeft;
	private TradeListAdapter												aaTradeLeft;
	private ArrayList<CardData>	lTradeLeft;

	private Button															bAddTradeRight;
	private TextView														tradePriceRight;
	private ListView														lvTradeRight;
	private TradeListAdapter												aaTradeRight;
	private ArrayList<CardData>	lTradeRight;

	private CardDbAdapter												mdbAdapter;
	private EditText	numberfield;
	
	private int priceSetting;
	
	public static final String card_not_found = "Card Not Found";
	public static final String mangled_url = "Mangled URL";
	public static final String database_busy = "Database Busy";
	public static final String card_dne = "Card Does Not Exist";
	public static final String fetch_failed = "Fetch Failed";

	private static final int LOW_PRICE = 0;
	private static final int AVG_PRICE = 1;
	private static final int HIGH_PRICE = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trader_activity);

		MenuFragmentCompat.init(this, R.menu.trader_menu, "trading_menu_fragment");

		mCtx = this;
		mdbAdapter = new CardDbAdapter(this);

		namefield = (AutoCompleteTextView) findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(this, null));

		numberfield = (EditText)findViewById(R.id.numberInput);
		numberfield.setText("1");
		
		lTradeLeft = new ArrayList<CardData>();
		bAddTradeLeft = (Button) findViewById(R.id.addCardLeft);
		tradePriceLeft = (TextView) findViewById(R.id.priceTextLeft);
		lvTradeLeft = (ListView) findViewById(R.id.tradeListLeft);
		aaTradeLeft = new TradeListAdapter(mCtx, R.layout.trader_row, lTradeLeft);
		lvTradeLeft.setAdapter(aaTradeLeft);

		lTradeRight = new ArrayList<CardData>();
		bAddTradeRight = (Button) findViewById(R.id.addCardRight);
		tradePriceRight = (TextView) findViewById(R.id.priceTextRight);
		lvTradeRight = (ListView) findViewById(R.id.tradeListRight);
		aaTradeRight = new TradeListAdapter(mCtx, R.layout.trader_row, lTradeRight);
		lvTradeRight.setAdapter(aaTradeRight);

		bAddTradeLeft.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().toString().length() > 0) {
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					int numberOf = Integer.parseInt(numberOfFromField);

					CardData data = new CardData(namefield.getText().toString(), "", "", numberOf, "loading");

					lTradeLeft.add(data);
					aaTradeLeft.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(data, aaTradeLeft);
					loadPrice.execute();
					namefield.setText("");
					numberfield.setText("1");
				}
				else{
					Toast.makeText(getApplicationContext(), getString(R.string.type_card_first), Toast.LENGTH_SHORT).show();
				}
			}
		});

		bAddTradeRight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().toString().length() > 0) {
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					int numberOf = Integer.parseInt(numberOfFromField);

					CardData data = new CardData(namefield.getText().toString(), "", "", numberOf, "loading");

					lTradeRight.add(data);
					aaTradeRight.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(data, aaTradeRight);
					loadPrice.execute();
					namefield.setText("");
					numberfield.setText("1");
				}
				else{
					Toast.makeText(getApplicationContext(), getString(R.string.type_card_first), Toast.LENGTH_SHORT).show();
				}
			}
		});

		lvTradeLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				sideForDialog = "left";
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});
		lvTradeRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				sideForDialog = "right";
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		priceSetting = Integer.parseInt(prefs.getString("tradePrice", String.valueOf(AVG_PRICE)));
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		try {
			removeDialog(DIALOG_UPDATE_CARD);
		}
		catch (IllegalArgumentException e) {
		}
		
		MyApp appState = ((MyApp) getApplicationContext());
		appState.setState(0);
	}
	
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder;
		switch (id) {
			case DIALOG_UPDATE_CARD:
				final int position = positionForDialog;
				final String side = (sideForDialog.equals("left") ? "left" : "right");
				final ArrayList<CardData> lSide = (sideForDialog.equals("left") ? lTradeLeft : lTradeRight);
				final TradeListAdapter aaSide = (sideForDialog.equals("left") ? aaTradeLeft : aaTradeRight);
				//final ListView lview = (sideForDialog.equals("left") ? lvTradeLeft : lvTradeRight);
				final int numberOfCards = lSide.get(position).getNumberOf();
				
				View view = LayoutInflater.from(mCtx).inflate(R.layout.trader_card_click_dialog, null);
				builder = new AlertDialog.Builder(CardTradingActivity.this);
				builder
				.setTitle(lSide.get(position).getName())
				.setView(view);
				
				dialog = builder.create();
				
				Button removeAll = (Button)view.findViewById(R.id.traderDialogRemove);
				Button cancel = (Button)view.findViewById(R.id.traderDialogCancel);
				Button changeSet = (Button)view.findViewById(R.id.traderDialogChangeSet);
				EditText numberOf = (EditText)view.findViewById(R.id.traderDialogNumber);
				
				String numberOfStr = String.valueOf(numberOfCards);
				numberOf.setText(numberOfStr);
				numberOf.setSelection(numberOfStr.length());
				numberOf.addTextChangedListener(new TextWatcher() {
					
					public void onTextChanged(CharSequence s, int start, int before, int count) {

					}
					
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						
					}
					
					public void afterTextChanged(Editable s) {
						String text;
						if (s.toString().length() == 0) {
							text = "1";
						}
						else {
							text = s.toString();
						}
						int numberOf = Integer.parseInt(text);
						lSide.get(position).setNumberOf(numberOf);
						aaSide.notifyDataSetChanged();
//						if (text.equals("1")) {
//							lview.getChildAt(position).findViewById(R.id.traderNumber).setVisibility(View.INVISIBLE);
//							lview.getChildAt(position).findViewById(R.id.traderMultipler).setVisibility(View.INVISIBLE);
//						}
//						else {
//							lview.getChildAt(position).findViewById(R.id.traderNumber).setVisibility(View.VISIBLE);
//							lview.getChildAt(position).findViewById(R.id.traderMultipler).setVisibility(View.VISIBLE);
//						}
						UpdateTotalPrices(side);
					}
				});
				
				removeAll.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						lSide.remove(position);
						aaSide.notifyDataSetChanged();
						UpdateTotalPrices(side);						
						removeDialog(DIALOG_UPDATE_CARD);
					}
				});
				
				changeSet.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						removeDialog(DIALOG_UPDATE_CARD);
						ChangeSet(side, position);
					}
				});
				cancel.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						removeDialog(DIALOG_UPDATE_CARD);
					}
				});

				dialog.show();
				break;	
			case DIALOG_PRICE_SETTING:
				builder = new AlertDialog.Builder(this);
				
				builder.setTitle("Price Options");
				builder.setSingleChoiceItems(new String[] {"Low", "Average", "High"}, priceSetting, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								priceSetting = which;
								dialog.dismiss();
								
								//Update ALL the prices!
								for(CardData data : lTradeLeft) {
									data.setPrice("loading");
									FetchPriceTask task = new FetchPriceTask(data, aaTradeLeft);
									task.execute();
								}
								aaTradeLeft.notifyDataSetChanged();
								
								for(CardData data : lTradeRight) {
									data.setPrice("loading");
									FetchPriceTask task = new FetchPriceTask(data, aaTradeRight);
									task.execute();
								}
								aaTradeRight.notifyDataSetChanged();
								
								//And also update the preference
								SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(CardTradingActivity.this).edit();
								edit.putString("tradePrice", String.valueOf(priceSetting));
								edit.commit();
							}
						});
				
				dialog = builder.create();
				dialog.show();
				break;
			default:
				dialog = null;
		}
		return dialog;
	}

	protected void ChangeSet(final String _side, final int _position) {
		CardData data = (_side.equals("left") ? lTradeLeft.get(_position) : lTradeRight.get(_position));
		String name = data.getName();
		mdbAdapter.open();
		Cursor cards = mdbAdapter.fetchCardByName(name);
		ArrayList<String> sets = new ArrayList<String>();
		ArrayList<String> setCodes = new ArrayList<String>();
		while (!cards.isAfterLast()) {
			sets.add(mdbAdapter.getTCGname(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET))));
			setCodes.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET)));
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
				if (_side.equals("left")) {
					lTradeLeft.get(_position).setSetCode(aSetCodes[item]);
					lTradeLeft.get(_position).setTcgName(aSets[item]);
					lTradeLeft.get(_position).setPrice("loading");
					aaTradeLeft.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(lTradeLeft.get(_position), aaTradeLeft);
					loadPrice.execute();
				}
				else if (_side.equals("right")) {
					lTradeRight.get(_position).setSetCode(aSetCodes[item]);
					lTradeRight.get(_position).setTcgName(aSets[item]);
					lTradeRight.get(_position).setPrice("loading");
					aaTradeRight.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(lTradeRight.get(_position), aaTradeRight);
					loadPrice.execute();
				}
				return;
			}
		});
		builder.create().show();
		
		cards.deactivate();
		cards.close();
		mdbAdapter.close();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Read values from the "savedInstanceState"-object and put them in your
		// textview
		namefield.setText(savedInstanceState.getString("nameBox"));
		tradePriceLeft.setText(savedInstanceState.getString("leftTotalPrice"));
		tradePriceRight.setText(savedInstanceState.getString("rightTotalPrice"));

		ArrayList<String> cardDataIn = savedInstanceState.getStringArrayList("tradeCards");
		for (String card : cardDataIn) {
			String[] cardData = card.split("\\|");
			int numberOf = Integer.parseInt(cardData[5]);
			CardData data = new CardData(cardData[1], cardData[2], cardData[3], numberOf, cardData[4]);

			if (cardData[0].equals("left"))
				lTradeLeft.add(data);
			else if (cardData[0].equals("right"))
				lTradeRight.add(data);
		}
		aaTradeLeft.notifyDataSetChanged();
		aaTradeRight.notifyDataSetChanged();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("nameBox", namefield.getText().toString());
		outState.putString("leftTotalPrice", (String) tradePriceLeft.getText());
		outState.putString("rightTotalPrice", (String) tradePriceRight.getText());

		ArrayList<String> cardDataOut = new ArrayList<String>();
		for (CardData data : lTradeLeft) {
			String cardData = String.format("%s|%s|%s|%s|%s|%s", "left", data.getName(), data.getSetCode(), data.getTcgName(), data.getPrice(), data.getNumberOf());
			cardDataOut.add(cardData);
		}
		for (CardData data : lTradeRight) {
			String cardData = String.format("%s|%s|%s|%s|%s|%s", "right", data.getName(), data.getSetCode(), data.getTcgName(), data.getPrice(), data.getNumberOf());
			cardDataOut.add(cardData);
		}
		outState.putStringArrayList("tradeCards", cardDataOut);

		// Save the values you need from your textview into "outState"-object
		super.onSaveInstanceState(outState);
	}

	private void UpdateTotalPrices() {
		UpdateTotalPrices("both");
	}
	
	private void UpdateTotalPrices(String _side) {
		if (_side.equals("left") || _side.equals("both")) {
			int totalPriceLeft = GetPricesFromTradeList(lTradeLeft);
			String sTotalLeft = "$" + (totalPriceLeft / 100) + "." + String.format("%02d", (totalPriceLeft % 100));
			tradePriceLeft.setText(sTotalLeft);
		}
		if (_side.equals("right") || _side.equals("both")) {
			int totalPriceRight = GetPricesFromTradeList(lTradeRight);
			String sTotalRight = "$" + (totalPriceRight / 100) + "." + String.format("%02d", (totalPriceRight % 100));
			tradePriceRight.setText(sTotalRight);
		}
	}

	private int GetPricesFromTradeList(ArrayList<CardData> _trade) {
		int totalPrice = 0;

		for (CardData data : _trade) {
			if (data.hasPrice()) {

				String price = data.getPrice();
	
				String dollar = price.substring(1, price.indexOf('.'));
				String cents = price.substring(price.indexOf('.') + 1);
	
				int iDollar = Integer.parseInt(dollar) * 100;
				int iCent = Integer.parseInt(cents);
	
				int numberOf = data.getNumberOf();
				
				totalPrice += numberOf * (iDollar + iCent);
			}
			else {
				String price = data.getPrice();

				// Remove the card from the list, unless it was just a fetch failed.
				// Otherwise, the card does not exist, or there is a database problem
				
				if(price.compareTo(card_not_found) == 0){
					_trade.remove(data);
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + card_not_found, Toast.LENGTH_LONG).show();
				}
				else if(price.compareTo(mangled_url) == 0){
					_trade.remove(data);
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + mangled_url, Toast.LENGTH_LONG).show();
				}
				else if(price.compareTo(database_busy) == 0){
					_trade.remove(data);
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + database_busy, Toast.LENGTH_LONG).show();
				}
				else if(price.compareTo(card_dne) == 0){
					_trade.remove(data);
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + card_dne, Toast.LENGTH_LONG).show();
				}
				else if(price.compareTo(fetch_failed) == 0){
					
				}
			}
		}
		return totalPrice;
	}
	
	private class CardData	{
		
		private String name;
		private String number;
		private String tcgName;
		private String setCode;
		private int numberOf;
		private String price;
		
		private String priceRegex = "(^\\$[0-9]+\\.[0-9]{2}$)";
		
		public CardData(String name, String tcgName, String setCode, int numberOf, String price) {
			this.name = name;
			this.number = "";
			this.tcgName = tcgName;
			this.setCode = setCode;
			this.numberOf = numberOf;
			this.price = price;
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
		
		public String getPrice() {
			return this.price;
		}
		
		public void setPrice(String newPrice) {
			this.price = newPrice;
		}
		
		public boolean hasPrice() {
			return this.price.matches(priceRegex);
		}
	}
	
	private class TradeListAdapter extends ArrayAdapter<CardData> {

		private int layoutResourceId;
		private ArrayList<CardData> items;
		
		public TradeListAdapter(Context context, int textViewResourceId, ArrayList<CardData> items) {
			super(context, textViewResourceId, items);
			
			this.layoutResourceId = textViewResourceId;
			this.items = items;
		}
		
		@Override
        public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if(v == null)
			{
				LayoutInflater inf = getLayoutInflater();
				v = inf.inflate(layoutResourceId, null);
			}
			CardData data = items.get(position);
			if(data != null)
			{
				((TextView)v.findViewById(R.id.traderRowName)).setText(data.getName());
				((TextView)v.findViewById(R.id.traderRowSet)).setText(data.getTcgName());
				((TextView)v.findViewById(R.id.traderNumber)).setText(data.hasPrice() ? data.getNumberOf() + "x" : "");
				((TextView)v.findViewById(R.id.traderRowPrice)).setText(data.getPrice());
			}
			
			return v;
		}
	}

	private class FetchPriceTask extends AsyncTask<Void, Void, Integer> {
		CardData	data;
		TradeListAdapter						toNotify;
		String								price	= "";
		String								setCode		= "";
		String 								tcgName ="";

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
				if(number.equals("") || setCode.equals("") || tcgName.equals("")) {
					mdbAdapter.open();
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
						mdbAdapter.close();
					}
					else {
						price = card_dne;
						card.deactivate();
						card.close();
						mdbAdapter.close();
						return 1;
					}
				}
			}
			catch (SQLiteException e) {
				price = card_not_found;
				mdbAdapter.close();
				return 1;
			}
			catch (IllegalStateException e) {
				price = database_busy;
				mdbAdapter.close();
				return 1;
			}

			URL priceurl = null;

			try {
				if (number.contains("b")) {
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
				mdbAdapter.close();
				return 1;
			}

			price = fetchAveragePrice(priceurl);

			if (price.equals(fetch_failed)) {
				mdbAdapter.close();
				return 1;
			}
			else {
				price = "$" + price;
			}
			return 0;
		}

		// Look into progress bar "loading..." style updating

		@Override
		protected void onPostExecute(Integer result) {
			data.setTcgName(tcgName);
			data.setSetCode(setCode);
			data.setPrice(price);
				
			UpdateTotalPrices();
			toNotify.notifyDataSetChanged();
		}

		String fetchAveragePrice(URL _priceURL) {
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
				if(priceSetting == LOW_PRICE) {
					return XMLhandler.lowprice;
				}
				else if(priceSetting == HIGH_PRICE) {
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
			case R.id.trader_menu_clear:
				lTradeRight.clear();
				aaTradeRight.notifyDataSetChanged();
				lTradeLeft.clear();
				aaTradeLeft.notifyDataSetChanged();
				UpdateTotalPrices("both");
				return true;
			case R.id.trader_menu_settings:
				showDialog(DIALOG_PRICE_SETTING);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
