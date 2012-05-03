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
	public static final String number_of_invalid = "Number of Cards Invalid";
	public static final String price_invalid = "Price Invalid";
	
//	private static final String priceRegex = "(^\\$[0-9]+\\.[0-9]{2}$)";

	private static final int LOW_PRICE = 0;
	private static final int AVG_PRICE = 1;
	private static final int HIGH_PRICE = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trader_activity);

		MenuFragmentCompat.init(this, R.menu.trader_menu, "trading_menu_fragment");

		mCtx = this;
		mdbAdapter = new CardDbAdapter(this).open();

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

					CardData data = new CardData(namefield.getText().toString(), "", "", numberOf, 0, "loading","");

					lTradeLeft.add(0,data);
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

					CardData data = new CardData(namefield.getText().toString(), "", "", numberOf, 0, "loading","");

					lTradeRight.add(0,data);
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
		
		//Give this a default value so we don't get the null pointer-induced FC. It shouldn't matter what
		//we set it to, as long as we set it, since we dismiss the dialog if it's showing in onResume().
		sideForDialog = "left";
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
		if(mdbAdapter != null) {
			mdbAdapter.close();
		}
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
				final int numberOfCards = lSide.get(position).getNumberOf();
				final String priceOfCard = lSide.get(position).getPriceString();
				
				View view = LayoutInflater.from(mCtx).inflate(R.layout.trader_card_click_dialog, null);
				Button removeAll = (Button)view.findViewById(R.id.traderDialogRemove);
				Button changeSet = (Button)view.findViewById(R.id.traderDialogChangeSet);
				Button cancelbtn = (Button)view.findViewById(R.id.traderDialogCancel);
				Button donebtn   = (Button)view.findViewById(R.id.traderDialogDone);
				final EditText numberOf = (EditText)view.findViewById(R.id.traderDialogNumber);
				final EditText priceText = (EditText)view.findViewById(R.id.traderDialogPrice);				
				
				builder = new AlertDialog.Builder(CardTradingActivity.this);
				builder
				.setTitle(lSide.get(position).getName())
				.setView(view);
				
				dialog = builder.create();
				
				String numberOfStr = String.valueOf(numberOfCards);
				numberOf.setText(numberOfStr);
				numberOf.setSelection(numberOfStr.length());
				
				String priceNumberStr = lSide.get(position).hasPrice() ? priceOfCard.substring(1) : "";
				priceText.setText(priceNumberStr);
				priceText.setSelection(priceNumberStr.length());
				
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
				
				cancelbtn.setOnClickListener( new OnClickListener() {
					public void onClick(View v) {
						removeDialog(DIALOG_UPDATE_CARD);
					}
				});
				
				donebtn.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						
						//validate number of cards text
						if (numberOf.length() == 0){
							Toast.makeText(getApplicationContext(), number_of_invalid, Toast.LENGTH_LONG).show();
							return;
						}
						
						//validate the price text
						String userInputPrice = priceText.getText().toString();
						double uIP;
						try {
							uIP = Double.parseDouble(userInputPrice);
							lSide.get(position).setMessage(""); //Clear the message so the user's specified price will display
						}
						catch (NumberFormatException e) {
							uIP = 0;
						}
						
						lSide.get(position).setNumberOf(Integer.parseInt(numberOf.getEditableText().toString()));
						lSide.get(position).setPrice((int)Math.round(uIP * 100));
						aaSide.notifyDataSetChanged();
						UpdateTotalPrices(side);
						
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
			case DIALOG_PRICE_SETTING:
				builder = new AlertDialog.Builder(this);
				
				builder.setTitle("Price Options");
				builder.setSingleChoiceItems(new String[] {"Low", "Average", "High"}, priceSetting, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								priceSetting = which;
								dialog.dismiss();
								
								//Update ALL the prices!
								for(CardData data : lTradeLeft) {
									data.setMessage("loading");
									FetchPriceTask task = new FetchPriceTask(data, aaTradeLeft);
									task.execute();
								}
								aaTradeLeft.notifyDataSetChanged();
								
								for(CardData data : lTradeRight) {
									data.setMessage("loading");
									FetchPriceTask task = new FetchPriceTask(data, aaTradeRight);
									task.execute();
								}
								aaTradeRight.notifyDataSetChanged();
								
								//And also update the preference
								SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(CardTradingActivity.this).edit();
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
			default:
				dialog = null;
		}
		return dialog;
	}

	protected void ChangeSet(final String _side, final int _position) {
		CardData data = (_side.equals("left") ? lTradeLeft.get(_position) : lTradeRight.get(_position));
		String name = data.getName();
		
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
					lTradeLeft.get(_position).setMessage("loading");
					aaTradeLeft.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(lTradeLeft.get(_position), aaTradeLeft);
					loadPrice.execute();
				}
				else if (_side.equals("right")) {
					lTradeRight.get(_position).setSetCode(aSetCodes[item]);
					lTradeRight.get(_position).setTcgName(aSets[item]);
					lTradeRight.get(_position).setMessage("loading");
					aaTradeRight.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(lTradeRight.get(_position), aaTradeRight);
					loadPrice.execute();
				}
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
		tradePriceLeft.setText(savedInstanceState.getString("leftTotalPrice"));
		tradePriceRight.setText(savedInstanceState.getString("rightTotalPrice"));

		ArrayList<String> cardDataIn = savedInstanceState.getStringArrayList("tradeCards");
		for (String card : cardDataIn) {
			String[] cardData = card.split("\\|");
			int numberOf = Integer.parseInt(cardData[7]);
			CardData data = new CardData(cardData[1], cardData[2], cardData[3], numberOf, Integer.parseInt(cardData[4]), cardData[5], cardData[6]);

			if (cardData[0].equals("left"))
				lTradeLeft.add(data);
			else if (cardData[0].equals("right"))
				lTradeRight.add(data);
		}
		aaTradeLeft.notifyDataSetChanged();
		aaTradeRight.notifyDataSetChanged();
		UpdateTotalPrices("both");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("nameBox", namefield.getText().toString());
		outState.putString("leftTotalPrice", (String) tradePriceLeft.getText());
		outState.putString("rightTotalPrice", (String) tradePriceRight.getText());

		ArrayList<String> cardDataOut = new ArrayList<String>();
		for (CardData data : lTradeLeft) {
			String cardData = String.format("%s|%s|%s|%s|%s|%s|%s|%s", "left", data.getName(), data.getTcgName(),
					data.getSetCode(), data.getPrice(), data.getMessage(), data.getNumber(), data.getNumberOf());
			cardDataOut.add(cardData);
		}
		for (CardData data : lTradeRight) {
			String cardData = String.format("%s|%s|%s|%s|%s|%s|%s|%s", "right", data.getName(), data.getTcgName(),
					data.getSetCode(), data.getPrice(), data.getMessage(), data.getNumber(), data.getNumberOf());
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
			int color = PriceListHasBadValues(lTradeLeft) ? mCtx.getResources().getColor(R.color.red) : mCtx.getResources().getColor(R.color.white);
			String sTotalLeft = "$" + (totalPriceLeft / 100) + "." + String.format("%02d", (totalPriceLeft % 100));
			tradePriceLeft.setText(sTotalLeft);
			tradePriceLeft.setTextColor(color);
		}
		if (_side.equals("right") || _side.equals("both")) {
			int totalPriceRight = GetPricesFromTradeList(lTradeRight);
			int color = PriceListHasBadValues(lTradeRight) ? mCtx.getResources().getColor(R.color.red) : mCtx.getResources().getColor(R.color.white);
			String sTotalRight = "$" + (totalPriceRight / 100) + "." + String.format("%02d", (totalPriceRight % 100));
			tradePriceRight.setText(sTotalRight);
			tradePriceRight.setTextColor(color);
		}
	}
	
	private boolean PriceListHasBadValues(ArrayList<CardData> trade) {
		for (CardData data : trade) {
			if(!data.hasPrice()) {
				return true;
			}
		}
		return false;
	}

	private int GetPricesFromTradeList(ArrayList<CardData> _trade) {
		int totalPrice = 0;

		for (int i=0; i < _trade.size(); i++){//CardData data : _trade) {
			CardData data = _trade.get(i);
			if (data.hasPrice()) {
				totalPrice += data.getNumberOf() * data.getPrice();
			}
			else {
				String message = data.getMessage();

				// Remove the card from the list, unless it was just a fetch failed.
				// Otherwise, the card does not exist, or there is a database problem
				
				if(message.compareTo(card_not_found) == 0){
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + card_not_found, Toast.LENGTH_LONG).show();
				}
				else if(message.compareTo(mangled_url) == 0){
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + mangled_url, Toast.LENGTH_LONG).show();
				}
				else if(message.compareTo(database_busy) == 0){
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + database_busy, Toast.LENGTH_LONG).show();
				}
				else if(message.compareTo(card_dne) == 0){
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.getName() + ": " + card_dne, Toast.LENGTH_LONG).show();
				}
				else if(message.compareTo(fetch_failed) == 0){
					
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
		private int price; //In cents
		private String message;
		
		public CardData(String name, String tcgName, String setCode, int numberOf, int price, String message, String number) {
			this.name = name;
			this.number = number;
			this.tcgName = tcgName;
			this.setCode = setCode;
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
	}
	
	private class TradeListAdapter extends ArrayAdapter<CardData> {

		private int layoutResourceId;
		private ArrayList<CardData> items;
		private Context mCtx;
		
		public TradeListAdapter(Context context, int textViewResourceId, ArrayList<CardData> items) {
			super(context, textViewResourceId, items);
			
			this.mCtx = context;
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
				TextView nameField = (TextView)v.findViewById(R.id.traderRowName);
				TextView setField = (TextView)v.findViewById(R.id.traderRowSet);
				TextView numberField = (TextView)v.findViewById(R.id.traderNumber);
				TextView priceField = (TextView)v.findViewById(R.id.traderRowPrice);
				
				nameField.setText(data.getName());
				setField.setText(data.getTcgName());
				numberField.setText(data.hasPrice() ? data.getNumberOf() + "x" : "");
				priceField.setText(data.hasPrice() ? data.getPriceString() : data.getMessage());
			
				if (data.hasPrice()) {
//					nameField.setTextColor(mCtx.getResources().getColor(R.color.white));
//					setField.setTextColor(mCtx.getResources().getColor(R.color.light_gray));
//					numberField.setTextColor(mCtx.getResources().getColor(R.color.light_gray));
					priceField.setTextColor(mCtx.getResources().getColor(R.color.light_gray));
				}
				else {
//					nameField.setTextColor(mCtx.getResources().getColor(R.color.red));
//					setField.setTextColor(mCtx.getResources().getColor(R.color.red));
//					numberField.setTextColor(mCtx.getResources().getColor(R.color.red));
					priceField.setTextColor(mCtx.getResources().getColor(R.color.red));
				}}
			
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
				//If this fails, it means price contains an error string, not a number
				dPrice = 0;
				data.setMessage(price);
			}
			data.setPrice((int)(dPrice * 100));
				
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
