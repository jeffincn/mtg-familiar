package com.mtg.fam;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.util.Log;

import com.google.gson.stream.JsonReader;

public class JSONparser {
	private main					mMain;
	private CardDbAdapter	mDbHelper;
	private String	ptstr;

	public JSONparser(main m, CardDbAdapter cda) {
		mMain = m;
		mDbHelper = cda;
	}

	public void readJsonStream(InputStream in) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));

		String s;

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
				String s1;

				reader.beginObject();
				while (reader.hasNext()) {
					s1 = reader.nextName();
					if (s1.equalsIgnoreCase("b")) { // set
						String s2;
						MtgSet set;

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
				reader.endObject();
			}
			if (s.equalsIgnoreCase("p")) { // cards
				String s1;

				reader.beginObject();
				while (reader.hasNext()) {
					s1 = reader.nextName();
					if (s1.equalsIgnoreCase("o")) { // card
						String s2;
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
								if (s2.equalsIgnoreCase("b")) { // set
									c.set = reader.nextString();
								}
								if (s2.equalsIgnoreCase("c")) { // type
									c.type = reader.nextString();
								}
								if (s2.equalsIgnoreCase("d")) { // rarity
									c.rarity = reader.nextString().charAt(0);
								}
								if (s2.equalsIgnoreCase("e")) { // manacost
									c.manacost = reader.nextString();
								}
								if (s2.equalsIgnoreCase("f")) { // converted_manacost
									try {
										c.cmc = reader.nextInt();
									}
									catch (Exception e) {
										reader.skipValue();
									}

								}
								if (s2.equalsIgnoreCase("g")) { // power
									try {
										ptstr = reader.nextString();
										try{
											c.power = Integer.parseInt(ptstr);
										}
										catch(NumberFormatException e){
											if(ptstr.equals("*")){
												c.power = CardDbAdapter.STAR;
											}
											else if(ptstr.equals("1+*")){
												c.power = CardDbAdapter.ONEPLUSSTAR;
											}
											else if(ptstr.equals("2+*")){
												c.power = CardDbAdapter.TWOPLUSSTAR;
											}
											else if(ptstr.equals("7-*")){
												c.power = CardDbAdapter.SEVENMINUSSTAR;
											}
											else if(ptstr.length() > 0){
												Log.d("power", ptstr);
											}
										}
									}
									catch (Exception e) {
										reader.skipValue();
									}
								}
								if (s2.equalsIgnoreCase("h")) { // toughness
									try {
										ptstr = reader.nextString();
										try{
											c.toughness = Integer.parseInt(ptstr);
										}
										catch(NumberFormatException e){
											if(ptstr.equals("*")){
												c.toughness = CardDbAdapter.STAR;
											}
											else if(ptstr.equals("1+*")){
												c.toughness = CardDbAdapter.ONEPLUSSTAR;
											}
											else if(ptstr.equals("2+*")){
												c.toughness = CardDbAdapter.TWOPLUSSTAR;
											}
											else if(ptstr.equals("7-*")){
												c.toughness = CardDbAdapter.SEVENMINUSSTAR;
											}
											else if(ptstr.length() > 0){
												Log.d("toughness", ptstr);
											}
										}
									}
									catch (Exception e) {
										reader.skipValue();
									}
								}
								if (s2.equalsIgnoreCase("i")) { // loyalty
									try {
										c.loyalty = reader.nextInt();
									}
									catch (Exception e) {
										reader.skipValue();
									}
								}
								if (s2.equalsIgnoreCase("j")) { // ability
									c.ability = reader.nextString();
								}
								if (s2.equalsIgnoreCase("k")) { // flavor
									c.flavor = reader.nextString();
								}
								if (s2.equalsIgnoreCase("l")) { // artist
									c.artist = reader.nextString();
								}
								if (s2.equalsIgnoreCase("m")) { // number
									try {
										c.number = reader.nextInt();
									}
									catch (Exception e) {
										reader.skipValue();
									}
								}
								if (s2.equalsIgnoreCase("n")) { // color
									c.color = reader.nextString();
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
}
