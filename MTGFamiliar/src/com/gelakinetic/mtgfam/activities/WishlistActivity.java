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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
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

	private Button							bAdd;
	private TextView						tradePrice;
	private ExpandableListView				expWishist;
	private WishlistAdapter					aaExpWishlist;
	private ArrayList<String>				cardNames;
	private ArrayList<ArrayList<String>>	cardSetNames;
	private static ArrayList<ArrayList<CardData>>	cardSetWishlists;

	private EditText						numberfield;

	public int								priceSetting;
	private boolean							showTotalPrice;
	private boolean							verbose;
	private TradeListHelpers				mTradeListHelper;

	public static final String				card_not_found			= "Card Not Found";
	public static final String				mangled_url				= "Mangled URL";
	public static final String				database_busy			= "Database Busy";
	public static final String				card_dne				= "Card Does Not Exist";
	public static final String				fetch_failed			= "Fetch Failed";
	public static final String				number_of_invalid		= "Number of Cards Invalid";
	public static final String				price_invalid			= "Price Invalid";

	private static final int				AVG_PRICE				= 1;
	private boolean							doneLoading				= false;

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

		cardNames = new ArrayList<String>();
		cardSetNames = new ArrayList<ArrayList<String>>();
		cardSetWishlists = new ArrayList<ArrayList<CardData>>();
		bAdd = (Button) findViewById(R.id.addCard);
		tradePrice = (TextView) findViewById(R.id.priceText);

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

					CardData data = mTradeListHelper.new CardData(namefield.getText().toString(), "", "", numberOf, 0, "loading", null, null, null, null, null, null,
							CardDbAdapter.NOONECARES, '-');
					
					AddCardOrUpdateSetCounts(data);
					aaExpWishlist.notifyDataSetChanged();

					namefield.setText("");
					numberfield.setText("1");
				}
				else {
					Toast.makeText(getApplicationContext(), getString(R.string.wishlist_toast_select_card), Toast.LENGTH_SHORT).show();
				}
			}
		});

		priceSetting = Integer.parseInt(preferences.getString("tradePrice", String.valueOf(AVG_PRICE)));
		
		expWishist = (ExpandableListView) this.findViewById(R.id.wishlist);
       
		expWishist.setGroupIndicator(null);
		expWishist.setChildIndicator(null);
		expWishist.setDividerHeight(0);
        
		aaExpWishlist = new WishlistAdapter(this,expWishist);
		expWishist.setAdapter(aaExpWishlist);

		expWishist.setOnChildClickListener(
			new OnChildClickListener() {
	            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
					try {
						removeDialog(DIALOG_UPDATE_CARD);
					}
					catch (IllegalArgumentException e) {
					}
					positionForDialog = groupPosition;
					showDialog(DIALOG_UPDATE_CARD);
	            	return false;
	            }
	        }
		);
	}

	@Override
	protected void onPause() {
		super.onPause();
		ArrayList<CardData> lWishlist=new ArrayList<CardData>();
		for(ArrayList<CardData> lCardlist : cardSetWishlists){
			//add all the non-zeros to the persistence list
			for(CardData card:lCardlist)
				if(card.numberOf!=0)
					lWishlist.add(card);
		}
		WishlistHelpers.WriteWishlist(getApplicationContext(),lWishlist);
	}

	@Override
	protected void onResume() {
		super.onResume();
		doneLoading = false;
		ArrayList<CardData> lWishlist = new ArrayList<CardData>();

		WishlistHelpers.ReadWishlist(getApplicationContext(), (WishlistActivity) me, mDbHelper, lWishlist);

		//split each card into its own mini-list
		for(CardData card: (ArrayList<CardData>)lWishlist){
			AddCardOrUpdateSetCounts(card);
		}
		aaExpWishlist.notifyDataSetChanged();
		lWishlist.clear();
		
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
		doneLoading = true;
	}
	
	private void AddCardOrUpdateSetCounts(CardData card){
		ArrayList<String> setCodes;
		ArrayList<CardData> lCardlist;

		//if this is the first copy of the card, build a full list of set code/counts for that card
		int position=cardNames.indexOf(card.name);
		boolean ensureFirst = false;
		if(position == -1){
			ensureFirst = true;
			setCodes = new ArrayList<String>();
			lCardlist = new ArrayList<CardData>();
			Cursor c = mDbHelper.fetchCardByName(card.name);
			
			//make a place holder item for each version set of this card
			while (!c.isAfterLast()) {
				String setCode=c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET));
				String tcgName=mDbHelper.getTCGname(setCode);
				setCodes.add(setCode);

				lCardlist.add(new TradeListHelpers().new CardData(card.name, tcgName, setCode, 0,
						0, "loading", null, null, null, null, null, null,
						CardDbAdapter.NOONECARES, '-'));
				c.moveToNext();
			}
			c.deactivate();
			c.close();
			cardNames.add(card.name);
			cardSetNames.add(setCodes);
			cardSetWishlists.add(lCardlist);

			position=cardNames.indexOf(card.name);
		}
		else
		{
			//otherwise, we've already made a place holder - just find it
			setCodes = cardSetNames.get(position);
			lCardlist = cardSetWishlists.get(position);
		}
		//now that we have the correct place holder, add the card or update the set count with what as passed in
		if(card.setCode != null  && card.setCode != ""){
			lCardlist.set(setCodes.indexOf(card.setCode),card);
		}
		else {
			int i = card.numberOf;
			card = lCardlist.get(0);
			card.numberOf += i;
			lCardlist.set(0,card);
		}
		cardSetWishlists.set(position,lCardlist);
		//we bind to the first card in the card list, so ensure that it has details if we're showing those details
		if(ensureFirst && position != 0 && verbose){
			FetchPriceTask loadPrice = mTradeListHelper.new FetchPriceTask(lCardlist.get(0), aaExpWishlist, priceSetting, null, (WishlistActivity) me);
			loadPrice.execute();
		}
		FetchPriceTask loadPrice = mTradeListHelper.new FetchPriceTask(card, aaExpWishlist, priceSetting, null, (WishlistActivity) me);
		loadPrice.execute();
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		if(doneLoading){
			AlertDialog.Builder builder;
			switch (id) {
				case DIALOG_UPDATE_CARD: {
					final WishlistHelpers wh =new WishlistHelpers(); 
					dialog = (wh).getDialog(cardNames.get(positionForDialog), this, cardSetWishlists.get(positionForDialog));
					dialog.show();
	
					final Dialog dlg = dialog;
					dialog.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface di) {
							switch (wh.dismissReason) {
								case 2:
									break;
								case 1:
								default:
									LinearLayout lvSets = (LinearLayout) dlg.findViewById(R.id.setList);
	
									ArrayList<CardData> cardlist = cardSetWishlists.get(positionForDialog);
									int totalCards = 0;
									for (int i = 0; i < lvSets.getChildCount(); i++) {
										View v = lvSets.getChildAt(i);
										int numberField;
										try{
											numberField = Integer.valueOf(((EditText) v.findViewById(R.id.numberInput)).getText().toString());
										}
										catch(NumberFormatException e){
											numberField = 0;
										}
										CardData cd = cardlist.get(i);
										int prior = cd.numberOf;
										cd.numberOf = numberField;
										totalCards += numberField;
										if(prior != numberField && numberField != 0){
											cd.message = ("loading");
											FetchPriceTask task = mTradeListHelper.new FetchPriceTask(cd, aaExpWishlist, priceSetting, null, (WishlistActivity) me);
											task.execute();
										}
										cardlist.set(i,cd);
									}
									if (totalCards == 0) {
										cardSetNames.remove(positionForDialog);
										cardSetWishlists.remove(positionForDialog);
										cardNames.remove(positionForDialog);
									}
									aaExpWishlist.notifyDataSetChanged();
									break;
							}
						}
					});
					
					break;
				}
				case DIALOG_PRICE_SETTING: {
					builder = new AlertDialog.Builder(this);
	
					builder.setTitle("Price Options");
					builder.setSingleChoiceItems(new String[] { "Low", "Average", "High" }, priceSetting, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							priceSetting = which;
							dialog.dismiss();
	
							// Update ALL the prices (for non-zero counts)!
							for (int i = 0; i < cardNames.size(); i++){
								for (CardData data : cardSetWishlists.get(i)) {
									if(data.numberOf > 0){
										data.message = ("loading");
										FetchPriceTask task = mTradeListHelper.new FetchPriceTask(data, aaExpWishlist, priceSetting, null, (WishlistActivity) me);
										task.execute();
									}
								}
							}
							aaExpWishlist.notifyDataSetChanged();
	
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
		}
		return dialog;
	}

	public void UpdateTotalPrices() {
		int totalPrice = GetPricesFromCardLists(cardSetWishlists);
		if (showTotalPrice) {
			Resources resources = mCtx.getResources();
			int color = PriceListsHaveBadValues(cardSetWishlists) ? resources.getColor(R.color.red) : resources.getColor(R.color.white);
			String sTotal = "$" + (totalPrice / 100) + "." + String.format("%02d", (totalPrice % 100));
			tradePrice.setText(sTotal);
			tradePrice.setTextColor(color);
		}
		aaExpWishlist.notifyDataSetChanged();
		expWishist.invalidate();
		expWishist.postInvalidate();
	}

	private boolean PriceListsHaveBadValues(ArrayList<ArrayList<CardData>> cardlists) {

		for (int i = 0; i < cardlists.size(); i++) {
			for (CardData data : cardlists.get(i)) {
				if (data.numberOf > 0 && !data.hasPrice()) {
					return true;
				}
			}
		}
		return false;
	}

	private int GetPricesFromCardLists(ArrayList<ArrayList<CardData>> cardlists) {
		int totalPrice = 0;

		for (int j = 0; j < cardlists.size(); j++) {
			ArrayList<CardData> cardlist = cardlists.get(j);
			for (int i = 0; i < cardlist.size(); i++) {
				CardData data = cardlist.get(i);
				if (data.hasPrice()) {
					totalPrice += data.numberOf * data.price;
				}
				else {
					String message = data.message;
	
					// Remove the card from the list, unless it was just a fetch failed.
					// Otherwise, the card does not exist, or there is a database problem
	
					if (message.compareTo(card_not_found) == 0) {
						cardlist.remove(data);
						i--;
						aaExpWishlist.notifyDataSetChanged();
						Toast.makeText(getApplicationContext(), data.name + ": " + card_not_found, Toast.LENGTH_LONG).show();
					}
					else if (message.compareTo(mangled_url) == 0) {
						cardlist.remove(data);
						i--;
						aaExpWishlist.notifyDataSetChanged();
						Toast.makeText(getApplicationContext(), data.name + ": " + mangled_url, Toast.LENGTH_LONG).show();
					}
					else if (message.compareTo(database_busy) == 0) {
						cardlist.remove(data);
						i--;
						aaExpWishlist.notifyDataSetChanged();
						Toast.makeText(getApplicationContext(), data.name + ": " + database_busy, Toast.LENGTH_LONG).show();
					}
					else if (message.compareTo(card_dne) == 0) {
						cardlist.remove(data);
						i--;
						aaExpWishlist.notifyDataSetChanged();
						Toast.makeText(getApplicationContext(), data.name + ": " + card_dne, Toast.LENGTH_LONG).show();
					}
					else if (message.compareTo(fetch_failed) == 0) {
	
					}
				}
			}
		}
		return totalPrice;
	}


	class WishlistAdapter extends BaseExpandableListAdapter {

	    private Resources resources;
	    ExpandableListView _list;
		private ImageGetter						imgGetter;

	    public WishlistAdapter(Context context, ExpandableListView list) {
	        _list=list;
	        resources = context.getResources();
			this.imgGetter = ImageGetterHelper.GlyphGetter(resources);
	    }

	    public Object getChild(int groupPosition, int childPosition) {
	        return null;
	    }

	    public long getChildId(int groupPosition, int childPosition) {
	        return 0;
	    }

	    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View v = convertView;
			CardData data = WishlistActivity.cardSetWishlists.get(groupPosition).get(childPosition);
			//empty view for 0-count rows
			if (data == null || data.numberOf == 0) 
				v = new View(mCtx);
			else {
				LayoutInflater inf = getLayoutInflater();
				v = inf.inflate(R.layout.wishlist_cardset_row, null);
				TextView setField = (TextView) v.findViewById(R.id.wishlistRowSet);
				TextView priceField = (TextView) v.findViewById(R.id.wishlistRowPrice);

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
					priceField.setTextColor(resources.getColor(R.color.light_gray));
				}
				else {
					priceField.setTextColor(resources.getColor(R.color.red));
				}
			}
			return v;
	    }

	    public int getChildrenCount(int groupPosition) {
	        return WishlistActivity.cardSetWishlists.get(groupPosition).size();
	    }

	    public Object getGroup(int groupPosition) {
	        return null;
	    }

	    public int getGroupCount() {
	        return WishlistActivity.cardSetWishlists.size();
	    }

	    public long getGroupId(int groupPosition) {
	        return 0;
	    }    
	    
	    @Override
	    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inf = getLayoutInflater();
				v = inf.inflate(R.layout.wishlist_row, null);
			}
			CardData data = WishlistActivity.cardSetWishlists.get(groupPosition).get(0);
			if (data != null) {
				final int pos = groupPosition; 
				View.OnClickListener onClick = new View.OnClickListener() {
					public void onClick(View v) {
						try {
							removeDialog(DIALOG_UPDATE_CARD);
						}
						catch (IllegalArgumentException e) {
						}
						positionForDialog = pos;
						showDialog(DIALOG_UPDATE_CARD);
					}}; 
					
				TextView nameField = (TextView) v.findViewById(R.id.wishlistRowName);
				TextView typeField = (TextView) v.findViewById(R.id.cardtype);
				TextView costField = (TextView) v.findViewById(R.id.cardcost);
				TextView abilityField = (TextView) v.findViewById(R.id.cardability);
				TextView pField = (TextView) v.findViewById(R.id.cardp);
				TextView slashField = (TextView) v.findViewById(R.id.cardslash);
				TextView tField = (TextView) v.findViewById(R.id.cardt);

				nameField.setText(data.name);
				nameField.setOnClickListener(onClick);
				
				String type = data.type;
				// we check the actual values for visibility here (instead of just the verbose flag)
				// because some card types won't have some of these - i.e. tokens don't have cost, etc.
				if (!verbose || type == null || type == "") {
					typeField.setVisibility(View.GONE);
				}
				else {
					typeField.setVisibility(View.VISIBLE);
					typeField.setText(type);
					typeField.setOnClickListener(onClick);
				}
				String manaCost = data.cost;
				if (!verbose || manaCost == null || manaCost == "") {
					costField.setVisibility(View.GONE);
				}
				else {
					costField.setVisibility(View.VISIBLE);
					manaCost = manaCost.replace("{", "<img src=\"").replace("}", "\"/>");
					costField.setText(Html.fromHtml(manaCost, imgGetter, null));
					costField.setOnClickListener(onClick);
				}
				String ability = data.ability;
				if (!verbose || ability == null || ability == "") {
					abilityField.setVisibility(View.GONE);
				}
				else {
					abilityField.setVisibility(View.VISIBLE);
					ability = ability.replace("{", "<img src=\"").replace("}", "\"/>");
					abilityField.setText(Html.fromHtml(ability, imgGetter, null));
					abilityField.setOnClickListener(onClick);
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
				pField.setOnClickListener(onClick);
				slashField.setOnClickListener(onClick);
				tField.setOnClickListener(onClick);

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
	        _list.expandGroup(groupPosition);  //used to Expand the child list automatically at the time of displaying
			return v;
	    }

	    public boolean hasStableIds() {
	        return false;
	    }

	    public boolean isChildSelectable(int groupPosition, int childPosition) {
	        return true;
	    }
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.wishlist_menu_clear:
				cardNames.clear();
				cardSetNames.clear();
				cardSetWishlists.clear();
				aaExpWishlist.notifyDataSetChanged();
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
