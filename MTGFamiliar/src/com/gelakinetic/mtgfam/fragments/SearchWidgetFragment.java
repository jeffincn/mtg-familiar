package com.gelakinetic.mtgfam.fragments;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.ResultListAdapter;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class SearchWidgetFragment extends FamiliarFragment {

	public EditText						namefield;
	private ImageView					searchButton;
	private ListView					resultList;

	private ResultListAdapter	rla;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View myFragmentView = inflater.inflate(R.layout.widget_search_frag, container, false);

		namefield = (EditText) myFragmentView.findViewById(R.id.widget_namefield);
		searchButton = (ImageView) myFragmentView.findViewById(R.id.search_button);
		resultList = (ListView) myFragmentView.findViewById(R.id.result_list);

		searchButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				doSearch();
			}
		});

		registerForContextMenu(resultList);

		resultList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// add a fragment
				Bundle args = new Bundle();
				args.putBoolean(SearchViewFragment.RANDOM, false);
				args.putSerializable("id", id);
				ResultListFragment rlFrag = new ResultListFragment();
				rlFrag.setArguments(args);

				FragmentTransaction fragmentTransaction = anchor.getFamiliarActivity().mFragmentManager.beginTransaction();
				fragmentTransaction.addToBackStack(null);

				fragmentTransaction.replace(R.id.frag_view, rlFrag);
				fragmentTransaction.commit();
				anchor.getFamiliarActivity().hideKeyboard();
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

		return myFragmentView;
	}

	private void fillData(Cursor c) {

		String[] from = { CardDbAdapter.KEY_NAME };
		int[] to = { R.id.cardname };

		rla = new ResultListAdapter(this.getActivity(), R.layout.card_row, c, from, to, this.getResources());
		resultList.setAdapter(rla);
		rla.notifyDataSetChanged();
	}

	private void doSearch() {
		String name = namefield.getText().toString();

		if (name.length() == 0) {
			name = null;
		}

		SearchCriteria criteria = new SearchCriteria();
		criteria.Name = name;
		// add a fragment
		Bundle args = new Bundle();
		args.putBoolean(SearchViewFragment.RANDOM, false);
		args.putSerializable(SearchViewFragment.CRITERIA, criteria);
		ResultListFragment rlFrag = new ResultListFragment();
		rlFrag.setArguments(args);

		FragmentTransaction fragmentTransaction = this.getFamiliarActivity().mFragmentManager.beginTransaction();
		fragmentTransaction.addToBackStack(null);

		fragmentTransaction.replace(R.id.frag_view, rlFrag);
		fragmentTransaction.commit();
		anchor.getFamiliarActivity().hideKeyboard();
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		// MenuInflater inflater = new MenuInflater(this);
		// inflater.inflate(R.menu.main_menu, menu);
	}
}
