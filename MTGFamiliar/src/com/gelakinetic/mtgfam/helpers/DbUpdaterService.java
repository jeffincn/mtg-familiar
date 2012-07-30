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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;

public class DbUpdaterService extends IntentService {

    public static final int UPDATING_NOTIFICATION = 31;
    public static final int UPDATED_NOTIFICATION = 32;

    protected SharedPreferences mPreferences;
    protected CardDbAdapter mDbHelper;
    protected NotificationManager mNotificationManager;
    protected PendingIntent mNotificationIntent;

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

        showUpdatingNotification();

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
                int code = rp.loadRulesAndGlossary();
                
                //Only save the timestamp of this if the update was 100% successful; if
                //something went screwy, we should let them know and try again next update.
                if(code == RulesParser.SUCCESS) {
                    newRulesParsed = true;	
                }
                else {
                    //TODO - We should indicate failure here somehow (toasts don't work in the async task)
                }
            }
        }

        long curTime = new Date().getTime();
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putLong("lastLegalityUpdate", curTime);
        if(newRulesParsed) {
            editor.putLong("lastRulesUpdate", curTime);
        }
        editor.commit();

        mDbHelper.closeTransactional();

        cancelUpdatingNotification();
        showUpdatedNotification(updatedStuff);

        return;
    }

    protected Notification showUpdatingNotification() {

        String title = getString(R.string.app_name);
        String body = getString(R.string.update_notification);

		Notification notification = new Notification(
                R.drawable.rt_notification_icon, body,
                System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(this,
                title, body, mNotificationIntent);

        mNotificationManager.notify(UPDATING_NOTIFICATION, notification);

        return notification;
    }

    protected void cancelUpdatingNotification() {
        mNotificationManager.cancel(UPDATING_NOTIFICATION);
    }

    protected Notification showUpdatedNotification(List<String> newStuff) {
        if(newStuff.size() < 1) {
            return null;
        }

        String title = getString(R.string.app_name);
        String body = getString(R.string.update_added) + " ";
        for(int ii = 0; ii < newStuff.size(); ii++) {
            body += newStuff.get(ii);
            if(ii < newStuff.size() - 1) {
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

        return notification;
    }

    protected class ProgressReporter implements JsonParser.CardProgressReporter,
                                  RulesParser.ProgressReporter {
        public void reportJsonCardProgress(String... args) {
        }
        public void reportRulesProgress(String... args) {
        }
    }

}
