package com.mtg.fam;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.content.SharedPreferences;

import com.google.gson.stream.JsonReader;

public class JsonUpdateParser {

	private static String	label;
	private static String	date;
	private static String	label2;

	public static ArrayList<String[]> readJsonStream(main mMain) {
		ArrayList<String[]> patchInfo = new ArrayList<String[]>();
		URL update;
		
    SharedPreferences settings = mMain.getSharedPreferences("prefs", 0);

		try {
			update = new URL("http://members.cox.net/aefeinstein/patches.json");
			InputStreamReader isr = new InputStreamReader(update.openStream(), "ISO-8859-1");
			JsonReader reader = new JsonReader(isr);
			
			reader.beginObject();
			while (reader.hasNext()) {
				label = reader.nextName();
				
				if(label.equals("Date")){
			    String lastUpdate = settings.getString("lastUpdate", "");
					date = reader.nextString();
					if(lastUpdate.equals(date)){
						return null;
					}
				}
				else if(label.equals("Patches")){
					reader.beginArray();
					while(reader.hasNext()){
						reader.beginObject();
						String[] setdata = new String[3];
						while(reader.hasNext()){
							label2 = reader.nextName();
							if(label2.equals("Name")){
								setdata[0] = reader.nextString();
							}
							else if(label2.equals("URL")){
								setdata[1] = reader.nextString();								
							}
							else if(label2.equals("Code")){
								setdata[2] = reader.nextString();
							}
						}
						patchInfo.add(setdata);
						reader.endObject();
					}
					reader.endArray();
				}
			}
			reader.endObject();
		}
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    SharedPreferences.Editor editor = settings.edit();
    editor.putString("lastUpdate", date);
    editor.commit();

		return patchInfo;
	}

}
