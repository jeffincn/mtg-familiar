package com.gelakinetic.mtgfam;

import java.util.Calendar;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.gelakinetic.mtgfam.RoundTimerService.RoundTimerBinder;

public class RoundTimerActivity extends Activity {
	private Handler timerHandler = new Handler();
	private Runnable updateTimeViewTask = new Runnable() 
	{
		public void run()
		{
			if(bound) //To avoid potential null pointer exceptions
			{				
				timeView.setText(tService.getTimeLeftDisplay());
				
				if(tService.isRunning())
				{
					timerHandler.postDelayed(updateTimeViewTask, 100);
					actionButton.setText(R.string.cancel_timer);
				}
				else
				{
					actionButton.setText(R.string.set_timer);
				}
			}
		}
	};
	
	private RoundTimerService tService;
	private boolean bound = false;
	
	private ServiceConnection connection = new ServiceConnection() 
	{
		@Override
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
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName className)
		{
			bound = false;
		}
	};

	private TimePicker picker;
	private Button actionButton;
	private TextView timeView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.round_timer_activity);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		this.picker = (TimePicker)findViewById(R.id.rt_time_picker);
		picker.setIs24HourView(true);
		picker.setCurrentHour(0);
		picker.setCurrentMinute(0);
		
		this.actionButton = (Button)findViewById(R.id.rt_action_button);
		
		this.timeView = (TextView)findViewById(R.id.rt_time_display);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();

		Intent i = new Intent(this, RoundTimerService.class);
		bindService(i, connection, BIND_AUTO_CREATE);
	}
	
	public void handleClick(View view)
	{
		if(bound)
		{
			if(!tService.isRunning())
			{
				if(setTimer())
				{
					actionButton.setText(R.string.cancel_timer);
				}
			}
			else
			{
				if(cancel())
				{
					actionButton.setText(R.string.set_timer);
				}
			}
		}
		else
		{
			Toast.makeText(this, "Unable to perform action: the timer service is not properly bound.", Toast.LENGTH_LONG).show();
		}
	}
	
	private boolean setTimer()
	{		
		//Get the duration
		int hours = picker.getCurrentHour();
		int minutes = picker.getCurrentMinute();
		
		//Input is all set, so set the timer
		long timeInMillis = ((hours * 3600) + (minutes * 60)) * 1000;
		if(timeInMillis == 0)
		{
			Toast.makeText(this, "Please specify an amount of time greater than 0 seconds.", Toast.LENGTH_LONG).show();
			return false;
		}
		
		if(!tService.setTimer(timeInMillis))
		{
			Toast.makeText(this, "Timer could not be set: the clock is already running.", Toast.LENGTH_LONG).show();
			return false;
		}
		
		startUpdatingDisplay();
		
		Calendar then = Calendar.getInstance();
		then.add(Calendar.HOUR, hours);
		then.add(Calendar.MINUTE, minutes);
		Toast.makeText(this, String.format("Timer has been set. An alarm\nwill sound at %1$tl:%1$tM %1$Tp.", then), Toast.LENGTH_LONG).show();
		return true;
	}
	
	private boolean cancel()
	{
		if(!tService.cancel())
		{
			Toast.makeText(this, "Timer could not be canceled: no countdown is currently running.", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	private void startUpdatingDisplay()
	{
		timeView.setText(tService.getTimeLeftDisplay());
		timerHandler.removeCallbacks(updateTimeViewTask);
		timerHandler.postDelayed(updateTimeViewTask, 100);
	}
}
