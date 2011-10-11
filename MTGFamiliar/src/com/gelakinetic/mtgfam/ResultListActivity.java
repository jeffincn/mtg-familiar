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

package com.gelakinetic.mtgfam;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ResultListActivity extends ListActivity {

	static final int			NO_RESULT	= 1;
	private CardDbAdapter	mDbHelper;
	private ListView			lv;
	private Context				mCtx;
	private Cursor				c;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.result_list_activity);

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();

		mCtx = this;

		Bundle extras = getIntent().getExtras();
		extras.getString(SearchActivity.POW_CHOICE);

		c = mDbHelper.Search(this.getApplicationContext(), extras.getString(CardDbAdapter.KEY_NAME),
				extras.getString(SearchActivity.TEXT), extras.getString(SearchActivity.TYPE),
				extras.getString(SearchActivity.COLOR), extras.getInt(SearchActivity.COLORLOGIC),
				extras.getString(SearchActivity.SET), extras.getFloat(SearchActivity.POW_CHOICE),
				extras.getString(SearchActivity.POW_LOGIC), extras.getFloat(SearchActivity.TOU_CHOICE),
				extras.getString(SearchActivity.TOU_LOGIC), extras.getInt(SearchActivity.CMC),
				extras.getString(SearchActivity.CMC_LOGIC), extras.getString(SearchActivity.FORMAT),
				extras.getString(SearchActivity.RARITY), extras.getString(SearchActivity.FLAVOR),
				extras.getString(SearchActivity.ARTIST));

		if (c.getCount() == 0) {
			Intent i = new Intent();
			setResult(NO_RESULT, i);
			finish();
		}
		fillData(c);

		registerForContextMenu(getListView());

		lv = getListView();

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent i = new Intent(mCtx, CardViewActivity.class);
				i.putExtra("id", id);
				startActivityForResult(i, 0);
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (c != null) {
			c.deactivate();
			c.close();
		}
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	private void fillData(Cursor c) {

		// Create an array to specify the fields we want to display in the list
		// (only TITLE)
		String[] from = new String[] { CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_SET, CardDbAdapter.KEY_MANACOST };

		// and an array of the fields we want to bind those fields to (in this case
		// just text1)
		int[] to = new int[] { R.id.cardname, R.id.cardset, R.id.cardcost };
		
		ResultListAdapter rla = new ResultListAdapter(this, R.layout.card_row, c, from, to, this.getResources());
		setListAdapter(rla);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			if (resultCode == CardViewActivity.TRANSFORM) {
				String number = data.getStringExtra(CardViewActivity.NUMBER);
				String set = data.getStringExtra(CardViewActivity.SET);
				if (number.contains("a")) {
					number = number.replace("a", "b");
				}
				else if (number.contains("b")) {
					number = number.replace("b", "a");
				}

				long id = mDbHelper.getTransform(set, number);
				if (id != -1) {
					Intent i = new Intent(mCtx, CardViewActivity.class);
					i.putExtra("id", id);
					// Froyo+ only, disabled animations
					// i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
					startActivityForResult(i, 0);
				}
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}
