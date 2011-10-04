package com.gelakinetic.mtgfam;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.SharedPreferences;

import com.google.gson.stream.JsonReader;

public class JsonLegalityParser {
	private static CardDbAdapter	mDbHelper;
	private static String					date;
	private static String					legalSet;
	private static String					bannedCard;
	private static String					restrictedCard;
	private static String					formatName;
	private static String					jsonArrayName;
	private static String					jsonTopLevelName;

	public JsonLegalityParser(main m, CardDbAdapter cda) {
		mDbHelper = cda;
	}

	public static void readJsonStream(InputStream in, CardDbAdapter cda, SharedPreferences settings) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(in, "ISO-8859-1"));

		mDbHelper = cda;
		
		reader.beginObject();
		while (reader.hasNext()) {

			jsonTopLevelName = reader.nextName();
			if (jsonTopLevelName.equalsIgnoreCase("Date")) {
				date = reader.nextString();
				
				//compare date, maybe return, update sharedprefs
				String spDate = settings.getString("date", null);
				if(spDate != null && spDate.equals(date)){
					//TODO this always refreshes, shouldn't when checking OTA
//					return; // dates match, nothing new here.
				}

				// refresh everything!
				mDbHelper.dropFormatTable();
				mDbHelper.createFormatTable();
			}
			else if (jsonTopLevelName.equalsIgnoreCase("Formats")) {

				reader.beginObject();
				while (reader.hasNext()) {
					formatName = reader.nextName();
					
					mDbHelper.createFormat(formatName);
					mDbHelper.createFormatSetTable(formatName);
					mDbHelper.createFormatBanTable(formatName);
					mDbHelper.createFormatRestrictedTable(formatName);

					reader.beginObject();
					while (reader.hasNext()) {
						jsonArrayName = reader.nextName();

						if (jsonArrayName.equalsIgnoreCase("Sets")) {
							reader.beginArray();
							while (reader.hasNext()) {
								legalSet = reader.nextString();
								mDbHelper.createFormatSet(formatName, legalSet);
							}
							reader.endArray();
						}
						else if (jsonArrayName.equalsIgnoreCase("Banlist")) {
							reader.beginArray();
							while (reader.hasNext()) {
								bannedCard = reader.nextString();
								mDbHelper.createFormatBan(formatName, bannedCard);
							}
							reader.endArray();
						}
						else if (jsonArrayName.equalsIgnoreCase("Restrictedlist")) {
							reader.beginArray();
							while (reader.hasNext()) {
								restrictedCard = reader.nextString();
								mDbHelper.createFormatRestricted(formatName, restrictedCard);
							}
							reader.endArray();
						}
					}
					reader.endObject();
				}
				reader.endObject();
			}
		}
		reader.endObject();
		
		SharedPreferences.Editor editor = settings.edit();
    editor.putString("date", date);
    editor.commit();
    
		return;
	}
}
