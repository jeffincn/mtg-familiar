/**
Copyright 2011 Adam Feinstein

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

package com.gelakinetic.mtgfam.activities;

import java.util.ArrayList;
import java.util.Random;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.SearchActivity.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.MyApp;
import com.gelakinetic.mtgfam.helpers.ResultListAdapter;
//import com.gelakinetic.mtgfam.helpers.TradeListHelpers;
//import com.gelakinetic.mtgfam.helpers.TradeListHelpers.CardData;
//import com.gelakinetic.mtgfam.helpers.WishlistHelpers;

public class ResultListActivity extends FamiliarActivity {

	public static final int	NO_RESULT							= 1;
	static int							cursorPosition				= 0;
	static int							cursorPositionOffset	= 0;
	private ListView				lv;
	private Cursor					c;
	private boolean					isSingle							= false;
	private boolean					isRandom							= false;
	private int[]						randomSequence;
	private int							randomIndex						= 0;
	private int							numChoices;
	private boolean					randomFromMenu				= false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_list_activity);

		MyApp appState = ((MyApp) getApplicationContext());
		if (appState.getState() == CardViewActivity.QUITTOSEARCH) {
			this.finish();
			return;
		}

		// After a search, make sure the position is on top
		cursorPosition = 0;
		cursorPositionOffset = 0;

		String[] returnTypes = new String[] { CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_SET,
				CardDbAdapter.KEY_RARITY, CardDbAdapter.KEY_MANACOST, CardDbAdapter.KEY_TYPE, CardDbAdapter.KEY_ABILITY,
				CardDbAdapter.KEY_POWER, CardDbAdapter.KEY_TOUGHNESS, CardDbAdapter.KEY_LOYALTY };

		long id;

		Intent intent = getIntent();

		Bundle extras = intent.getExtras();

		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			// handles a click on a search suggestion; launches activity to show word
			boolean consolidate = preferences.getBoolean("consolidateSearch", true);
			Uri u = intent.getData();
			id = Long.parseLong(u.getLastPathSegment());
			String name = mDbHelper.getNameFromId(id);
			c = mDbHelper.Search(name, null, null, "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
					CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, 0, 0, CardDbAdapter.ALLPRINTINGS, true,
					returnTypes, consolidate);
		}
		else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			boolean consolidate = preferences.getBoolean("consolidateSearch", true);
			String query = intent.getStringExtra(SearchManager.QUERY);
			c = mDbHelper.Search(query, null, null, "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
					CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, 0, 0, CardDbAdapter.ALLPRINTINGS, true,
					returnTypes, consolidate);
		}
		else if ((id = extras.getLong("id")) != 0L) {
			c = mDbHelper.fetchCard(id, null);
		}
		else if ((id = extras.getLong("id0")) != 0L) {
			long id1 = extras.getLong("id1");
			long id2 = extras.getLong("id2");
			Cursor cs[] = new Cursor[3];
			cs[0] = mDbHelper.fetchCard(id, null);
			cs[1] = mDbHelper.fetchCard(id1, null);
			cs[2] = mDbHelper.fetchCard(id2, null);
			c = new MergeCursor(cs);
		}
		else {
			SearchCriteria criteria = (SearchCriteria) extras.getSerializable(SearchActivity.CRITERIA);
			int setLogic = criteria.Set_Logic;
			boolean consolidate = (setLogic==CardDbAdapter.MOSTRECENTPRINTING || setLogic==CardDbAdapter.FIRSTPRINTING)?true:false; 
			c = mDbHelper.Search(criteria.Name, criteria.Text,
					criteria.Type, criteria.Color,
					criteria.Color_Logic, criteria.Set,
					criteria.Pow_Choice, criteria.Pow_Logic,
					criteria.Tou_Choice, criteria.Tou_Logic,
					criteria.Cmc, criteria.Cmc_Logic,
					criteria.Format, criteria.Rarity,
					criteria.Flavor, criteria.Artist,
					criteria.Type_Logic, criteria.Text_Logic,
					criteria.Set_Logic, true, returnTypes, consolidate);
		}

		if (c == null || c.getCount() == 0) {
			Intent i = new Intent();
			setResult(NO_RESULT, i);
			Toast.makeText(this, getString(R.string.search_toast_no_results), Toast.LENGTH_SHORT).show();
			finish();
		}
		else {

			lv = (ListView) findViewById(R.id.resultList);// getListView();
			registerForContextMenu(lv);

			if (extras.getBoolean(SearchActivity.RANDOM)) {
				startRandom();
			}
			else {
				lv.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						Intent i = new Intent(mCtx, CardViewActivity.class);
						i.putExtra("id", id);
						i.putExtra(SearchActivity.RANDOM, isRandom);
						startActivityForResult(i, 0);
					}
				});

				if (c.getCount() == 1) {
					isSingle = true;
					Intent i = new Intent(mCtx, CardViewActivity.class);
					c.moveToFirst();
					id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
					i.putExtra("id", id);
					i.putExtra(SearchActivity.RANDOM, isRandom);
					i.putExtra("IsSingle", isSingle);
					startActivityForResult(i, 0);
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		MyApp appState = ((MyApp) getApplicationContext());
		if (appState.getState() == CardViewActivity.QUITTOSEARCH) {
			this.finish();
			return;
		}
		fillData(c);
		lv.setSelectionFromTop(cursorPosition, cursorPositionOffset);
	}

	@Override
	public void onPause() {
		super.onPause();
		cursorPosition = lv.getFirstVisiblePosition();
		View cursorPositionView = lv.getChildAt(0);
		cursorPositionOffset = (cursorPositionView == null) ? 0 : cursorPositionView.getTop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (c != null) {
			c.deactivate();
			c.close();
		}
	}

	private void fillData(Cursor c) {

		ArrayList<String> fromList = new ArrayList<String>();
		ArrayList<Integer> toList = new ArrayList<Integer>();
		fromList.add(CardDbAdapter.KEY_NAME);
		toList.add(R.id.cardname);
		if (preferences.getBoolean("setPref", true)) {
			fromList.add(CardDbAdapter.KEY_SET);
			toList.add(R.id.cardset);
		}
		if (preferences.getBoolean("manacostPref", true)) {
			fromList.add(CardDbAdapter.KEY_MANACOST);
			toList.add(R.id.cardcost);
		}
		if (preferences.getBoolean("typePref", true)) {
			fromList.add(CardDbAdapter.KEY_TYPE);
			toList.add(R.id.cardtype);
		}
		if (preferences.getBoolean("abilityPref", true)) {
			fromList.add(CardDbAdapter.KEY_ABILITY);
			toList.add(R.id.cardability);
		}
		if (preferences.getBoolean("ptPref", true)) {
			fromList.add(CardDbAdapter.KEY_POWER);
			toList.add(R.id.cardp);
			fromList.add(CardDbAdapter.KEY_TOUGHNESS);
			toList.add(R.id.cardt);
			fromList.add(CardDbAdapter.KEY_LOYALTY);
			toList.add(R.id.cardt);
		}
		String[] from = new String[fromList.size()];
		fromList.toArray(from);

		int[] to = new int[toList.size()];
		for (int i = 0; i < to.length; i++) {
			to[i] = toList.get(i);
		}

//		ArrayList<CardData> wishlist = new ArrayList<CardData>();
//		TradeListHelpers tlh = new TradeListHelpers();
//		c.moveToFirst();
//		while (!c.isAfterLast()) {
//			CardData cd = (tlh).new CardData(
//					c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME)), 
//					c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)),
//					1);
//			wishlist.add(cd);
//			c.moveToNext();
//		}
//
//		new WishlistHelpers();
//		WishlistHelpers.WriteWishlist(mCtx,wishlist);
		
		ResultListAdapter rla = new ResultListAdapter(this, R.layout.card_row, c, from, to, this.getResources());
		lv.setAdapter(rla);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Intent i;
		if (requestCode == 0) {

			long id;
			long lastID;
			switch (resultCode) {
				case CardViewActivity.RANDOMLEFT:
					randomIndex--;
					if (randomIndex < 0) {
						randomIndex += numChoices;
					}
					i = new Intent(mCtx, CardViewActivity.class);
					c.moveToPosition(randomSequence[randomIndex]);
					id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
					i.putExtra("id", id);
					i.putExtra(SearchActivity.RANDOM, isRandom);
					// i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
					startActivityForResult(i, 0);
					break;
				case CardViewActivity.RANDOMRIGHT:
					randomIndex++;
					if (randomIndex >= numChoices) {
						randomIndex -= numChoices;
					}
					i = new Intent(mCtx, CardViewActivity.class);
					c.moveToPosition(randomSequence[randomIndex]);
					id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
					i.putExtra("id", id);
					i.putExtra(SearchActivity.RANDOM, isRandom);
					startActivityForResult(i, 0);
					break;
				case CardViewActivity.SWIPELEFT:
					lastID = data.getLongExtra("lastID", -1l);

					c.moveToFirst();
					while (!c.isAfterLast()) {
						if (lastID == c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID))) {
							c.moveToPrevious();

							// In case the id was matched against the first item.
							if (c.isBeforeFirst())
								c.moveToLast();

							break;
						}
						else
							c.moveToNext();
					}

					i = new Intent(mCtx, CardViewActivity.class);
					id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
					i.putExtra("id", id);
					i.putExtra(SearchActivity.RANDOM, isRandom);
					startActivityForResult(i, 0);
					break;
				case CardViewActivity.SWIPERIGHT:
					lastID = data.getLongExtra("lastID", -1l);

					c.moveToFirst();
					while (!c.isAfterLast()) {
						if (lastID == c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID))) {
							c.moveToNext();

							// In case the id was matched against the last item.
							if (c.isAfterLast())
								c.moveToFirst();

							break;
						}
						else
							c.moveToNext();
					}

					i = new Intent(mCtx, CardViewActivity.class);
					id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
					i.putExtra("id", id);
					i.putExtra(SearchActivity.RANDOM, isRandom);
					startActivityForResult(i, 0);
					break;
				default:
					if (isSingle || isRandom && !randomFromMenu) {
						this.finish();
					}
					if (randomFromMenu) {
						randomFromMenu = false;
					}
					break;
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	private void startRandom() {
		isRandom = true;

		// implements http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
		numChoices = c.getCount();
		Random rand = new Random(System.currentTimeMillis());
		randomSequence = new int[numChoices];
		int temp, k, j;
		for (k = 0; k < numChoices; k++) {
			randomSequence[k] = k;
		}
		for (k = numChoices - 1; k > 0; k--) {
			j = rand.nextInt(k + 1);// j = random integer with 0 <= j <= i
			temp = randomSequence[j];
			randomSequence[j] = randomSequence[k];
			randomSequence[k] = temp;
		}

		Intent i = new Intent(mCtx, CardViewActivity.class);
		c.moveToPosition(randomSequence[randomIndex]);
		long id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
		i.putExtra("id", id);
		i.putExtra(SearchActivity.RANDOM, isRandom);
		startActivityForResult(i, 0);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.search_menu_random_search:
				randomFromMenu = true;
				startRandom();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.result_list_menu, menu);
		return true;
	}
}
