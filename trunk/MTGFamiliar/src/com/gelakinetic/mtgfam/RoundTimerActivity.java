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

package com.gelakinetic.mtgfam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

public class RoundTimerActivity extends FragmentActivity {
	
	public static String RESULT_FILTER = "com.gelakinetic.mtgfam.RESULT_FILTER";
	public static String EXTRA_END_TIME = "EndTime";
	
	private Handler timerHandler = new Handler();
	private Runnable updateTimeViewTask = new Runnable() 
	{
		public void run()
		{	
			displayTimeLeft();
			
			if(endTime > SystemClock.elapsedRealtime())
			{
				actionButton.setText(R.string.cancel_timer);
				timerHandler.postDelayed(updateTimeViewTask, 100);
			}
			else
			{
				actionButton.setText(R.string.start_timer);
			}
		}
	};

	private TimePicker picker;
	private Button actionButton;
	private TextView timeView;
	
	private long endTime;
	private boolean updatingDisplay;
	
	private BroadcastReceiver resultReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = intent.getLongExtra(RoundTimerService.EXTRA_END_TIME, SystemClock.elapsedRealtime());
			startUpdatingDisplay();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.round_timer_activity);
		//this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		registerReceiver(resultReceiver, new IntentFilter(RESULT_FILTER));
		
		MenuFragmentCompat.init(this, R.menu.timer_menu, "round_timer_menu_fragment");
		
		this.picker = (TimePicker)findViewById(R.id.rt_time_picker);
		picker.setIs24HourView(true);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(RoundTimerActivity.this);
		int length;
		try	{
			length = Integer.parseInt(settings.getString("roundLength", "50"));
		}
		catch(Exception ex)	{
			//Eat the exception; this should never happen in practice, and if it does we just want to
			//default to 50 and pretend nothing broke
			length = 50;
		}
		picker.setCurrentHour(length / 60);
		picker.setCurrentMinute(length % 60);
		
		this.actionButton = (Button)findViewById(R.id.rt_action_button);
		actionButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startCancelClick(v);
			}
		});
		
		this.timeView = (TextView)findViewById(R.id.rt_time_display);
		
		Intent i = new Intent(RoundTimerService.REQUEST_FILTER);
		sendBroadcast(i);
		updatingDisplay = true; //So onResume() doesn't start the updates before we get the response broadcast
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		
		MyApp appState = ((MyApp)getApplicationContext());
		appState.setState(0);
		
		if(!updatingDisplay)
		{
			startUpdatingDisplay();
		}
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		updatingDisplay = false; //So we resume the updates when we resume the activity
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(resultReceiver);
	}
	
	public void startCancelClick(View view)
	{
		if(endTime > SystemClock.elapsedRealtime())
		{
			//We're running, so this command is cancel
			endTime = SystemClock.elapsedRealtime();
			
			Intent i = new Intent(RoundTimerService.CANCEL_FILTER);
			sendBroadcast(i);
			
			actionButton.setText(R.string.start_timer);
		}
		else
		{
			//We're not running, so this command is start
			picker.clearFocus(); //This forces the inner value to update, in case the user typed it in manually
			int hours = picker.getCurrentHour();
			int minutes = picker.getCurrentMinute();

			long timeInMillis = ((hours * 3600) + (minutes * 60)) * 1000;
			if(timeInMillis == 0)
			{
				return;
			}
			
			endTime = SystemClock.elapsedRealtime() + timeInMillis;
			
			Intent i = new Intent(RoundTimerService.START_FILTER);
			i.putExtra(EXTRA_END_TIME, endTime);
			sendBroadcast(i);
			
			startUpdatingDisplay();
			actionButton.setText(R.string.cancel_timer);
		}
	}
	
	private void startUpdatingDisplay()
	{
		updatingDisplay = true;
		displayTimeLeft();
		timerHandler.removeCallbacks(updateTimeViewTask);
		timerHandler.postDelayed(updateTimeViewTask, 100);
	}
	
	private void displayTimeLeft()
	{
		long timeLeftMillis = endTime - SystemClock.elapsedRealtime();
		String timeLeftStr = "";
		
		if(timeLeftMillis <= 0)
		{
			timeLeftStr = "00:00:00"; 
		}
		else
		{
			long timeLeftInSecs = (timeLeftMillis / 1000);
			
			//This is a slight hack to handle the fact that it always rounds down. It makes the clock look much nicer this way.
			timeLeftInSecs++;
			
			String hours = String.valueOf(timeLeftInSecs / (3600));
			String minutes = String.valueOf((timeLeftInSecs % 3600) / 60);
			String seconds = String.valueOf(timeLeftInSecs % 60);
			
			if(hours.length() == 1)
			{
				timeLeftStr += "0";
			}
			timeLeftStr += hours + ":";
			
			if(minutes.length() == 1)
			{
				timeLeftStr += "0";
			}
			timeLeftStr += minutes + ":";
			
			if(seconds.length() == 1)
			{
				timeLeftStr += "0";
			}
			timeLeftStr += seconds;
		}
		
		timeView.setText(timeLeftStr);
	}
}
