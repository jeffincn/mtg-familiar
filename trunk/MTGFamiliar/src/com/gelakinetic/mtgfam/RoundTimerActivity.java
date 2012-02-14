package com.gelakinetic.mtgfam;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.gelakinetic.mtgfam.RoundTimerService.RoundTimerBinder;

public class RoundTimerActivity extends FragmentActivity {
	private Handler timerHandler = new Handler();
	private Runnable updateTimeViewTask = new Runnable() 
	{
		public void run()
		{
			if(bound) //To avoid potential null pointer exceptions
			{				
				timeView.setText(tService.getTimeLeftDisplay());
				
				if(tService.isRunning() && !tService.isPaused())
				{
					timerHandler.postDelayed(updateTimeViewTask, 100);
					actionButton.setText(R.string.pause_timer);
				}
				else if(!tService.isRunning())
				{
					displaySelectedTime();
				}
			}
		}
	};
	
	private RoundTimerService tService;
	private boolean bound = false;
	
	private ServiceConnection connection = new ServiceConnection() 
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			RoundTimerBinder binder = (RoundTimerBinder)service;
			if(binder != null)
			{
				tService = binder.getService();
				bound = true;
				if(tService.isRunning())
				{
					startUpdatingDisplay();
				}
				else
				{
					displaySelectedTime();
				}
			}
		}
		
		public void onServiceDisconnected(ComponentName className)
		{
			bound = false;
		}
	};

	private TimePicker picker;
	private Button actionButton;
	private Button resetButton;
	private TextView timeView;
	private Fragment	mFragment1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.round_timer_activity);
		//this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		mFragment1 = (MenuFragment) fm.findFragmentByTag("f1");
		if (mFragment1 == null) {
			try{
				mFragment1 = new MenuFragment(this, R.menu.timer_menu);
			}
			catch(VerifyError e){
				mFragment1 = new MenuFragmentCompat(R.menu.timer_menu);
			}
			ft.add(mFragment1, "f1");
		}
		ft.commit();
		
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
				startPauseClick(v);
			}
		});
		
		this.resetButton = (Button)findViewById(R.id.rt_reset_button);
		resetButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetClick(v);
			}
		});
		
		this.timeView = (TextView)findViewById(R.id.rt_time_display);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();

		Intent i = new Intent(this, RoundTimerService.class);
		bindService(i, connection, BIND_AUTO_CREATE);
	}
	
	public void startPauseClick(View view)
	{
		if(bound)
		{
			if(!tService.isRunning())
			{
				if(setTimer())
				{
					actionButton.setText(R.string.pause_timer);
				}
			}
			else
			{
				if(tService.isPaused())
				{
					if(resume())
					{
						actionButton.setText(R.string.pause_timer);
					}
				}
				else
				{
					if(pause())
					{
						actionButton.setText(R.string.resume_timer);
					}
				}
			}
		}
		else
		{
			Toast.makeText(this, "Unable to perform action: the timer service is not properly bound.", Toast.LENGTH_LONG).show();
		}
	}
	
	public void resetClick(View view)
	{
		if(bound)
		{
			if(tService.isRunning())
			{
				reset();
				actionButton.setText(R.string.start_timer);
			}
			displaySelectedTime();
		}
	}
	
	private boolean setTimer()
	{
		//Hide the soft keyboard if it's showing
		InputMethodManager manager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		manager.hideSoftInputFromWindow(picker.getWindowToken(), 0);
		
		//Get the duration
		picker.clearFocus(); //This forces the inner value to update, in case the user typed it in manually
		int hours = picker.getCurrentHour();
		int minutes = picker.getCurrentMinute();
		
		//Input is all set, so set the timer
		long timeInMillis = ((hours * 3600) + (minutes * 60)) * 1000;
		if(timeInMillis == 0)
		{
			return false;
		}
		
		if(!tService.setTimer(timeInMillis))
		{
			Toast.makeText(this, "Timer could not be set: it is already running.", Toast.LENGTH_LONG).show();
			return false;
		}
		
		startUpdatingDisplay();
		return true;
	}
	
	private boolean pause()
	{
		if(!tService.pauseTimer())
		{
			Toast.makeText(this, "Timer could not be paused.", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	private boolean resume()
	{
		if(!tService.resumeTimer())
		{
			Toast.makeText(this, "Timer could not be resumed.", Toast.LENGTH_LONG).show();
			return false;
		}
		
		startUpdatingDisplay();
		return true;
	}
	
	private void reset()
	{
		tService.reset();
	}
	
	private void startUpdatingDisplay()
	{
		timeView.setText(tService.getTimeLeftDisplay());
		timerHandler.removeCallbacks(updateTimeViewTask);
		timerHandler.postDelayed(updateTimeViewTask, 100);
	}
	
	private void displaySelectedTime()
	{
		String selectedTime = "";
		picker.clearFocus(); //This forces the inner value to update, in case the user typed it in manually
		int hour = picker.getCurrentHour();
		int minute = picker.getCurrentMinute();
		
		if(hour < 10)
		{
			selectedTime += "0";
		}
		selectedTime += String.valueOf(hour);
		selectedTime += ":";
		
		if(minute < 10)
		{
			selectedTime += "0";
		}
		selectedTime += String.valueOf(minute);
		selectedTime += ":00";
		
		timeView.setText(selectedTime);
	}
}
