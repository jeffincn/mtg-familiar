/**
Copyright 2011 Jonathan Bettger

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
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.ListView;
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trader_activity);

		MenuFragmentCompat.init(this, R.menu.trader_menu, "trading_menu_fragment");

		mCtx = this;
		mdbAdapter = new CardDbAdapter(this);

		namefield = (AutoCompleteTextView) findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(this, null));

		lTradeLeft = new ArrayList<HashMap<String, String>>();
		bAddTradeLeft = (Button) findViewById(R.id.addCardLeft);
		tradePriceLeft = (TextView) findViewById(R.id.priceTextLeft);
		lvTradeLeft = (ListView) findViewById(R.id.tradeListLeft);
		aaTradeLeft = new SimpleAdapter(mCtx, lTradeLeft, R.layout.trader_row, new String[] { "name", "set", "price" },
				new int[] { R.id.traderRowName, R.id.traderRowSet, R.id.traderRowPrice });
		lvTradeLeft.setAdapter(aaTradeLeft);

		lTradeRight = new ArrayList<HashMap<String, String>>();
		bAddTradeRight = (Button) findViewById(R.id.addCardRight);
		tradePriceRight = (TextView) findViewById(R.id.priceTextRight);
		lvTradeRight = (ListView) findViewById(R.id.tradeListRight);
		aaTradeRight = new SimpleAdapter(mCtx, lTradeRight, R.layout.trader_row, new String[] { "name", "set", "price" },
				new int[] { R.id.traderRowName, R.id.traderRowSet, R.id.traderRowPrice });
		lvTradeRight.setAdapter(aaTradeRight);

		bAddTradeLeft.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().toString().length() > 0) {
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("name", namefield.getText().toString());
					data.put("setCode", "");
					data.put("tcgName", "");
					data.put("price", "loading");

					lTradeLeft.add(data);
					aaTradeLeft.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(data, aaTradeLeft);
					loadPrice.execute();
					namefield.setText("");
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

					lTradeRight.add(data);
					aaTradeRight.notifyDataSetChanged();
					FetchPriceTask loadPrice = new FetchPriceTask(data, aaTradeRight);
					loadPrice.execute();
					namefield.setText("");
				}
			}
		});

		lvTradeLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				final int removeAt = arg2;
				new AlertDialog.Builder(mCtx).setMessage(lTradeLeft.get(removeAt).get("name")).setCancelable(true)
						.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								lTradeLeft.remove(removeAt);
								aaTradeLeft.notifyDataSetChanged();
								UpdateTotalPrices();
							}
						}).setNeutralButton("Change Set", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								ChangeSet("left", removeAt);
							}
						}).setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						}).show();
			}
		});
		lvTradeRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				final int removeAt = arg2;
				new AlertDialog.Builder(mCtx).setMessage(lTradeRight.get(removeAt).get("name")).setCancelable(true)
						.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								lTradeRight.remove(removeAt);
								aaTradeRight.notifyDataSetChanged();
								UpdateTotalPrices();
							}
						}).setNeutralButton("Change Set", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								ChangeSet("right", removeAt);
							}
						}).setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						}).show();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		MyApp appState = ((MyApp) getApplicationContext());
		appState.setState(0);
	}

	protected void ChangeSet(final String _side, final int _position) {
		HashMap<String, String> data = (_side.equals("left") ? lTradeLeft.get(_position) : lTradeRight.get(_position));
		String name = data.get("name");
		mdbAdapter.open();
		Cursor cards = mdbAdapter.fetchCardByName(name);
		ArrayList<String> sets = new ArrayList<String>();
		while (!cards.isAfterLast()) {
			sets.add(mdbAdapter.getTCGname(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET))));
			cards.moveToNext();
		}

		final String[] aSets = sets.toArray(new String[sets.size()]);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pick a Set");
		builder.setItems(aSets, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int item) {
				if (_side.equals("left")) {
					lTradeLeft.get(_position).put("set", aSets[item]);
					FetchPriceTask loadPrice = new FetchPriceTask(lTradeLeft.get(_position), aaTradeLeft);
					loadPrice.execute();
				}
				else if (_side.equals("right")) {
					lTradeRight.get(_position).put("set", aSets[item]);
					FetchPriceTask loadPrice = new FetchPriceTask(lTradeRight.get(_position), aaTradeRight);
					loadPrice.execute();
				}
				return;
			}
		});
		builder.create().show();

		cards.deactivate();
		cards.close();
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
			data.put("set", cardData[2]);
			data.put("price", cardData[3]);

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
			String cardData = String.format("%s|%s|%s|%s", "left", data.get("name"), data.get("set"), data.get("price"));
			cardDataOut.add(cardData);
		}
		for (HashMap<String, String> data : lTradeRight) {
			String cardData = String.format("%s|%s|%s|%s", "right", data.get("name"), data.get("set"), data.get("price"));
			cardDataOut.add(cardData);
		}
		outState.putStringArrayList("tradeCards", cardDataOut);

		// Save the values you need from your textview into "outState"-object
		super.onSaveInstanceState(outState);
	}

	private void UpdateTotalPrices() {
		int totalPriceLeft = GetPricesFromTradeList(lTradeLeft);
		int totalPriceRight = GetPricesFromTradeList(lTradeRight);

		String sTotalLeft = "$" + (totalPriceLeft / 100) + "." + String.format("%02d", (totalPriceLeft % 100));
		String sTotalRight = "$" + (totalPriceRight / 100) + "." + String.format("%02d", (totalPriceRight % 100));

		tradePriceLeft.setText(sTotalLeft);
		tradePriceRight.setText(sTotalRight);
	}

	private int GetPricesFromTradeList(ArrayList<HashMap<String, String>> _trade) {
		int totalPrice = 0;

		for (HashMap<String, String> data : _trade) {
			if (data.get("error") != null)
				continue;

			String price = data.get("price");

			String dollar = price.substring(1, price.indexOf('.'));
			String cents = price.substring(price.indexOf('.') + 1);

			int iDollar = Integer.parseInt(dollar) * 100;
			int iCent = Integer.parseInt(cents);

			totalPrice += iDollar + iCent;
		}
		return totalPrice;
	}

	private class FetchPriceTask extends AsyncTask<Void, Void, Integer> {
		HashMap<String, String>	data;
		SimpleAdapter						toNotify;
		String									price	= "";
		String									setCode		= "";
		String tcgName ="";

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
					price = "E- Card Does Not Exist";
					return 1;
				}
			}
			catch (SQLiteException e) {
				price = "E- Card Not Found";
				return 1;
			}
			catch (IllegalStateException e) {
				price = "E- Database Busy";
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
				price = "E- Mangled URL";
				return 1;
			}

			price = fetchAveragePrice(priceurl);

			if (price.contains("E-"))
				return 1;
			else
				price = "$" + price;

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
				toNotify.notifyDataSetChanged();
				UpdateTotalPrices();
			}
			else if (result == 1) {
				data.put("tcgName", "");
				data.put("setCode", "");
				data.put("price", price.substring(3));
				data.put("error", "error");
				toNotify.notifyDataSetChanged();
			}
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
				return "E- Fetch Failed";
			else
				return XMLhandler.avgprice;
		}
	}

}
