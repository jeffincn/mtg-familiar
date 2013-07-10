/**
Copyright 2012 Michael Shick

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
package com.gelakinetic.mtgfam.helpers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;

public class DbUpdaterService extends IntentService {

	public static final int				STATUS_NOTIFICATION		= 31;
	public static final int				UPDATED_NOTIFICATION	= 32;

	protected PreferencesAdapter mPrefAdapter;
	protected CardDbAdapter				mDbHelper;
	protected NotificationManager	mNotificationManager;
	protected PendingIntent				mNotificationIntent;

	protected Notification				mUpdateNotification;
	protected Handler							mHandler							= new Handler();
	protected Runnable						mProgressUpdater;

	protected int									mProgress;
	
	public DbUpdaterService() {
		super("com.gelakinetic.mtgfam.helpers.DbUpdaterService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mPrefAdapter = new PreferencesAdapter(this);
		try {
			mDbHelper = new CardDbAdapter(this);
		}
		catch(FamiliarDbException e) {
			mDbHelper = null;
			return; // couldnt open the db, might as well return
		}
		mDbHelper.close(); // close the newly opened db so we can transact it later
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		Intent intent = new Intent(this, MainActivity.class);
		mNotificationIntent = PendingIntent.getActivity(this, 0, intent, 0);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getApplicationContext());
		mUpdateNotification = builder.setContentTitle(getString(R.string.app_name))
				.setContentText(getString(R.string.update_notification)).setSmallIcon(R.drawable.rt_notification_icon)
				.setContentIntent(mNotificationIntent).setWhen(System.currentTimeMillis()).build();

		mUpdateNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		mUpdateNotification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
	}

	// Throw this switch to reparse the entire database from a custom URL (currently UpToRTR.josn.gzip
	// THIS SHOULD NEVER EVER EVER BE TRUE IN A PLAY STORE RELEASE
	private static final boolean reparseDatabase = false;

	@Override
	public void onHandleIntent(Intent intent) {

		if(mDbHelper == null) {
			return; // couldnt open db before
		}
		ProgressReporter reporter = new ProgressReporter();
		ArrayList<String> updatedStuff = new ArrayList<String>();
		JsonParser parser = new JsonParser();
		boolean commitDates = true;

		try {

			mDbHelper.openTransactional();

			showStatusNotification();

			if(reparseDatabase) {
				mDbHelper.dropCreateDB();
				parser.readLegalityJsonStream(mDbHelper, mPrefAdapter, reparseDatabase);
				GZIPInputStream upToGIS = new GZIPInputStream(new URL("https://sites.google.com/site/mtgfamiliar/patches/UpToRTR.json.gzip").openStream());
				switchToUpdating(String.format(getString(R.string.update_updating_set), "EVERYTHING!!"));
				parser.readCardJsonStream(upToGIS, reporter, "upToRTR", mDbHelper, this);
				parser.readTCGNameJsonStream(mPrefAdapter, mDbHelper, reparseDatabase);
			}
			else {
				parser.readLegalityJsonStream(mDbHelper, mPrefAdapter, reparseDatabase);
				ArrayList<String[]> patchInfo = parser.readUpdateJsonStream(mPrefAdapter);
				if (patchInfo != null) {

					for (int i = 0; i < patchInfo.size(); i++) {
						String[] set = patchInfo.get(i);
						if (!mDbHelper.doesSetExist(set[2])) {
							try {
								switchToUpdating(String.format(getString(R.string.update_updating_set), set[0]));
								GZIPInputStream gis = new GZIPInputStream(new URL(set[1]).openStream());
								parser.readCardJsonStream(gis, reporter, set[0], mDbHelper, this);
								updatedStuff.add(set[0]);
							}
							catch (MalformedURLException e) {
							}
							catch (IOException e) {
							}

							switchToChecking();
						}
					}
					parser.readTCGNameJsonStream(mPrefAdapter, mDbHelper, reparseDatabase);
				}
			}
		}
		catch (MalformedURLException e1) {
			commitDates = false; // dont commit the dates
		}
		catch (IOException e) {
			commitDates = false; // dont commit the dates
		}
		catch (FamiliarDbException e) {
			commitDates = false; // dont commit the dates
		}
		
		// Instead of using a hardcoded string, the default lastRulesUpdate is the
		// timestamp of when the APK was built.
		// This is a safe assumption to make, since any market release will have the
		// latest database baked in.
		boolean newRulesParsed = false;
		try{
			long lastRulesUpdate = mPrefAdapter.getLastRulesUpdate();
			if(reparseDatabase) {
				lastRulesUpdate = 0; //1979 anybody?
			}
			RulesParser rp = new RulesParser(new Date(lastRulesUpdate), mDbHelper, this, reporter);
			if (rp.needsToUpdate()) {
				if (rp.parseRules()) {
					switchToUpdating(getString(R.string.update_updating_rules));
					int code = rp.loadRulesAndGlossary();
	
					// Only save the timestamp of this if the update was 100% successful; if
					// something went screwy, we should let them know and try again next
					// update.
					if (code == RulesParser.SUCCESS) {
						newRulesParsed = true;
						updatedStuff.add(getString(R.string.update_added_rules));
					}
					else {
						// TODO - We should indicate failure here somehow (toasts don't work
						// in the async task)
					}
	
					switchToChecking();
				}
			}
	
			mDbHelper.closeTransactional();
	
			cancelStatusNotification();
		}
		catch(FamiliarDbException e) {
			commitDates = false; // dont commit the dates
		}
		
		boolean mtrUpdated = false;
		long lastMTRUpdate = mPrefAdapter.getLastMTRUpdate();
		MTRIPGParser mtrParser = new MTRIPGParser(new Date(lastMTRUpdate), this);
		if (mtrParser.performMtrUpdateIfNeeded()) {
			mtrUpdated = true;
			updatedStuff.add(getString(R.string.update_added_mtr));
		}
		
		boolean ipgUpdated = false;
		long lastIPGUpdate = mPrefAdapter.getLastIPGUpdate();
		MTRIPGParser ipgParser = new MTRIPGParser(new Date(lastIPGUpdate), this);
		if (ipgParser.performIpgUpdateIfNeeded()) {
			ipgUpdated = true;
			updatedStuff.add(getString(R.string.update_added_ipg));
		}
		
		if(commitDates) {
			showUpdatedNotification(updatedStuff);
			
			parser.commitDates(mPrefAdapter);

			long curTime = new Date().getTime();
			mPrefAdapter.setLastLegalityUpdate((int)(curTime / 1000));
			if (newRulesParsed) {
				mPrefAdapter.setLastRulesUpdate(curTime);
			}
			if (mtrUpdated) {
				mPrefAdapter.setLastMTRUpdate(curTime);
			}
			if (ipgUpdated) {
				mPrefAdapter.setLastIPGUpdate(curTime);
			}
		}
		return;
	}

	protected void showStatusNotification() {
		mNotificationManager.notify(STATUS_NOTIFICATION, mUpdateNotification);
	}

	protected void cancelStatusNotification() {
		mNotificationManager.cancel(STATUS_NOTIFICATION);
	}

	protected void switchToChecking() {
		mHandler.removeCallbacks(mProgressUpdater);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getApplicationContext());
		mUpdateNotification = builder.setContentTitle(getString(R.string.app_name))
				.setContentText(getString(R.string.update_notification)).setContentIntent(mNotificationIntent)
				.build();

		mNotificationManager.notify(STATUS_NOTIFICATION, mUpdateNotification);
	}

	protected void switchToUpdating(String title) {
		final RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.progress_notification);
		contentView.setProgressBar(R.id.progress_notification_bar, 100, 0, false);
		contentView.setTextViewText(R.id.progress_notification_title, title);

		mUpdateNotification.contentView = contentView;
		mUpdateNotification.contentIntent = mNotificationIntent;

		mNotificationManager.notify(STATUS_NOTIFICATION, mUpdateNotification);

		mProgressUpdater = new Runnable() {
			public void run() {
				contentView.setProgressBar(R.id.progress_notification_bar, 100, mProgress, false);
				mNotificationManager.notify(STATUS_NOTIFICATION, mUpdateNotification);
				mHandler.postDelayed(mProgressUpdater, 200);
			}
		};
		mHandler.postDelayed(mProgressUpdater, 200);
	}

	protected void showUpdatedNotification(List<String> newStuff) {
		if (newStuff.size() < 1) {
			return;
		}

		String title = getString(R.string.app_name);
		String body = getString(R.string.update_added) + " ";
		for (int i = 0; i < newStuff.size(); i++) {
			body += newStuff.get(i);
			if (i < newStuff.size() - 1) {
				body += ", ";
			}
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getApplicationContext());
		Notification notification = builder.setContentTitle(title)
				.setContentText(body).setSmallIcon(R.drawable.rt_notification_icon)
				.setContentIntent(mNotificationIntent).setWhen(System.currentTimeMillis()).build();

		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		mNotificationManager.notify(UPDATED_NOTIFICATION, notification);
	}

	protected class ProgressReporter implements JsonParser.CardProgressReporter, RulesParser.ProgressReporter {
		public void reportJsonCardProgress(String... args) {
			if (args.length == 3) {
				// We only care about this; it has a number
				mProgress = Integer.parseInt(args[2]);
			}
		}

		public void reportRulesProgress(String... args) {
			if (args.length == 3) {
				// We only care about this; it has a number
				mProgress = Integer.parseInt(args[2]);
			}
		}
	}

}
