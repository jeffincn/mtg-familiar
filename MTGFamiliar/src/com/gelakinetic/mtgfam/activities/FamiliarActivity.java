package com.gelakinetic.mtgfam.activities;

import android.app.Instrumentation;
import android.view.KeyEvent;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.MyApp;

public abstract class FamiliarActivity extends SherlockActivity {

	/*
	 * Always add a virtual search key to the menu on the actionbar
	 * super.onCreateOptionsMenu should always be called from FamiliarActivities
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(R.string.search_hint).setIcon(R.drawable.menu_search)
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

		return true;
	}

	// clear this in every activity. except not cardview
	@Override
	protected void onResume() {
		super.onResume();
		MyApp appState = ((MyApp) getApplicationContext());
		String classname = this.getClass().getCanonicalName();
		if (classname.equalsIgnoreCase("com.gelakinetic.mtgfam.activities.CardViewActivity")) {
			if (appState.getState() == CardViewActivity.QUITTOSEARCH) {
				this.finish();
				return;
			}
		}
		else {
			appState.setState(0);
		}
	}
}
