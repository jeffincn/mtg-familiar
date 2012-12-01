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

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.SearchView;

/**
 * ACHTUNG! The SearchView comes from API11. It's the only way I can see to get
 * the search in the Action Bar. Be careful using it on preAPI11 devices.
 * Thank goodness for try/catch blocks
 */

/**
 * A fragment that displays a menu. This fragment happens to not have a UI (it
 * does not implement onCreateView), but it could also have one if it wanted.
 */
public class MenuFragment extends Fragment {

	private Activity	mActivity;
	private int			mRes;

	public MenuFragment(Activity a, int menuRes) {
		super();
		mRes = menuRes;
		mActivity = a;
	}

	public MenuFragment() {
		super();
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(mRes, menu);

		for (int i = 0; i < menu.size(); i++) {
			if (i == 0) {
				try {
					if(Build.VERSION.SDK_INT >= 11){ // honeycomb or greater
						// Get the SearchView and set the searchable configuration
						SearchManager searchManager = (SearchManager) mActivity.getSystemService(Context.SEARCH_SERVICE);
						SearchView searchView = (SearchView) menu.getItem(i).getActionView();
						SearchableInfo si = searchManager.getSearchableInfo(mActivity.getComponentName());
						if(si == null){
							// this activity is not searchable. just throw the exception to hide the icon
							throw new NoSuchMethodError();
						}
						searchView.setSearchableInfo(si);
						searchView.setIconifiedByDefault(true);
						MenuItemCompat.setShowAsAction(menu.getItem(i), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
					}
					else{
						menu.getItem(0).setVisible(false);
					}
				}
				catch (NoSuchMethodError e) {
					// this must be a pre honeycomb platform, without the action bar. hide the menu option then!
					menu.getItem(0).setVisible(false);
				}
				catch (VerifyError e) {
					// this must be a pre honeycomb platform, without the action bar. hide the menu option then!
					menu.getItem(0).setVisible(false);
				}
			}
			else {

				MenuItemCompat.setShowAsAction(menu.getItem(i), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
						| MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
			}
		}
	}

	public void setVaribles(Activity a, int res) {
		// TODO Auto-generated method stub
		mActivity = a;
		mRes = res;
	}
}