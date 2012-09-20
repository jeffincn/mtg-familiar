package com.gelakinetic.mtgfam.activities;

import java.util.Date;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Instrumentation;
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
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentManager;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.DbUpdaterService;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.MyApp;
import com.gelakinetic.mtgfam.helpers.RoundTimerService;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.OnClosedListener;
import com.slidingmenu.lib.SlidingMenu.OnOpenedListener;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public abstract class FamiliarActivity extends SlidingFragmentActivity {

	protected FamiliarActivity	me;
	public SharedPreferences		preferences;
	public CardDbAdapter				mDbHelper;
	protected Context						mCtx;
	private PackageInfo					pInfo;
	private static final int		ABOUTDIALOG			= 100;
	private static final int		CHANGELOGDIALOG	= 101;
	private static final int		DONATEDIALOG		= 102;

	private static final int		TTS_CHECK_CODE	= 23;

	private Class<?>						pendingClass		= null;
	private int									pendingDialog		= -1;
	public FragmentManager			mFragmentManager;
	private Bundle							mFragResults;

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

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayHomeAsUpEnabled(true);

		getSlidingMenu().setBehindOffsetRes(R.dimen.actionbar_home_width);
		getSlidingMenu().setBehindScrollScale(0.0f);
		getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		setBehindContentView(R.layout.sliding_menu);

		me = this;
		mCtx = this;

		getSupportActionBar().setDisplayShowTitleEnabled(false);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		boolean autoupdate = preferences.getBoolean("autoupdate", true);
		if (autoupdate) {
			// Only update the banning list if it hasn't been updated recently
			long curTime = new Date().getTime();
			int updatefrequency = Integer.valueOf(preferences.getString("updatefrequency", "3"));
			int lastLegalityUpdate = preferences.getInt("lastLegalityUpdate", 0);
			// days to ms
			if (((curTime / 1000) - lastLegalityUpdate) > (updatefrequency * 24 * 60 * 60)) {
				// If we should be updating, check to see if we already are
				MyApp appState = (MyApp) getApplicationContext();
				boolean update;
				synchronized (this) {
					if (!appState.isUpdating()) {
						appState.setUpdating(true);
						update = true;
					}
					else {
						update = false;
					}
				}

				if (update) {
					startService(new Intent(this, DbUpdaterService.class));
				}
			}
		}

		timerHandler = new Handler();
		registerReceiver(endTimeReceiver, new IntentFilter(RoundTimerActivity.RESULT_FILTER));
		registerReceiver(startTimeReceiver, new IntentFilter(RoundTimerService.START_FILTER));
		registerReceiver(cancelTimeReceiver, new IntentFilter(RoundTimerService.CANCEL_FILTER));

		mDbHelper = new CardDbAdapter(this);
		try {
			mDbHelper.openReadable();
			// throw new android.database.sqlite.SQLiteDatabaseLockedException();
		}
		catch (SQLiteException e) {
			String name = e.getClass().getName();
			String parts[] = name.split("\\.");
			String msg = parts[parts.length - 1] + getString(R.string.error_couldnt_open_db_toast);
			Toast.makeText(mCtx, msg, Toast.LENGTH_LONG).show();
			this.finish();
			return;
		}

		updatingDisplay = false;
		timeShowing = false;

		TextView search = (TextView) findViewById(R.id.cardsearch);
		TextView rules = (TextView) findViewById(R.id.rules);
		TextView rng = (TextView) findViewById(R.id.rng);
		TextView manapool = (TextView) findViewById(R.id.manapool);
		TextView randomCard = (TextView) findViewById(R.id.randomCard);
		TextView nbplayerbutton = (TextView) findViewById(R.id.Nplayerlifecounter);
		TextView roundTimer = (TextView) findViewById(R.id.roundTimer);
		TextView trader = (TextView) findViewById(R.id.trade);
		TextView wishlist = (TextView) findViewById(R.id.wishlist);

		TextView checkUpdate = (TextView) findViewById(R.id.checkUpdate);
		TextView preferencesButton = (TextView) findViewById(R.id.preferences);
		TextView donate = (TextView) findViewById(R.id.donate);
		TextView whatsnew = (TextView) findViewById(R.id.whatsnew);
		TextView aboutapp = (TextView) findViewById(R.id.aboutapp);

		getSlidingMenu().setOnClosedListener(new OnClosedListener() {
			@Override
			public void onClosed() {
				if (pendingClass != null) {
					Intent i = new Intent(mCtx, pendingClass);
					startActivity(i);
					pendingClass = null;
				}
				else if (pendingDialog != -1) {
					showDialog(pendingDialog);
					pendingDialog = -1;
				}
			}
		});

		getSlidingMenu().setOnOpenedListener(new OnOpenedListener() {
			@Override
			public void onOpened() {
				// Close the keyboard if the slidingMenu is opened
				hideKeyboard();
			}
		});

		/*
		 * Activity Launchers
		 */
		nbplayerbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(NPlayerLifeActivity.class);
			}
		});

		search.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(SearchActivity.class);
			}
		});

		rules.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(RulesActivity.class);
			}
		});

		rng.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(DiceActivity.class);
			}
		});

		manapool.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(ManaPoolActivity.class);
			}
		});

		randomCard.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(RandomCardActivity.class);
			}
		});

		roundTimer.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(RoundTimerActivity.class);
			}
		});

		trader.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(CardTradingActivity.class);
			}
		});

		wishlist.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(WishlistActivity.class);
			}
		});

		/*
		 * Old Main Page Menu Buttons
		 */
		checkUpdate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Set the last legality update time back to zero on a forced update
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("lastLegalityUpdate", 0);
				editor.commit();
				startService(new Intent(me, DbUpdaterService.class));
				showAbove();
			}
		});
		preferencesButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingActivityLauncher(PreferencesActivity.class);
			}
		});
		donate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingDialogLauncher(DONATEDIALOG);
			}
		});
		whatsnew.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingDialogLauncher(CHANGELOGDIALOG);
			}
		});
		aboutapp.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				slidingDialogLauncher(ABOUTDIALOG);
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

	protected void slidingActivityLauncher(final Class<?> class1) {
		pendingClass = class1;
		showAbove();
	}

	protected void slidingDialogLauncher(final int id) {
		pendingDialog = id;
		showAbove();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
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
		showAbove(); // always close the sliding menu when returning to this
									// activity
		// MyApp appState = ((MyApp) getApplicationContext());
		// String classname = this.getClass().getCanonicalName();
		// if
		// (classname.equalsIgnoreCase("com.gelakinetic.mtgfam.activities.CardViewActivity"))
		// {
		// if (appState.getState() == CardViewFragment.QUITTOSEARCH) {
		// this.finish();
		// return;
		// }
		// }
		// else {
		// appState.setState(0);
		// }

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

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
			case DONATEDIALOG: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Donate to the Devs");
				builder.setNeutralButton(R.string.dialog_thanks, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

				LayoutInflater inflater = getLayoutInflater();
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
				AlertDialog.Builder builder = new AlertDialog.Builder(this);

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

				LayoutInflater inflater = getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.dialog_layout_root));

				TextView text = (TextView) dialoglayout.findViewById(R.id.aboutfield);
				text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.main_about_text)));
				text.setMovementMethod(LinkMovementMethod.getInstance());

				builder.setView(dialoglayout);
				return builder.create();
			}
			case CHANGELOGDIALOG: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);

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
				return null;
			}
		}
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
								new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_SEARCH);
							}
						}).start();
						return true;
					}
				}).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		return true;
	}

	// Displays the timer in the actionbar

	private boolean						updatingDisplay;
	private long							endTime;
	protected Handler					timerHandler;
	private boolean						timeShowing;

	private Runnable					timerUpdate					= new Runnable() {
																									@Override
																									public void run() {
																										displayTimeLeft();

																										if (endTime > SystemClock.elapsedRealtime()) {
																											getSupportActionBar().setDisplayShowTitleEnabled(true);
																											timeShowing = true;
																											timerHandler.postDelayed(timerUpdate, 200);
																										}
																										else {
																											timeShowing = false;
																											getSupportActionBar().setDisplayShowTitleEnabled(false);
																											timerHandler.removeCallbacks(timerUpdate);
																										}
																									}
																								};

	private BroadcastReceiver	endTimeReceiver			= new BroadcastReceiver() {
																									@Override
																									public void onReceive(Context context, Intent intent) {
																										endTime = intent.getLongExtra(RoundTimerService.EXTRA_END_TIME,
																												SystemClock.elapsedRealtime());
																										startUpdatingDisplay();
																										timerHandler.postDelayed(timerUpdate, 200);
																									}
																								};

	private BroadcastReceiver	cancelTimeReceiver	= new BroadcastReceiver() {
																									@Override
																									public void onReceive(Context context, Intent intent) {
																										endTime = 0;
																										stopUpdatingDisplay();
																									}
																								};

	private BroadcastReceiver	startTimeReceiver		= new BroadcastReceiver() {
																									@Override
																									public void onReceive(Context context, Intent intent) {
																										Intent i = new Intent(RoundTimerService.REQUEST_FILTER);
																										sendBroadcast(i);
																									}
																								};

	private void startUpdatingDisplay() {
		updatingDisplay = true;
		displayTimeLeft();
		timerHandler.removeCallbacks(timerUpdate);
		timerHandler.postDelayed(timerUpdate, 200);
	}

	private void stopUpdatingDisplay() {
		updatingDisplay = false;
		timeShowing = false;
		displayTimeLeft();
		timerHandler.removeCallbacks(timerUpdate);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
	}

	private void displayTimeLeft() {
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
}
