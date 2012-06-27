package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class WidgetSearchActivity extends Activity {

	public EditText						namefield;
	private ImageView					searchButton;
	private ListView					resultList;

	private CardDbAdapter			mDbHelper;

	private Context						mCtx;
	private ResultListAdapter	rla;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.widget_search_activity);

		mCtx = this;

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.openReadable();

		namefield = (EditText) findViewById(R.id.widget_namefield);
		searchButton = (ImageView) findViewById(R.id.search_button);
		resultList = (ListView) findViewById(R.id.result_list);

		searchButton.setOnClickListener(new OnClickListener() {

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
				i.putExtra("IsSingle", true);
				startActivityForResult(i, 0);
			}
		});

		namefield.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() > 0) {
					new AutocompleteQueryTask().execute(s.toString());
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			public void afterTextChanged(Editable s) {

			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		MyApp appState = ((MyApp) getApplicationContext());
		appState.setState(0);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
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

		i.putExtra(SearchActivity.NAME, name);
		i.putExtra(SearchActivity.TEXT, (String) null);
		i.putExtra(SearchActivity.TYPE, (String) null);
		i.putExtra(SearchActivity.COLOR, "wubrgl");
		i.putExtra(SearchActivity.COLORLOGIC, 0);
		i.putExtra(SearchActivity.SET, (String) null);
		i.putExtra(SearchActivity.FORMAT, (String) null);
		i.putExtra(SearchActivity.POW_CHOICE, (float) CardDbAdapter.NOONECARES);
		i.putExtra(SearchActivity.POW_LOGIC, (String) null);
		i.putExtra(SearchActivity.TOU_CHOICE, (float) CardDbAdapter.NOONECARES);
		i.putExtra(SearchActivity.TOU_LOGIC, (String) null);
		i.putExtra(SearchActivity.CMC, -1);
		i.putExtra(SearchActivity.CMC_LOGIC, (String) null);
		i.putExtra(SearchActivity.RARITY, (String) null);
		i.putExtra(SearchActivity.ARTIST, (String) null);
		i.putExtra(SearchActivity.FLAVOR, (String) null);
		i.putExtra(SearchActivity.RANDOM, false);
		// Lines below added by Reuben Kriegel
		i.putExtra(SearchActivity.TYPELOGIC, 0);
		i.putExtra(SearchActivity.TEXTLOGIC, 0);
		// End addition
		startActivityForResult(i, 0);
	}

	private class AutocompleteQueryTask extends AsyncTask<String, Void, Void> {

		private Cursor	c;

		@Override
		protected Void doInBackground(String... params) {
			c = mDbHelper.PrefixSearch(params[0], new String[] { CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME });
			return null;
		}

		@Override
		protected void onPostExecute(Void param) {
			fillData(c);
		}
	}
}