package com.gelakinetic.mtgfam.fragments;

import android.app.Instrumentation;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.MyApp;

public class FamiliarFragment extends SherlockFragment {

	public CardDbAdapter								mDbHelper;
	protected FamiliarFragment	anchor;
	public static final String	DIALOG_TAG	= "dialog";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		anchor = this;
		mDbHelper = new CardDbAdapter(this.getMainActivity());
		mDbHelper.openReadable();
		this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		anchor = this;
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		MyApp appState = ((MyApp) getActivity().getApplicationContext());
		String classname = this.getClass().getCanonicalName();
		if (classname.equalsIgnoreCase("com.gelakinetic.mtgfam.fragments.CardViewFragment")) {
			if (appState.getState() == CardViewFragment.QUITTOSEARCH) {
				if (this.getMainActivity().mFragmentManager.getBackStackEntryCount() == 0) {
					getActivity().finish();
				}
				else {
					getMainActivity().mFragmentManager.popBackStack();
				}
				return;
			}
		}
		else {
			appState.setState(0);
		}

		// Clear any results. We don't want them persisting past this fragment, and
		// they should have been looked at by now anyway
		getMainActivity().getFragmentResults();
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

	public MainActivity getMainActivity() {
		return (MainActivity) this.getActivity();
	}

	/*
	 * When the search key is pressed, it will tell the fragment If the fragment
	 * doesn't care what happens, return false Otherwise override this, do
	 * whatever, and return true
	 */
	public boolean onInterceptSearchKey() {
		return false;
	}

	protected void startNewFragment(FamiliarFragment frag, Bundle args) {
		frag.setArguments(args);

		FragmentTransaction fragmentTransaction = this.getMainActivity().mFragmentManager.beginTransaction();
		fragmentTransaction.addToBackStack(null);

		fragmentTransaction.replace(R.id.frag_view, frag);
		fragmentTransaction.commit();
		this.getMainActivity().hideKeyboard();
	}

	void removeDialog() {
		FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.commit();
	}
}
