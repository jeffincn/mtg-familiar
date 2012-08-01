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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;

public class DbUpdaterService extends IntentService {

    public static final int CHECKING_NOTIFICATION = 31;
    public static final int UPDATING_NOTIFICATION = 32;
    public static final int UPDATED_NOTIFICATION = 33;

    protected SharedPreferences mPreferences;
    protected CardDbAdapter mDbHelper;
    protected NotificationManager mNotificationManager;
    protected PendingIntent mNotificationIntent;

    protected Handler mHandler = new Handler();
    protected Runnable mProgressUpdater;
    
    protected int mProgress;

    public DbUpdaterService() {
        super("com.gelakinetic.mtgfam.helpers.DbUpdaterService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mDbHelper = new CardDbAdapter(this);
		mNotificationManager = (NotificationManager)
            getSystemService(NOTIFICATION_SERVICE);

		Intent intent = new Intent(this, MainActivity.class);
		mNotificationIntent = PendingIntent.getActivity(this, 0, intent, 0);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        
        //this.publishProgress(new String[] { "indeterminate", mCtx.getString(R.string.update_legality) });

        showCheckingNotification();

        ArrayList<String> updatedStuff = new ArrayList<String>();
        ProgressReporter reporter = new ProgressReporter();
        mDbHelper.openTransactional();

        try {				
            ArrayList<String[]> patchInfo = JsonParser.readUpdateJsonStream(mPreferences);

            URL legal = new URL("https://sites.google.com/site/mtgfamiliar/manifests/legality.json");
            InputStream in = new BufferedInputStream(legal.openStream());
            JsonParser.readLegalityJsonStream(in, mDbHelper, mPreferences);

            //this.publishProgress(new String[] { "indeterminate", mCtx.getString(R.string.update_cards) });

            if (patchInfo != null) {
                
                for (int i = 0; i < patchInfo.size(); i++) {
                    String[] set = patchInfo.get(i);
                    if (!mDbHelper.doesSetExist(set[2])) {
                        try {
                        	cancelCheckingNotification();
                        	showUpdatingNotification(String.format(getString(R.string.update_updating_set), set[0]));
                            GZIPInputStream gis = new GZIPInputStream(new URL(set[1]).openStream());
                            JsonParser.readCardJsonStream(gis, reporter, set[0], mDbHelper, this);
                            updatedStuff.add(set[0]);
                        }
                        catch (MalformedURLException e) {
                            // Log.e("JSON error", e.toString());
                        }
                        catch (IOException e) {
                            // Log.e("JSON error", e.toString());
                        }
                        cancelUpdatingNotification();
                        showCheckingNotification();
                    }
                }
                JsonParser.readTCGNameJsonStream(mPreferences, mDbHelper);
            }
        }
        catch (MalformedURLException e1) {
            // eat it
        }
        catch (IOException e) {
            // eat it
        }

        //this.publishProgress(new String[] { "indeterminate", mCtx.getString(R.string.update_rules) });

        long lastRulesUpdate = mPreferences.getLong("lastRulesUpdate", BuildDate.get(this).getTime());
        RulesParser rp = new RulesParser(new Date(lastRulesUpdate), mDbHelper, this, reporter);
        boolean newRulesParsed = false;
        if (rp.needsToUpdate()) {
            if (rp.parseRules()) {
            	cancelCheckingNotification();
            	showUpdatingNotification(getString(R.string.update_updating_rules));
                int code = rp.loadRulesAndGlossary();
                
                //Only save the timestamp of this if the update was 100% successful; if
                //something went screwy, we should let them know and try again next update.
                if(code == RulesParser.SUCCESS) {
                    newRulesParsed = true;	
                    updatedStuff.add(getString(R.string.update_added_rules));
                }
                else {
                    //TODO - We should indicate failure here somehow (toasts don't work in the async task)
                }
                cancelUpdatingNotification();
                showCheckingNotification();
            }
        }

        long curTime = new Date().getTime();
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt("lastLegalityUpdate", (int)(curTime / 1000));
        if(newRulesParsed) {
            editor.putLong("lastRulesUpdate", curTime);
        }
        editor.commit();

        mDbHelper.closeTransactional();

        cancelCheckingNotification();
        showUpdatedNotification(updatedStuff);

        return;
    }

    protected void showCheckingNotification() {

        String title = getString(R.string.app_name);
        String body = getString(R.string.update_notification);

		Notification notification = new Notification(
                R.drawable.rt_notification_icon, body,
                System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(this,
                title, body, mNotificationIntent);

        mNotificationManager.notify(CHECKING_NOTIFICATION, notification);
    }

    protected void cancelCheckingNotification() {
        mNotificationManager.cancel(CHECKING_NOTIFICATION);
    }
    
    protected void showUpdatingNotification(String title) {
		final Notification notification = new Notification(R.drawable.rt_notification_icon, title, System.currentTimeMillis());
    	final RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.progress_notification);
    	contentView.setProgressBar(R.id.progress_notification_bar, 100, 0, false);
    	contentView.setTextViewText(R.id.progress_notification_title, title);
    	notification.contentView = contentView;
    	notification.contentIntent = mNotificationIntent;
    	notification.flags |= Notification.FLAG_ONGOING_EVENT;
    	notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
    	
    	mNotificationManager.notify(UPDATING_NOTIFICATION, notification);
    	
    	mProgressUpdater = new Runnable() {    		
			public void run() {
				contentView.setProgressBar(R.id.progress_notification_bar, 100, mProgress, false);
				mNotificationManager.notify(UPDATING_NOTIFICATION, notification);
				mHandler.postDelayed(mProgressUpdater, 200);
			}
		};
		mHandler.postDelayed(mProgressUpdater, 200);
    }
    
    protected void cancelUpdatingNotification() {
    	mNotificationManager.cancel(UPDATING_NOTIFICATION);
    	mHandler.removeCallbacks(mProgressUpdater);
    }

    protected void showUpdatedNotification(List<String> newStuff) {
        if(newStuff.size() < 1) {
            return;
        }

        String title = getString(R.string.app_name);
        String body = getString(R.string.update_added) + " ";
        for(int i = 0; i < newStuff.size(); i++) {
            body += newStuff.get(i);
            if(i < newStuff.size() - 1) {
                body += ", ";
            }
        }

		Notification notification = new Notification(
                R.drawable.rt_notification_icon, body,
                System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(this,
                title, body, mNotificationIntent);

        mNotificationManager.notify(UPDATED_NOTIFICATION, notification);
    }

    protected class ProgressReporter implements JsonParser.CardProgressReporter,
                                  RulesParser.ProgressReporter {
        public void reportJsonCardProgress(String... args) {
        	if(args.length == 3) {
        		//We only care about this; it has a number
        		mProgress = Integer.parseInt(args[2]);
        	}
        }
        public void reportRulesProgress(String... args) {
        	if(args.length == 3) {
        		//We only care about this; it has a number
        		mProgress = Integer.parseInt(args[2]);
        	}
        }
    }

}
