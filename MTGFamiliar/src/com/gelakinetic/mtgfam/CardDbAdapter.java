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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Simple Cards database access helper class. Defines the basic CRUD operations
 * and gives the ability to list all Cards as well as retrieve or modify a
 * specific Card.
 */
public class CardDbAdapter {

	public static final int			STAR										= -1000;
	public static final int			ONEPLUSSTAR							= -1001;
	public static final int			TWOPLUSSTAR							= -1002;
	public static final int			SEVENMINUSSTAR					= -1003;
	public static final int			STARSQUARED							= -1004;
	public static final int			NOONECARES							= -1005;

	public static final int			AND											= 0;
	public static final int			OR											= 1;

	private static final String	DATABASE_NAME						= "data";
	private static final String	DATABASE_TABLE_CARDS		= "cards";
	private static final String	DATABASE_TABLE_FORMATS	= "formats";
	private static final String	DATABASE_TABLE_SETS			= "sets";

	private static final int		DATABASE_VERSION				= 11;

	public static final String	KEY_ID									= "_id";
	public static final String	KEY_NAME								= "name";
	public static final String	KEY_SET									= "expansion";
	public static final String	KEY_TYPE								= "type";
	public static final String	KEY_ABILITY							= "cardtext";
	public static final String	KEY_COLOR								= "color";
	public static final String	KEY_MANACOST						= "manacost";
	public static final String	KEY_CMC									= "cmc";
	public static final String	KEY_POWER								= "power";
	public static final String	KEY_TOUGHNESS						= "toughness";
	public static final String	KEY_RARITY							= "rarity";
	public static final String	KEY_LOYALTY							= "loyalty";
	public static final String	KEY_FLAVOR							= "flavor";
	public static final String	KEY_ARTIST							= "artist";
	public static final String	KEY_NUMBER							= "number";
	public static final String	KEY_MULTIVERSEID				= "multiverseID";

	public static final String	KEY_CODE								= "code";
	public static final String	KEY_CODE_MTGI						= "code_mtgi";
	public static final String	KEY_NAME_TCGPLAYER			= "name_tcgplayer";

	private static final String	SET_POSTIFX							= "_SET";
	private static final String	BAN_POSTIFX							= "_BAN";
	private static final String	RESTRICT_POSTIFX				= "_RESTRICT";

	private DatabaseHelper			mDbHelper;
	private SQLiteDatabase			mDb;

	private static final String	DATABASE_CREATE_CARDS		= "create table " + DATABASE_TABLE_CARDS + "(" + KEY_ID
																													+ " integer primary key autoincrement, " + KEY_NAME
																													+ " text not null, " + KEY_SET + " text not null, "
																													+ KEY_TYPE + " text not null, " + KEY_RARITY + " integer, "
																													+ KEY_MANACOST + " text, " + KEY_CMC + " integer not null, "
																													+ KEY_POWER + " real, " + KEY_TOUGHNESS + " real, "
																													+ KEY_LOYALTY + " integer, " + KEY_ABILITY + " text, "
																													+ KEY_FLAVOR + " text, " + KEY_ARTIST + " text, "
																													+ KEY_NUMBER + " text, " + KEY_MULTIVERSEID
																													+ " integer not null, " + KEY_COLOR + " text not null);";

	private static final String	DATABASE_CREATE_SETS		= "create table " + DATABASE_TABLE_SETS + "(" + KEY_ID
																													+ " integer primary key autoincrement, " + KEY_NAME
																													+ " text not null, " + KEY_CODE + " text not null unique, "
																													+ KEY_CODE_MTGI + " text not null, " + KEY_NAME_TCGPLAYER
																													+ " text);";

	private static final String	DATABASE_CREATE_FORMATS	= "create table " + DATABASE_TABLE_FORMATS + "(" + KEY_ID
																													+ " integer primary key autoincrement, " + KEY_NAME
																													+ " text not null);";

	private final Context				mCtx;

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
			// Log.w(TAG, "Upgrading database from version " + oldVersion + " to " +
			// newVersion + ", which will destroy all old data");
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

	public void dropCreateDB() {
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
	public long createCard(String name, String set, String type, char rarity, String manacost, int cmc, float power,
			float toughness, int loyalty, String ability, String flavor, String artist, String number, String color, int m_id) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_SET, set);
		initialValues.put(KEY_TYPE, type);
		initialValues.put(KEY_RARITY, (int) rarity);
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
		initialValues.put(KEY_MULTIVERSEID, m_id);

		return mDb.insert(DATABASE_TABLE_CARDS, null, initialValues);
	}

	public long createCard(MtgCard c) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_NAME, c.name);
		initialValues.put(KEY_SET, c.set);
		initialValues.put(KEY_TYPE, c.type);
		initialValues.put(KEY_RARITY, (int) c.rarity);
		initialValues.put(KEY_MANACOST, c.manacost);
		initialValues.put(KEY_CMC, c.cmc);
		initialValues.put(KEY_POWER, c.power);
		initialValues.put(KEY_TOUGHNESS, c.toughness);
		initialValues.put(KEY_LOYALTY, c.loyalty);
		initialValues.put(KEY_ABILITY, c.ability);
		initialValues.put(KEY_FLAVOR, c.flavor);
		initialValues.put(KEY_ARTIST, c.artist);
		initialValues.put(KEY_NUMBER, c.number);
		initialValues.put(KEY_COLOR, c.color);
		initialValues.put(KEY_MULTIVERSEID, c.multiverse_id);

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

	public boolean addTCGname(String name, String code) {
		ContentValues args = new ContentValues();

		args.put(KEY_NAME_TCGPLAYER, name);

		return mDb.update(DATABASE_TABLE_SETS, args, KEY_CODE + " = '" + code + "'", null) > 0;
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

		return mDb.query(DATABASE_TABLE_SETS, new String[] { KEY_ID, KEY_NAME, KEY_CODE, KEY_CODE_MTGI }, null, null, null,
				null, KEY_NAME);
	}

	public boolean doesSetExist(String code) {

		String statement = "(" + KEY_CODE + " LIKE '%" + code + "%')";

		Cursor c = mDb.query(true, DATABASE_TABLE_SETS, new String[] { KEY_ID }, statement, null, null, null, KEY_NAME,
				null);

		if (c.getCount() > 0) {
			return true;
		}
		return false;
	}

	public String getCodeMtgi(String code) {
		Cursor c = mDb.query(DATABASE_TABLE_SETS, new String[] { KEY_CODE_MTGI }, KEY_CODE + "=\"" + code + "\"", null,
				null, null, null);
		c.moveToFirst();
		String retval = c.getString(c.getColumnIndex(KEY_CODE_MTGI));
		c.deactivate();
		c.close();
		return retval;
	}

	public String getFullSetName(String code) {
		Cursor c = mDb.query(DATABASE_TABLE_SETS, new String[] { KEY_NAME }, KEY_CODE + "=\"" + code + "\"", null, null,
				null, null);
		c.moveToFirst();
		String retval = c.getString(c.getColumnIndex(KEY_NAME));
		c.deactivate();
		c.close();
		return retval;
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

		Cursor mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_ID, KEY_NAME, KEY_SET, KEY_TYPE, KEY_RARITY,
				KEY_MANACOST, KEY_CMC, KEY_POWER, KEY_TOUGHNESS, KEY_LOYALTY, KEY_ABILITY, KEY_FLAVOR, KEY_ARTIST, KEY_NUMBER,
				KEY_COLOR, KEY_MULTIVERSEID }, KEY_ID + "=" + id, null, null, null, KEY_NAME, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

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
	public Cursor fetchCardByName(String name) throws SQLException {
		name = name.replace("'", "''");
		String statement = "(" + KEY_NAME + " = '" + name + "')";
		Cursor mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_NAME, KEY_SET, KEY_TYPE, KEY_RARITY,
				KEY_MANACOST, KEY_CMC, KEY_POWER, KEY_TOUGHNESS, KEY_LOYALTY, KEY_ABILITY, KEY_FLAVOR, KEY_ARTIST, KEY_NUMBER,
				KEY_COLOR, KEY_MULTIVERSEID }, statement, null, null, null, KEY_NAME, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public long fetchIdByName(String name) throws SQLException {
		name = name.replace("'", "''");
		String statement = "(" + KEY_NAME + " = '" + name + "')";
		Cursor mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_ID }, statement, null, null, null,
				KEY_NAME, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
			return mCursor.getLong(mCursor.getColumnIndex(CardDbAdapter.KEY_ID));
		}
		return -1;
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
	public boolean updateCard(long id, String name, String set, String type, char rarity, String manacost, int cmc,
			float power, float toughness, int loyalty, String ability, String flavor, String artist, String number,
			String color, int m_id) {
		ContentValues args = new ContentValues();

		args.put(KEY_NAME, name);
		args.put(KEY_SET, set);
		args.put(KEY_TYPE, type);
		args.put(KEY_RARITY, (int) rarity);
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
		args.put(KEY_MULTIVERSEID, m_id);

		return mDb.update(DATABASE_TABLE_CARDS, args, KEY_ID + "=" + id, null) > 0;
	}

	public Cursor Search(String cardname, String cardtext, String cardtype, String color, int colorlogic, String sets,
			float pow_choice, String pow_logic, float tou_choice, String tou_logic, int cmc, String cmcLogic, String formats,
			String rarity, String flavor, String artist, boolean backface, String[] returnTypes) {
		Cursor mCursor = null;

		if (cardname != null)
			cardname = cardname.replace("'", "''");
		if (cardtext != null)
			cardtext = cardtext.replace("'", "''");
		if (cardtype != null)
			cardtype = cardtype.replace("'", "''");
		if (flavor != null)
			flavor = flavor.replace("'", "''");
		if (artist != null)
			artist = artist.replace("'", "''");

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
			if (statement == null) {
				statement = "(";
			}
			else {
				statement += " AND (";
			}

			boolean firstType = false;
			for (String s : types) {
				if (firstType) {
					statement += " AND ";
				}
				statement += KEY_TYPE + " LIKE '%" + s + "%'";
				firstType = true;
			}

			statement += ")";
		}

		if (flavor != null) {
			if (statement == null) {
				statement = "(" + KEY_FLAVOR + " LIKE '%" + flavor + "%')";
			}
			else {
				statement += " AND (" + KEY_FLAVOR + " LIKE '%" + flavor + "%')";
			}
		}

		if (artist != null) {
			if (statement == null) {
				statement = "(" + KEY_ARTIST + " LIKE '%" + artist + "%')";
			}
			else {
				statement += " AND (" + KEY_ARTIST + " LIKE '%" + artist + "%')";
			}
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
				char c = (char) b;

				if (c > 'a') {
					if (firstprint == false) {
						statement += " AND ";
					}
					firstprint = false;

					if (c == 'l' || c == 'L') {
						statement += KEY_COLOR + " NOT GLOB '[CLA]'";
					}
					else {
						statement += KEY_COLOR + " NOT LIKE '%" + Character.toUpperCase(c) + "%'";
					}
				}
			}

			statement += ") AND (";
			firstprint = true;

			// Might contain these colors
			for (byte b : color.getBytes()) {
				char c = (char) b;
				if (c < 'a') {
					if (firstprint == false && colorlogic == 1) {
						statement += " AND ";
					}
					else if (firstprint == false) {
						statement += " OR ";
					}
					firstprint = false;

					if (c == 'l' || c == 'L') {
						statement += KEY_COLOR + " GLOB '[CLA]'";
					}
					else {
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
				statement += KEY_SET + "=\"" + s + "\"";
			}

			statement += ")";
		}

		if (pow_choice != NOONECARES) {
			if (statement == null) {
				statement = "(";
			}
			else {
				statement += " AND (";
			}

			if (pow_choice > STAR) {
				statement += KEY_POWER + " " + pow_logic + " " + pow_choice;
				if (pow_logic.equals("<")) {
					statement += " AND " + KEY_POWER + " > " + STAR;
				}
			}
			else if (pow_logic.equals("=")) {
				statement += KEY_POWER + " " + pow_logic + " " + pow_choice;
			}
			statement += ")";
		}

		if (tou_choice != NOONECARES) {
			if (statement == null) {
				statement = "(";
			}
			else {
				statement += " AND (";
			}

			if (tou_choice > STAR) {
				statement += KEY_TOUGHNESS + " " + tou_logic + " " + tou_choice;
				if (tou_logic.equals("<")) {
					statement += " AND " + KEY_TOUGHNESS + " > " + STAR;
				}
			}
			else if (tou_logic.equals("=")) {
				statement += KEY_TOUGHNESS + " " + tou_logic + " " + tou_choice;
			}
			statement += ")";
		}

		if (cmc != -1) {
			if (statement == null) {
				statement = "(";
			}
			else {
				statement += " AND (";
			}

			statement += KEY_CMC + " " + cmcLogic + " " + cmc + ")";
		}

		if (rarity != null) {
			if (statement == null) {
				statement = "(";
			}
			else {
				statement += " AND (";
			}

			boolean firstprint = false;
			for (int i = 0; i < rarity.length(); i++) {
				if (!firstprint) {
					statement += KEY_RARITY + " = " + (int) rarity.toUpperCase().charAt(i) + "";
					firstprint = true;
				}
				else {
					statement += " OR " + KEY_RARITY + " = " + (int) rarity.toUpperCase().charAt(i) + "";
				}
			}
			statement += ")";
		}

		if (formats != null) {

			Cursor c = fetchAllFormats();
			String[] formatNames = new String[c.getCount()];
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++) {
				formatNames[i] = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
				c.moveToNext();
			}
			c.deactivate();
			c.close();

			if (statement == null) {
				statement = "(";
			}
			else {
				statement += " AND (";
			}

			boolean first = true;

			for (int i = 0; i < formatNames.length; i++) {
				if (formats.contains(formatNames[i])) {
					if (first) {
						first = false;
					}
					else {
						statement += " AND ";
					}

					// get all sets in the format
					Cursor cSets = mDb.query(formatNames[i] + SET_POSTIFX, new String[] { KEY_ID, KEY_NAME, }, null, null, null,
							null, KEY_NAME);

					if (cSets.getCount() != 0) {
						statement += "EXISTS (SELECT * FROM " + formatNames[i] + SET_POSTIFX + " WHERE " + DATABASE_TABLE_CARDS
								+ "." + KEY_SET + " = " + formatNames[i] + SET_POSTIFX + "." + KEY_NAME + ")";

						statement += " AND NOT EXISTS (SELECT * FROM " + formatNames[i] + BAN_POSTIFX + " WHERE "
								+ DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + formatNames[i] + BAN_POSTIFX + "." + KEY_NAME + ")";
					}
					else {
						statement += "NOT EXISTS (SELECT * FROM " + formatNames[i] + BAN_POSTIFX + " WHERE " + DATABASE_TABLE_CARDS
								+ "." + KEY_NAME + " = " + formatNames[i] + BAN_POSTIFX + "." + KEY_NAME + ")";
					}

					if (cSets != null) {
						cSets.deactivate();
						cSets.close();
					}
				}
			}
			statement += ")";
		}

		if (!backface) {
			if (statement == null) {
				statement = "(" + KEY_NUMBER + " NOT LIKE '%b%')";
			}
			else {
				statement += " AND (" + KEY_NUMBER + " NOT LIKE '%b%')";
			}
		}

		if (statement == null) {
			return null;
		}

		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS, returnTypes, statement, null, null, null, KEY_NAME, null);
		}
		catch (SQLiteException e) {
			// Log.v("tag", e.toString());
		}
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public int getTransform(String set, String number) {
		Cursor mCursor = null;
		int ID;
		String statement = "(" + KEY_NUMBER + " = '" + number + "') AND (" + KEY_SET + " = '" + set + "')";
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_ID }, statement, null, null, null, KEY_ID,
					null);
			mCursor.moveToFirst();
			ID = mCursor.getInt(mCursor.getColumnIndex(KEY_ID));
		}
		catch (Exception e) {
			return -1;
		}
		return ID;
	}

	public String getTransformName(String set, String number) {
		Cursor mCursor = null;
		String name = null;
		String statement = "(" + KEY_NUMBER + " = '" + number + "') AND (" + KEY_SET + " = '" + set + "')";
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_NAME }, statement, null, null, null, KEY_NAME,
					null);
			mCursor.moveToFirst();
			name = mCursor.getString(mCursor.getColumnIndex(KEY_NAME));
		}
		catch (Exception e) {
			return null;
		}
		return name;
	}

	public void createFormatTable() {
		mDb.execSQL(DATABASE_CREATE_FORMATS);
	}

	public void createFormatSetTable(String format) {
		mDb.execSQL("create table " + format + SET_POSTIFX + "(" + KEY_ID + " integer primary key autoincrement, "
				+ KEY_NAME + " text not null);");
	}

	public void createFormatBanTable(String format) {
		mDb.execSQL("create table " + format + BAN_POSTIFX + "(" + KEY_ID + " integer primary key autoincrement, "
				+ KEY_NAME + " text not null);");
	}

	public void createFormatRestrictedTable(String format) {
		mDb.execSQL("create table " + format + RESTRICT_POSTIFX + "(" + KEY_ID + " integer primary key autoincrement, "
				+ KEY_NAME + " text not null);");
	}

	public void dropFormatTable() {
		String format;
		Cursor c = fetchAllFormats();
		if (c != null) {
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++) {
				format = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
				mDb.execSQL("DROP TABLE IF EXISTS " + format + "" + SET_POSTIFX);
				mDb.execSQL("DROP TABLE IF EXISTS " + format + "" + BAN_POSTIFX);
				mDb.execSQL("DROP TABLE IF EXISTS " + format + "" + RESTRICT_POSTIFX);
				c.moveToNext();
			}
			c.deactivate();
			c.close();
		}
		mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_FORMATS);
	}

	public long createFormat(String name) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		return mDb.insert(DATABASE_TABLE_FORMATS, null, initialValues);
	}

	public long createFormatSet(String format, String set) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, set);
		return mDb.insert(format + SET_POSTIFX, null, initialValues);
	}

	public long createFormatBan(String format, String set) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, set);
		return mDb.insert(format + BAN_POSTIFX, null, initialValues);
	}

	public long createFormatRestricted(String format, String set) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, set);
		return mDb.insert(format + RESTRICT_POSTIFX, null, initialValues);
	}

	public Cursor fetchAllFormats() {
		try {
			return mDb.query(DATABASE_TABLE_FORMATS, new String[] { KEY_ID, KEY_NAME, }, null, null, null, null, KEY_NAME);
		}
		catch (SQLiteException e) {
			return null;
		}
	}

	public static final int	LEGAL				= 0;
	public static final int	BANNED			= 1;
	public static final int	RESTRICTED	= 2;

	public int checkLegality(long mCardID, String name) {

		if (name != null)
			name = name.replace("'", "''");
		// get all sets in the format
		Cursor cSets = mDb.query(name + SET_POSTIFX, new String[] { KEY_ID, KEY_NAME, }, null, null, null, null, KEY_NAME);

		String banStatement = "(" + KEY_ID + "=" + mCardID + ")";
		String restrictStatement = "(" + KEY_ID + "=" + mCardID + ")";

		if (cSets.getCount() != 0) {
			banStatement += "AND EXISTS (SELECT * FROM " + name + SET_POSTIFX + " WHERE " + DATABASE_TABLE_CARDS + "."
					+ KEY_SET + " = " + name + SET_POSTIFX + "." + KEY_NAME + ")";

			banStatement += " AND NOT EXISTS (SELECT * FROM " + name + BAN_POSTIFX + " WHERE " + DATABASE_TABLE_CARDS + "."
					+ KEY_NAME + " = " + name + BAN_POSTIFX + "." + KEY_NAME + ")";

			restrictStatement += "AND EXISTS (SELECT * FROM " + name + SET_POSTIFX + " WHERE " + DATABASE_TABLE_CARDS + "."
					+ KEY_SET + " = " + name + SET_POSTIFX + "." + KEY_NAME + ")";

			restrictStatement += " AND NOT EXISTS (SELECT * FROM " + name + RESTRICT_POSTIFX + " WHERE "
					+ DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + name + RESTRICT_POSTIFX + "." + KEY_NAME + ")";
		}
		else {
			banStatement += "AND NOT EXISTS (SELECT * FROM " + name + BAN_POSTIFX + " WHERE " + DATABASE_TABLE_CARDS + "."
					+ KEY_NAME + " = " + name + BAN_POSTIFX + "." + KEY_NAME + ")";

			restrictStatement += "AND NOT EXISTS (SELECT * FROM " + name + RESTRICT_POSTIFX + " WHERE "
					+ DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + name + RESTRICT_POSTIFX + "." + KEY_NAME + ")";
		}

		Cursor banCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_ID, }, banStatement, null, null, null,
				KEY_ID, null);

		Cursor restrictCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_ID, }, restrictStatement, null,
				null, null, KEY_ID, null);

		if (banCursor.getCount() >= 1) {
			if (restrictCursor.getCount() >= 1) {
				cSets.deactivate();
				cSets.close();
				banCursor.deactivate();
				banCursor.close();
				restrictCursor.deactivate();
				restrictCursor.close();
				return LEGAL;
			}
			else {
				cSets.deactivate();
				cSets.close();
				banCursor.deactivate();
				banCursor.close();
				restrictCursor.deactivate();
				restrictCursor.close();
				return RESTRICTED;
			}
		}
		cSets.deactivate();
		cSets.close();
		banCursor.deactivate();
		banCursor.close();
		restrictCursor.deactivate();
		restrictCursor.close();
		return BANNED;
	}

	public String getTCGname(String setCode) {
		Cursor mCursor = null;
		String name;
		String statement = "(" + KEY_CODE + " = '" + setCode + "')";
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_SETS, new String[] { KEY_NAME_TCGPLAYER }, statement, null, null, null,
					KEY_NAME_TCGPLAYER, null);
			mCursor.moveToFirst();
			name = mCursor.getString(mCursor.getColumnIndex(KEY_NAME_TCGPLAYER));
		}
		catch (Exception e) {
			return null;
		}
		return name;
	}
}
