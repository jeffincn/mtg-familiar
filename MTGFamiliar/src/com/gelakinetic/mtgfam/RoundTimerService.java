package com.gelakinetic.mtgfam;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
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
	private static int NOTIFICATION_ID = 53; //Arbitrary; we just need something no one else is using
	
	private boolean running;
	private boolean paused;
	private long endTime;
	private long timeLeft;
	private Uri soundFile;
	private MediaPlayer player;

	private NotificationManager nm;
	private PendingIntent contentIntent;
	private Context c;
	private String titleText = "Round Timer";
	private String timerStartText = "The round will end at %1$tl:%1$tM %1$Tp.";
	private String timerEndText = "The round has ended.";
	
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
	public void onStart(Intent intent, int startId)
	{
		handleStartup(intent);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		handleStartup(intent);		
		return START_NOT_STICKY;
	}
	
	private void handleStartup(Intent intent)
	{
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) 
			{
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(RoundTimerService.this);
				soundFile = Uri.parse(settings.getString("timerSound", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()));
				
				//When we get the alarm broadcast, we first start playing the alert sound
				player = MediaPlayer.create(RoundTimerService.this, soundFile);
				if(player != null)
				{
					//If create fails for some reason, it will return null and we don't want null pointer exceptions
					player.start();
				}	
				
				//Then we clear the ongoing status bar notification and make a new one
				showEndNotification();
				
				//And then we schedule a cleanup in 10 seconds, which will release the old player so we aren't wasting resources
				cleanupHandler.removeCallbacks(cleanupTask);
				cleanupHandler.postDelayed(cleanupTask, 10000);
			}
		}, new IntentFilter(FILTER));
		
		nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		c = getApplication().getApplicationContext();
		
		Intent notificationIntent = new Intent(c, RoundTimerActivity.class);
		contentIntent = PendingIntent.getActivity(c, 0, notificationIntent, 0);
		
		running = false;
		paused = false;
		timeLeft = 0;
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
	 * Starts the timer, provided that it is not already running
	 * @param durationInMillis How long the timer should run for
	 * @param sound The ID for the sound that should be played when time runs out
	 * @return true if the timer gets set, false otherwise
	 **/
	public boolean setTimer(long durationInMillis)
	{
		if(durationInMillis > 0 && !running)
		{
			timeLeft = durationInMillis;
			endTime = SystemClock.elapsedRealtime() + timeLeft;
			running = true;
			paused = false;
			
			AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(FILTER);
			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
			alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, endTime, pi);
			
			showRunningNotification();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Pauses the timer, provided that it is running and not paused
	 * @return true if the timer gets paused, false otherwise
	 **/
	public boolean pauseTimer()
	{
		if(running && !paused)
		{
			timeLeft = endTime - SystemClock.elapsedRealtime();
			
			AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(FILTER);
			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
			alarm.cancel(pi);
			
			nm.cancel(NOTIFICATION_ID);
			
			paused = true;
			
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Resumes the timer, provided that it is running and paused
	 * @return true if the timer resumes, false otherwise
	 **/
	public boolean resumeTimer()
	{
		if(running && paused)
		{
			endTime = SystemClock.elapsedRealtime() + timeLeft;
			
			AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(FILTER);
			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
			alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, endTime, pi);
			
			showRunningNotification();
			
			paused = false;
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Resets the current timer, provided that it is currently running
	 **/
	public void reset()
	{
		if(running)
		{
			endTime = SystemClock.elapsedRealtime();
			running = false;
			paused = false;
			
			AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(FILTER);
			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
			alarm.cancel(pi);
			
			nm.cancel(NOTIFICATION_ID);
		}
	}
	
	/**
	 * Returns a string representation of the time left, formatted as HH:MM:SS
	 * @return The remaining time on the clock
	 **/
	public String getTimeLeftDisplay()
	{
		if(running && !paused)
		{
			timeLeft = endTime - SystemClock.elapsedRealtime();
			
			if(timeLeft < 0)
			{
				timeLeft = 0;
				running = false;
			}
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
	
	/**
	 * Returns whether or not the timer is currently paused
	 * @return true if the timer is paused, false otherwise
	 **/
	public boolean isPaused()
	{
		return paused;
	}
	
	private void showRunningNotification()
	{
		Calendar then = Calendar.getInstance();
		then.add(Calendar.MILLISECOND, (int)timeLeft);
		String messageText = String.format(timerStartText, then);
		
		Notification n = new Notification(R.drawable.rt_notification_icon, messageText, System.currentTimeMillis());
		n.flags |= Notification.FLAG_ONGOING_EVENT;
		n.setLatestEventInfo(c, titleText, messageText, contentIntent);
		
		//Clear any existing notifications just in case there's still one there
		nm.cancel(NOTIFICATION_ID);
		
		//Then show the new one
		nm.notify(NOTIFICATION_ID, n);
	}
	
	private void showEndNotification()
	{
		Notification n = new Notification(R.drawable.rt_notification_icon, timerEndText, System.currentTimeMillis());
		n.flags |= Notification.FLAG_AUTO_CANCEL;
		n.setLatestEventInfo(getApplication().getApplicationContext(), titleText, timerEndText, contentIntent);
		
		nm.cancel(NOTIFICATION_ID);
		
		nm.notify(NOTIFICATION_ID, n);
	}
}
