package com.gelakinetic.mtgfam.fragments;

import android.app.Instrumentation;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.FamiliarActivity;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.MyApp;

public class FamiliarFragment extends SherlockFragment {

	CardDbAdapter								mDbHelper;
	protected FamiliarFragment	anchor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		anchor = this;
		mDbHelper = new CardDbAdapter(this.getFamiliarActivity());
		mDbHelper.openReadable();
		this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		anchor = this;
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onResume(){
		super.onResume();
		MyApp appState = ((MyApp) getActivity().getApplicationContext());
		String classname = this.getClass().getCanonicalName();
		if (classname.equalsIgnoreCase("com.gelakinetic.mtgfam.fragments.CardViewFragment")) {
			if (appState.getState() == CardViewFragment.QUITTOSEARCH) {
				if(this.getFamiliarActivity().mFragmentManager.getBackStackEntryCount() == 0) {
					getActivity().finish();
				}
				else {
					getFamiliarActivity().mFragmentManager.popBackStack();
				}
				return;
			}
		}
		else {
			appState.setState(0);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		menu.clear();
		menu.add(R.string.name_search_hint).setIcon(R.drawable.menu_search)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_SEARCH);
							}
						}).start();
						return true;
					}
				}).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	public FamiliarActivity getFamiliarActivity() {
		return (FamiliarActivity) this.getActivity();
	}
}