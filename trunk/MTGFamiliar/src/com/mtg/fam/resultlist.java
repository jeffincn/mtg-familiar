package com.mtg.fam;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class resultlist extends ListActivity {

	private CardDbAdapter	mDbHelper;
	private ListView			lv;
	private Context				mCtx;
	private Cursor				c;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.resultlist);

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();

		mCtx = this;

		Bundle extras = getIntent().getExtras();
		extras.getString(search.POW_CHOICE);
		
		c = mDbHelper.Search(this.getApplicationContext(), extras.getString(CardDbAdapter.KEY_NAME),
				extras.getString(search.TEXT), extras.getString(search.TYPE), extras.getString(search.COLOR),
				extras.getInt(search.COLORLOGIC), extras.getString(search.SET), extras.getFloat(search.POW_CHOICE),
				extras.getString(search.POW_LOGIC), extras.getFloat(search.TOU_CHOICE), extras.getString(search.TOU_LOGIC),
				extras.getInt(search.CMC), extras.getString(search.CMC_LOGIC), extras.getString(search.FORMAT),
				extras.getString(search.RARITY), extras.getString(search.FLAVOR), extras.getString(search.ARTIST));
		fillData(c);

		registerForContextMenu(getListView());

		lv = getListView();

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent i = new Intent(mCtx, cardview.class);
				i.putExtra("id", id);
				startActivity(i);
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
		String[] from = new String[] { CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_SET };

		// and an array of the fields we want to bind those fields to (in this case
		// just text1)
		int[] to = new int[] { R.id.cardname, R.id.cardset };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter notes = new SimpleCursorAdapter(this, R.layout.card_row, c, from, to);
		setListAdapter(notes);
	}
}
