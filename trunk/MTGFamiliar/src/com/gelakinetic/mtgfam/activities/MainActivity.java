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

import java.lang.reflect.Field;
import java.util.Date;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.DiceFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.fragments.JudgesCornerFragment;
import com.gelakinetic.mtgfam.fragments.LifeFragment;
import com.gelakinetic.mtgfam.fragments.ManaPoolFragment;
import com.gelakinetic.mtgfam.fragments.MenuFragment;
import com.gelakinetic.mtgfam.fragments.MoJhoStoFragment;
import com.gelakinetic.mtgfam.fragments.ResultListFragment;
import com.gelakinetic.mtgfam.fragments.RoundTimerFragment;
import com.gelakinetic.mtgfam.fragments.RulesFragment;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment.SearchCriteria;
import com.gelakinetic.mtgfam.fragments.SearchWidgetFragment;
import com.gelakinetic.mtgfam.fragments.TradeFragment;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.DbUpdaterService;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.PreferencesAdapter;
import com.gelakinetic.mtgfam.helpers.PriceFetchService;
import com.gelakinetic.mtgfam.helpers.RoundTimerService;
import com.octo.android.robospice.SpiceManager;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.OnOpenedListener;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class MainActivity extends SlidingFragmentActivity {

	public static String ACTION_WIDGET_SEARCH = "android.intent.action.WIDGET_SEARCH";
	public static String ACTION_MAIN = "android.intent.action.MAIN";
	public static String ACTION_FULL_SEARCH = "android.intent.action.FULL_SEARCH";
	public static String ACTION_ROUND_TIMER = "android.intent.action.ROUND_TIMER";

	public static final int ABOUTDIALOG = 100;
	public static final int CHANGELOGDIALOG = 101;
	public static final int DONATEDIALOG = 102;

	private PackageInfo pInfo;

	protected static MainActivity me;
	private PreferencesAdapter prefAdapter;
	
	public FragmentManager mFragmentManager;
	private Bundle mFragResults;
	private boolean bounceMenu = false;

	/*
	 * Robospice setup
	 */
	private SpiceManager spiceManager = new SpiceManager( PriceFetchService.class );
    
    @Override
    protected void onStart() {
        super.onStart();
        spiceManager.start( this );
    }

    @Override
    protected void onStop() {
        super.onStop();
        spiceManager.shouldStop();
    }

    public SpiceManager getSpiceManager() {
        return spiceManager;
    }
    
    /*
     * End Robospice
     */
	  
	public static final int DEVICE_VERSION   = Build.VERSION.SDK_INT;
	public static final int DEVICE_HONEYCOMB = Build.VERSION_CODES.HONEYCOMB;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (DEVICE_VERSION >= DEVICE_HONEYCOMB) {
			try {
				ViewConfiguration config = ViewConfiguration.get(this);
				Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
				if (menuKeyField != null) {
					menuKeyField.setAccessible(true);
					menuKeyField.setBoolean(config, false);
				}
			} catch (Exception ex) {
				// Ignore
			}
		}
	    
		mFragmentManager = getSupportFragmentManager();

		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		}
		catch (NameNotFoundException e) {
			pInfo = null;
		}
		
		if(prefAdapter == null) {
			prefAdapter = new PreferencesAdapter(this);
		}

		int lastVersion = prefAdapter.getLastVersion();
		if (pInfo.versionCode != lastVersion) {
			// Clear the robospice cache on upgrade. This way, no cached values w/o foil prices will exist
			spiceManager.removeAllDataFromCache();
			showDialogFragment(CHANGELOGDIALOG);
			prefAdapter.setLastVersion(pInfo.versionCode);
			bounceMenu = lastVersion <= 15; //Only bounce if the last version is 1.8.1 or lower (or a fresh install) 
		}

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setIcon(R.drawable.sliding_menu_icon);

		SlidingMenu slidingMenu = getSlidingMenu();
		slidingMenu.setBehindWidthRes(R.dimen.sliding_menu_width);
		slidingMenu.setBehindScrollScale(0.0f);
		slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		slidingMenu.setShadowWidthRes(R.dimen.shadow_width);
		slidingMenu.setShadowDrawable(R.drawable.sliding_menu_shadow);
		setBehindContentView(R.layout.fragment_menu);

		me = this;

		boolean autoupdate = prefAdapter.getAutoUpdate();
		if (autoupdate) {
			// Only update the banning list if it hasn't been updated recently
			long curTime = new Date().getTime();
			int updatefrequency = Integer.valueOf(prefAdapter.getUpdateFrequency());
			int lastLegalityUpdate = prefAdapter.getLastLegalityUpdate();
			// days to ms
			if (((curTime / 1000) - lastLegalityUpdate) > (updatefrequency * 24 * 60 * 60)) {
				startService(new Intent(this, DbUpdaterService.class));
			}
		}

		timerHandler = new Handler();
		registerReceiver(endTimeReceiver, new IntentFilter(RoundTimerFragment.RESULT_FILTER));
		registerReceiver(startTimeReceiver, new IntentFilter(RoundTimerService.START_FILTER));
		registerReceiver(cancelTimeReceiver, new IntentFilter(RoundTimerService.CANCEL_FILTER));

		updatingDisplay = false;
		timeShowing = false;

		getSlidingMenu().setOnOpenedListener(new OnOpenedListener() {

			@Override
			public void onOpened() {
				// Close the keyboard if the slidingMenu is opened
				hideKeyboard();
			}
		});
		
		setContentView(R.layout.fragment_activity);
		getSupportFragmentManager().beginTransaction().replace(R.id.frag_menu, new MenuFragment()).commit();

		if (findViewById(R.id.middle_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mThreePane = true;
		}
		else {
			mThreePane = false;
		}
		
		Intent intent = getIntent();

		if (intent.getAction().equals(Intent.ACTION_VIEW)) {
			// handles a click on a search suggestion; launches activity to show word
			Uri u = intent.getData();
			long id = Long.parseLong(u.getLastPathSegment());

			// add a fragment
			Bundle args = new Bundle();
			args.putBoolean("isSingle", true);
			args.putLong("id", id);
			CardViewFragment rlFrag = new CardViewFragment();
			rlFrag.setArguments(args);

			attachSingleFragment(rlFrag, "left_frag", false, false);
			showOnePane();
			hideKeyboard();
		}
		else if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
			boolean consolidate = prefAdapter.getConsolidateSearch();
			String query = intent.getStringExtra(SearchManager.QUERY);
			SearchCriteria sc = new SearchCriteria();
			sc.Name = query;
			sc.Set_Logic = (consolidate ? CardDbAdapter.FIRSTPRINTING : CardDbAdapter.ALLPRINTINGS);

			// add a fragment
			Bundle args = new Bundle();
			args.putBoolean(SearchViewFragment.RANDOM, false);
			args.putSerializable(SearchViewFragment.CRITERIA, sc);
			if(mThreePane) {
				SearchViewFragment svFrag = new SearchViewFragment();
				svFrag.setArguments(args);
				attachSingleFragment(svFrag, "left_frag", false, false);
			}
			else {
				ResultListFragment rlFrag = new ResultListFragment();
				rlFrag.setArguments(args);
				attachSingleFragment(rlFrag, "left_frag", false, false);
			}
			hideKeyboard();
		}
		else if (intent.getAction().equals(ACTION_FULL_SEARCH)) {
			attachSingleFragment(new SearchViewFragment(), "left_frag", false, false);
			showOnePane();
		}
		else if (intent.getAction().equals(ACTION_WIDGET_SEARCH)) {
			attachSingleFragment(new SearchWidgetFragment(), "left_frag", false, false);
			showOnePane();
		}
		else if (intent.getAction().equals(ACTION_ROUND_TIMER)) {
			attachSingleFragment(new RoundTimerFragment(), "left_frag", false, false);
			showOnePane();
		}
		else {
			if (savedInstanceState == null) {

				String defaultFragment = prefAdapter.getDefaultFragment();

				FamiliarFragment frag;
				if (defaultFragment.equals(this.getString(R.string.main_card_search))) {
					frag = new SearchViewFragment();
				}
				else if (defaultFragment.equals(this.getString(R.string.main_life_counter))) {
					frag = new LifeFragment();
				}
				else if (defaultFragment.equals(this.getString(R.string.main_mana_pool))) {
					frag = new ManaPoolFragment();
				}
				else if (defaultFragment.equals(this.getString(R.string.main_dice))) {
					frag = new DiceFragment();
				}
				else if (defaultFragment.equals(this.getString(R.string.main_trade))) {
					frag = new TradeFragment();
				}
				else if (defaultFragment.equals(this.getString(R.string.main_wishlist))) {
					frag = new WishlistFragment();
				}
				else if (defaultFragment.equals(this.getString(R.string.main_timer))) {
					frag = new RoundTimerFragment();
				}
				else if (defaultFragment.equals(this.getString(R.string.main_rules))) {
					frag = new RulesFragment();
				}
				else if (defaultFragment.equals(this.getString(R.string.main_judges_corner))) {
					frag = new JudgesCornerFragment();
				}
				else if (defaultFragment.equals(this.getString(R.string.main_mojhosto))) {
					frag = new MoJhoStoFragment();
				}
				else {
					frag = new SearchViewFragment();
				}
				attachSingleFragment(frag, "left_frag", false, false);
			}
		}
	}

	/*
	 * From superclass
	 */

	public static final int OPEN = 0;
	public static final int CLOSE = 1;

	private static Handler bounceHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.arg1) {
				case OPEN:
					me.showMenu();
					break;
				case CLOSE:
					me.showContent();
					break;
			}
		}
	};

	public void showDialogFragment(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		this.showContent();
		FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(FamiliarFragment.DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}

		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public void onDismiss(DialogInterface mDialog) {
				super.onDismiss(mDialog);
				if (bounceMenu) {
					getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
					bounceMenu = false;
					Runnable r = new Runnable() {

						@Override
						public void run() {
							long timeStarted = System.currentTimeMillis();
							Message msg = Message.obtain();
							msg.arg1 = OPEN;
							bounceHandler.sendMessage(msg);
							while (System.currentTimeMillis() < (timeStarted + 1500)) {
								;
							}
							msg = Message.obtain();
							msg.arg1 = CLOSE;
							bounceHandler.sendMessage(msg);
							runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);									
								}
							});
						}
					};

					Thread t = new Thread(r);
					t.start();
				}
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				switch (id) {
					case DONATEDIALOG: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
						builder.setTitle(R.string.main_donate_dialog_title);
						builder.setNeutralButton(R.string.dialog_thanks_anyway, new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

						LayoutInflater inflater = this.getActivity().getLayoutInflater();
						View dialoglayout = inflater.inflate(R.layout.about_dialog, (ViewGroup)findViewById(R.id.dialog_layout_root));

						TextView text = (TextView)dialoglayout.findViewById(R.id.aboutfield);
						text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.main_donate_text)));
						text.setMovementMethod(LinkMovementMethod.getInstance());

						text.setTextSize(15);

						ImageView paypal = (ImageView)dialoglayout.findViewById(R.id.imageview1);
						paypal.setImageResource(R.drawable.paypal);
						paypal.setOnClickListener(new View.OnClickListener() {

							public void onClick(View v) {
								Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
										.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=SZK4TAH2XBZNC&lc=US&item_name=MTG%20Familiar&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted"));

								startActivity(myIntent);
							}
						});
						((ImageView)dialoglayout.findViewById(R.id.imageview2)).setVisibility(View.GONE);

						builder.setView(dialoglayout);
						return builder.create();
					}
					case ABOUTDIALOG: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

						// You have to catch the exception because the package stuff is all
						// run-time
						if (pInfo != null) {
							builder.setTitle(getString(R.string.main_about) + " " + getString(R.string.app_name) + " " + pInfo.versionName);
						}
						else {
							builder.setTitle(getString(R.string.main_about) + " " + getString(R.string.app_name));
						}

						builder.setNeutralButton(R.string.dialog_thanks, new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

						LayoutInflater inflater = this.getActivity().getLayoutInflater();
						View dialoglayout = inflater.inflate(R.layout.about_dialog, (ViewGroup)findViewById(R.id.dialog_layout_root));

						TextView text = (TextView)dialoglayout.findViewById(R.id.aboutfield);
						text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.main_about_text)));
						text.setMovementMethod(LinkMovementMethod.getInstance());

						builder.setView(dialoglayout);
						return builder.create();
					}
					case CHANGELOGDIALOG: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

						if (pInfo != null) {
							builder.setTitle(getString(R.string.main_whats_new_in_title) +  " " + pInfo.versionName);
						}
						else {
							builder.setTitle(R.string.main_whats_new_title);
						}

						builder.setNeutralButton(R.string.dialog_enjoy, new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

						builder.setMessage(ImageGetterHelper.jellyBeanHack(getString(R.string.main_whats_new_text)));
						return builder.create();
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(ft, FamiliarFragment.DIALOG_TAG);
	}

	public void hideKeyboard() {
		try {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getCurrentFocus().getApplicationWindowToken(), 0);
		}
		catch (NullPointerException e) {
			// eat it
		}
	}
	
	public void showKeyboard(View v) {
		try {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(v, 0);
		}
		catch (NullPointerException e) {
			// eat it
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unregisterReceiver(endTimeReceiver);
			unregisterReceiver(startTimeReceiver);
			unregisterReceiver(cancelTimeReceiver);
			
			if (endTime > SystemClock.elapsedRealtime()) {
				//Timer Active
			} else {
				Intent i = new Intent(this, RoundTimerService.class);
				stopService(i);
			}
				
			
		}
		catch (IllegalArgumentException e) {
			// EAT IT
		}
	}

	// clear this in every activity. except not cardview
	@Override
	protected void onResume() {
		super.onResume();
		// always close the sliding menu when returning to this activity
		showContent();

		Intent i = new Intent(this, RoundTimerService.class);
		startService(i);

		Intent i2 = new Intent(RoundTimerService.REQUEST_FILTER);
		sendBroadcast(i2);

		if (!updatingDisplay) {
			startUpdatingDisplay();
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				toggle();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();
		updatingDisplay = false;
		timerHandler.removeCallbacks(timerUpdate);
	}

	/*
	 * Round Timer Display
	 */

	public boolean updatingDisplay;
	public long endTime;
	public Handler timerHandler;
	public boolean timeShowing;

	public Runnable timerUpdate = new Runnable() {

		@Override
		public void run() {
			if (endTime > SystemClock.elapsedRealtime()) {
				displayTimeLeft();
				timerHandler.postDelayed(timerUpdate, 200);
			}
			else {
				stopUpdatingDisplay();
			}
		}
	};

	public BroadcastReceiver endTimeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = intent.getLongExtra(RoundTimerService.EXTRA_END_TIME, SystemClock.elapsedRealtime());
			startUpdatingDisplay();
		}
	};

	public BroadcastReceiver cancelTimeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = 0;
			stopUpdatingDisplay();
		}
	};

	public BroadcastReceiver startTimeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Intent i = new Intent(RoundTimerService.REQUEST_FILTER);
			sendBroadcast(i);
		}
	};
	public boolean mThreePane;

	public void startUpdatingDisplay() {
		updatingDisplay = true;
		timeShowing = true;
		timerHandler.removeCallbacks(timerUpdate);
		timerHandler.postDelayed(timerUpdate, 1);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
	}

	public void stopUpdatingDisplay() {
		updatingDisplay = false;
		timeShowing = false;
		timerHandler.removeCallbacks(timerUpdate);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
	}

	public void displayTimeLeft() {
		long timeLeftMillis = endTime - SystemClock.elapsedRealtime();
		String timeLeftStr = "";

		if (timeLeftMillis <= 0) {
			timeLeftStr = "00:00:00";
		}
		else {
			long timeLeftInSecs = (timeLeftMillis / 1000);

			// This is a slight hack to handle the fact that it always rounds down. It
			// makes the clock look much nicer this way.
			timeLeftInSecs++;

			String hours = String.valueOf(timeLeftInSecs / (3600));
			String minutes = String.valueOf((timeLeftInSecs % 3600) / 60);
			String seconds = String.valueOf(timeLeftInSecs % 60);

			if (hours.length() == 1) {
				timeLeftStr += "0";
			}
			timeLeftStr += hours + ":";

			if (minutes.length() == 1) {
				timeLeftStr += "0";
			}
			timeLeftStr += minutes + ":";

			if (seconds.length() == 1) {
				timeLeftStr += "0";
			}
			timeLeftStr += seconds;
		}

		if (timeShowing) {
			getSupportActionBar().setTitle(timeLeftStr);
		}
	}

	/*
	 * Random crap
	 */

	public void showTtsWarningIfShould() {
		boolean shouldShow = prefAdapter.getTtsShowDialog();

		if (shouldShow) {
			// So we don't display this dialog again and bother the user
			prefAdapter.setTtsShowDialog(false);

			// Then display a dialog informing them of TTS
			AlertDialog dialog = new Builder(this).setTitle(R.string.main_tts_warning_title).setMessage(R.string.main_tts_warning_text)
					.setPositiveButton(R.string.dialog_ok, new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							// Do nothing, just dismiss
						}
					}).create();

			dialog.show();
		}
	}

	public void setFragmentResult(Bundle result) {
		mFragResults = result;
	}

	public Bundle getFragmentResults() {
		if (mFragResults != null) {
			Bundle res = mFragResults;
			mFragResults = null;
			return res;
		}
		return null;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		/*
		 * This is for ForceOverflow
		 */
		if (DEVICE_VERSION < DEVICE_HONEYCOMB) {
			if (event.getAction() == KeyEvent.ACTION_UP
					&& keyCode == KeyEvent.KEYCODE_MENU) {
				openOptionsMenu();
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			// Send the search key to the leftmost fragment
			Fragment f = mFragmentManager.findFragmentById(R.id.left_container);
			if (((FamiliarFragment)f).onInterceptSearchKey() == false) {
				return super.onKeyDown(keyCode, event);
			}
			else {
				return true;
			}
		}
		else if (keyCode == KeyEvent.KEYCODE_BACK) {
			// If we're not at the root of a hierarchy, the back button should do as it pleases
			if(getSupportFragmentManager().getBackStackEntryCount() > 0 || !this.isTaskRoot()) {
				return super.onKeyDown(keyCode, event);
			}
			// Else if were at the root, and the SlidingMenu is closed, it should open the menu
			else if(!this.getSlidingMenu().isMenuShowing()) {
				this.getSlidingMenu().showMenu();
				return true;
			}
			// If the SlidingMenu is open, it should close the app
			else {
				return super.onKeyDown(keyCode, event);				
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public PreferencesAdapter getPreferencesAdapter() {
		// On rotations, this could get called from a fragment's onCreateView before the activity's onCreate. Weird
		if(this.prefAdapter == null) {
			this.prefAdapter = new PreferencesAdapter(this);
		}
		return this.prefAdapter;
	}

	public void showDbErrorToast() {
		try {
			Toast.makeText(this, getString(R.string.error_database), Toast.LENGTH_LONG).show();
		} 
		catch (RuntimeException re) {
			// Eat it; this will happen if we try to toast in a non-UI thread.
			// It can happen when we get an error in autocomplete.
		}
	}
	
	/********************************
	 *                              *
	 *    Three Pane Management     *
	 *                              *
	 ********************************/
	
	/**
	 * 
	 * @param containerId The resource id of the container view
	 * @param frag The fragment to be added
	 * @param tag Optional tag name for the fragment, to later retrieve the fragment with FragmentManager.findFragmentByTag(String)
	 * @param addToBackStack Add this transaction to the back stack. This means that the transaction will be remembered after it is committed, and will reverse its operation when later popped off the stack.
	 * @param replace Set this to true to replace the fragment in the container, or false to add the fragment to the container
	 */
	private void attachFragment(int containerId, FamiliarFragment frag, String tag, boolean addToBackStack, boolean replace) {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if(replace) {
			ft.replace(containerId, frag, tag);
		}
		else {
			ft.add(containerId, frag, tag);
		}
		if (addToBackStack) {
			ft.addToBackStack(null);
		}
		ft.commit();
	}
	
	/**
	 * Attach a single fragment. This should be used even on tablets when only one fragment is showing
	 * @param frag The fragment to attach
	 * @param tag Optional tag name for the fragment, to later retrieve the fragment with FragmentManager.findFragmentByTag(String)
	 * @param addToBackStack Add this transaction to the back stack. This means that the transaction will be remembered after it is committed, and will reverse its operation when later popped off the stack.
	 * @param replace Set this to true to replace the fragment in the container, or false to add the fragment to the container
	 */
	public void attachSingleFragment(FamiliarFragment frag, String tag, boolean addToBackStack, boolean replace) {
		attachFragment(R.id.left_container, frag, tag, addToBackStack, replace);
	}
	
	/**
	 * Attach a fragment to the leftmost container. It will be added, not replaced.
	 * This usually isn't called, as attachSingleFragment() does pretty much the same thing
	 * @param frag The fragment to attach
	 * @param tag Optional tag name for the fragment, to later retrieve the fragment with FragmentManager.findFragmentByTag(String)
	 * @param addToBackStack Add this transaction to the back stack. This means that the transaction will be remembered after it is committed, and will reverse its operation when later popped off the stack.
	 */
	public void attachLeftFragment(FamiliarFragment frag, String tag, boolean addToBackStack) {
		if (!mThreePane) {
			return;
		}
		attachFragment(R.id.left_container, frag, tag, addToBackStack, false);
	}

	/**
	 * Attach a fragment to the middle container. It will be added, not replaced.
	 * @param frag The fragment to attach
	 * @param tag Optional tag name for the fragment, to later retrieve the fragment with FragmentManager.findFragmentByTag(String)
	 * @param addToBackStack Add this transaction to the back stack. This means that the transaction will be remembered after it is committed, and will reverse its operation when later popped off the stack.
	 */
	public void attachMiddleFragment(FamiliarFragment frag, String tag, boolean addToBackStack) {
		if (!mThreePane) {
			return;
		}
		attachFragment(R.id.middle_container, frag, tag, addToBackStack, false);
	}

	/**
	 * Attach a fragment to the rightmost container. It will be added, not replaced.
	 * @param frag The fragment to attach
	 * @param tag Optional tag name for the fragment, to later retrieve the fragment with FragmentManager.findFragmentByTag(String)
	 * @param addToBackStack Add this transaction to the back stack. This means that the transaction will be remembered after it is committed, and will reverse its operation when later popped off the stack.
	 */
	public void attachRightFragment(FamiliarFragment frag, String tag, boolean addToBackStack) {
		if (!mThreePane) {
			return;
		}
		attachFragment(R.id.right_container, frag, tag, addToBackStack, false);
	}

	/**
	 * Send a message to the fragment in the leftmost container
	 * @param bundle The message
	 */
	public void sendMessageToLeftFragment(Bundle bundle) {
		if (!mThreePane) {
			return;
		}
		((FamiliarFragment) getSupportFragmentManager().findFragmentById(R.id.left_container)).receiveMessage(bundle);
	}

	/**
	 * Send a message to the fragment in the middle container
	 * @param bundle The message
	 */
	public void sendMessageToMiddleFragment(Bundle bundle) {
		if (!mThreePane) {
			return;
		}
		((FamiliarFragment) getSupportFragmentManager().findFragmentById(R.id.middle_container)).receiveMessage(bundle);
	}

	/**
	 * Send a message to the fragment in the rightmost container
	 * @param bundle The message
	 */
	public void sendMessageToRightFragment(Bundle bundle) {
		if (!mThreePane) {
			return;
		}
		((FamiliarFragment) getSupportFragmentManager().findFragmentById(R.id.right_container)).receiveMessage(bundle);
	}
	
	/**
	 * Show all three panes and dividers. Middle and right panes should be populated after this
	 */
	public void showThreePanes() {
		if(!mThreePane) {
			return;
		}
		findViewById(R.id.middle_container).setVisibility(View.VISIBLE);
		findViewById(R.id.right_container).setVisibility(View.VISIBLE);
		findViewById(R.id.firstDivider).setVisibility(View.VISIBLE);
		findViewById(R.id.secondDivider).setVisibility(View.VISIBLE);

	}
	
	/**
	 * Remove the right and middle fragments, if they exist, and set the container and divider visibilities to View.GONE
	 */
	public void showOnePane() {
		if(!mThreePane){
			return;
		}
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

		Fragment middle = getSupportFragmentManager().findFragmentById(R.id.middle_container);
		if(middle != null) {
			ft.remove(middle);
		}
		
		Fragment right = getSupportFragmentManager().findFragmentById(R.id.right_container);
		if(right != null) {
			ft.remove(right);
		}

		ft.commit();
		
		findViewById(R.id.middle_container).setVisibility(View.GONE);
		findViewById(R.id.right_container).setVisibility(View.GONE);
		findViewById(R.id.firstDivider).setVisibility(View.GONE);
		findViewById(R.id.secondDivider).setVisibility(View.GONE);
	}
}
