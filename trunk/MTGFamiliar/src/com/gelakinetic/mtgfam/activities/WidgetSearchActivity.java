package com.gelakinetic.mtgfam.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.SearchActivity.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.ResultListAdapter;

public class WidgetSearchActivity extends FamiliarActivity {

	public EditText						namefield;
	private ImageView					searchButton;
	private ListView					resultList;

	private ResultListAdapter	rla;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.widget_search_activity);

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
		SearchCriteria criteria = new SearchCriteria();
		criteria.Name = name;
		
		i.putExtra(SearchActivity.CRITERIA,criteria);
		i.putExtra(SearchActivity.RANDOM, false);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// MenuInflater inflater = new MenuInflater(this);
		// inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
}