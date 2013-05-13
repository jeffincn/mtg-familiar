package com.gelakinetic.mtgfam.fragments;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.InFragmentMenuLoader;
import com.gelakinetic.mtgfam.helpers.ResultListAdapter;

public class SearchWidgetFragment extends FamiliarFragment {

	public EditText						namefield;
	private ImageView					searchButton;
	private ListView					resultList;

	private ResultListAdapter	rla;

	public SearchWidgetFragment() {
		/* http://developer.android.com/reference/android/app/Fragment.html
		 * All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View myFragmentView = inflater.inflate(R.layout.widget_search_frag, container, false);

		if(getMainActivity().mThreePane) {
			getMainActivity().showThreePanes();
			getMainActivity().attachMiddleFragment(new ResultListFragment(), "result_list", false);
			getMainActivity().attachRightFragment(new CardViewFragment(), "card_view", false);
		}
		
		masterLayout = (LinearLayout)myFragmentView.findViewById(R.id.master_layout);
		
		namefield = (EditText) myFragmentView.findViewById(R.id.widget_namefield);
		setKeyboardFocus(savedInstanceState, namefield, false);
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
				if(getMainActivity().mThreePane) {
					getMainActivity().sendMessageToRightFragment(args, true);
				}
				else {
					CardViewFragment cvFrag = new CardViewFragment();
					startNewFragment(cvFrag, args);
				}
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
		criteria.Set_Logic = CardDbAdapter.MOSTRECENTPRINTING;
		// add a fragment
		Bundle args = new Bundle();
		args.putBoolean(SearchViewFragment.RANDOM, false);
		args.putSerializable(SearchViewFragment.CRITERIA, criteria);
		if(getMainActivity().mThreePane) {
			getMainActivity().sendMessageToMiddleFragment(args, true);
		}
		else{
			ResultListFragment rlFrag = new ResultListFragment();
			startNewFragment(rlFrag, args);
		}
	}

	private class AutocompleteQueryTask extends AsyncTask<String, Void, Void> {

		private Cursor	c;

		@Override
		protected Void doInBackground(String... params) {
			if(getActivity() == null) {
				// happens when the fragment is on the backstack, and being cleared
				return null;
			}

			try {
				c = mDbHelper.PrefixSearch(params[0], new String[] { CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME });
			} catch (FamiliarDbException e) {
				c = null;
			} catch (IllegalStateException e) {
				c = null;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void param) {
			if(getActivity() == null) {
				// happens when the fragment is on the backstack, and being cleared
				return;
			}

			if(c != null) {
				fillData(c);
			}
			else {
				getMainActivity().showDbErrorToast();
				getMainActivity().getSupportFragmentManager().popBackStack();
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		// MenuInflater inflater = new MenuInflater(this);
		// inflater.inflate(R.menu.main_menu, menu);
		if(getMainActivity().mThreePane) {
			InFragmentMenuLoader cml = new InFragmentMenuLoader(this);
			cml.inflate(R.menu.search_widget_menu, menu);
			mFragmentMenu = cml.getView();
			addFragmentMenu();
		}
		else {
			inflater.inflate(R.menu.search_widget_menu, menu);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.search_menu_clear:
				namefield.setText("");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
