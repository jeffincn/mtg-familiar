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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.RoundTimerService;
import com.gelakinetic.mtgfam.helpers.DbUpdaterService;

public class MainActivity extends FamiliarActivity {
	private static final int	ABOUTDIALOG			= 0;
	private static final int	CHANGELOGDIALOG	= 1;
	private static final int	DONATEDIALOG		= 2;
	private static final int	TTS_CHECK_CODE	= 23;
	private PackageInfo				pInfo;
	private TextView					search;
	private TextView					rules;
	private TextView					rng;
	private TextView					manapool;
	private TextView					randomCard;
	private TextView					nbplayerbutton;
	private TextView					roundTimer;
	private TextView					trader;
	private TextView					wishlist;
	private long							defaultLastRulesUpdate;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		mCtx = this;
		search = (TextView) findViewById(R.id.cardsearch);
		rules = (TextView) findViewById(R.id.rules);
		rng = (TextView) findViewById(R.id.rng);
		manapool = (TextView) findViewById(R.id.manapool);
		randomCard = (TextView) findViewById(R.id.randomCard);
		nbplayerbutton = (TextView) findViewById(R.id.Nplayerlifecounter);
		roundTimer = (TextView) findViewById(R.id.roundTimer);
		trader = (TextView) findViewById(R.id.trade);
		wishlist = (TextView) findViewById(R.id.wishlist);

		nbplayerbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, NPlayerLifeActivity.class);
				startActivity(i);
			}
		});

		search.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, SearchActivity.class);
				startActivity(i);
			}
		});

		rules.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, RulesActivity.class);
				startActivity(i);
			}
		});

		rng.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, DiceActivity.class);
				startActivity(i);
			}
		});

		manapool.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, ManaPoolActivity.class);
				startActivity(i);
			}
		});

		randomCard.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, RandomCardActivity.class);
				startActivity(i);
			}
		});

		roundTimer.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, RoundTimerActivity.class);
				startActivity(i);
			}
		});

		trader.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, CardTradingActivity.class);
				startActivity(i);
			}
		});

		wishlist.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(mCtx, WishlistActivity.class);
				startActivity(i);
			}
		});

		if (preferences.getLong("lastRulesUpdate", 0) == 0) {
			SharedPreferences.Editor edit = preferences.edit();
			edit.putLong("lastRulesUpdate", defaultLastRulesUpdate);
			edit.commit();
		}

		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		}
		catch (NameNotFoundException e) {
			pInfo = null;
		}

		int lastVersion = preferences.getInt("lastVersion", 0);
		if (pInfo.versionCode != lastVersion) {
			showDialog(CHANGELOGDIALOG);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt("lastVersion", pInfo.versionCode);
			editor.commit();
		}

		Intent i = new Intent(this, RoundTimerService.class);
		startService(i);

		try {
			Intent tts = new Intent();
			tts.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(tts, TTS_CHECK_CODE);
		}
		catch (ActivityNotFoundException anf) {
			showTtsWarningIfShould();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TTS_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// We have TTS, so flag it as such
				SharedPreferences.Editor edit = preferences.edit();
				edit.putBoolean("hasTts", true);
				edit.commit();
			}
			else {
				showTtsWarningIfShould();
			}
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

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		if (id == DONATEDIALOG) {
			builder.setTitle("Donate to the Devs");
			builder.setNeutralButton(R.string.dialog_thanks, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});

			LayoutInflater inflater = getLayoutInflater();
			View dialoglayout = inflater.inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.dialog_layout_root));

			TextView text = (TextView) dialoglayout.findViewById(R.id.aboutfield);
			text.setText(Html.fromHtml(getString(R.string.main_donate_text)));
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
		}
		else if (id == ABOUTDIALOG) {
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
			text.setText(Html.fromHtml(getString(R.string.main_about_text)));
			text.setMovementMethod(LinkMovementMethod.getInstance());

			builder.setView(dialoglayout);
		}
		else if (id == CHANGELOGDIALOG) {
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

			builder.setMessage(Html.fromHtml(getString(R.string.main_whats_new_text)));
		}
		return builder.create();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			// case R.id.buildWebDB:
			// startThread(DBFROMWEB);
			// return true;
			// case R.id.refreshDB:
			// startThread(DBFROMAPK);
			// return true;

			case R.id.checkUpdate:
				// Set the last legality update time back to zero on a forced update
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("lastLegalityUpdate", 0);
				editor.commit();

                startService(new Intent(this, DbUpdaterService.class));
				//asyncTask = new OTATask();
				//asyncTask.execute((Void[]) null);
				return true;
			case R.id.preferences:
				startActivity(new Intent().setClass(this, PreferencesActivity.class));
				return true;
			case R.id.aboutapp:
				showDialog(ABOUTDIALOG);
				return true;
			case R.id.whatsnew:
				showDialog(CHANGELOGDIALOG);
				return true;
			case R.id.donate:
				showDialog(DONATEDIALOG);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
