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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * A fragment that displays a menu. This fragment happens to not have a UI (it
 * does not implement onCreateView), but it could also have one if it wanted.
 */
public class MenuFragmentCompat extends Fragment {

	private int			mRes;

	public MenuFragmentCompat(int menuRes) {
		super();
		mRes = menuRes;
	}

	public MenuFragmentCompat() {
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
		menu.getItem(0).setVisible(false);

		for (int i = 1; i < menu.size(); i++) {
			MenuItemCompat.setShowAsAction(menu.getItem(i), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
					| MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
		}
	}

	public void setVaribles(int res) {
		mRes = res;
	}
	
	public static void init(FragmentActivity fa, int resID, String tag){

		FragmentManager fm = fa.getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment mFragment1;
		try{
			mFragment1 = (MenuFragment) fm.findFragmentByTag(tag);
		}
		catch(ClassCastException e){
			mFragment1 = (MenuFragmentCompat) fm.findFragmentByTag(tag);
		}
		if (mFragment1 == null) {
			try{
				mFragment1 = new MenuFragment(fa, resID);
			}
			catch(VerifyError e){
				mFragment1 = new MenuFragmentCompat(resID);
			}
			ft.add(mFragment1, tag);
		}
		else{
			try{
				((MenuFragment)mFragment1).setVaribles(fa, resID);
			}
			catch(ClassCastException e){
				((MenuFragmentCompat)mFragment1).setVaribles(resID);				
			}
			catch(VerifyError e){
				((MenuFragmentCompat)mFragment1).setVaribles(resID);
			}
		}
		ft.commit();
	
	}
}