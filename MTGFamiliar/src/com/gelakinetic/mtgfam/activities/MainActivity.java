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

import java.util.Date;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Instrumentation;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.CardViewFragment;
import com.gelakinetic.mtgfam.fragments.DiceFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarDialogFragment;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
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
import com.gelakinetic.mtgfam.helpers.RoundTimerService;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.OnOpenedListener;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class MainActivity extends SlidingFragmentActivity {

	public static String			ACTION_WIDGET_SEARCH	= "android.intent.action.WIDGET_SEARCH";
	public static String			ACTION_MAIN						= "android.intent.action.MAIN";
	public static String			ACTION_FULL_SEARCH		= "android.intent.action.FULL_SEARCH";
	public static String			ACTION_ROUND_TIMER		= "android.intent.action.ROUND_TIMER";

	public static final int			ABOUTDIALOG						= 100;
	public static final int			CHANGELOGDIALOG				= 101;
	public static final int			DONATEDIALOG					= 102;

	private PackageInfo				pInfo;

	protected static MainActivity		me;
	public SharedPreferences	preferences;

	private static final int	TTS_CHECK_CODE				= 23;

	public FragmentManager		mFragmentManager;
	private Bundle						mFragResults;
	private boolean	firstRun = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFragmentManager = getSupportFragmentManager();

		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		}
		catch (NameNotFoundException e) {
			pInfo = null;
		}

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		int lastVersion = preferences.getInt("lastVersion", 0);
		if (pInfo.versionCode != lastVersion) {
			showDialogFragment(CHANGELOGDIALOG);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt("lastVersion", pInfo.versionCode);
			editor.commit();
			firstRun  = true;
		}

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);

//		getSlidingMenu().setBehindOffsetRes(R.dimen.actionbar_home_width);
		getSlidingMenu().setBehindWidthRes(R.dimen.sliding_menu_width);
		getSlidingMenu().setBehindScrollScale(0.0f);
		getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		getSlidingMenu().setShadowWidthRes(R.dimen.shadow_width);
		getSlidingMenu().setShadowDrawable(R.drawable.sliding_menu_shadow);
		setBehindContentView(R.layout.fragment_menu);
		getSupportFragmentManager().beginTransaction()
		.add(R.id.frag_menu, new MenuFragment())
		.commit();

		me = this;

		boolean autoupdate = preferences.getBoolean("autoupdate", true);
		if (autoupdate) {
			// Only update the banning list if it hasn't been updated recently
			long curTime = new Date().getTime();
			int updatefrequency = Integer.valueOf(preferences.getString("updatefrequency", "3"));
			int lastLegalityUpdate = preferences.getInt("lastLegalityUpdate", 0);
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

		try {
			Intent tts = new Intent();
			tts.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(tts, TTS_CHECK_CODE);
		}
		catch (ActivityNotFoundException anf) {
			showTtsWarningIfShould();
		}

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
				String action = getIntent().getAction();
				// TODO a preference should toggle what fragment is loaded
				
				String defaultFragment = preferences.getString("defaultFragment", getString(R.string.main_card_search));
				
				Fragment frag;
				if(defaultFragment.equals(this.getString(R.string.main_card_search))) {
					frag = new SearchViewFragment();
				}
				else if(defaultFragment.equals(this.getString(R.string.main_life_counter))) {
					frag = new LifeFragment();
				}
				else if(defaultFragment.equals(this.getString(R.string.main_mana_pool))) {
					frag = new ManaPoolFragment();
				}
				else if(defaultFragment.equals(this.getString(R.string.main_dice))) {
					frag = new DiceFragment();
				}
				else if(defaultFragment.equals(this.getString(R.string.main_trade))) {
					frag = new TradeFragment();
				}
				else if(defaultFragment.equals(this.getString(R.string.main_wishlist))) {
					frag = new WishlistFragment();
				}
				else if(defaultFragment.equals(this.getString(R.string.main_timer))) {
					frag = new RoundTimerFragment();
				}
				else if(defaultFragment.equals(this.getString(R.string.main_rules))) {
					frag = new RulesFragment();
				}
				else if(defaultFragment.equals(this.getString(R.string.main_mojhosto))) {
					frag = new MoJhoStoFragment();
				}
				else {
					frag = new SearchViewFragment();
				}
				
				mFragmentManager = getSupportFragmentManager();
				FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

				if (action != null) {
					if (action.equals(ACTION_FULL_SEARCH)) {
						frag = new SearchViewFragment();
					}
					else if (action.equals(ACTION_WIDGET_SEARCH)) {
						frag = new SearchWidgetFragment();
					}
					else if (action.equals(ACTION_ROUND_TIMER)) {
						frag = new RoundTimerFragment();
					}
				}

				fragmentTransaction.add(R.id.frag_view, frag);
				fragmentTransaction.commit();
			}
		}
	}

	/*
	 * From superclass
	 */

	public static final int OPEN = 0;
	public static final int CLOSE = 1;

	private static Handler	bounceHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			switch(msg.arg1) {
			case OPEN:
				me.showBehind();
				break;
			case CLOSE:
				me.showAbove();
				break;
			}
		}
	};

	public void showDialogFragment(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		this.showAbove();
		FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(FamiliarFragment.DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}

		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public void onDismiss(DialogInterface mDialog){
				super.onDismiss(mDialog);
				if(firstRun) {
					firstRun = false;
					Runnable r = new Runnable(){

						@Override
						public void run() {
							long timeStarted = System.currentTimeMillis();
							Message msg = Message.obtain();
							msg.arg1 = OPEN;
							bounceHandler.sendMessage(msg);
							while(System.currentTimeMillis() < (timeStarted + 1500)) {;}
							msg = Message.obtain();
							msg.arg1 = CLOSE;
							bounceHandler.sendMessage(msg);
						}};

						Thread t = new Thread(r);
						t.start();
				}
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				switch (id) {
				case DONATEDIALOG: {
					AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
					builder.setTitle("Donate to the Devs");
					builder.setNeutralButton(R.string.dialog_thanks, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

					LayoutInflater inflater = this.getActivity().getLayoutInflater();
					View dialoglayout = inflater.inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.dialog_layout_root));

					TextView text = (TextView) dialoglayout.findViewById(R.id.aboutfield);
					text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.main_donate_text)));
					text.setMovementMethod(LinkMovementMethod.getInstance());

					text.setTextSize(15);

					ImageView paypal = (ImageView) dialoglayout.findViewById(R.id.imageview1);
					paypal.setImageResource(R.drawable.paypal);
					paypal.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri
									.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=ALR4TSXWPPHUL"));

							startActivity(myIntent);
						}
					});
					((ImageView) dialoglayout.findViewById(R.id.imageview2)).setVisibility(View.GONE);

					builder.setView(dialoglayout);
					return builder.create();
				}
				case ABOUTDIALOG: {
					AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

					// You have to catch the exception because the package stuff is all
					// run-time
					if (pInfo != null) {
						builder.setTitle("About " + getString(R.string.app_name) + " " + pInfo.versionName);
					}
					else {
						builder.setTitle("About " + getString(R.string.app_name));
					}

					builder.setNeutralButton("Thanks!", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

					LayoutInflater inflater = this.getActivity().getLayoutInflater();
					View dialoglayout = inflater.inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.dialog_layout_root));

					TextView text = (TextView) dialoglayout.findViewById(R.id.aboutfield);
					text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.main_about_text)));
					text.setMovementMethod(LinkMovementMethod.getInstance());

					builder.setView(dialoglayout);
					return builder.create();
				}
				case CHANGELOGDIALOG: {
					AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

					if (pInfo != null) {
						builder.setTitle("What's New in Version " + pInfo.versionName);
					}
					else {
						builder.setTitle("What's New");
					}

					builder.setNeutralButton("Enjoy!", new DialogInterface.OnClickListener() {
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

	protected void replaceFragment(Fragment frag) {
		FragmentTransaction fragmentTransaction = this.getSupportFragmentManager().beginTransaction();
		fragmentTransaction.replace(R.id.frag_view, frag);
		fragmentTransaction.commit();
		this.hideKeyboard();
		showAbove();
	}

	public void hideKeyboard() {
		try {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getCurrentFocus().getApplicationWindowToken(), 0);
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
		showAbove();

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

	/*
	 * Round Timer Display
	 */

	public boolean						updatingDisplay;
	public long								endTime;
	public Handler						timerHandler;
	public boolean						timeShowing;

	public Runnable						timerUpdate					= new Runnable() {
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

	public BroadcastReceiver	endTimeReceiver			= new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = intent.getLongExtra(RoundTimerService.EXTRA_END_TIME,
					SystemClock.elapsedRealtime());
			startUpdatingDisplay();
		}
	};

	public BroadcastReceiver	cancelTimeReceiver	= new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = 0;
			stopUpdatingDisplay();
		}
	};

	public BroadcastReceiver	startTimeReceiver		= new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Intent i = new Intent(RoundTimerService.REQUEST_FILTER);
			sendBroadcast(i);
		}
	};

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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case TTS_CHECK_CODE:
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// We have TTS, so flag it as such
				SharedPreferences.Editor edit = preferences.edit();
				edit.putBoolean("hasTts", true);
				edit.commit();
			}
			else {
				showTtsWarningIfShould();
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

	private void showTtsWarningIfShould() {
		SharedPreferences.Editor edit = preferences.edit();
		boolean shouldShow = preferences.getBoolean("ttsShowDialog", true);

		if (shouldShow) {
			// So we don't display this dialog again and bother the user
			edit.putBoolean("ttsShowDialog", false);

			// Then display a dialog informing them of TTS
			AlertDialog dialog = new Builder(this)
			.setTitle("Text-to-Speech")
			.setMessage(
					"This application has text-to-speech capability for some of its features, but you don't "
							+ "seem to have it installed. If you want to install it, use the \"Install Text-to-Speech\" link "
							+ "in the settings menu.").setPositiveButton("OK", new OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									// Do nothing, just dismiss
								}
							}).create();

			dialog.show();
		}

		// Also, even if we aren't showing the dialog, set a boolean indicating
		// that we don't have TTS
		edit.putBoolean("has_tts", false);
		edit.commit();
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			Fragment f = mFragmentManager.findFragmentById(R.id.frag_view);
			if (((FamiliarFragment) f).onInterceptSearchKey() == false) {
				return super.onKeyDown(keyCode, event);
			}
			else {
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}
