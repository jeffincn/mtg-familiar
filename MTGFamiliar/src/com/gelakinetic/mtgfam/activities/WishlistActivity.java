/**
Copyright 2012 

This file is part of MTG Familiar.

MTG Familiar is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MTG Familiar is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MTG Familiar. If not, see <http://www.gnu.org/licenses/>.
 */
package com.gelakinetic.mtgfam.activities;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers.CardData;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers.FetchPriceTask;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;

public class WishlistActivity extends FamiliarActivity {
	private final static int			DIALOG_UPDATE_CARD		= 1;
	private final static int			DIALOG_PRICE_SETTING	= 2;
	private int										positionForDialog;

	private AutoCompleteTextView	namefield;
	// private String selectedName = "";

	private Button								bAdd;
	private TextView							tradePrice;
	private ListView							lvWishlist;
	private WishListAdapter				aaWishlist;
	private ArrayList<CardData>		lWishlist;

	private EditText							numberfield;

	public int										priceSetting;
	private boolean								showTotalPrice;
	private boolean								verbose;
	private TradeListHelpers			mTradeListHelper;

	public static final String		card_not_found				= "Card Not Found";
	public static final String		mangled_url						= "Mangled URL";
	public static final String		database_busy					= "Database Busy";
	public static final String		card_dne							= "Card Does Not Exist";
	public static final String		fetch_failed					= "Fetch Failed";
	public static final String		number_of_invalid			= "Number of Cards Invalid";
	public static final String		price_invalid					= "Price Invalid";

	private static final int			AVG_PRICE							= 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wishlist_activity);

		mTradeListHelper = new TradeListHelpers();

		showTotalPrice = preferences.getBoolean("showTotalPriceWishlistPref", false);
		verbose = preferences.getBoolean("verboseWishlistPref", true);

		namefield = (AutoCompleteTextView) findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(this, null));

		numberfield = (EditText) findViewById(R.id.numberInput);
		numberfield.setText("1");

		lWishlist = new ArrayList<CardData>();
		bAdd = (Button) findViewById(R.id.addCard);
		tradePrice = (TextView) findViewById(R.id.priceText);
		lvWishlist = (ListView) findViewById(R.id.wishlist);
		aaWishlist = new WishListAdapter(mCtx, R.layout.wishlist_row, lWishlist, this.getResources());
		lvWishlist.setAdapter(aaWishlist);

		if (!showTotalPrice) {
			tradePrice.setVisibility(View.GONE);
		}
		else {
			tradePrice.setVisibility(View.VISIBLE);
		}

		bAdd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().length() > 0) {
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					int numberOf = Integer.parseInt(numberOfFromField);

					CardData data = mTradeListHelper.new CardData(namefield.getText().toString(), null, null, numberOf, 0, "loading", null, null, null, null, null, null,
							CardDbAdapter.NOONECARES, '-');

					data.getExtendedData();

					lWishlist.add(0, data);
					aaWishlist.notifyDataSetChanged();
					FetchPriceTask loadPrice = mTradeListHelper.new FetchPriceTask(data, aaWishlist, priceSetting, null, (WishlistActivity) me);
					loadPrice.execute();
					namefield.setText("");
					numberfield.setText("1");
				}
				else {
					Toast.makeText(getApplicationContext(), getString(R.string.wishlist_toast_select_card), Toast.LENGTH_SHORT).show();
				}
			}
		});

		lvWishlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});

		priceSetting = Integer.parseInt(preferences.getString("tradePrice", String.valueOf(AVG_PRICE)));
	}

	@Override
	protected void onPause() {
		super.onPause();
		WishlistHelpers.WriteWishlist(getApplicationContext(),lWishlist);
	}

	@Override
	protected void onResume() {
		super.onResume();
		WishlistHelpers.ReadWishlist(getApplicationContext(), (WishlistActivity) me, mDbHelper, lWishlist, aaWishlist);

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
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder;
		switch (id) {
			case DIALOG_UPDATE_CARD: {
				final int position = positionForDialog;
				final ArrayList<CardData> lList = lWishlist;
				final WishListAdapter aaList = aaWishlist;
				final int numberOfCards = lList.get(position).numberOf;
				final String priceOfCard = lList.get(position).getPriceString();

				View view = LayoutInflater.from(mCtx).inflate(R.layout.trader_card_click_dialog, null);
				Button removeAll = (Button) view.findViewById(R.id.traderDialogRemove);
				Button changeSet = (Button) view.findViewById(R.id.traderDialogChangeSet);
				Button cancelbtn = (Button) view.findViewById(R.id.traderDialogCancel);
				Button donebtn = (Button) view.findViewById(R.id.traderDialogDone);
				final EditText numberOf = (EditText) view.findViewById(R.id.traderDialogNumber);
				final EditText priceText = (EditText) view.findViewById(R.id.traderDialogPrice);

				builder = new AlertDialog.Builder(WishlistActivity.this);
				builder.setTitle(lList.get(position).name).setView(view);

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
							lList.get(position).message = ("");
						}
						catch (NumberFormatException e) {
							uIP = 0;
						}

						lList.get(position).numberOf = (Integer.parseInt(numberOf.getEditableText().toString()));
						lList.get(position).price = ((int) Math.round(uIP * 100));
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
				builder.setSingleChoiceItems(new String[] { "Low", "Average", "High" }, priceSetting, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						priceSetting = which;
						dialog.dismiss();

						// Update ALL the prices!
						for (CardData data : lWishlist) {
							data.message = ("loading");
							FetchPriceTask task = mTradeListHelper.new FetchPriceTask(data, aaWishlist, priceSetting, null, (WishlistActivity) me);
							task.execute();
						}
						aaWishlist.notifyDataSetChanged();

						// And also update the preference
						SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WishlistActivity.this).edit();
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
		String name = data.name;

		Cursor cards = mDbHelper.fetchCardByName(name);
		Set<String> sets = new LinkedHashSet<String>();
		Set<String> setCodes = new LinkedHashSet<String>();
		Set<Long> cardIds = new LinkedHashSet<Long>();
		while (!cards.isAfterLast()) {
			if (sets.add(mDbHelper.getTCGname(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET))))) {
				setCodes.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET)));
				cardIds.add(cards.getLong(cards.getColumnIndex(CardDbAdapter.KEY_ID)));
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
				lWishlist.get(_position).setCode = (aSetCodes[item]);
				lWishlist.get(_position).tcgName = (aSets[item]);
				lWishlist.get(_position).message = ("loading");
				aaWishlist.notifyDataSetChanged();
				FetchPriceTask loadPrice = mTradeListHelper.new FetchPriceTask(lWishlist.get(_position), aaWishlist, priceSetting, null, (WishlistActivity) me);
				loadPrice.execute();
				return;
			}
		});
		builder.create().show();
	}

	public void UpdateTotalPrices() {
		int totalPrice = GetPricesFromTradeList(lWishlist);
		if (showTotalPrice) {
			int color = PriceListHasBadValues(lWishlist) ? mCtx.getResources().getColor(R.color.red) : mCtx.getResources().getColor(R.color.white);
			String sTotal = "$" + (totalPrice / 100) + "." + String.format("%02d", (totalPrice % 100));
			tradePrice.setText(sTotal);
			tradePrice.setTextColor(color);
		}
		aaWishlist.notifyDataSetChanged();
		lvWishlist.invalidate();
		lvWishlist.postInvalidate();
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
				totalPrice += data.numberOf * data.price;
			}
			else {
				String message = data.message;

				// Remove the card from the list, unless it was just a fetch failed.
				// Otherwise, the card does not exist, or there is a database problem

				if (message.compareTo(card_not_found) == 0) {
					_trade.remove(data);
					i--;
					aaWishlist.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.name + ": " + card_not_found, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(mangled_url) == 0) {
					_trade.remove(data);
					i--;
					aaWishlist.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.name + ": " + mangled_url, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(database_busy) == 0) {
					_trade.remove(data);
					i--;
					aaWishlist.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.name + ": " + database_busy, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(card_dne) == 0) {
					_trade.remove(data);
					i--;
					aaWishlist.notifyDataSetChanged();
					Toast.makeText(getApplicationContext(), data.name + ": " + card_dne, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(fetch_failed) == 0) {

				}
			}
		}
		return totalPrice;
	}

	private class WishListAdapter extends ArrayAdapter<CardData> {

		private int									layoutResourceId;
		private ArrayList<CardData>	items;
		private Context							mCtx;
		private Resources						resources;
		private ImageGetter					imgGetter;

		public WishListAdapter(Context context, int textViewResourceId, ArrayList<CardData> items, Resources r) {
			super(context, textViewResourceId, items);

			this.mCtx = context;
			this.layoutResourceId = textViewResourceId;
			this.items = items;
			resources = r;
			imgGetter = ImageGetterHelper.GlyphGetter(r);
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
				TextView typeField = (TextView) v.findViewById(R.id.cardtype);
				TextView costField = (TextView) v.findViewById(R.id.cardcost);
				TextView abilityField = (TextView) v.findViewById(R.id.cardability);
				TextView setField = (TextView) v.findViewById(R.id.wishlistRowSet);
				TextView priceField = (TextView) v.findViewById(R.id.wishlistRowPrice);
				TextView pField = (TextView) v.findViewById(R.id.cardp);
				TextView slashField = (TextView) v.findViewById(R.id.cardslash);
				TextView tField = (TextView) v.findViewById(R.id.cardt);

				nameField.setText(data.name);
				String type = data.type;
				// we check the actual values for visibility here (instead of just the verbose flag)
				// because some card types won't have some of these - i.e. tokens don't have cost, etc.
				if (!verbose || type == null || type == "") {
					typeField.setVisibility(View.GONE);
				}
				else {
					typeField.setVisibility(View.VISIBLE);
					typeField.setText(type);
				}
				String manaCost = data.cost;
				if (!verbose || manaCost == null || manaCost == "") {
					costField.setVisibility(View.GONE);
				}
				else {
					costField.setVisibility(View.VISIBLE);
					manaCost = manaCost.replace("{", "<img src=\"").replace("}", "\"/>");
					costField.setText(Html.fromHtml(manaCost, imgGetter, null));
				}
				String ability = data.ability;
				if (!verbose || ability == null || ability == "") {
					abilityField.setVisibility(View.GONE);
				}
				else {
					abilityField.setVisibility(View.VISIBLE);
					ability = ability.replace("{", "<img src=\"").replace("}", "\"/>");
					abilityField.setText(Html.fromHtml(ability, imgGetter, null));
				}
				setField.setText(data.tcgName);
				char r = (char) data.rarity;
				switch (r) {
					case 'C':
						setField.setTextColor(resources.getColor(R.color.common));
						break;
					case 'U':
						setField.setTextColor(resources.getColor(R.color.uncommon));
						break;
					case 'R':
						setField.setTextColor(resources.getColor(R.color.rare));
						break;
					case 'M':
						setField.setTextColor(resources.getColor(R.color.mythic));
						break;
					case 'T':
						setField.setTextColor(resources.getColor(R.color.timeshifted));
						break;
				}
				priceField.setText(data.numberOf + "x" + (data.hasPrice() ? data.getPriceString() : data.message));
				if (data.hasPrice()) {
					priceField.setTextColor(mCtx.getResources().getColor(R.color.light_gray));
				}
				else {
					priceField.setTextColor(mCtx.getResources().getColor(R.color.red));
				}
				if (verbose) {
					priceField.setGravity(Gravity.CENTER);
				}
				else {
					priceField.setGravity(Gravity.RIGHT);
				}
				boolean hidePT = true;
				try {
					float p = Float.parseFloat(data.power);
					if (p != CardDbAdapter.NOONECARES) {
						String pow;
						hidePT = false;
						if (p == CardDbAdapter.STAR)
							pow = "*";
						else if (p == CardDbAdapter.ONEPLUSSTAR)
							pow = "1+*";
						else if (p == CardDbAdapter.TWOPLUSSTAR)
							pow = "2+*";
						else if (p == CardDbAdapter.SEVENMINUSSTAR)
							pow = "7-*";
						else if (p == CardDbAdapter.STARSQUARED)
							pow = "*^2";
						else {
							if (p == (int) p) {
								pow = Integer.valueOf((int) p).toString();
							}
							else {
								pow = Float.valueOf(p).toString();
								;
							}
						}
						pField.setText(pow);
					}
				}
				catch (Exception e) {
				}
				finally {
				}
				try {
					float t = Float.parseFloat(data.toughness);
					if (t != CardDbAdapter.NOONECARES) {
						String tou;
						hidePT = false;
						if (t == CardDbAdapter.STAR)
							tou = "*";
						else if (t == CardDbAdapter.ONEPLUSSTAR)
							tou = "1+*";
						else if (t == CardDbAdapter.TWOPLUSSTAR)
							tou = "2+*";
						else if (t == CardDbAdapter.SEVENMINUSSTAR)
							tou = "7-*";
						else if (t == CardDbAdapter.STARSQUARED)
							tou = "*^2";
						else {
							if (t == (int) t) {
								tou = Integer.valueOf((int) t).toString();
							}
							else {
								tou = Float.valueOf(t).toString();
								;
							}
						}
						tField.setText(tou);
					}
				}
				catch (Exception e) {
				}
				finally {
				}
				boolean hideLoyalty = true;
				float l = data.loyalty;
				if (l != -1 && l != CardDbAdapter.NOONECARES) {
					hideLoyalty = false;
					if (l == (int) l) {
						tField.setText(Integer.toString((int) l));
					}
					else {
						tField.setText(Float.toString(l));
					}
				}
				pField.setVisibility(View.VISIBLE);
				slashField.setVisibility(View.VISIBLE);
				tField.setVisibility(View.VISIBLE);

				if (!hideLoyalty) {
					pField.setVisibility(View.GONE);
					slashField.setVisibility(View.GONE);
				}
				else if (hidePT) {
					pField.setVisibility(View.GONE);
					slashField.setVisibility(View.GONE);
					tField.setVisibility(View.GONE);
				}
				if (!verbose) {
					pField.setVisibility(View.GONE);
					slashField.setVisibility(View.GONE);
					tField.setVisibility(View.GONE);
				}
			}
			return v;
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.wishlist_menu, menu);
		return true;
	}
}
