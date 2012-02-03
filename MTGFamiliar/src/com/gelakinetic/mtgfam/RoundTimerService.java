package com.gelakinetic.mtgfam;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class RoundTimerService extends Service {
	
	private final RoundTimerBinder binder = new RoundTimerBinder();
	public class RoundTimerBinder extends Binder
	{
		RoundTimerService getService()
		{
			return RoundTimerService.this;
		}
	}
	
	private static String FILTER = "AlarmEvent";
	
	private boolean running;
	private long endTime;
	private Uri soundFile;
	private MediaPlayer player;
	
	private Handler cleanupHandler = new Handler();
	private Runnable cleanupTask = new Runnable() 
	{
		public void run()
		{
			if(player != null) //We don't want null pointer exceptions
			{
				player.release();
			}
		}
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) 
			{
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(RoundTimerService.this);
				soundFile = Uri.parse(settings.getString("timerSound", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()));
				
				//When we get the alarm broadcast, we first start playing the alert sound
				player = MediaPlayer.create(RoundTimerService.this, soundFile);
				player.start();
				
				//And then we schedule a cleanup in 10 seconds, which will release the old player so we aren't wasting resources
				cleanupHandler.removeCallbacks(cleanupTask);
				cleanupHandler.postDelayed(cleanupTask, 10000);
			}
		}, new IntentFilter(FILTER));
		return START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) 
	{
		return binder;
	}
	
	@Override
	public boolean onUnbind(Intent intent)
	{		
		return true;
	}
	
	/**
	 * Returns the soundFile property of this service so we can see if the user has changed it.
	 * @return The URI to the current sound file
	 **/
	public Uri getSoundFile()
	{
		return this.soundFile;
	}
	
	/**
	 * Sets the soundFile property of this service so it can play an alert tone.
	 * @param soundFile The URI to the desired sound file
	 **/
	public void setSoundFile(Uri soundFile)
	{
		this.soundFile = soundFile;
	}
	
	/**
	 * Returns whether the service has been assigned a sound file yet.
	 * @return true if soundFile is not null, false otherwise
	 **/
	public boolean hasSoundFile()
	{
		return soundFile != null;
	}

	/**
	 * Starts the timer, provided that it is not already running
	 * @param durationInMillis How long the timer should run for
	 * @param sound The ID for the sound that should be played when time runs out
	 * @return true if the timer gets set, false otherwise
	 **/
	public boolean setTimer(long durationInMillis)
	{
		if(durationInMillis > 0 && !running)
		{
			endTime = SystemClock.elapsedRealtime() + durationInMillis;
			running = true;
			
			AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(FILTER);
			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
			alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, endTime, pi);
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Cancels the current timer, provided that it is currently running
	 * @return true if the timer gets canceled, false otherwise
	 **/
	public boolean cancel()
	{
		if(running)
		{
			endTime = SystemClock.elapsedRealtime();
			running = false;
			
			AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(FILTER);
			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
			alarm.cancel(pi);
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Returns a string representation of the time left, formatted as HH:MM:SS
	 * @return The remaining time on the clock
	 **/
	public String getTimeLeftDisplay()
	{
		long timeLeft = endTime - SystemClock.elapsedRealtime();
		if(timeLeft < 0)
		{
			timeLeft = 0;
			running = false;
		}
		
		long timeLeftInSecs = (timeLeft / 1000);
		String retval = "";
		
		if(running)
		{
			//This is a slight hack to handle the fact that it always rounds down. It makes the clock look much nicer this way.
			timeLeftInSecs++; 
		}
		
		String hours = String.valueOf(timeLeftInSecs / (60 * 60));
		String minutes = String.valueOf((timeLeftInSecs % 3600) / 60);
		String seconds = String.valueOf(timeLeftInSecs % 60);
		
		if(hours.length() == 1)
		{
			retval += "0";
		}
		retval += hours + ":";
		
		if(minutes.length() == 1)
		{
			retval += "0";
		}
		retval += minutes + ":";
		
		if(seconds.length() == 1)
		{
			retval += "0";
		}
		retval += seconds;
		
		return retval;
	}
	
	/**
	 * Returns whether or not the timer is currently running
	 * @return true if the timer is running, false otherwise
	 **/
	public boolean isRunning()
	{
		return running;
	}
}
