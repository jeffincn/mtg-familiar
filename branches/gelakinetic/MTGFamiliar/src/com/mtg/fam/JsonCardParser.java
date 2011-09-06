package com.mtg.fam;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class JsonCardParser {
	
	public static void readJsonStream(InputStream in, main mMain, CardDbAdapter mDbHelper) throws IOException {
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
						if(jt.equals(JsonToken.BEGIN_OBJECT)){
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
						else if(jt.equals(JsonToken.BEGIN_ARRAY)){
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
											else if(ptstr.equals("*{^2}")){
												c.power = CardDbAdapter.STARSQUARED;
											}
											else if(ptstr.equals("{1/2}")){
												c.power = 0.5f;
											}
											else if(ptstr.equals("1{1/2}")){
												c.power = 1.5f;
											}
											else if(ptstr.equals("2{1/2}")){
												c.power = 2.5f;
											}
											else if(ptstr.equals("3{1/2}")){
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
											else if(ptstr.equals("*{^2}")){
												c.toughness = CardDbAdapter.STARSQUARED;
											}
											else if(ptstr.equals("{1/2}")){
												c.toughness = 0.5f;
											}
											else if(ptstr.equals("1{1/2}")){
												c.toughness = 1.5f;
											}
											else if(ptstr.equals("2{1/2}")){
												c.toughness = 2.5f;
											}
											else if(ptstr.equals("3{1/2}")){
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
										c.number = reader.nextInt();
									}
									catch (Exception e) {
										reader.skipValue();
									}
								}
								else if (s2.equalsIgnoreCase("n")) { // color
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
