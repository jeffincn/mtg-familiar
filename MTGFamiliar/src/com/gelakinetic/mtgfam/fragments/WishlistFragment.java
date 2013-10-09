package com.gelakinetic.mtgfam.fragments;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers.CardData;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

public class WishlistFragment extends FamiliarFragment {

	private final static int											DIALOG_UPDATE_CARD		= 1;
	private final static int											DIALOG_PRICE_SETTING	= 2;
	private final static int											DIALOG_CONFIRMATION		= 3;
	private final static int											DIALOG_SHARE					= 4;
	private int																		positionForDialog;

	private AutoCompleteTextView									namefield;

	private Button																bAdd;
	private ImageButton															camerabutton;
	private TextView															tradePrice;
	private CheckBox												foilButton;
	private ExpandableListView										expWishlist;
	private WishlistAdapter												aaExpWishlist;
	private ArrayList<String>											cardNames;
	private ArrayList<ArrayList<String>>					cardSetNames;
	private static ArrayList<ArrayList<CardData>>	cardSetWishlists;

	private EditText															numberfield;

	public int																		priceSetting;
	private boolean																showTotalPrice;
	private boolean																showIndividualPrices;
	private boolean																verbose;
	private TradeListHelpers											mTradeListHelper;

	public static final String										card_not_found				= "Card Not Found";
	public static final String										mangled_url						= "Mangled URL";
	public static final String										database_busy					= "Database Busy";
	public static final String										fetch_failed					= "Fetch Failed";

	private boolean																doneLoading						= false;

	private static final int	LOW_PRICE	= 0;
	private static final int	AVG_PRICE	= 1;
	private static final int	HIGH_PRICE	= 2;
	private static final int	FOIL_PRICE	= 3;

	public WishlistFragment() {
		/* http://developer.android.com/reference/android/app/Fragment.html
		 * All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// This is necessary to keep the image search task running through rotation
		this.setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View myFragmentView = inflater.inflate(R.layout.wishlist_activity, container, false);

		mTradeListHelper = new TradeListHelpers();

		namefield = (AutoCompleteTextView) myFragmentView.findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(getActivity(), null));
		setKeyboardFocus(savedInstanceState, namefield, false, 100);
		
		numberfield = (EditText) myFragmentView.findViewById(R.id.numberInput);
		numberfield.setText("1");

		foilButton = (CheckBox) myFragmentView.findViewById(R.id.wishlistFoil);
		
		camerabutton = (ImageButton) myFragmentView.findViewById(R.id.cameraButton);

		cardNames = new ArrayList<String>();
		cardSetNames = new ArrayList<ArrayList<String>>();
		cardSetWishlists = new ArrayList<ArrayList<CardData>>();
		bAdd = (Button) myFragmentView.findViewById(R.id.addCard);
		tradePrice = (TextView) myFragmentView.findViewById(R.id.priceText);

		camerabutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				takePictureAndSearchImageIntent();
			}
		});
		
		bAdd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().length() > 0) {
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					int numberOf = Integer.parseInt(numberOfFromField);

					
					boolean foil = foilButton.isChecked();	
					try {
						String cardName = namefield.getText().toString();
						Cursor cards = mDbHelper.fetchCardByName(cardName, new String[]{CardDbAdapter.KEY_SET, CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_RARITY});
						String setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
						if (!TradeListHelpers.canBeFoil(setCode, mDbHelper)){						
							foil = false;
						}
					} catch (FamiliarDbException e1) {
					}
					
					CardData data = mTradeListHelper.new CardData(namefield.getText().toString(), "", "", numberOf, 0, "loading",
							null, null, null, null, null, null, CardDbAdapter.NOONECARES, '-');
		
					data.setIsFoil(foil);
					
					try {
						AddCardOrUpdateSetCounts(data);
					} catch (FamiliarDbException e) {
						getMainActivity().showDbErrorToast();
						getMainActivity().getSupportFragmentManager().popBackStack();
						return;
					}
					aaExpWishlist.notifyDataSetChanged();

					namefield.setText("");
					numberfield.setText("1");
					foilButton.setChecked(false);
				}
				else {
					Toast.makeText(getActivity(), getString(R.string.wishlist_toast_select_card), Toast.LENGTH_SHORT).show();
				}
			}
		});

		expWishlist = (ExpandableListView) myFragmentView.findViewById(R.id.wishlist);

		expWishlist.setGroupIndicator(null);
		expWishlist.setChildIndicator(null);
		expWishlist.setDividerHeight(0);

		aaExpWishlist = new WishlistAdapter(getActivity(), expWishlist);
		expWishlist.setAdapter(aaExpWishlist);

		expWishlist.setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				try {
					removeDialog();
				}
				catch (IllegalArgumentException e) {
				}
				positionForDialog = groupPosition;
				showDialog(DIALOG_UPDATE_CARD);
				return false;
			}
		});

		return myFragmentView;
	}

	@Override
    protected void onImageSearchSuccess(long multiverseId, String name) {
    	// this method must be overridden by each class calling takePictureAndSearchImageIntent
		super.onImageSearchSuccess(multiverseId, name);
		namefield.setText(name);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		ArrayList<CardData> lWishlist = new ArrayList<CardData>();
		for (ArrayList<CardData> lCardlist : cardSetWishlists) {
			// add all the non-zeros to the persistence list
			for (CardData card : lCardlist)
				if (card.numberOf != 0)
					lWishlist.add(card);
		}
		WishlistHelpers.WriteWishlist(getActivity(), lWishlist);
		getMainActivity().getSpiceManager().cancelAllRequests();
	}

	@Override
	public void onResume() {
		super.onResume();
		doneLoading = false;
		showTotalPrice = getMainActivity().getPreferencesAdapter().getShowTotalWishlistPrice();
		showIndividualPrices = getMainActivity().getPreferencesAdapter().getShowIndividualWishlistPrices();
		verbose = getMainActivity().getPreferencesAdapter().getVerboseWishlist();
		priceSetting = Integer.parseInt(getMainActivity().getPreferencesAdapter().getTradePrice());

		if (!showTotalPrice) {
			tradePrice.setVisibility(View.GONE);
		}
		else {
			tradePrice.setVisibility(View.VISIBLE);
		}

		ArrayList<CardData> lWishlist = new ArrayList<CardData>();
		cardNames.clear();
		cardSetNames.clear();
		cardSetWishlists.clear();

		try {
			WishlistHelpers.ReadWishlist(getActivity(), mDbHelper, lWishlist);
		} catch (FamiliarDbException e1) {
			getMainActivity().showDbErrorToast();
			getMainActivity().getSupportFragmentManager().popBackStack();
			return;
		}

		// split each card into its own mini-list
		try {
			for (CardData card : (ArrayList<CardData>) lWishlist) {
				AddCardOrUpdateSetCounts(card);
			}
		} catch (FamiliarDbException e) {
			getMainActivity().showDbErrorToast();
			getMainActivity().getSupportFragmentManager().popBackStack();
			return;
		}
		aaExpWishlist.notifyDataSetChanged();
		lWishlist.clear();

		removeDialog();
		doneLoading = true;
	}

	private void AddCardOrUpdateSetCounts(CardData cardToAdd) throws FamiliarDbException {
		ArrayList<String> setCodes;
		ArrayList<CardData> lCardlist;

		int position = cardNames.indexOf(cardToAdd.name);
		// if the card's not in the list yet
		if (position == -1) {
			setCodes = new ArrayList<String>();
			lCardlist = new ArrayList<CardData>();
			boolean isFoil = cardToAdd.foil;
			if (verbose || cardToAdd.setCode == "" || cardToAdd.rarity == 45) {
				Cursor c;
				if (cardToAdd.setCode != "" && cardToAdd.rarity != 45) {
					c = mDbHelper.fetchCardByNameAndSet(cardToAdd.name, cardToAdd.setCode);
				}
				else {
					if (verbose) {
						c = mDbHelper.fetchLatestCardByName(cardToAdd.name, CardDbAdapter.allData);
					}
					else {
						c = mDbHelper.fetchLatestCardByName(cardToAdd.name, new String[] { CardDbAdapter.KEY_SET,
								CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_RARITY });
					}
				}
				if (c.getCount() == 0) {
					Toast.makeText(getActivity(), R.string.wishlist_toast_no_card, Toast.LENGTH_LONG).show();
					c.close();
					return;
				}
				String setCode = c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET));
				String tcgName = mDbHelper.getTCGname(setCode);
				setCodes.add(setCode);

				if (verbose) {
					cardToAdd = mTradeListHelper.new CardData(cardToAdd.name, tcgName, setCode, cardToAdd.numberOf, 0, "loading", c.getString(c
							.getColumnIndex(CardDbAdapter.KEY_NUMBER)), c.getString(c.getColumnIndex(CardDbAdapter.KEY_TYPE)),
							c.getString(c.getColumnIndex(CardDbAdapter.KEY_MANACOST)), c.getString(c
									.getColumnIndex(CardDbAdapter.KEY_ABILITY)), c.getString(c.getColumnIndex(CardDbAdapter.KEY_POWER)),
							c.getString(c.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS)), c.getInt(c
									.getColumnIndex(CardDbAdapter.KEY_LOYALTY)), c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY)));
				}
				else {
					cardToAdd = mTradeListHelper.new CardData(cardToAdd.name, tcgName, setCode, cardToAdd.numberOf, 0, "loading", c.getString(c
							.getColumnIndex(CardDbAdapter.KEY_NUMBER)), null, null, null, null, null, 0, c.getInt(c
							.getColumnIndex(CardDbAdapter.KEY_RARITY)));
				}
				c.close();
				lCardlist.add(cardToAdd);
			}
			else {
				setCodes.add(cardToAdd.setCode);
				lCardlist.add(cardToAdd);
			}
			cardToAdd.setIsFoil(isFoil);
			
			// add it (with child lists)
			cardNames.add(cardToAdd.name);
			cardSetNames.add(setCodes);
			cardSetWishlists.add(lCardlist);
		}
		else {
			setCodes = cardSetNames.get(position);
			lCardlist = cardSetWishlists.get(position);
			if (cardToAdd.setCode == "" || cardToAdd.rarity == 45) {
				Cursor c;
				c = mDbHelper.fetchLatestCardByName(cardToAdd.name, new String[] { CardDbAdapter.KEY_SET, CardDbAdapter.KEY_NUMBER,
						CardDbAdapter.KEY_RARITY });
				if (c.getCount() == 0) {
					Toast.makeText(getActivity(), R.string.wishlist_toast_no_card, Toast.LENGTH_LONG).show();
					c.close();
					return;
				}
				cardToAdd.setCode = c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET));
				cardToAdd.rarity = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY));
				c.close();
				cardToAdd.tcgName = mDbHelper.getTCGname(cardToAdd.setCode);
			}
			int location = setCodes.indexOf(cardToAdd.setCode);
			if (location != -1) {
				if (lCardlist.get(location).foil == cardToAdd.foil){
					CardData existingCard = lCardlist.get(location);
					existingCard.numberOf += cardToAdd.numberOf;
				} else {
					lCardlist.add(cardToAdd);
				}
				// we shouldn't need this if it's a pointer
				// lCardlist.set(location, card);
			}
			else {
				setCodes.add(cardToAdd.setCode);
				lCardlist.add(cardToAdd);
			}

		}

		if (showTotalPrice || showIndividualPrices) {
			loadPrice(cardToAdd, aaExpWishlist);
		}
	}

	private void mailWishlist(boolean includeTcgName) {
		// Use a more generic send text intent. It can also do emails
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "MTG Familiar Wishlist");
		sendIntent.putExtra(Intent.EXTRA_TEXT, WishlistHelpers.GetReadableWishlist(cardSetWishlists, includeTcgName));
		sendIntent.setType("text/plain");
		
		try {
		startActivity(Intent.createChooser(sendIntent, "Share Wishlist"));
		}
		catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(getActivity(), getString(R.string.error_no_email_client), Toast.LENGTH_SHORT).show();
		}
	}

	public void UpdateTotalPrices() {
		if (doneLoading) {
			int totalPrice = GetPricesFromCardLists(cardSetWishlists);
			if (showTotalPrice) {
				Resources resources = getActivity().getResources();
				int color = PriceListsHaveBadValues(cardSetWishlists) ? resources.getColor(R.color.red) : resources
						.getColor(R.color.white);
				String sTotal = "$" + (totalPrice / 100) + "." + String.format("%02d", (totalPrice % 100));
				tradePrice.setText(sTotal);
				tradePrice.setTextColor(color);
			}
			aaExpWishlist.notifyDataSetChanged();
			expWishlist.invalidate();
			expWishlist.postInvalidate();
		}
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

					// Remove the card from the list, unless it was just a fetch
					// failed.
					// Otherwise, the card does not exist, or there is a
					// database problem

					if (message.compareTo(card_not_found) == 0) {
						cardlist.remove(data);
						i--;
						aaExpWishlist.notifyDataSetChanged();
						Toast.makeText(getActivity(), data.name + ": " + card_not_found, Toast.LENGTH_LONG).show();
					}
					else if (message.compareTo(mangled_url) == 0) {
						cardlist.remove(data);
						i--;
						aaExpWishlist.notifyDataSetChanged();
						Toast.makeText(getActivity(), data.name + ": " + mangled_url, Toast.LENGTH_LONG).show();
					}
					else if (message.compareTo(database_busy) == 0) {
						cardlist.remove(data);
						i--;
						aaExpWishlist.notifyDataSetChanged();
						Toast.makeText(getActivity(), data.name + ": " + database_busy, Toast.LENGTH_LONG).show();
					}
					else if (message.compareTo(fetch_failed) == 0) {

					}
				}
			}
		}
		return totalPrice;
	}

	class WishlistAdapter extends BaseExpandableListAdapter {

		private Resources		resources;
		ExpandableListView	_list;
		private ImageGetter	imgGetter;

		public WishlistAdapter(Context context, ExpandableListView list) {
			_list = list;
			resources = context.getResources();
			this.imgGetter = ImageGetterHelper.GlyphGetter(resources);
		}

		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			View v = convertView;
			CardData data = WishlistFragment.cardSetWishlists.get(groupPosition).get(childPosition);

			if (v == null) {
				LayoutInflater inf = getActivity().getLayoutInflater();
				v = inf.inflate(R.layout.wishlist_cardset_row, null);
			}

			// // empty view for 0-count rows
			// if (data == null || data.numberOf == 0)
			// v = new View(getActivity());
			// else {
			// LayoutInflater inf = getLayoutInflater();
			// v = inf.inflate(R.layout.wishlist_cardset_row, null);
			TextView setField = (TextView) v.findViewById(R.id.wishlistRowSet);
			TextView priceField = (TextView) v.findViewById(R.id.wishlistRowPrice);
			ImageView foilField = (ImageView) v.findViewById(R.id.wishlistSetRowFoil);

			
			if(setField == null || priceField == null) {
				LayoutInflater inf = getActivity().getLayoutInflater();
				v = inf.inflate(R.layout.wishlist_cardset_row, null);
				setField = (TextView) v.findViewById(R.id.wishlistRowSet);
				priceField = (TextView) v.findViewById(R.id.wishlistRowPrice);
			}

			setField.setText(data.tcgName);
			char r = (char) data.rarity;
			switch (r) {
				case 'C':
				case 'c':
					setField.setTextColor(resources.getColor(R.color.common));
					break;
				case 'U':
				case 'u':
					setField.setTextColor(resources.getColor(R.color.uncommon));
					break;
				case 'R':
				case 'r':
					setField.setTextColor(resources.getColor(R.color.rare));
					break;
				case 'M':
				case 'm':
					setField.setTextColor(resources.getColor(R.color.mythic));
					break;
				case 'T':
				case 't':
					setField.setTextColor(resources.getColor(R.color.timeshifted));
					break;
			}
			
			foilField.setVisibility((data.foil ? View.VISIBLE : View.GONE));
			
			
			priceField.setText((showIndividualPrices ? "" : "x") + data.numberOf
					+ (showIndividualPrices ? ("x" + (data.hasPrice() ? data.getPriceString() : data.message)) : ""));
			if (data.hasPrice() || !showIndividualPrices) {
				priceField.setTextColor(resources.getColor(R.color.light_gray));
			}
			else {
				priceField.setTextColor(resources.getColor(R.color.red));
			}
			// }
			return v;
		}

		public int getChildrenCount(int groupPosition) {
			return WishlistFragment.cardSetWishlists.get(groupPosition).size();
		}

		public Object getGroup(int groupPosition) {
			return null;
		}

		public int getGroupCount() {
			return WishlistFragment.cardSetWishlists.size();
		}

		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inf = getActivity().getLayoutInflater();
				v = inf.inflate(R.layout.wishlist_row, null);
			}
			CardData data = WishlistFragment.cardSetWishlists.get(groupPosition).get(0);
			if (data != null) {
				final CardData finalData = data;
				final int pos = groupPosition;
				View.OnClickListener onClick = new View.OnClickListener() {
					public void onClick(View v) {
						removeDialog();
						positionForDialog = pos;
						showDialog(DIALOG_UPDATE_CARD);
					}
				};

				TextView nameField = (TextView) v.findViewById(R.id.wishlistRowName);
				TextView typeField = (TextView) v.findViewById(R.id.cardtype);
				TextView costField = (TextView) v.findViewById(R.id.cardcost);
				TextView abilityField = (TextView) v.findViewById(R.id.cardability);
				TextView pField = (TextView) v.findViewById(R.id.cardp);
				TextView slashField = (TextView) v.findViewById(R.id.cardslash);
				TextView tField = (TextView) v.findViewById(R.id.cardt);
				ImageButton cardviewButton = (ImageButton) v.findViewById(R.id.cardview_button);
				
				if (nameField == null) {
					return v;
				}

				cardviewButton.setOnClickListener(new View.OnClickListener(){

					@Override
					public void onClick(View v) {
						Bundle args = new Bundle();
						try {
							args.putLong("id", mDbHelper.fetchIdByName(finalData.name));
							args.putBoolean(SearchViewFragment.RANDOM, false);
							args.putBoolean("isSingle", true);
							CardViewFragment cvFrag = new CardViewFragment();
							startNewFragment(cvFrag, args);
						} catch (FamiliarDbException e) {
						}
					}});
				
				nameField.setText(data.name);
				nameField.setOnClickListener(onClick);

				if (!verbose) {
					typeField.setVisibility(View.GONE);
					costField.setVisibility(View.GONE);
					abilityField.setVisibility(View.GONE);
					pField.setVisibility(View.GONE);
					slashField.setVisibility(View.GONE);
					tField.setVisibility(View.GONE);
				}
				else {
					if (data.ability == null || data.ability == "") {
						try {
							data = TradeListHelpers.FetchCardData(data, mDbHelper);
						} catch (FamiliarDbException e) {
							getMainActivity().showDbErrorToast();
							getMainActivity().getSupportFragmentManager().popBackStack();
							return v;
						}
					}
					String type = data.type;
					// we check the actual values for visibility here (instead
					// of just the verbose flag)
					// because some card types won't have some of these - i.e.
					// tokens don't have cost, etc.
					if (type == null || type == "") {
						typeField.setVisibility(View.GONE);
					}
					else {
						typeField.setVisibility(View.VISIBLE);
						typeField.setText(type);
						typeField.setOnClickListener(onClick);
					}
					String manaCost = data.cost;
					if (manaCost == null || manaCost == "") {
						costField.setVisibility(View.GONE);
					}
					else {
						costField.setVisibility(View.VISIBLE);
						manaCost = manaCost.replace("{", "<img src=\"").replace("}", "\"/>");
						costField.setText(ImageGetterHelper.jellyBeanHack(manaCost, imgGetter, null));
						costField.setOnClickListener(onClick);
					}
					String ability = data.ability;
					if (ability == null || ability == "") {
						abilityField.setVisibility(View.GONE);
					}
					else {
						abilityField.setVisibility(View.VISIBLE);
						ability = ability.replace("{", "<img src=\"").replace("}", "\"/>");
						abilityField.setText(ImageGetterHelper.jellyBeanHack(ability, imgGetter, null));
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
								}
							}
							pField.setText(pow);
						}
					}
					catch (NumberFormatException e) {

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
								}
							}
							tField.setText(tou);
						}
					}
					catch (NumberFormatException e) {
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
				}
			}
			_list.expandGroup(groupPosition); // used to Expand the child list
			// automatically at the time of
			// displaying
			_list.collapseGroup(groupPosition); // used to Expand the child list
			// automatically at the time of
			// displaying
			_list.expandGroup(groupPosition); // used to Expand the child list
			// automatically at the time of
			// displaying
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
				showDialog(DIALOG_CONFIRMATION);
				return true;
			case R.id.wishlist_menu_settings:
				showDialog(DIALOG_PRICE_SETTING);
				return true;
			case R.id.wishlist_menu_share:
				showDialog(DIALOG_SHARE);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.wishlist_menu, menu);
	}

	protected void showDialog(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		
		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			final WishlistHelpers	wh	= new WishlistHelpers();
			Dialog								dlg;

			@Override
			public void onDismiss(DialogInterface dialog) {
				super.onDismiss(dialog);

				if (id == DIALOG_UPDATE_CARD) {
					switch (wh.dismissReason) {
						case WishlistHelpers.CANCEL: {
							break;
						}
						case WishlistHelpers.DONE:
						default: {
							if(dlg == null) {
								return;
							}
							LinearLayout lvSets = (LinearLayout) dlg.findViewById(R.id.setList);

							ArrayList<CardData> cardlist = cardSetWishlists.get(positionForDialog);
							ArrayList<String> setNames = cardSetNames.get(positionForDialog);

							cardlist.clear();
							setNames.clear();
							int totalCards = 0;
							for (int i = 0; i < lvSets.getChildCount(); i++) {
								View v = lvSets.getChildAt(i);
								int numberField;
								try {
									numberField = Integer.valueOf(((EditText) v.findViewById(R.id.numberInput)).getText().toString());
								}
								catch (NumberFormatException e) {
									numberField = 0;
								}

								if (numberField > 0) {
									String setName = ((TextView) v.findViewById(R.id.cardset)).getText().toString();
									int visiblity = ((ImageView) v.findViewById(R.id.wishlistDialogFoil)).getVisibility();
									boolean setIsFoil = ( visiblity == View.VISIBLE ? true : false);
									
									String setCode;
									try {
										setCode = mDbHelper.getSetCode(setName);
									} catch (FamiliarDbException e) {
										getMainActivity().showDbErrorToast();
										getMainActivity().getSupportFragmentManager().popBackStack();
										return;
									}
									totalCards += numberField;
									CardData cd = mTradeListHelper.new CardData(cardNames.get(positionForDialog), setName, setCode,
											numberField, 0, "loading", null, null, null, null, null, null, CardDbAdapter.NOONECARES, '-');
									cd.setIsFoil(setIsFoil);

									try {
										if (showTotalPrice || showIndividualPrices) {
											cd = TradeListHelpers.FetchCardData(cd, mDbHelper);
											cd.message = ("loading");
											loadPrice(cd, aaExpWishlist);
										}
										else
											cd = TradeListHelpers.FetchCardData(cd, mDbHelper);
										setNames.add(setName);
										cardlist.add(cd);
									}
									catch (FamiliarDbException e) {
										getMainActivity().showDbErrorToast();
										getMainActivity().getSupportFragmentManager().popBackStack();
										return;
									}
								}else if (showTotalPrice || showIndividualPrices)
									UpdateTotalPrices();
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
				}
				//removeDialog();
				dlg = null;
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				setShowsDialog(true);
				switch (id) {
					case DIALOG_UPDATE_CARD: {
						if (doneLoading) {
							try{
								dlg = (wh).getDialog(cardNames.get(positionForDialog), WishlistFragment.this, getMainActivity(), 
										cardSetWishlists.get(positionForDialog), cardSetWishlists.get(positionForDialog).get(0).foil);
							}
							catch(IndexOutOfBoundsException ex){
								try {
									dlg = (wh).getDialog(cardNames.get(positionForDialog), WishlistFragment.this, getMainActivity());
								} catch (FamiliarDbException e) {
									getMainActivity().showDbErrorToast();
									getMainActivity().getSupportFragmentManager().popBackStack();
									return null;
								}
							} catch (FamiliarDbException e) {
								getMainActivity().showDbErrorToast();
								getMainActivity().getSupportFragmentManager().popBackStack();
								setShowsDialog(false);
								return null;
							}
							return dlg;
						}
						else {
							setShowsDialog(false);
							return null;
						}
					}
					case DIALOG_PRICE_SETTING: {
						if (doneLoading) {
							AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

							builder.setTitle(R.string.trader_pricing_dialog_title);
							builder.setSingleChoiceItems(new String[] { getString(R.string.trader_Low), getString(R.string.trader_Average), getString(R.string.trader_High) }, priceSetting,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											priceSetting = which;
											dialog.dismiss();

											if (showTotalPrice || showIndividualPrices) {
												// Update ALL the prices (for non-zero counts)!
												for (int i = 0; i < cardNames.size(); i++) {
													for (CardData data : cardSetWishlists.get(i)) {
														if (data.numberOf > 0) {
															data.message = ("loading");
															loadPrice(data, aaExpWishlist);
														}
													}
												}
												aaExpWishlist.notifyDataSetChanged();
											}

											// And also update the preference
											getMainActivity().getPreferencesAdapter().setTradePrice(String.valueOf(priceSetting));

											removeDialog();
										}
									});

							Dialog dialog = builder.create();

							// TODO verify this
							// dialog.setOnDismissListener(new
							// DialogInterface.OnDismissListener() {
							// public void onDismiss(DialogInterface dialog) {
							// removeDialog();
							// }
							// });

							return dialog;
						}
						else {
							setShowsDialog(false);
							return null;
						}
					}
					case DIALOG_CONFIRMATION: {
						if (doneLoading) {
							View dialogLayout = this.getActivity().getLayoutInflater().inflate(R.layout.simple_message_layout, null);
							TextView text = (TextView) dialogLayout.findViewById(R.id.message);
							text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.wishlist_empty_dialog_text)));
							text.setMovementMethod(LinkMovementMethod.getInstance());

							return new AlertDialog.Builder(this.getActivity()).setTitle(R.string.wishlist_empty_dialog_title)
									.setView(dialogLayout).setPositiveButton(R.string.dialog_ok, new OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											cardNames.clear();
											cardSetNames.clear();
											cardSetWishlists.clear();
											aaExpWishlist.notifyDataSetChanged();
											if (showTotalPrice || showIndividualPrices)
												UpdateTotalPrices();
											removeDialog();
										}
									}).setNegativeButton(R.string.dialog_cancel, new OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											removeDialog();
										}
									}).setCancelable(true).create();
						}
						else {
							setShowsDialog(false);
							return null;
						}
					}
					case DIALOG_SHARE: {
						if (doneLoading) {
							View dialogLayout = this.getActivity().getLayoutInflater().inflate(R.layout.simple_message_layout, null);
							TextView text = (TextView) dialogLayout.findViewById(R.id.message);
							text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.wishlist_share_include_set)));
							text.setMovementMethod(LinkMovementMethod.getInstance());

							return new AlertDialog.Builder(this.getActivity()).setTitle(R.string.wishlist_share)
									.setView(dialogLayout).setPositiveButton(R.string.dialog_yes, new OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											mailWishlist(true);
											removeDialog();
										}
									}).setNegativeButton(R.string.dialog_no, new OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											mailWishlist(false);
											removeDialog();
										}
									}).setCancelable(true).create();
						}
						else {
							setShowsDialog(false);
							return null;
						}
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(ft, DIALOG_TAG);
	}

	private void loadPrice(final CardData data, final WishlistAdapter adapter) {
		
		PriceFetchRequest priceRequest = new PriceFetchRequest(data.name, data.setCode, data.cardNumber, -1,mDbHelper);
		final boolean foilOverride = data.foil;
		getMainActivity().getSpiceManager().execute( priceRequest, data.name + "-" + data.setCode, DurationInMillis.ONE_DAY, new RequestListener< PriceInfo >(){
	        @Override
	        public void onRequestFailure( SpiceException spiceException ) {
				data.message = spiceException.getMessage();
				
				UpdateTotalPrices();
				adapter.notifyDataSetChanged();
	        }

	        @Override
	        public void onRequestSuccess( final PriceInfo result ) {
	        	if (result != null) {
	        		int cardPrice = (foilOverride ? FOIL_PRICE : priceSetting);
	        		
	        		switch(cardPrice) {
		        		case LOW_PRICE:
		        		{
		        			data.price = (int) (result.low * 100);
		        			break;
		        		}
		        		default:
		        		case AVG_PRICE:
		        		{
		        			data.price = (int) (result.average * 100);
		        			break;
		        		}
		        		case HIGH_PRICE:
		        		{
		        			data.price = (int) (result.high * 100);
		        			break;
		        		}
		        		case FOIL_PRICE:
		        		{
		        			data.price = (int) (result.foil_average * 100);
		        			break;
		        		}
	        		}
	        		data.message = null;
	        	}
	        	else {
	        		data.message = getString(R.string.trader_no_price);
	        	}
	        	
				UpdateTotalPrices();
				adapter.notifyDataSetChanged();
	        }
		} );
	}
}
