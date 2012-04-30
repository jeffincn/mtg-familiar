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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class CardTradingActivity extends FragmentActivity {
	private Context															mCtx;
	private final static int DIALOG_UPDATE_CARD					= 1;
	private String sideForDialog;
	private int positionForDialog;
	
	private AutoCompleteTextView								namefield;

	private Button															bAddTradeLeft;
	private TextView														tradePriceLeft;
	private ListView														lvTradeLeft;
	private SimpleAdapter												aaTradeLeft;
	private ArrayList<HashMap<String, String>>	lTradeLeft;

	private Button															bAddTradeRight;
	private TextView														tradePriceRight;
	private ListView														lvTradeRight;
	private SimpleAdapter												aaTradeRight;
	private ArrayList<HashMap<String, String>>	lTradeRight;

	private CardDbAdapter												mdbAdapter;
	private EditText	numberfield;
	
	public static final String ERROR_PREFIX = "E- ";
	public static final String card_not_found = "Card Not Found";
	public static final String mangled_url = "Mangled URL";
	public static final String database_busy = "Database Busy";
	public static final String card_dne = "Card Does Not Exist";
	public static final String fetch_failed = "Fetch Failed";


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
		
		lTradeLeft = new ArrayList<HashMap<String, String>>();
		bAddTradeLeft = (Button) findViewById(R.id.addCardLeft);
		tradePriceLeft = (TextView) findViewById(R.id.priceTextLeft);
		lvTradeLeft = (ListView) findViewById(R.id.tradeListLeft);
		aaTradeLeft = new SimpleAdapter(mCtx, lTradeLeft, R.layout.trader_row, new String[] { "name", "tcgName", "price", "numberOf" },
				new int[] { R.id.traderRowName, R.id.traderRowSet, R.id.traderRowPrice, R.id.traderNumber });
		lvTradeLeft.setAdapter(aaTradeLeft);

		lTradeRight = new ArrayList<HashMap<String, String>>();
		bAddTradeRight = (Button) findViewById(R.id.addCardRight);
		tradePriceRight = (TextView) findViewById(R.id.priceTextRight);
		lvTradeRight = (ListView) findViewById(R.id.tradeListRight);
		aaTradeRight = new SimpleAdapter(mCtx, lTradeRight, R.layout.trader_row, new String[] { "name", "tcgName", "price", "numberOf" },
				new int[] { R.id.traderRowName, R.id.traderRowSet, R.id.traderRowPrice, R.id.traderNumber });
		lvTradeRight.setAdapter(aaTradeRight);

		bAddTradeLeft.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().toString().length() > 0) {
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("name", namefield.getText().toString());
					data.put("setCode", "");
					data.put("tcgName", "");
					data.put("price", "loading");
					
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					data.put("numberOf", numberOfFromField + "x");

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
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("name", namefield.getText().toString());
					data.put("setCode", "");
					data.put("tcgName", "");
					data.put("price", "loading");
					
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					data.put("numberOf", numberOfFromField + "x");

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
		final int position = positionForDialog;
		final String side = (sideForDialog.equals("left") ? "left" : "right");
		final ArrayList<HashMap<String, String>> lSide = (sideForDialog.equals("left") ? lTradeLeft : lTradeRight);
		final SimpleAdapter aaSide = (sideForDialog.equals("left") ? aaTradeLeft : aaTradeRight);
		//final ListView lview = (sideForDialog.equals("left") ? lvTradeLeft : lvTradeRight);
		String rawNumber = lSide.get(position).get("numberOf");
		final String numberOfCards = rawNumber.length() == 0 ? "1" : rawNumber.substring(0, rawNumber.length() - 1);
		switch (id) {
			case DIALOG_UPDATE_CARD:
				View view = LayoutInflater.from(mCtx).inflate(R.layout.trader_card_click_dialog, null);
				AlertDialog.Builder builder = new AlertDialog.Builder(CardTradingActivity.this);
				builder
				.setTitle(lSide.get(position).get("name"))
				.setView(view);
				
				dialog = builder.create();
				
				Button removeAll = (Button)view.findViewById(R.id.traderDialogRemove);
				Button cancel = (Button)view.findViewById(R.id.traderDialogCancel);
				Button changeSet = (Button)view.findViewById(R.id.traderDialogChangeSet);
				EditText numberOf = (EditText)view.findViewById(R.id.traderDialogNumber);
				
				numberOf.setText(numberOfCards);
				numberOf.setSelection(numberOfCards.length());
				numberOf.addTextChangedListener(new TextWatcher() {
					
					public void onTextChanged(CharSequence s, int start, int before, int count) {

					}
					
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						
					}
					
					public void afterTextChanged(Editable s) {
						String error = lSide.get(position).get("error"); 
						if(error == null || error.length() == 0)
						{
							String text;
							if (s.toString().length() == 0) {
								text = "1";
							}
							else {
								text = s.toString();
							}
							lSide.get(position).put("numberOf", text + "x");
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
			default:
				dialog = null;
		}
		return dialog;
	}

	protected void ChangeSet(final String _side, final int _position) {
		HashMap<String, String> data = (_side.equals("left") ? lTradeLeft.get(_position) : lTradeRight.get(_position));
		String name = data.get("name");
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
					lTradeLeft.get(_position).put("setCode", aSetCodes[item]);
					lTradeLeft.get(_position).put("tcgName", aSets[item]);
					lTradeLeft.get(_position).put("price", "loading");
					aaTradeLeft.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(lTradeLeft.get(_position), aaTradeLeft);
					loadPrice.execute();
				}
				else if (_side.equals("right")) {
					lTradeRight.get(_position).put("setCode", aSetCodes[item]);
					lTradeRight.get(_position).put("tcgName", aSets[item]);
					lTradeRight.get(_position).put("price", "loading");
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
			HashMap<String, String> data = new HashMap<String, String>();

			String[] cardData = card.split("\\|");
			data.put("name", cardData[1]);
			data.put("setCode", cardData[2]);
			data.put("tcgName", cardData[3]);
			data.put("price", cardData[4]);
			data.put("numberOf", cardData[5]);

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
		for (HashMap<String, String> data : lTradeLeft) {
			String cardData = String.format("%s|%s|%s|%s|%s|%s", "left", data.get("name"), data.get("setCode"), data.get("tcgName"), data.get("price"), data.get("numberOf"));
			cardDataOut.add(cardData);
		}
		for (HashMap<String, String> data : lTradeRight) {
			String cardData = String.format("%s|%s|%s|%s|%s|%s", "right", data.get("name"), data.get("set"), data.get("tcgName"), data.get("price"), data.get("numberOf"));
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

	private int GetPricesFromTradeList(ArrayList<HashMap<String, String>> _trade) {
		int totalPrice = 0;

		for (HashMap<String, String> data : _trade) {
			if (data.get("error") == null && data.get("price") != null){

				String price = data.get("price");
	
				String dollar = price.substring(1, price.indexOf('.'));
				String cents = price.substring(price.indexOf('.') + 1);
	
				int iDollar = Integer.parseInt(dollar) * 100;
				int iCent = Integer.parseInt(cents);
	
				String numberOf = data.get("numberOf");
				if (numberOf.length() == 0)
				{
					numberOf = "1";
				}
				else
				{
					numberOf = numberOf.substring(0, numberOf.length() - 1);
				}
				int inumberOf = Integer.parseInt(numberOf);
				
				totalPrice += inumberOf * (iDollar + iCent);
			}
			else{
				String price = data.get("price");

				// Remove the card from the list, unless it was just a fetch failed.
				// Otherwise, the card does not exist, or there is a database problem
				
				if(price.compareTo(card_not_found) == 0){
					_trade.remove(data);
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.get("name") + ": " + card_not_found, Toast.LENGTH_LONG).show();
				}
				else if(price.compareTo(mangled_url) == 0){
					_trade.remove(data);
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.get("name") + ": " + mangled_url, Toast.LENGTH_LONG).show();
				}
				else if(price.compareTo(database_busy) == 0){
					_trade.remove(data);
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.get("name") + ": " + database_busy, Toast.LENGTH_LONG).show();
				}
				else if(price.compareTo(card_dne) == 0){
					_trade.remove(data);
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.get("name") + ": " + card_dne, Toast.LENGTH_LONG).show();
				}
				else if(price.compareTo(fetch_failed) == 0){
					
				}
			}
		}
		return totalPrice;
	}

	private class FetchPriceTask extends AsyncTask<Void, Void, Integer> {
		HashMap<String, String>	data;
		SimpleAdapter						toNotify;
		String								price	= "";
		String								setCode		= "";
		String 								tcgName ="";

		public FetchPriceTask(HashMap<String, String> _data, SimpleAdapter _toNotify) {
			data = _data;
			toNotify = _toNotify;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			mdbAdapter.open();
			Cursor card;
			String cardName;
			String number;
			try {
				card = mdbAdapter.fetchCardByName((String) data.get("name"));
				if (card.moveToFirst()) {
					cardName = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NAME));
					if (data.get("setCode").equals("")) {
						setCode = card.getString(card.getColumnIndex(CardDbAdapter.KEY_SET));
						tcgName = mdbAdapter.getTCGname(setCode);
					}
					else {
						setCode = data.get("setCode");
						tcgName = data.get("tcgName");
					}
					number = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NUMBER));
				}
				else {
					price = ERROR_PREFIX + card_dne;
					mdbAdapter.close();
					return 1;
				}
			}
			catch (SQLiteException e) {
				price = ERROR_PREFIX + card_not_found;
				mdbAdapter.close();
				return 1;
			}
			catch (IllegalStateException e) {
				price = ERROR_PREFIX + database_busy;
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
				price = ERROR_PREFIX + mangled_url;
				mdbAdapter.close();
				return 1;
			}

			price = fetchAveragePrice(priceurl);

			if (price.contains(ERROR_PREFIX)){
				mdbAdapter.close();
				return 1;
			}
			else{
				price = "$" + price;
			}

			card.deactivate();
			card.close();
			mdbAdapter.close();
			return 0;
		}

		// Look into progress bar "loading..." style updating

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				data.put("tcgName", tcgName);
				data.put("setCode", setCode);
				data.put("price", price);
				if(data.get("numberOf").length() == 0)
				{
					data.put("numberOf", "1x"); //If the previous fetch failed, we want to reset this to display 1x
				}
				data.remove("error"); // if the original fetch failed, but a subsequent one worked, clear the error flag
			}
			else if (result == 1) {
				data.put("tcgName", "");
				data.put("setCode", "");
				data.put("price", price.substring(3));
				data.put("numberOf", "");
				data.put("error", "error");
			}
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

			if (XMLhandler == null || XMLhandler.link == null)
				return ERROR_PREFIX+fetch_failed;
			else
				return XMLhandler.avgprice;
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
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
