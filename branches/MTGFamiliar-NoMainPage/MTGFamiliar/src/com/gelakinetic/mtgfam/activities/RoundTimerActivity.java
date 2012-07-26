/**
Copyright 2012 Alex Levine

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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings.System;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.RoundTimerService;

public class RoundTimerActivity extends FamiliarActivity {

	public static String RESULT_FILTER = "com.gelakinetic.mtgfam.RESULT_FILTER";
	public static String TTS_FILTER = "com.gelakinetic.mtgfam.TTS_FILTER";
	public static String EXTRA_END_TIME = "EndTime";

	private static int RINGTONE_REQUEST_CODE = 17;

	private static int DIALOG_SET_WARNINGS = 0;

	private Handler timerHandler = new Handler();
	private Runnable updateTimeViewTask = new Runnable() {
		public void run() {
			displayTimeLeft();

			if (endTime > SystemClock.elapsedRealtime()) {
				actionButton.setText(R.string.timer_cancel);
				timerHandler.postDelayed(updateTimeViewTask, 100);
			}
			else {
				actionButton.setText(R.string.timer_start);
			}
		}
	};

	private TimePicker picker;
	private Button actionButton;
	private TextView timeView;

	private long endTime;
	private boolean updatingDisplay;
	private boolean ttsInitialized = false;

	private BroadcastReceiver resultReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = intent.getLongExtra(RoundTimerService.EXTRA_END_TIME,
					SystemClock.elapsedRealtime());
			startUpdatingDisplay();
		}
	};

	private BroadcastReceiver ttsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			ttsInitialized = intent.getBooleanExtra(
					RoundTimerService.EXTRA_TTS_INITIALIZED, false);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.round_timer_activity);
		// this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		registerReceiver(resultReceiver, new IntentFilter(RESULT_FILTER));
		registerReceiver(ttsReceiver, new IntentFilter(TTS_FILTER));

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		this.picker = (TimePicker) findViewById(R.id.rt_time_picker);
		picker.setIs24HourView(true);

		int length;
		try {
			length = Integer.parseInt(preferences.getString("roundLength", "50"));
		}
		catch (Exception ex) {
			// Eat the exception; this should never happen in practice, and if it does
			// we just want to
			// default to 50 and pretend nothing broke
			length = 50;
		}
		picker.setCurrentHour(length / 60);
		picker.setCurrentMinute(length % 60);

		this.actionButton = (Button) findViewById(R.id.rt_action_button);
		actionButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startCancelClick(v);
			}
		});

		this.timeView = (TextView) findViewById(R.id.rt_time_display);

		Intent i = new Intent(RoundTimerService.REQUEST_FILTER);
		sendBroadcast(i);
		updatingDisplay = true; // So onResume() doesn't start the updates before we
														// get the response broadcast

		if (preferences.getBoolean("hasTts", false)) {
			i = new Intent(RoundTimerService.TTS_INITIALIZED_FILTER); // Find out if
																																// TTS is
																																// initialized
			sendBroadcast(i);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!updatingDisplay) {
			startUpdatingDisplay();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		updatingDisplay = false; // So we resume the updates when we resume the
															// activity
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(resultReceiver);
		unregisterReceiver(ttsReceiver);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.set_timer_warnings).setVisible(ttsInitialized);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.set_timer_ringtone:
				Uri soundFile = Uri.parse(preferences.getString("timerSound",
						RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()));

				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alert Tone");
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundFile);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, System.DEFAULT_NOTIFICATION_URI);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);

				startActivityForResult(intent, RINGTONE_REQUEST_CODE);
				return true;
			case R.id.set_timer_warnings:
				showDialog(DIALOG_SET_WARNINGS);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		if (id == DIALOG_SET_WARNINGS) {
			final View v = View.inflate(this, R.layout.timer_warning_dialog, null);
			final CheckBox chkFifteen = (CheckBox) v.findViewById(R.id.timer_pref_fifteen);
			final CheckBox chkTen = (CheckBox) v.findViewById(R.id.timer_pref_ten);
			final CheckBox chkFive = (CheckBox) v.findViewById(R.id.timer_pref_five);

			dialog = new AlertDialog.Builder(this).setView(v).setTitle(R.string.timer_warning_dialog_title)
					.setPositiveButton("OK", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RoundTimerActivity.this)
									.edit();
							edit.putBoolean("fifteenMinutePref", chkFifteen.isChecked());
							edit.putBoolean("tenMinutePref", chkTen.isChecked());
							edit.putBoolean("fiveMinutePref", chkFive.isChecked());
							edit.commit();
						}
					}).create();
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == DIALOG_SET_WARNINGS) {
			boolean fifteen = preferences.getBoolean("fifteenMinutePref", false);
			boolean ten = preferences.getBoolean("tenMinutePref", false);
			boolean five = preferences.getBoolean("fiveMinutePref", false);

			((CheckBox) dialog.findViewById(R.id.timer_pref_fifteen)).setChecked(fifteen);
			((CheckBox) dialog.findViewById(R.id.timer_pref_ten)).setChecked(ten);
			((CheckBox) dialog.findViewById(R.id.timer_pref_five)).setChecked(five);
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == RINGTONE_REQUEST_CODE) {
				Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

				if (uri != null) {
					// Save it
					SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
					edit.putString("timerSound", uri.toString());
					edit.commit();
				}
			}
		}
	}

	public void startCancelClick(View view) {
		if (endTime > SystemClock.elapsedRealtime()) {
			// We're running, so this command is cancel
			endTime = SystemClock.elapsedRealtime();

			Intent i = new Intent(RoundTimerService.CANCEL_FILTER);
			sendBroadcast(i);

			actionButton.setText(R.string.timer_start);
		}
		else {
			// We're not running, so this command is start
			picker.clearFocus(); // This forces the inner value to update, in case the
														// user typed it in manually
			int hours = picker.getCurrentHour();
			int minutes = picker.getCurrentMinute();

			long timeInMillis = ((hours * 3600) + (minutes * 60)) * 1000;
			if (timeInMillis == 0) {
				return;
			}

			endTime = SystemClock.elapsedRealtime() + timeInMillis;

			Intent i = new Intent(RoundTimerService.START_FILTER);
			i.putExtra(EXTRA_END_TIME, endTime);
			sendBroadcast(i);

			startUpdatingDisplay();
			actionButton.setText(R.string.timer_cancel);
		}
	}

	private void startUpdatingDisplay() {
		updatingDisplay = true;
		displayTimeLeft();
		timerHandler.removeCallbacks(updateTimeViewTask);
		timerHandler.postDelayed(updateTimeViewTask, 100);
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

		timeView.setText(timeLeftStr);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.timer_menu, menu);
		return true;
	}
}
