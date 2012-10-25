package com.gelakinetic.mtgfam.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings.System;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.RoundTimerService;

public class RoundTimerFragment extends FamiliarFragment {

	public static final String RESULT_FILTER = "com.gelakinetic.mtgfam.RESULT_FILTER";
	public static final String TTS_FILTER = "com.gelakinetic.mtgfam.TTS_FILTER";
	public static final String EXTRA_END_TIME = "EndTime";

	private static final String SAVED_HOURS = "SavedHours";
	private static final String SAVED_MINUTES = "SavedMinutes";

	private static final int RINGTONE_REQUEST_CODE = 17;

	private static final int DIALOG_SET_WARNINGS = 1;

	private TimePicker picker;
	private Button actionButton;

	private int hours;
	private int minutes;
	private boolean firstLoad;

	private long endTime;
	private boolean ttsInitialized = false;

	private BroadcastReceiver resultReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = intent.getLongExtra(RoundTimerService.EXTRA_END_TIME, SystemClock.elapsedRealtime());
		}
	};

	private BroadcastReceiver ttsReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			ttsInitialized = intent.getBooleanExtra(RoundTimerService.EXTRA_TTS_INITIALIZED, false);
			if (timerWarning != null) {
				timerWarning.setVisible(ttsInitialized);
			}
			if (!ttsInitialized) {
				getMainActivity().showTtsWarningIfShould();
			}
		}
	};

	private Runnable updateButtonTextTask = new Runnable() {

		public void run() {
			if (endTime > SystemClock.elapsedRealtime()) {
				actionButton.setText(R.string.timer_cancel);
			}
			else {
				actionButton.setText(R.string.timer_start);
			}
			getMainActivity().timerHandler.postDelayed(updateButtonTextTask, 200);
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View myFragmentView = inflater.inflate(R.layout.round_timer_activity, container, false);

		getActivity().registerReceiver(resultReceiver, new IntentFilter(RESULT_FILTER));
		getActivity().registerReceiver(ttsReceiver, new IntentFilter(TTS_FILTER));

		getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

		this.picker = (TimePicker)myFragmentView.findViewById(R.id.rt_time_picker);
		this.picker.setIs24HourView(true);

		if (savedInstanceState != null) {
			this.firstLoad = false;
			this.hours = savedInstanceState.getInt(SAVED_HOURS);
			this.minutes = savedInstanceState.getInt(SAVED_MINUTES);
		}
		else {
			this.firstLoad = true;
		}

		this.actionButton = (Button)myFragmentView.findViewById(R.id.rt_action_button);
		actionButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				startCancelClick(v);
			}
		});

		Intent i = new Intent(RoundTimerService.REQUEST_FILTER);
		getActivity().sendBroadcast(i);

		// Find out if TTS is initialized
		i = new Intent(RoundTimerService.TTS_INITIALIZED_FILTER);
		getActivity().sendBroadcast(i);

		getMainActivity().timerHandler.postDelayed(updateButtonTextTask, 200);
		return myFragmentView;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (firstLoad) {
			try {
				int length = Integer.parseInt(getMainActivity().getPreferencesAdapter().getRoundLength());
				this.hours = length / 60;
				this.minutes = length % 60;
			}
			catch (Exception ex) {
				// Eat the exception; this should never happen in practice, and if it does we just want to default to 50
				// and pretend nothing broke
				this.hours = 0;
				this.minutes = 50;
			}
			this.firstLoad = false;
		}

		this.picker.setCurrentHour(this.hours);
		this.picker.setCurrentMinute(this.minutes);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(resultReceiver);
		getActivity().unregisterReceiver(ttsReceiver);
		getMainActivity().timerHandler.removeCallbacks(updateButtonTextTask);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		this.hours = this.picker.getCurrentHour();
		this.minutes = this.picker.getCurrentMinute();
		outState.putInt(SAVED_HOURS, this.picker.getCurrentHour());
		outState.putInt(SAVED_MINUTES, this.picker.getCurrentMinute());
	}

	MenuItem timerWarning = null;

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		timerWarning = menu.findItem(R.id.set_timer_warnings);
		timerWarning.setVisible(ttsInitialized);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.set_timer_ringtone:
				Uri soundFile = Uri.parse(getMainActivity().getPreferencesAdapter().getTimerSound());

				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.timer_tone_dialog_title));
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

	protected void showDialog(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}

		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				switch (id) {
					case DIALOG_SET_WARNINGS: {
						final View v = View.inflate(this.getActivity(), R.layout.timer_warning_dialog, null);
						final CheckBox chkFifteen = (CheckBox)v.findViewById(R.id.timer_pref_fifteen);
						final CheckBox chkTen = (CheckBox)v.findViewById(R.id.timer_pref_ten);
						final CheckBox chkFive = (CheckBox)v.findViewById(R.id.timer_pref_five);

						boolean fifteen = getMainActivity().getPreferencesAdapter().getFifteenMinutePref();
						boolean ten = getMainActivity().getPreferencesAdapter().getTenMinutePref();
						boolean five = getMainActivity().getPreferencesAdapter().getFiveMinutePref();

						chkFifteen.setChecked(fifteen);
						chkTen.setChecked(ten);
						chkFive.setChecked(five);

						return new AlertDialog.Builder(getActivity()).setView(v).setTitle(R.string.timer_warning_dialog_title)
								.setPositiveButton("OK", new OnClickListener() {

									public void onClick(DialogInterface dialog, int which) {
										getMainActivity().getPreferencesAdapter().setFifteenMinutePref(chkFifteen.isChecked());
										getMainActivity().getPreferencesAdapter().setTenMinutePref(chkTen.isChecked());
										getMainActivity().getPreferencesAdapter().setFiveMinutePref(chkFive.isChecked());
									}
								}).create();
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(ft, DIALOG_TAG);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == RINGTONE_REQUEST_CODE) {
				Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

				if (uri != null) {
					// Save it
					getMainActivity().getPreferencesAdapter().setTimerSound(uri.toString());
				}
			}
		}
	}

	public void startCancelClick(View view) {
		if (endTime > SystemClock.elapsedRealtime()) {
			// We're running, so this command is cancel
			endTime = SystemClock.elapsedRealtime();

			Intent i = new Intent(RoundTimerService.CANCEL_FILTER);
			getActivity().sendBroadcast(i);

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
			getActivity().sendBroadcast(i);

			actionButton.setText(R.string.timer_cancel);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.timer_menu, menu);
	}
}
