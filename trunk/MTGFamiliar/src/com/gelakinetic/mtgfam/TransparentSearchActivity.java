package com.gelakinetic.mtgfam;

import java.util.ArrayList;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class TransparentSearchActivity extends Activity {

	public EditText				namefield;
	private ImageButton		searchButton;
	private ListView			resultList;

	private CardDbAdapter	mDbHelper;
	private Cursor				c;

	private Context				mCtx;
	private ResultListAdapter	rla;
	private Cursor	emptyCursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.transparent_search_activity);

		mCtx = this;

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();

		namefield = (EditText) findViewById(R.id.transparent_namefield);
		searchButton = (ImageButton) findViewById(R.id.search_button);
		resultList = (ListView) findViewById(R.id.result_list);

		emptyCursor = mDbHelper.Search("there is no way a card has this name", null, null, "wubrgl", 0, null, CardDbAdapter.NOONECARES,
				null, CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, 0, 0, true, new String[] {
						CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME }, true);
		
		searchButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				doSearch();
			}
		});

		registerForContextMenu(resultList);

		resultList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent i = new Intent(mCtx, CardViewActivity.class);
				i.putExtra("id", id);
				i.putExtra(SearchActivity.RANDOM, false);
				startActivityForResult(i, 0);
			}
		});

		namefield.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() >= 3) {
					c = mDbHelper.Search(s.toString(), null, null, "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
							CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, 0, 0, true, new String[] {
									CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME }, true);
					fillData(c);
				}
				else{
					fillData(emptyCursor);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});

		
	}

	private void fillData(Cursor c) {

		String[] from = { CardDbAdapter.KEY_NAME };
		int[] to = { R.id.cardname };

		rla = new ResultListAdapter(this, R.layout.card_row, c, from, to, this.getResources());
		resultList.setAdapter(rla);
		rla.notifyDataSetChanged();
	}

	private void doSearch() {
		String name = namefield.getText().toString();

		if (name.length() == 0) {
			name = null;
		}

		Intent i = new Intent(mCtx, ResultListActivity.class);
		
//		s.toString(), null, null, "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
//		CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, 0, 0, true, new String[] {
//				CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME }, true
				
		i.putExtra(SearchActivity.NAME, name);
		i.putExtra(SearchActivity.TEXT, (String)null);
		i.putExtra(SearchActivity.TYPE, (String)null);
		i.putExtra(SearchActivity.COLOR, "wubrgl");
		i.putExtra(SearchActivity.COLORLOGIC, 0);
		i.putExtra(SearchActivity.SET, (String)null);
		i.putExtra(SearchActivity.FORMAT, (String)null);
		i.putExtra(SearchActivity.POW_CHOICE, (float)CardDbAdapter.NOONECARES);
		i.putExtra(SearchActivity.POW_LOGIC, (String)null);
		i.putExtra(SearchActivity.TOU_CHOICE, (float)CardDbAdapter.NOONECARES);
		i.putExtra(SearchActivity.TOU_LOGIC, (String)null);
		i.putExtra(SearchActivity.CMC, -1);
		i.putExtra(SearchActivity.CMC_LOGIC, (String)null);
		i.putExtra(SearchActivity.RARITY, (String)null);
		i.putExtra(SearchActivity.ARTIST, (String)null);
		i.putExtra(SearchActivity.FLAVOR, (String)null);
		i.putExtra(SearchActivity.RANDOM, false);
		// Lines below added by Reuben Kriegel
		i.putExtra(SearchActivity.TYPELOGIC, 0);
		i.putExtra(SearchActivity.TEXTLOGIC, 0);
		// End addition
		startActivityForResult(i, 0);
	}
}
