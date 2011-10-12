/**
Copyright 2011 Adam Feinstein

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

package com.gelakinetic.mtgfam;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class JsonParser {

	public static void readCardJsonStream(InputStream in, MainActivity mMain, CardDbAdapter mDbHelper) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(in, "ISO-8859-1"));

		String s, s1, s2, ptstr;

		reader.beginObject();
		s = reader.nextName();

		reader.beginObject();
		while (reader.hasNext()) {

			s = reader.nextName();
			if (s.equalsIgnoreCase("v")) { // bdd_date
				reader.skipValue();
			}
			if (s.equalsIgnoreCase("u")) { // bdd_version
				reader.skipValue();
			}
			if (s.equalsIgnoreCase("s")) { // sets
				reader.beginObject();
				while (reader.hasNext()) {
					s1 = reader.nextName();
					if (s1.equalsIgnoreCase("b")) { // set
						MtgSet set;

						JsonToken jt = reader.peek();
						if (jt.equals(JsonToken.BEGIN_OBJECT)) {
							set = new MtgSet();
							reader.beginObject();
							while (reader.hasNext()) {
								s2 = reader.nextName();
								if (s2.equalsIgnoreCase("a")) { // name
									set.name = reader.nextString();
								}
								if (s2.equalsIgnoreCase("r")) { // code_magiccards
									set.code_magiccards = reader.nextString();
								}
								if (s2.equalsIgnoreCase("q")) { // code
									set.code = reader.nextString();
								}
							}
							mDbHelper.createSet(set);
							reader.endObject();
						}
						else if (jt.equals(JsonToken.BEGIN_ARRAY)) {
							reader.beginArray();
							while (reader.hasNext()) {
								set = new MtgSet();
								reader.beginObject();
								while (reader.hasNext()) {
									s2 = reader.nextName();
									if (s2.equalsIgnoreCase("a")) { // name
										set.name = reader.nextString();
									}
									if (s2.equalsIgnoreCase("r")) { // code_magiccards
										set.code_magiccards = reader.nextString();
									}
									if (s2.equalsIgnoreCase("q")) { // code
										set.code = reader.nextString();
									}
								}
								mDbHelper.createSet(set);
								reader.endObject();
							}
							reader.endArray();
						}
					}
				}
				reader.endObject();
			}
			if (s.equalsIgnoreCase("p")) { // cards

				reader.beginObject();
				while (reader.hasNext()) {
					s1 = reader.nextName();
					if (s1.equalsIgnoreCase("o")) { // card
						MtgCard c;
						reader.beginArray();
						while (reader.hasNext()) {

							reader.beginObject();
							c = new MtgCard();
							while (reader.hasNext()) {
								s2 = reader.nextName();
								if (s2.equalsIgnoreCase("a")) { // name
									c.name = reader.nextString();
								}
								else if (s2.equalsIgnoreCase("b")) { // set
									c.set = reader.nextString();
								}
								else if (s2.equalsIgnoreCase("c")) { // type
									c.type = reader.nextString();
								}
								else if (s2.equalsIgnoreCase("d")) { // rarity
									c.rarity = reader.nextString().charAt(0);
								}
								else if (s2.equalsIgnoreCase("e")) { // manacost
									c.manacost = reader.nextString();
								}
								else if (s2.equalsIgnoreCase("f")) { // converted_manacost
									try {
										c.cmc = reader.nextInt();
									}
									catch (Exception e) {
										reader.skipValue();
									}

								}
								else if (s2.equalsIgnoreCase("g")) { // power
									try {
										ptstr = reader.nextString();
										try {
											c.power = Integer.parseInt(ptstr);
										}
										catch (NumberFormatException e) {
											if (ptstr.equals("*")) {
												c.power = CardDbAdapter.STAR;
											}
											else if (ptstr.equals("1+*")) {
												c.power = CardDbAdapter.ONEPLUSSTAR;
											}
											else if (ptstr.equals("2+*")) {
												c.power = CardDbAdapter.TWOPLUSSTAR;
											}
											else if (ptstr.equals("7-*")) {
												c.power = CardDbAdapter.SEVENMINUSSTAR;
											}
											else if (ptstr.equals("*{^2}")) {
												c.power = CardDbAdapter.STARSQUARED;
											}
											else if (ptstr.equals("{1/2}")) {
												c.power = 0.5f;
											}
											else if (ptstr.equals("1{1/2}")) {
												c.power = 1.5f;
											}
											else if (ptstr.equals("2{1/2}")) {
												c.power = 2.5f;
											}
											else if (ptstr.equals("3{1/2}")) {
												c.power = 3.5f;
											}
										}
									}
									catch (Exception e) {
										reader.skipValue();
									}
								}
								else if (s2.equalsIgnoreCase("h")) { // toughness
									try {
										ptstr = reader.nextString();
										try {
											c.toughness = Integer.parseInt(ptstr);
										}
										catch (NumberFormatException e) {
											if (ptstr.equals("*")) {
												c.toughness = CardDbAdapter.STAR;
											}
											else if (ptstr.equals("1+*")) {
												c.toughness = CardDbAdapter.ONEPLUSSTAR;
											}
											else if (ptstr.equals("2+*")) {
												c.toughness = CardDbAdapter.TWOPLUSSTAR;
											}
											else if (ptstr.equals("7-*")) {
												c.toughness = CardDbAdapter.SEVENMINUSSTAR;
											}
											else if (ptstr.equals("*{^2}")) {
												c.toughness = CardDbAdapter.STARSQUARED;
											}
											else if (ptstr.equals("{1/2}")) {
												c.toughness = 0.5f;
											}
											else if (ptstr.equals("1{1/2}")) {
												c.toughness = 1.5f;
											}
											else if (ptstr.equals("2{1/2}")) {
												c.toughness = 2.5f;
											}
											else if (ptstr.equals("3{1/2}")) {
												c.toughness = 3.5f;
											}
										}
									}
									catch (Exception e) {
										reader.skipValue();
									}
								}
								else if (s2.equalsIgnoreCase("i")) { // loyalty
									try {
										c.loyalty = reader.nextInt();
									}
									catch (Exception e) {
										reader.skipValue();
									}
								}
								else if (s2.equalsIgnoreCase("j")) { // ability
									c.ability = reader.nextString();
								}
								else if (s2.equalsIgnoreCase("k")) { // flavor
									c.flavor = reader.nextString();
								}
								else if (s2.equalsIgnoreCase("l")) { // artist
									c.artist = reader.nextString();
								}
								else if (s2.equalsIgnoreCase("m")) { // number
									try {
										c.number = reader.nextString();
									}
									catch (Exception e) {
										reader.skipValue();
									}
								}
								else if (s2.equalsIgnoreCase("n")) { // color
									c.color = reader.nextString();
								}
								else if (s2.equalsIgnoreCase("x")) { // multiverse id
									c.multiverse_id = reader.nextInt();
								}
							}
							mDbHelper.createCard(c);
							mMain.cardAdded();
							reader.endObject();
						}
						reader.endArray();
					}
				}
				reader.endObject();
			}
			if (s.equalsIgnoreCase("w")) { // num_cards
				mMain.setNumCards(reader.nextInt());
			}
		}
		reader.endObject();
		return;
	}

	public static ArrayList<String[]> readUpdateJsonStream(MainActivity mMain) {
		ArrayList<String[]> patchInfo = new ArrayList<String[]>();
		URL update;
		String label;
		String date = null;
		String label2;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mMain);

		try {
			update = new URL("http://members.cox.net/aefeinstein/patches.json");
			InputStreamReader isr = new InputStreamReader(update.openStream(), "ISO-8859-1");
			JsonReader reader = new JsonReader(isr);

			reader.beginObject();
			while (reader.hasNext()) {
				label = reader.nextName();

				if (label.equals("Date")) {
					String lastUpdate = settings.getString("lastUpdate", "");
					date = reader.nextString();
					if (lastUpdate.equals(date)) {
						return null;
					}
				}
				else if (label.equals("Patches")) {
					reader.beginArray();
					while (reader.hasNext()) {
						reader.beginObject();
						String[] setdata = new String[3];
						while (reader.hasNext()) {
							label2 = reader.nextName();
							if (label2.equals("Name")) {
								setdata[0] = reader.nextString();
							}
							else if (label2.equals("URL")) {
								setdata[1] = reader.nextString();
							}
							else if (label2.equals("Code")) {
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

	public static void readLegalityJsonStream(InputStream in, CardDbAdapter cda, SharedPreferences settings)
			throws IOException {

		CardDbAdapter mDbHelper;
		String date = null;
		String legalSet;
		String bannedCard;
		String restrictedCard;
		String formatName;
		String jsonArrayName;
		String jsonTopLevelName;

		JsonReader reader = new JsonReader(new InputStreamReader(in, "ISO-8859-1"));

		mDbHelper = cda;

		reader.beginObject();
		while (reader.hasNext()) {

			jsonTopLevelName = reader.nextName();
			if (jsonTopLevelName.equalsIgnoreCase("Date")) {
				date = reader.nextString();

				// compare date, maybe return, update sharedprefs
				String spDate = settings.getString("date", null);
				if (spDate != null && spDate.equals(date)) {
					// TODO this always refreshes, shouldn't when checking OTA
					// return; // dates match, nothing new here.
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
	
	public static void readTCGNameJsonStream(MainActivity mMain, CardDbAdapter mDbHelper) {
		URL update;
		String label;
		String date = null;
		String label2;
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mMain);
		String name = null, code = null;
		
		try {
			update = new URL("http://members.cox.net/aefeinstein/TCGnames.json");
			InputStreamReader isr = new InputStreamReader(update.openStream(), "ISO-8859-1");
			JsonReader reader = new JsonReader(isr);

			reader.beginObject();
			while (reader.hasNext()) {
				label = reader.nextName();

				if (label.equals("Date")) {
					String lastUpdate = settings.getString("lastTCGNameUpdate", "");
					date = reader.nextString();
					if (lastUpdate.equals(date)) {
						return;
					}
				}
				else if (label.equals("Sets")) {
					reader.beginArray();
					while (reader.hasNext()) {
						reader.beginObject();
						while (reader.hasNext()) {
							label2 = reader.nextName();
							if (label2.equals("Code")) {
								code = reader.nextString();
							}
							else if (label2.equals("TCGName")) {
								name = reader.nextString();
							}
						}
						mDbHelper.createTCGname(name, code);
						reader.endObject();
					}
					reader.endArray();
				}
			}
			reader.endObject();
		}
		catch (MalformedURLException e) {
			return;
		}
		catch (IOException e) {
			return;
		}
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("lastTCGNameUpdate", date);
		editor.commit();
	}
}
