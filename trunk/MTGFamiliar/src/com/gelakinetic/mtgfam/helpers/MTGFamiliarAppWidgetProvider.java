package com.gelakinetic.mtgfam.helpers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.activities.SearchActivity;
import com.gelakinetic.mtgfam.activities.WidgetSearchActivity;

public class MTGFamiliarAppWidgetProvider extends AppWidgetProvider {

  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
      final int N = appWidgetIds.length;

      // Perform this loop procedure for each App Widget that belongs to this provider
      for (int i=0; i<N; i++) {
          int appWidgetId = appWidgetIds[i];

          // Create an Intent to launch ExampleActivity
          Intent intentQuick = new Intent(context, WidgetSearchActivity.class);
          PendingIntent pendingIntentQuick = PendingIntent.getActivity(context, 0, intentQuick, 0);

          Intent intentMain = new Intent(context, MainActivity.class);
          PendingIntent pendingIntentMain = PendingIntent.getActivity(context, 0, intentMain, 0);

          Intent intentFullSearch = new Intent(context, SearchActivity.class);
          PendingIntent pendingIntentFullSearch = PendingIntent.getActivity(context, 0, intentFullSearch, 0);

          // Get the layout for the App Widget and attach an on-click listener
          // to the button
          RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mtgfamiliar_appwidget);
          views.setOnClickPendingIntent(R.id.widget_namefield, pendingIntentQuick);

          views.setOnClickPendingIntent(R.id.image_icon, pendingIntentMain);

          views.setOnClickPendingIntent(R.id.search_button, pendingIntentFullSearch);

          // Tell the AppWidgetManager to perform an update on the current app widget
          appWidgetManager.updateAppWidget(appWidgetId, views);
      }
  }
}