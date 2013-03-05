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

import android.app.Instrumentation;
import android.os.Bundle;
import android.view.KeyEvent;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.gelakinetic.mtgfam.R;

public class PreferencesActivity extends SherlockPreferenceActivity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
	}
	
	/*
	 * Always add a virtual search key to the menu on the actionbar
	 * super.onCreateOptionsMenu should always be called from FamiliarActivities
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.name_search_hint).setIcon(R.drawable.menu_search)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								try{
									new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_SEARCH);
								}
								catch(java.lang.SecurityException e){
									//apparently this can inject an event into another app if the user switches fast enough
								}
							}
						}).start();
						return true;
					}
				}).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return true;
	}

}
