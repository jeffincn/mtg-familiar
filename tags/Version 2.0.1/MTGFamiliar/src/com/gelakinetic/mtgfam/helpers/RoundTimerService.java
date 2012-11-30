package com.gelakinetic.mtgfam.helpers;

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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.NotificationCompat;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.fragments.RoundTimerFragment;

public class RoundTimerService extends Service implements OnInitListener {

	private static String ALARM_FILTER = "com.gelakinetic.mtgfam.ALARM_FILTER";
	private static String FINAL_DAY_FILTER = "com.gelakinetic.mtgfam.FINAL_DAY_FILTER";
	private static String FIFTEEN_FILTER = "com.gelakinetic.mtgfam.FIFTEEN_FILTER";
	private static String TEN_FILTER = "com.gelakinetic.mtgfam.TEN_FILTER";
	private static String FIVE_FILTER = "com.gelakinetic.mtgfam.FIVE_FILTER";
	public static String REQUEST_FILTER = "com.gelakinetic.mtgfam.REQUEST_FILTER";
	public static String START_FILTER = "com.gelakinetic.mtgfam.START_FILTER";
	public static String CANCEL_FILTER = "com.gelakinetic.mtgfam.CANCEL_FILTER";
	public static String TTS_INITIALIZED_FILTER = "com.gelakinetic.mtgfam.HAS_TTS_FILTER";

	// Arbitrary; we just need something no one else is using
	private static int NOTIFICATION_ID = 53;

	public static String EXTRA_END_TIME = "EndTime";
	public static String EXTRA_TTS_INITIALIZED = "TtsInitialized";

	private long endTime;
	private Uri soundFile;
	private MediaPlayer player;
	private TextToSpeech tts;
	private boolean ttsInitialized = false;
	private PreferencesAdapter prefAdapter;

	private NotificationManager nm;
	private AlarmManager alarm;
	private PendingIntent contentIntent;
	private Context c;
	private String titleText = "Round Timer";
	private String timerStartText = "The round will end at %1$tl:%1$tM %1$Tp.";
	private String timerEndText = "The round has ended.";

	private BroadcastReceiver finalDayReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (ttsInitialized) {
				tts.speak("Night of the final day: twelve hours remain", TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	};

	private BroadcastReceiver fifteenReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (ttsInitialized && prefAdapter.getFifteenMinutePref()) {
				tts.speak("The round will end in fifteen minutes", TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	};

	private BroadcastReceiver tenReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (ttsInitialized && prefAdapter.getTenMinutePref()) {
				tts.speak("The round will end in ten minutes", TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	};

	private BroadcastReceiver fiveReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (ttsInitialized && prefAdapter.getFiveMinutePref()) {
				tts.speak("The round will end in five minutes", TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	};

	private BroadcastReceiver alarmReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			soundFile = Uri.parse(prefAdapter.getTimerSound());

			// When we get the alarm broadcast, we first start playing the alert sound
			player = MediaPlayer.create(RoundTimerService.this, soundFile);
			if (player != null) {
				// If create fails for some reason, it will return null and we don't want null pointer exceptions
				player.start();
			}

			// Then we clear the ongoing status bar notification and make a new one
			showEndNotification();

			// And then we schedule a cleanup in 10 seconds, which will release the old player so we aren't wasting
			// resources
			cleanupHandler.removeCallbacks(cleanupTask);
			cleanupHandler.postDelayed(cleanupTask, 10000);
		}
	};

	private BroadcastReceiver requestReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Intent i = new Intent(RoundTimerFragment.RESULT_FILTER);
			i.putExtra(EXTRA_END_TIME, endTime);
			sendBroadcast(i);
		}
	};

	private BroadcastReceiver startReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = intent.getLongExtra(RoundTimerFragment.EXTRA_END_TIME, SystemClock.elapsedRealtime());

			Intent i;
			PendingIntent pi;

			long finalDay = endTime - (12 * 60 * 60 * 1000);
			long fifteen = endTime - (15 * 60 * 1000);
			long ten = endTime - (10 * 60 * 1000);
			long five = endTime - (5 * 60 * 1000);
			long now = SystemClock.elapsedRealtime();

			if (finalDay > now) {
				// NIGHT OF THE FINAL DAY
				i = new Intent(FINAL_DAY_FILTER);
				pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
				alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, finalDay, pi);
			}

			if (fifteen > now) {
				// 15-minute warning
				i = new Intent(FIFTEEN_FILTER);
				pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
				alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, fifteen, pi);
			}

			if (ten > now) {
				// 10-minute warning
				i = new Intent(TEN_FILTER);
				pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
				alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, ten, pi);
			}

			if (five > now) {
				// 5-minute warning
				i = new Intent(FIVE_FILTER);
				pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
				alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, five, pi);
			}

			// Round end
			i = new Intent(ALARM_FILTER);
			pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
			alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, endTime, pi);

			showRunningNotification();
		}
	};

	private BroadcastReceiver cancelReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			endTime = SystemClock.elapsedRealtime();

			Intent i;
			PendingIntent pi;

			i = new Intent(FINAL_DAY_FILTER);
			pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
			alarm.cancel(pi);

			i = new Intent(FIFTEEN_FILTER);
			pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
			alarm.cancel(pi);

			i = new Intent(TEN_FILTER);
			pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
			alarm.cancel(pi);

			i = new Intent(FIVE_FILTER);
			pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
			alarm.cancel(pi);

			i = new Intent(ALARM_FILTER);
			pi = PendingIntent.getBroadcast(RoundTimerService.this, 0, i, 0);
			alarm.cancel(pi);

			nm.cancel(NOTIFICATION_ID);
		}
	};

	private BroadcastReceiver hasTtsReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Intent i = new Intent(RoundTimerFragment.TTS_FILTER);
			i.putExtra(EXTRA_TTS_INITIALIZED, ttsInitialized);
			sendBroadcast(i);
		}
	};

	private Handler cleanupHandler = new Handler();
	private Runnable cleanupTask = new Runnable() {

		public void run() {
			if (player != null) {
				player.release();
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();

		registerReceiver(finalDayReceiver, new IntentFilter(FINAL_DAY_FILTER));
		registerReceiver(fifteenReceiver, new IntentFilter(FIFTEEN_FILTER));
		registerReceiver(tenReceiver, new IntentFilter(TEN_FILTER));
		registerReceiver(fiveReceiver, new IntentFilter(FIVE_FILTER));
		registerReceiver(alarmReceiver, new IntentFilter(ALARM_FILTER));
		registerReceiver(requestReceiver, new IntentFilter(REQUEST_FILTER));
		registerReceiver(startReceiver, new IntentFilter(START_FILTER));
		registerReceiver(cancelReceiver, new IntentFilter(CANCEL_FILTER));
		registerReceiver(hasTtsReceiver, new IntentFilter(TTS_INITIALIZED_FILTER));

		nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

		c = getApplication().getApplicationContext();
		prefAdapter = new PreferencesAdapter(this);

		Intent notificationIntent = new Intent(c, MainActivity.class);
		notificationIntent.setAction(MainActivity.ACTION_ROUND_TIMER);
		contentIntent = PendingIntent.getActivity(c, 7, notificationIntent, 0);

		tts = new TextToSpeech(this, this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(finalDayReceiver);
		unregisterReceiver(fifteenReceiver);
		unregisterReceiver(tenReceiver);
		unregisterReceiver(fiveReceiver);
		unregisterReceiver(alarmReceiver);
		unregisterReceiver(requestReceiver);
		unregisterReceiver(startReceiver);
		unregisterReceiver(cancelReceiver);
		unregisterReceiver(hasTtsReceiver);

		if (ttsInitialized) {
			tts.shutdown();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void showRunningNotification() {
		Calendar then = Calendar.getInstance();
		then.add(Calendar.MILLISECOND, (int)(endTime - SystemClock.elapsedRealtime()));
		String messageText = String.format(timerStartText, then);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getApplication().getApplicationContext());
		Notification n = builder.setSmallIcon(R.drawable.rt_notification_icon).setWhen(System.currentTimeMillis()).setContentTitle(titleText)
				.setContentText(messageText).setContentIntent(contentIntent).getNotification();

		n.flags |= Notification.FLAG_ONGOING_EVENT;

		// Clear any existing notifications just in case there's still one there
		nm.cancel(NOTIFICATION_ID);

		// Then show the new one
		nm.notify(NOTIFICATION_ID, n);
	}

	private void showEndNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getApplication().getApplicationContext());
		Notification n = builder.setSmallIcon(R.drawable.rt_notification_icon).setWhen(System.currentTimeMillis()).setContentTitle(titleText)
				.setContentText(timerEndText).setContentIntent(contentIntent).getNotification();
		n.flags |= Notification.FLAG_AUTO_CANCEL;

		nm.cancel(NOTIFICATION_ID);

		nm.notify(NOTIFICATION_ID, n);
	}

	@Override
	public void onInit(int status) {
		ttsInitialized = (status == TextToSpeech.SUCCESS);
	}
}
