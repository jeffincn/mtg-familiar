package com.gelakinetic.mtgfam;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * A fragment that displays a menu. This fragment happens to not have a UI (it
 * does not implement onCreateView), but it could also have one if it wanted.
 */
public class MenuFragmentCompat extends Fragment {

	private static int			mRes;

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
}