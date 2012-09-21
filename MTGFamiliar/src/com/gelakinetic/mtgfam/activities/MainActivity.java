/**
Copyright 2011 Adam Feinstein and April King.

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

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.fragments.RulesFragment;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;

public class MainActivity extends FamiliarActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_activity);

		Intent intent = getIntent();

		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			// handles a click on a search suggestion; launches activity to show word
			Uri u = intent.getData();
			long id = Long.parseLong(u.getLastPathSegment());

			// add a fragment
			Bundle args = new Bundle();
			args.putBoolean("isSingle", true);
			args.putLong("id", id);
			CardViewFragment rlFrag = new CardViewFragment();
			rlFrag.setArguments(args);

			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
			fragmentTransaction.add(R.id.frag_view, rlFrag);
			fragmentTransaction.commit();
			hideKeyboard();
		}
		else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			boolean consolidate = preferences.getBoolean("consolidateSearch", true);
			String query = intent.getStringExtra(SearchManager.QUERY);
			SearchCriteria sc = new SearchCriteria();
			sc.Name = query;
			sc.Set_Logic = (consolidate ? CardDbAdapter.FIRSTPRINTING : CardDbAdapter.ALLPRINTINGS);

			// add a fragment
			Bundle args = new Bundle();
			args.putBoolean(SearchViewFragment.RANDOM, false);
			args.putSerializable(SearchViewFragment.CRITERIA, sc);
			ResultListFragment rlFrag = new ResultListFragment();
			rlFrag.setArguments(args);

			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
			fragmentTransaction.add(R.id.frag_view, rlFrag);
			fragmentTransaction.commit();
			hideKeyboard();
		}
		else {
			if (savedInstanceState == null) {
				mFragmentManager = getSupportFragmentManager();
				FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

				// TODO a preference should toggle what fragment is loaded
				//SearchViewFragment svFrag = new SearchViewFragment();
				RulesFragment svFrag = new RulesFragment();
				fragmentTransaction.add(R.id.frag_view, svFrag);
				fragmentTransaction.commit();
			}
		}
	}
}
