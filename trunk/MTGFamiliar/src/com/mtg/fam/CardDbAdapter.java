/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/*
 * This code is modified from the excellent example code given by Google here:
 * http://developer.android.com/resources/tutorials/notepad/index.html
 * 
 */

package com.mtg.fam;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple Cards database access helper class. Defines the basic CRUD operations
 * and gives the ability to list all Cards as well as retrieve or modify a
 * specific Card.
 */
public class CardDbAdapter {

	public static final int AND = 0;
	public static final int OR = 1;

	private static final String DATABASE_NAME = "data";
	private static final String DATABASE_TABLE_CARDS = "cards";
	private static final String DATABASE_TABLE_SETS = "sets";
	private static final int DATABASE_VERSION = 8;

	public static final String KEY_ID = "_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_SET = "expansion";
	public static final String KEY_TYPE = "type";
	public static final String KEY_ABILITY = "cardtext";
	public static final String KEY_COLOR = "color";
	public static final String KEY_MANACOST = "manacost";
	public static final String KEY_CMC = "cmc";
	public static final String KEY_POWER = "power";
	public static final String KEY_TOUGHNESS = "toughness";
	public static final String KEY_RARITY = "rarity";
	public static final String KEY_LOYALTY = "loyalty";
	public static final String KEY_FLAVOR = "flavor";
	public static final String KEY_ARTIST = "artist";
	public static final String KEY_NUMBER = "number";
	
	public static final String KEY_CODE = "code";
	public static final String KEY_CODE_MTGI = "code_mtgi";

	private static final String TAG = "CardDbAdapter";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String DATABASE_CREATE_CARDS =
			"create table "	+ DATABASE_TABLE_CARDS + "(" +
			KEY_ID + " integer primary key autoincrement, " +
			KEY_NAME + " text not null, " +
			KEY_SET + " text not null, " +
			KEY_TYPE + " text not null, " +
			KEY_RARITY + " integer, " +
			KEY_MANACOST + " text, " +
			KEY_CMC + " integer not null, " +
			KEY_POWER + " text, " +
			KEY_TOUGHNESS + " text, " +
			KEY_LOYALTY + " integer, " +
			KEY_ABILITY	+ " text, " +
			KEY_FLAVOR	+ " text, " +
			KEY_ARTIST	+ " text, " +
			KEY_NUMBER	+ " integer, " +
			KEY_COLOR + " text not null);";

	private static final String DATABASE_CREATE_SETS =
			"create table "	+ DATABASE_TABLE_SETS + "(" +
			KEY_ID + " integer primary key autoincrement, " +
			KEY_NAME + " text not null, " +
			KEY_CODE + " text not null, " +
			KEY_CODE_MTGI + " text not null);";

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(DATABASE_CREATE_CARDS);
			db.execSQL(DATABASE_CREATE_SETS);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARDS);
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SETS);
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be opened/created
	 * 
	 * @param ctx
	 *          the Context within which to work
	 */
	public CardDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the Cards database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *           if the database could be neither opened or created
	 */
	public CardDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}
	
	public void dropCreateDB(){
		mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARDS);
		mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SETS);
		mDb.execSQL(DATABASE_CREATE_CARDS);
		mDb.execSQL(DATABASE_CREATE_SETS);
	}

	/**
	 * Create a new Card using the title and body provided. If the Card is
	 * successfully created return the new rowId for that Card, otherwise return a
	 * -1 to indicate failure.
	 * 
	 * @param title
	 *          the title of the Card
	 * @param body
	 *          the body of the Card
	 * @return rowId or -1 if failed
	 */
	public long createCard(String name, String set, String type, char rarity, String manacost,
			int cmc, String power, String toughness, int loyalty, String ability, String flavor,
			String artist, int number, String color) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_SET, set);
		initialValues.put(KEY_TYPE, type);
		initialValues.put(KEY_RARITY, (int)rarity);
		initialValues.put(KEY_MANACOST, manacost);
		initialValues.put(KEY_CMC, cmc);
		initialValues.put(KEY_POWER, power);
		initialValues.put(KEY_TOUGHNESS, toughness);
		initialValues.put(KEY_LOYALTY, loyalty);
		initialValues.put(KEY_ABILITY, ability);
		initialValues.put(KEY_FLAVOR, flavor);
		initialValues.put(KEY_ARTIST, artist);
		initialValues.put(KEY_NUMBER, number);
		initialValues.put(KEY_COLOR, color);

		return mDb.insert(DATABASE_TABLE_CARDS, null, initialValues);
	}
	
	public long createCard(MtgCard c) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_NAME, c.name);
		initialValues.put(KEY_SET, c.set);
		initialValues.put(KEY_TYPE, c.type);
		initialValues.put(KEY_RARITY, (int)c.rarity);
		initialValues.put(KEY_MANACOST, c.manacost);
		initialValues.put(KEY_CMC, c.cmc);
		initialValues.put(KEY_POWER, c.power+"");
		initialValues.put(KEY_TOUGHNESS, c.toughness+"");
		initialValues.put(KEY_LOYALTY, c.loyalty);
		initialValues.put(KEY_ABILITY, c.ability);
		initialValues.put(KEY_FLAVOR, c.flavor);
		initialValues.put(KEY_ARTIST, c.artist);
		initialValues.put(KEY_NUMBER, c.number);
		initialValues.put(KEY_COLOR, c.color);

		return mDb.insert(DATABASE_TABLE_CARDS, null, initialValues);
	}


	public long createSet(String name, String code, String code_mtgi) {
		ContentValues initialValues = new ContentValues();

		// initialValues.put(KEY_ID, 0);// wrong
		initialValues.put(KEY_CODE, code);
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_CODE_MTGI, code_mtgi);

		return mDb.insert(DATABASE_TABLE_SETS, null, initialValues);
	}

	public long createSet(MtgSet set) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_CODE, set.code);
		initialValues.put(KEY_NAME, set.name);
		initialValues.put(KEY_CODE_MTGI, set.code_magiccards);

		return mDb.insert(DATABASE_TABLE_SETS, null, initialValues);
	}

	/**
	 * Delete the Card with the given rowId
	 * 
	 * @param rowId
	 *          id of Card to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteCard(long id) {

		return mDb.delete(DATABASE_TABLE_CARDS, KEY_ID + "=" + id, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all Cards in the database
	 * 
	 * @return Cursor over all Cards
	 */
	public Cursor fetchAllSets() {

		return mDb.query(DATABASE_TABLE_SETS, new String[] { KEY_ID, KEY_NAME,
				KEY_CODE, KEY_CODE_MTGI }, null, null, null, null, KEY_NAME);
	}
	
	public String getCodeMtgi(String code){
		Cursor c = mDb.query(DATABASE_TABLE_SETS, new String[] {KEY_CODE_MTGI}, KEY_CODE + "=\"" + code+"\"", null, null, null, null);
		c.moveToFirst();
		return c.getString(c.getColumnIndex(KEY_CODE_MTGI));
	}

	/**
	 * Return a Cursor positioned at the Card that matches the given rowId
	 * 
	 * @param rowId
	 *          id of Card to retrieve
	 * @return Cursor positioned to matching Card, if found
	 * @throws SQLException
	 *           if Card could not be found/retrieved
	 */
	public Cursor fetchCard(long id) throws SQLException {

		Cursor mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] {
				KEY_NAME, KEY_SET, KEY_TYPE, KEY_RARITY, KEY_MANACOST, KEY_CMC,
				KEY_POWER, KEY_TOUGHNESS, KEY_LOYALTY, KEY_ABILITY, KEY_FLAVOR,
				KEY_ARTIST, KEY_NUMBER, KEY_COLOR}, KEY_ID + "=" + id,
				null, null, null, KEY_NAME, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * Update the Card using the details provided. The Card to be updated is
	 * specified using the rowId, and it is altered to use the title and body
	 * values passed in
	 * 
	 * @param rowId
	 *          id of Card to update
	 * @param title
	 *          value to set Card title to
	 * @param body
	 *          value to set Card body to
	 * @return true if the Card was successfully updated, false otherwise
	 */
	public boolean updateCard(long id, String name, String set, String type, char rarity, String manacost,
			int cmc, String power, String toughness, int loyalty, String ability, String flavor,
			String artist, int number, String color) {
		ContentValues args = new ContentValues();

		args.put(KEY_NAME, name);
		args.put(KEY_SET, set);
		args.put(KEY_TYPE, type);
		args.put(KEY_RARITY, (int)rarity);
		args.put(KEY_MANACOST, manacost);
		args.put(KEY_CMC, cmc);
		args.put(KEY_POWER, power);
		args.put(KEY_TOUGHNESS, toughness);
		args.put(KEY_LOYALTY, loyalty);
		args.put(KEY_ABILITY, ability);
		args.put(KEY_FLAVOR, flavor);
		args.put(KEY_ARTIST, artist);
		args.put(KEY_NUMBER, number);
		args.put(KEY_COLOR, color);

		return mDb.update(DATABASE_TABLE_CARDS, args, KEY_ID + "=" + id, null) > 0;
	}

	public Cursor Search(Context mCtx, String cardname, String cardtext, String cardtype,
			String color, int colorlogic, String sets, String pow_choice,
			String pow_logic, String tou_choice, String tou_logic, int cmc, String cmcLogic, String formats, String rarity) {
		Cursor mCursor = null;

		String statement = null;

		if (cardname != null) {
			statement = "(" + KEY_NAME + " LIKE '%" + cardname + "%')";
		}

		if (cardtext != null) {
			if (statement == null) {
				statement = "(" + KEY_ABILITY + " LIKE '%" + cardtext + "%')";
			}
			else {
				statement += " AND (" + KEY_ABILITY + " LIKE '%" + cardtext + "%')";
			}
		}

		if (cardtype != null) {
			String[] types = cardtype.split(" ");
			if(statement == null){
				statement = "(";
			}
			else{
				statement += " AND (";
			}
			
			boolean firstType = false;
			for(String s : types){
				if(firstType){
					statement += " AND ";
				}
				statement += KEY_TYPE + " LIKE '%" + s + "%'";
				firstType = true;
			}
			
			statement += ")";
		}

		if (!(color.equals("wubrgl") || (color.equals("WUBRGL") && colorlogic == 0))) {
			boolean firstprint = true;

			if (statement == null) {
				statement = "((";
			}
			else {
				statement += " AND ((";
			}

			// Can't contain these colors
			for (byte b : color.getBytes()) {
				char c = (char)b;
				
				if (c > 'a') {
					if(firstprint == false){
						statement += " AND ";
					}
					firstprint = false;
					
					if(c == 'l' || c == 'L'){
						statement += KEY_COLOR + " NOT GLOB '[CLA]'";
					}
					else{
						statement += KEY_COLOR + " NOT LIKE '%"
						+ Character.toUpperCase(c) + "%'";						
					}
				}
			}

			statement += ") AND (";
			firstprint = true;

			// Might contain these colors
			for (byte b : color.getBytes()) {
				char c = (char)b;
				if (c < 'a') {
					if (firstprint == false && colorlogic == 1) {
						statement += " AND ";
					}
					else if(firstprint == false){
						statement += " OR ";
					}
					firstprint = false;

					if(c == 'l' || c == 'L'){
						statement += KEY_COLOR + " GLOB '[CLA]'";
					}
					else{
						statement += KEY_COLOR + " LIKE '%" + c + "%'";
					}
					
				}
			}
			statement += "))";
		}

		if (sets != null) {
			if (statement == null) {
				statement = "(";
			}
			else {
				statement += " AND (";
			}

			boolean first = true;

			for (String s : sets.split("-")) {
				if (first) {
					first = false;
				}
				else {
					statement += " OR ";
				}
				statement += KEY_SET + " LIKE '%" + s + "%'";
			}

			statement += ")";
		}
		
		// do something with string formats
		Resources res = mCtx.getResources();
		String[] formatNames = res.getStringArray(R.array.format_names);
		
		if(formats != null){
			if (statement == null) {
				statement = "(";
			}
			else {
				statement += " AND (";
			}
			
			boolean first = true;
			
			if(formats.contains(formatNames[0])){
				if (first) {
					first = false;
				}
				else {
					statement += " AND ";
				}
				statement += "(" +
						KEY_SET + " LIKE '%SOM%' OR " + 
						KEY_SET + " LIKE '%MBS%' OR " + 
						KEY_SET + " LIKE '%NPH%')";
			}
			if(formats.contains(formatNames[1])){
				if (first) {
					first = false;
				}
				else {
					statement += " AND ";
				}
				statement += "(" +
						KEY_SET + " LIKE '%ZEN%' OR " +
						KEY_SET + " LIKE '%WWK%' OR " + 
						KEY_SET + " LIKE '%ROE%' OR " + 
						KEY_SET + " LIKE '%SOM%' OR " + 
						KEY_SET + " LIKE '%MBS%' OR " + 
						KEY_SET + " LIKE '%NPH%' OR " + 
						KEY_SET + " LIKE '%M11%' OR " +
						KEY_SET + " LIKE '%M12%')";
			}
			if(formats.contains(formatNames[2])){
				if (first) {
					first = false;
				}
				else {
					statement += " AND ";
				}
				statement += "(" +
						KEY_SET + " LIKE '%LRW%' OR " +
						KEY_SET + " LIKE '%MOR%' OR " +
						KEY_SET + " LIKE '%SHM%' OR " +
						KEY_SET + " LIKE '%EVE%' OR " +
						KEY_SET + " LIKE '%SOA%' OR " +
						KEY_SET + " LIKE '%CFX%' OR " +
						KEY_SET + " LIKE '%ARB%' OR " +
						KEY_SET + " LIKE '%ZEN%' OR " +
						KEY_SET + " LIKE '%WWK%' OR " + 
						KEY_SET + " LIKE '%ROE%' OR " + 
						KEY_SET + " LIKE '%SOM%' OR " + 
						KEY_SET + " LIKE '%MBS%' OR " + 
						KEY_SET + " LIKE '%NPH%' OR " + 
						KEY_SET + " LIKE '%M10%' OR " +
						KEY_SET + " LIKE '%M11%' OR " +
						KEY_SET + " LIKE '%M12%')";
			}
			if(formats.contains(formatNames[3])){
				// Legacy, banlist?
			}
			if(formats.contains(formatNames[2])){
				// Vintage, banlist?
			}
			statement += ")";
		}

		if (pow_logic.equals("<") || pow_logic.equals(">")) {
			boolean cont = true;
			boolean firstprint = false;
			String powersNeg = "-[";
			String powers1 = "[";
			String powers2 = "1[";

			try {
				if (pow_logic.equals("<")) {
					for (int i = -1; i < 0 && i < Integer.parseInt(pow_choice) && i < 10; i++) {
						powersNeg += -i;
					}
					powersNeg += "]";

					for (int i = 0; i < Integer.parseInt(pow_choice) && i < 10; i++) {
						powers1 += i;
					}
					powers1 += "]";

					powers2 = "1[";
					for (int i = 0; i < (Integer.parseInt(pow_choice) - 10); i++) {
						powers2 += i;
					}
					powers2 += "]";
				}
				else if (pow_logic.equals(">")) {
					for (int i = Integer.parseInt(pow_choice); i < 0; i++) {
						powersNeg += -i;
					}
					powersNeg += "]";

					for (int i = Integer.parseInt(pow_choice) + 1; i < 10; i++) {
						powers1 += i;
					}
					powers1 += "]";

					powers2 = "1[";
					for (int i = Integer.parseInt(pow_choice) + 1; i < 16; i++) {
						if (i > 9) {
							powers2 += (i - 10);
						}
					}
					powers2 += "]";
				}
			}
			catch (NumberFormatException e) {
				cont = false;
			}

			if (cont) {
				if (statement == null) {
					statement = "(";
				}
				else {
					statement += " AND (";
				}

				if (!powersNeg.equals("-[]")) {
					statement += KEY_POWER + " GLOB '" + powersNeg + "'";
					firstprint = true;
				}
				if (!powers1.equals("[]")) {
					if (!firstprint) {
						firstprint = true;
					}
					else {
						statement += " OR ";
					}
					statement += KEY_POWER + " GLOB '" + powers1 + "'";
				}
				if (!powers2.equals("1[]")) {
					if (firstprint) {
						statement += " OR ";
					}
					statement += KEY_POWER + " GLOB '" + powers2 + "'";
				}
				statement += ")";
			}
		}
		else if (pow_logic.equals("=")) {
			if (statement == null) {
				statement = "(" + KEY_POWER + " GLOB '" + pow_choice + "')";
			}
			else {
				statement += " AND (" + KEY_POWER + " GLOB '" + pow_choice + "')";
			}
		}

		if (tou_logic.equals("<") || tou_logic.equals(">")) {
			boolean cont = true;
			boolean firstprint = false;
			String toughnessNeg = "-[";
			String toughness1 = "[";
			String toughness2 = "1[";

			try {
				if (tou_logic.equals("<")) {
					for (int i = -1; i < 0 && i < Integer.parseInt(tou_choice) && i < 10; i++) {
						toughnessNeg += -i;
					}
					toughnessNeg += "]";

					for (int i = 0; i < Integer.parseInt(tou_choice) && i < 10; i++) {
						toughness1 += i;
					}
					toughness1 += "]";

					toughness2 = "1[";
					for (int i = 0; i < (Integer.parseInt(tou_choice) - 10); i++) {
						toughness2 += i;
					}
					toughness2 += "]";
				}
				else if (tou_logic.equals(">")) {
					for (int i = Integer.parseInt(tou_choice); i < 0; i++) {
						toughnessNeg += -i;
					}
					toughnessNeg += "]";

					for (int i = Integer.parseInt(tou_choice) + 1; i < 10; i++) {
						toughness1 += i;
					}
					toughness1 += "]";

					toughness2 = "1[";
					for (int i = Integer.parseInt(tou_choice) + 1; i < 16; i++) {
						if (i > 9) {
							toughness2 += (i - 10);
						}
					}
					toughness2 += "]";
				}
			}
			catch (NumberFormatException e) {
				cont = false;
			}

			if (cont) {
				if (statement == null) {
					statement = "(";
				}
				else {
					statement += " AND (";
				}

				if (!toughnessNeg.equals("-[]")) {
					statement += KEY_TOUGHNESS + " GLOB '" + toughnessNeg + "'";
					firstprint = true;
				}
				if (!toughness1.equals("[]")) {
					if (!firstprint) {
						firstprint = true;
					}
					else {
						statement += " OR ";
					}
					statement += KEY_TOUGHNESS + " GLOB '" + toughness1 + "'";
				}
				if (!toughness2.equals("1[]")) {
					if (firstprint) {
						statement += " OR ";
					}
					statement += KEY_TOUGHNESS + " GLOB '" + toughness2 + "'";
				}
				statement += ")";
			}
		}
		else if (tou_logic.equals("=")) {
			if (statement == null) {
				statement = "(" + KEY_TOUGHNESS + " GLOB '" + tou_choice + "')";
			}
			else {
				statement += " AND (" + KEY_TOUGHNESS + " GLOB '" + tou_choice + "')";
			}
		}

		if(cmc != -1){
			if (statement == null){
				statement = "(";
			}
			else{
				statement += " AND (";
			}
			
			statement += KEY_CMC + " " + cmcLogic + " " + cmc +")";
		}
		
		if(rarity != null){
			if (statement == null){
				statement = "(";
			}
			else{
				statement += " AND (";
			}
			
			boolean firstprint = false;
			for (int i=0; i < rarity.length(); i++){
				if(!firstprint){
					statement += KEY_RARITY + " = " + (int)rarity.toUpperCase().charAt(i) + "";
					firstprint = true;
				}
				else{
					statement += " OR " + KEY_RARITY + " = " + (int)rarity.toUpperCase().charAt(i) + "";					
				}
			}
			statement += ")";
		}

		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_ID,
					KEY_NAME, KEY_SET}, statement, null, null, null, KEY_NAME, null);
		}
		catch (SQLiteException e) {
			Log.v("tag", e.toString());
		}
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
}
