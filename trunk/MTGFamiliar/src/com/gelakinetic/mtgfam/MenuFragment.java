package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.SearchView;

/**
 * A fragment that displays a menu. This fragment happens to not have a UI (it
 * does not implement onCreateView), but it could also have one if it wanted.
 */
public class MenuFragment extends Fragment {

	private static Activity	mActivity;
	private static int			mRes;

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
				catch (NoSuchMethodError e) {
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
}