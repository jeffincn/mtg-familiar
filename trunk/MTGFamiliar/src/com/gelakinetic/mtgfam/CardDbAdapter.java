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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.widget.Toast;

/**
 * Simple Cards database access helper class. Defines the basic CRUD operations
 * and gives the ability to list all Cards as well as retrieve or modify a
 * specific Card.
 */
public class CardDbAdapter {

	public static final int				STAR													= -1000;
	public static final int				ONEPLUSSTAR										= -1001;
	public static final int				TWOPLUSSTAR										= -1002;
	public static final int				SEVENMINUSSTAR								= -1003;
	public static final int				STARSQUARED										= -1004;
	public static final int				NOONECARES										= -1005;

	public static final int				AND														= 0;
	public static final int				OR														= 1;

	public static final String		DATABASE_NAME									= "data";
	public static final String		DATABASE_TABLE_CARDS					= "cards";
	public static final String		DATABASE_TABLE_SETS						= "sets";
	public static final String		DATABASE_TABLE_FORMATS				= "formats";
	private static final String		DATABASE_TABLE_LEGAL_SETS			= "legal_sets";
	private static final String		DATABASE_TABLE_BANNED_CARDS		= "banned_cards";

	public static final int				DATABASE_VERSION							= 16;

	public static final String		KEY_ID												= "_id";
	public static final String		KEY_NAME											= SearchManager.SUGGEST_COLUMN_TEXT_1;							// "name";
	public static final String		KEY_SET												= "expansion";
	public static final String		KEY_TYPE											= "type";
	public static final String		KEY_ABILITY										= "cardtext";
	public static final String		KEY_COLOR											= "color";
	public static final String		KEY_MANACOST									= "manacost";
	public static final String		KEY_CMC												= "cmc";
	public static final String		KEY_POWER											= "power";
	public static final String		KEY_TOUGHNESS									= "toughness";
	public static final String		KEY_RARITY										= "rarity";
	public static final String		KEY_LOYALTY										= "loyalty";
	public static final String		KEY_FLAVOR										= "flavor";
	public static final String		KEY_ARTIST										= "artist";
	public static final String		KEY_NUMBER										= "number";
	public static final String		KEY_MULTIVERSEID							= "multiverseID";
	public static final String		KEY_RULINGS										= "rulings";

	public static final String[]	KEYS													= { KEY_ID, KEY_NAME, KEY_SET, KEY_TYPE, KEY_ABILITY,
			KEY_COLOR, KEY_MANACOST, KEY_CMC, KEY_POWER, KEY_TOUGHNESS, KEY_RARITY, KEY_LOYALTY, KEY_FLAVOR, KEY_ARTIST,
			KEY_NUMBER, KEY_MULTIVERSEID, KEY_RULINGS							};

	public static final String		KEY_CODE											= "code";
	public static final String		KEY_CODE_MTGI									= "code_mtgi";
	public static final String		KEY_NAME_TCGPLAYER						= "name_tcgplayer";
	public static final String		KEY_DATE											= "date";

	public static final String		KEY_FORMAT										= "format";
	public static final String		KEY_LEGALITY									= "legality";

	private DatabaseHelper				mDbHelper;
	private SQLiteDatabase				mDb;

	private static final String		DATABASE_CREATE_CARDS					= "create table " + DATABASE_TABLE_CARDS + "(" + KEY_ID
																																	+ " integer primary key autoincrement, " + KEY_NAME
																																	+ " text not null, " + KEY_SET + " text not null, "
																																	+ KEY_TYPE + " text not null, " + KEY_RARITY
																																	+ " integer, " + KEY_MANACOST + " text, " + KEY_CMC
																																	+ " integer not null, " + KEY_POWER + " real, "
																																	+ KEY_TOUGHNESS + " real, " + KEY_LOYALTY
																																	+ " integer, " + KEY_ABILITY + " text, " + KEY_FLAVOR
																																	+ " text, " + KEY_ARTIST + " text, " + KEY_NUMBER
																																	+ " text, " + KEY_MULTIVERSEID
																																	+ " integer not null, " + KEY_COLOR
																																	+ " text not null, " + KEY_RULINGS + " text);";

	private static final String		DATABASE_CREATE_SETS					= "create table " + DATABASE_TABLE_SETS + "(" + KEY_ID
																																	+ " integer primary key autoincrement, " + KEY_NAME
																																	+ " text not null, " + KEY_CODE
																																	+ " text not null unique, " + KEY_CODE_MTGI
																																	+ " text not null, " + KEY_NAME_TCGPLAYER + " text, "
																																	+ KEY_DATE + " integer);";

	private static final String		DATABASE_CREATE_FORMATS				= "create table " + DATABASE_TABLE_FORMATS + "(" + KEY_ID
																																	+ " integer primary key autoincrement, " + KEY_NAME
																																	+ " text not null);";

	private static final String		DATABASE_CREATE_LEGAL_SETS		= "create table " + DATABASE_TABLE_LEGAL_SETS + "("
																																	+ KEY_ID + " integer primary key autoincrement, "
																																	+ KEY_SET + " text not null, " + KEY_FORMAT
																																	+ " text not null);";

	private static final String		DATABASE_CREATE_BANNED_CARDS	= "create table " + DATABASE_TABLE_BANNED_CARDS + "("
																																	+ KEY_ID + " integer primary key autoincrement, "
																																	+ KEY_NAME + " text not null, " + KEY_LEGALITY
																																	+ " integer not null, " + KEY_FORMAT
																																	+ " text not null);";

	private final Context					mCtx;

	public static final String		EXCLUDE_TOKEN									= "!";
	public static final int				EXCLUDE_TOKEN_START						= 1;

	public static final int				MAY_INCLUDE_ANY_COLOR					= 0;
	public static final int				MUST_INCLUDE_ALL_COLOR				= 1;
	public static final int				NO_OTHER_COLOR								= 2;
	public static final int				EXACT_COLOR										= 3;

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
			System.out.print(oldVersion + " to " + newVersion);
			// Log.w(TAG, "Upgrading database from version " + oldVersion + " to " +
			// newVersion + ", which will destroy all old data");
			// db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARDS);
			// db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SETS);
			// onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be opened/created
	 * 
	 * @param ctx
	 *          the Context within which to work
	 */
	public CardDbAdapter(Context ctx) {

		if (CardDbAdapter.isDbOutOfDate(ctx)) {
			CardDbAdapter.copyDB(ctx);
		}

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
		mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_FORMATS);
		mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_LEGAL_SETS);
		mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_BANNED_CARDS);
		
		mDb.execSQL(DATABASE_CREATE_CARDS);
		mDb.execSQL(DATABASE_CREATE_SETS);
		mDb.execSQL(DATABASE_CREATE_FORMATS);
		mDb.execSQL(DATABASE_CREATE_LEGAL_SETS);
		mDb.execSQL(DATABASE_CREATE_BANNED_CARDS);
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

	public long createSet(String name, String code, String code_mtgi, long date) {
		ContentValues initialValues = new ContentValues();

		// initialValues.put(KEY_ID, 0);// wrong
		initialValues.put(KEY_CODE, code);
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_CODE_MTGI, code_mtgi);
		initialValues.put(KEY_DATE, date);

		return mDb.insert(DATABASE_TABLE_SETS, null, initialValues);
	}

	public long createSet(MtgSet set) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_CODE, set.code);
		initialValues.put(KEY_NAME, set.name);
		initialValues.put(KEY_CODE_MTGI, set.code_magiccards);
		initialValues.put(KEY_DATE, set.date);

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

		Cursor c = null;
		try {
			c = mDb.query(DATABASE_TABLE_SETS, new String[] { KEY_ID, KEY_NAME, KEY_CODE, KEY_CODE_MTGI }, null, null, null,
					null, KEY_DATE + " DESC");
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

		return c;
	}

	public boolean doesSetExist(String code) {

		String statement = "(" + KEY_CODE + " LIKE '%" + code + "%')";

		Cursor c = null;
		try {
			c = mDb.query(true, DATABASE_TABLE_SETS, new String[] { KEY_ID }, statement, null, null, null, KEY_NAME, null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

		if (c.getCount() > 0) {
			return true;
		}
		return false;
	}

	public String getCodeMtgi(String code) {
		Cursor c = null;
		try {
			c = mDb.query(DATABASE_TABLE_SETS, new String[] { KEY_CODE_MTGI }, KEY_CODE + "=\"" + code + "\"", null, null,
					null, null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

		c.moveToFirst();
		String retval = c.getString(c.getColumnIndex(KEY_CODE_MTGI));
		c.deactivate();
		c.close();
		return retval;
	}

	public String getFullSetName(String code) {
		Cursor c = null;
		try {
			c = mDb.query(DATABASE_TABLE_SETS, new String[] { KEY_NAME }, KEY_CODE + "=\"" + code + "\"", null, null, null,
					null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

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
	public Cursor fetchCard(long id, String[] columns) throws SQLException {

		if (columns == null) {
			columns = new String[] { KEY_ID, KEY_NAME, KEY_SET, KEY_TYPE, KEY_RARITY, KEY_MANACOST, KEY_CMC, KEY_POWER,
					KEY_TOUGHNESS, KEY_LOYALTY, KEY_ABILITY, KEY_FLAVOR, KEY_ARTIST, KEY_NUMBER, KEY_COLOR, KEY_MULTIVERSEID };
		}
		Cursor mCursor = null;
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS, columns, KEY_ID + "=" + id, null, null, null, KEY_NAME, null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

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
		String sql = "SELECT " + DATABASE_TABLE_CARDS + "." + KEY_ID + ", " + DATABASE_TABLE_CARDS + "." + KEY_NAME + ", " +
				DATABASE_TABLE_CARDS + "." + KEY_SET + ", " + DATABASE_TABLE_CARDS + "." + KEY_NUMBER + " FROM " + DATABASE_TABLE_CARDS +
				" JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET +
				" WHERE " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " = '" + name + "' ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC";
		Cursor mCursor = null;

		try {
			mCursor = mDb.rawQuery(sql, null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public long fetchIdByName(String name) throws SQLException {
		name = name.replace("'", "''");
		String statement = "(" + KEY_NAME + " = '" + name + "')";
		Cursor mCursor = null;
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_ID }, statement, null, null, null, KEY_NAME,
					null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

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

	public Cursor autoComplete(String cardname) {
		Cursor mCursor = null;

		if (cardname != null)
			cardname = cardname.replace("'", "''").trim();

		String sql = "SELECT MIN(" + KEY_ID + ") AS " + KEY_ID + ", " + KEY_NAME + " FROM " + DATABASE_TABLE_CARDS
				+ " WHERE " + KEY_NAME + " LIKE '" + cardname + "%' GROUP BY " + KEY_NAME + " ORDER BY " + KEY_NAME;
		try {
			mCursor = mDb.rawQuery(sql, null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

		if (mCursor != null) {
			mCursor.moveToFirst();
		}

		return mCursor;
	}

	public Cursor Search(String cardname, String cardtext, String cardtype, String color, int colorlogic, String sets,
			float pow_choice, String pow_logic, float tou_choice, String tou_logic, int cmc, String cmcLogic, String format,
			String rarity, String flavor, String artist, int type_logic, int text_logic, boolean backface,
			String[] returnTypes, boolean consolidate) {
		Cursor mCursor = null;

		if (cardname != null)
			cardname = cardname.replace("'", "''").trim();
		if (cardtext != null)
			cardtext = cardtext.replace("'", "''").trim();
		if (cardtype != null)
			cardtype = cardtype.replace("'", "''").trim();
		if (flavor != null)
			flavor = flavor.replace("'", "''").trim();
		if (artist != null)
			artist = artist.replace("'", "''").trim();

		String statement = " WHERE 1=1";

		if (cardname != null) {
			String[] nameParts = cardname.split(" ");
			for (String s : nameParts) {
				statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '%" + s + "%')";
			}
		}

		/*************************************************************************************/
		/**
		 * Reuben's version Differences: Original code is verbose only, but mine
		 * allows for matching exact text, all words, or just any one word.
		 */
		if (cardtext != null) {
			String[] cardTextParts = cardtext.split(" "); // Separate each individual

			/**
			 * The following switch statement tests to see which text search logic was
			 * chosen by the user. If they chose the first option (0), then look for
			 * cards with text that includes all words, but not necessarily the exact
			 * phrase. The second option (1) finds cards that have 1 or more of the
			 * chosen words in their text. The third option (2) searches for the exact
			 * phrase as entered by the user. The 'default' option is impossible via
			 * the way the code is written, but I believe it's also mandatory to
			 * include it in case someone else is perhaps fussing with the code and
			 * breaks it. The if statement at the end is theorhetically unnecessary,
			 * because once we've entered the current if statement, there is no way to
			 * NOT change the statement variable. However, you never really know who's
			 * going to break open your code and fuss around with it, so it's always
			 * good to leave some small safety measures.
			 */
			switch (text_logic) {
				case 0:
					for (String s : cardTextParts) {
						if (s.contains(EXCLUDE_TOKEN))
							statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " NOT LIKE '%"
									+ s.substring(EXCLUDE_TOKEN_START) + "%')";
						else
							statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " LIKE '%" + s + "%')";
					}
					break;
				case 1:
					boolean firstRun = true;
					for (String s : cardTextParts) {
						if (firstRun) {
							firstRun = false;
							if (s.contains(EXCLUDE_TOKEN))
								statement += " AND ((" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " NOT LIKE '%"
										+ s.substring(EXCLUDE_TOKEN_START) + "%')";
							else
								statement += " AND ((" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " LIKE '%" + s + "%')";
						}
						else {
							if (s.contains(EXCLUDE_TOKEN))
								statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " NOT LIKE '%"
										+ s.substring(EXCLUDE_TOKEN_START) + "%')";
							else
								statement += " OR (" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " LIKE '%" + s + "%')";
						}
					}
					statement += ")";
					break;
				case 2:
					statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_ABILITY + " LIKE '%" + cardtext + "%')";
					break;
				default:
					break;
			}
		}
		/** End Reuben's version */

		/**
		 * Reuben's version Differences: Original version only allowed for including
		 * all types, not any of the types or excluding the given types.
		 */

		String supertypes = null;
		String subtypes = null;

		if (cardtype != null && !cardtype.equals("-")) {
			boolean containsSupertype = true;
			if (cardtype.substring(0, 2).equals("- ")) {
				containsSupertype = false;
			}
			String[] split = cardtype.split(" - ");
			if (split.length >= 2) {
				supertypes = split[0].replace(" -", "");
				subtypes = split[1].replace(" -", "");
			}
			else if (containsSupertype) {
				supertypes = cardtype.replace(" -", "");
			}
			else {
				subtypes = cardtype.replace("- ", "");
			}
		}

		if (supertypes != null) {
			String[] supertypesParts = supertypes.split(" "); // Separate each
																												// individual

			switch (type_logic) {
				case 0:
					for (String s : supertypesParts) {
						if (s.contains(EXCLUDE_TOKEN))
							statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE '%" + s.substring(1) + "%')";
						else
							statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " LIKE '%" + s + "%')";
					}
					break;
				case 1:
					boolean firstRun = true;
					for (String s : supertypesParts) {
						if (firstRun) {
							firstRun = false;

							if (s.contains(EXCLUDE_TOKEN))
								statement += " AND ((" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE '%" + s.substring(1)
										+ "%')";
							else
								statement += " AND ((" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " LIKE '%" + s + "%')";
						}
						else if (s.contains(EXCLUDE_TOKEN))
							statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE '%" + s.substring(1) + "%')";
						else
							statement += " OR (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " LIKE '%" + s + "%')";
					}
					statement += ")";
					break;
				case 2:
					for (String s : supertypesParts) {
						statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE '%" + s + "%')";
					}
					break;
				default:
					break;
			}
		}

		if (subtypes != null) {
			String[] subtypesParts = subtypes.split(" "); // Separate each individual

			switch (type_logic) {
				case 0:
					for (String s : subtypesParts) {
						if (s.contains(EXCLUDE_TOKEN))
							statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE '%" + s.substring(1) + "%')";
						else
							statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " LIKE '%" + s + "%')";
					}
					break;
				case 1:
					boolean firstRun = true;
					for (String s : subtypesParts) {
						if (firstRun) {
							firstRun = false;
							if (s.contains(EXCLUDE_TOKEN))
								statement += " AND ((" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE '%" + s.substring(1)
										+ "%')";
							else
								statement += " AND ((" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " LIKE '%" + s + "%')";
						}
						else if (s.contains(EXCLUDE_TOKEN))
							statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE '%" + s.substring(1) + "%')";
						else
							statement += " OR (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " LIKE '%" + s + "%')";
					}
					statement += ")";
					break;
				case 2:
					for (String s : subtypesParts) {
						statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_TYPE + " NOT LIKE '%" + s + "%')";
					}
					break;
				default:
					break;
			}
		}
		/** End Reuben's version */
		/*************************************************************************************/

		if (flavor != null) {
			statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_FLAVOR + " LIKE '%" + flavor + "%')";
		}

		if (artist != null) {
			statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_ARTIST + " LIKE '%" + artist + "%')";
		}

		/*************************************************************************************/
		/**
		 * Code below added/modified by Reuben. Differences: Original version only
		 * had 'Any' and 'All' options and lacked 'Exclusive' and 'Exact' matching.
		 * In addition, original programming only provided exclusive results.
		 */
		if (!(color.equals("wubrgl") || (color.equals("WUBRGL") && colorlogic == 0))) {
			boolean firstPrint = true;

			// Can't contain these colors
			/**
			 * ...if the chosen color logic was exactly (2) or none (3) of the
			 * selected colors
			 */
			if (colorlogic > 1) // if colorlogic is 2 or 3 it will be greater than 1
			{
				statement += " AND ((";
				for (byte b : color.getBytes()) {
					char c = (char) b;

					if (c > 'a') {
						if (firstPrint)
							firstPrint = false;
						else
							statement += " AND ";

						if (c == 'l' || c == 'L')
							statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR + " NOT GLOB '[CLA]'";
						else
							statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR + " NOT LIKE '%" + Character.toUpperCase(c) + "%'";
					}
				}
				statement += ") AND (";
			}

			firstPrint = true;

			// Might contain these colors
			if (colorlogic < 2)
				statement += " AND (";

			for (byte b : color.getBytes()) {
				char c = (char) b;
				if (c < 'a') {
					if (firstPrint)
						firstPrint = false;
					else {
						if (colorlogic == 1 || colorlogic == 3)
							statement += " AND ";
						else
							statement += " OR ";
					}

					if (c == 'l' || c == 'L')
						statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR + " GLOB '[CLA]'";
					else
						statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR + " LIKE '%" + c + "%'";
				}
			}
			if (colorlogic > 1)
				statement += "))";
			else
				statement += ")";
		}
		/** End of addition */
		/*************************************************************************************/

		if (sets != null) {
			statement += " AND (";

			boolean first = true;

			for (String s : sets.split("-")) {
				if (first) {
					first = false;
				}
				else {
					statement += " OR ";
				}
				statement += DATABASE_TABLE_CARDS + "." + KEY_SET + " = '" + s + "'";
			}

			statement += ")";
		}

		if (pow_choice != NOONECARES) {
			statement += " AND (";

			if (pow_choice > STAR) {
				statement += DATABASE_TABLE_CARDS + "." + KEY_POWER + " " + pow_logic + " " + pow_choice;
				if (pow_logic.equals("<")) {
					statement += " AND " + DATABASE_TABLE_CARDS + "." + KEY_POWER + " > " + STAR;
				}
			}
			else if (pow_logic.equals("=")) {
				statement += DATABASE_TABLE_CARDS + "." + KEY_POWER + " " + pow_logic + " " + pow_choice;
			}
			statement += ")";
		}

		if (tou_choice != NOONECARES) {
			statement += " AND (";

			if (tou_choice > STAR) {
				statement += DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " " + tou_logic + " " + tou_choice;
				if (tou_logic.equals("<")) {
					statement += " AND " + DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " > " + STAR;
				}
			}
			else if (tou_logic.equals("=")) {
				statement += DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " " + tou_logic + " " + tou_choice;
			}
			statement += ")";
		}

		if (cmc != -1) {
			statement += " AND (";

			statement += DATABASE_TABLE_CARDS + "." + KEY_CMC + " " + cmcLogic + " " + cmc + ")";
		}

		if (rarity != null) {
			statement += " AND (";

			boolean firstPrint = true;
			for (int i = 0; i < rarity.length(); i++) {
				if (firstPrint) {
					firstPrint = false;
				}
				else {
					statement += " OR ";
				}
				statement += DATABASE_TABLE_CARDS + "." + KEY_RARITY + " = " + (int) rarity.toUpperCase().charAt(i) + "";
			}
			statement += ")";
		}

		String tbl = DATABASE_TABLE_CARDS;
		if (format != null) {
			if (!(format.equals("Legacy") || format.equals("Vintage"))) {
				tbl = "(" + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_LEGAL_SETS + " ON " + DATABASE_TABLE_CARDS + "."
						+ KEY_SET + "=" + DATABASE_TABLE_LEGAL_SETS + "." + KEY_SET + " AND " + DATABASE_TABLE_LEGAL_SETS + "."
						+ KEY_FORMAT + "='" + format + "')";
			}
			else {
				statement += "AND NOT " + KEY_SET + "= 'UNH' AND NOT " + KEY_SET + "= 'UG'";
			}
			statement += " AND NOT EXISTS (SELECT * FROM " + DATABASE_TABLE_BANNED_CARDS + " WHERE " + DATABASE_TABLE_CARDS
					+ "." + KEY_NAME + " = " + DATABASE_TABLE_BANNED_CARDS + "." + KEY_NAME + " AND "
					+ DATABASE_TABLE_BANNED_CARDS + "." + KEY_FORMAT + " = '" + format + "' AND " + DATABASE_TABLE_BANNED_CARDS
					+ "." + KEY_LEGALITY + " = " + BANNED + ")";
		}

		if (!backface) {
			statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_NUMBER + " NOT LIKE '%b%')";
		}

		if (statement.equals(" WHERE 1=1")) {
			// If the statement is just this, it means we added nothing
			return null;
		}

		try {
			String sel = null;
			for (String s : returnTypes) {
				if (sel == null) {
					sel = DATABASE_TABLE_CARDS + "." + s + " AS " + s;
				}
				else {
					sel += ", " + DATABASE_TABLE_CARDS + "." + s + " AS " + s;
				}
			}
			sel += ", " + DATABASE_TABLE_SETS + "." + KEY_DATE;

			String sql = "SELECT * FROM (SELECT " + sel + " FROM " + tbl + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_CARDS
					+ "." + KEY_SET + " = " + DATABASE_TABLE_SETS + "." + KEY_CODE + statement;

			if (consolidate) {
				sql += " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + ") GROUP BY " + KEY_NAME + " ORDER BY " + KEY_NAME;
			}
			else {
				sql += " ORDER BY " + DATABASE_TABLE_CARDS + "." + KEY_NAME + ", " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC)";
			}
			mCursor = mDb.rawQuery(sql, null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public Cursor PrefixSearch(String cardname, String[] returnTypes) {
		Cursor mCursor = null;

		if (cardname != null)
			cardname = cardname.replace("'", "''").trim();

		String statement = " WHERE 1=1";

		statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '" + cardname + "%')";

		try {
			String sel = null;
			for (String s : returnTypes) {
				if (sel == null) {
					sel = DATABASE_TABLE_CARDS + "." + s + " AS " + s;
				}
				else {
					sel += ", " + DATABASE_TABLE_CARDS + "." + s + " AS " + s;
				}
			}
			sel += ", " + DATABASE_TABLE_SETS + "." + KEY_DATE;

			String sql = "SELECT * FROM (SELECT " + sel + " FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_CARDS
					+ "." + KEY_SET + " = " + DATABASE_TABLE_SETS + "." + KEY_CODE + statement;

			sql += " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + ") GROUP BY " + KEY_NAME + " ORDER BY " + KEY_NAME;
			mCursor = mDb.rawQuery(sql, null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public int getTransform(String set, String number) {
		Cursor mCursor = null;
		int ID = -1;
		String statement = "(" + KEY_NUMBER + " = '" + number + "') AND (" + KEY_SET + " = '" + set + "')";
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_ID }, statement, null, null, null, KEY_ID,
					null);
			mCursor.moveToFirst();
			ID = mCursor.getInt(mCursor.getColumnIndex(KEY_ID));
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
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
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

		return name;
	}

	public void createLegalTables() {
		mDb.execSQL(DATABASE_CREATE_FORMATS);
		mDb.execSQL(DATABASE_CREATE_LEGAL_SETS);
		mDb.execSQL(DATABASE_CREATE_BANNED_CARDS);
	}

	public void dropLegalTables() {
		mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_FORMATS);
		mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_LEGAL_SETS);
		mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_BANNED_CARDS);
	}

	public long createFormat(String name) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		return mDb.insert(DATABASE_TABLE_FORMATS, null, initialValues);
	}

	public long addLegalSet(String set, String format) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_SET, set);
		initialValues.put(KEY_FORMAT, format);
		return mDb.insert(DATABASE_TABLE_LEGAL_SETS, null, initialValues);
	}

	public long addLegalCard(String card, String format, int status) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, card);
		initialValues.put(KEY_LEGALITY, status);
		initialValues.put(KEY_FORMAT, format);
		return mDb.insert(DATABASE_TABLE_BANNED_CARDS, null, initialValues);
	}

	public Cursor fetchAllFormats() {
		try {
			return mDb.query(DATABASE_TABLE_FORMATS, new String[] { KEY_ID, KEY_NAME, }, null, null, null, null, KEY_NAME);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		return null;
	}

	public static final int	LEGAL				= 0;
	public static final int	BANNED			= 1;
	public static final int	RESTRICTED	= 2;

	public int checkLegality(String mCardName, String format) {
		mCardName = mCardName.replace("'", "''");
		format = format.replace("'", "''"); // Just to be safe; remember Bobby
		// Tables
		try {
			// The new way (single query per type, should be much faster) - Alex
			String sql = "SELECT COALESCE(CASE (SELECT " + KEY_SET + " FROM " + DATABASE_TABLE_CARDS + " WHERE " + KEY_NAME
					+ " = '" + mCardName + "') WHEN 'UG' THEN 1 WHEN 'UNH' THEN 1 ELSE NULL END, " 
					+ "CASE (SELECT 1 FROM " + DATABASE_TABLE_CARDS + " c INNER JOIN "
					+ DATABASE_TABLE_LEGAL_SETS + " ls ON ls." + KEY_SET + " = c." + KEY_SET + " WHERE ls." + KEY_FORMAT + " = '"
					+ format + "' AND c." + KEY_NAME + " = '" + mCardName + "') WHEN 1 THEN NULL ELSE CASE WHEN '" + format
					+ "' = 'Legacy' " + "THEN NULL WHEN '" + format + "' = 'Vintage' THEN NULL ELSE 1 END END, (SELECT "
					+ KEY_LEGALITY + " from " + DATABASE_TABLE_BANNED_CARDS + " WHERE " + KEY_NAME + " = '" + mCardName
					+ "' AND " + KEY_FORMAT + " = '" + format + "'), 0) AS " + KEY_LEGALITY;

			Cursor c = null;
			c = mDb.rawQuery(sql, null);

			c.moveToFirst();
			int legality = c.getInt(c.getColumnIndex(KEY_LEGALITY));
			c.close();
			return legality;
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		return -1;
	}

	public String getTCGname(String setCode) {
		Cursor mCursor = null;
		String name = null;
		String statement = "(" + KEY_CODE + " = '" + setCode + "')";
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_SETS, new String[] { KEY_NAME_TCGPLAYER }, statement, null, null, null,
					KEY_NAME_TCGPLAYER, null);
			mCursor.moveToFirst();
			name = mCursor.getString(mCursor.getColumnIndex(KEY_NAME_TCGPLAYER));
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

		return name;
	}

	public Cursor getAllNames() {
		try {
			return mDb.query(true, DATABASE_TABLE_CARDS, new String[] { /* KEY_ID, */KEY_NAME }, null, null, null, null,
					KEY_NAME, null);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		return null;
	}

	public SQLiteDatabase getReadableDatabase() {
		return mDb;
	}

	private static final HashMap<String, String>	mColumnMap	= buildColumnMap();

	/**
	 * Builds a map for all columns that may be requested, which will be given to
	 * the SQLiteQueryBuilder. This is a good way to define aliases for column
	 * names, but must include all columns, even if the value is the key. This
	 * allows the ContentProvider to request columns w/o the need to know real
	 * column names and create the alias itself.
	 */
	private static HashMap<String, String> buildColumnMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(KEY_NAME, KEY_NAME);
		map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
		return map;
	}

	/**
	 * Performs a database query.
	 * 
	 * @param selection
	 *          The selection clause
	 * @param selectionArgs
	 *          Selection arguments for "?" components in the selection
	 * @param columns
	 *          The columns to return
	 * @return A Cursor over all rows matching the query
	 */
	private Cursor query(String selection, String[] selectionArgs, String[] columns) {
		/*
		 * The SQLiteBuilder provides a map for all possible columns requested to
		 * actual columns in the database, creating a simple column alias mechanism
		 * by which the ContentProvider does not need to know the real column names
		 */
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(DATABASE_TABLE_CARDS);
		builder.setProjectionMap(mColumnMap);

		Cursor cursor = null;
		try {
			cursor = builder.query(mDb, columns, selection, selectionArgs, KEY_NAME, null, KEY_NAME);
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

		if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}

		return cursor;
	}

	/**
	 * Returns a Cursor positioned at the word specified by rowId
	 * 
	 * @param rowId
	 *          id of word to retrieve
	 * @param columns
	 *          The columns to include, if null then all are included
	 * @return Cursor positioned to matching word, or null if not found.
	 */
	public Cursor getWord(String rowId, String[] columns) {
		String selection = "rowid = ?";
		String[] selectionArgs = new String[] { rowId };

		return query(selection, selectionArgs, columns);

		/*
		 * This builds a query that looks like: SELECT <columns> FROM <table> WHERE
		 * rowid = <rowId>
		 */
	}

	/**
	 * Returns a Cursor over all words that match the given query
	 * 
	 * @param query
	 *          The string to search for
	 * @param columns
	 *          The columns to include, if null then all are included
	 * @return Cursor over all words that match, or null if none found.
	 */
	public Cursor getWordMatches(String query, String[] columns) {
		String selection = KEY_NAME + " LIKE '" + query + "%'";
		String[] selectionArgs = null;

		return query(selection, selectionArgs, columns);

		/*
		 * This builds a query that looks like: SELECT <columns> FROM <table> WHERE
		 * <KEY_WORD> MATCH 'query*' which is an FTS3 search for the query text
		 * (plus a wildcard) inside the word column.
		 * 
		 * - "rowid" is the unique id for all rows but we need this value for the
		 * "_id" column in order for the Adapters to work, so the columns need to
		 * make "_id" an alias for "rowid" - "rowid" also needs to be used by the
		 * SUGGEST_COLUMN_INTENT_DATA alias in order for suggestions to carry the
		 * proper intent data. These aliases are defined in the DictionaryProvider
		 * when queries are made. - This can be revised to also search the
		 * definition text with FTS3 by changing the selection clause to use
		 * FTS_VIRTUAL_TABLE instead of KEY_WORD (to search across the entire table,
		 * but sorting the relevance could be difficult.
		 */
	}

	public static final String	DB_PATH	= "/data/data/com.gelakinetic.mtgfam/databases/";
	public static final String	DB_NAME	= "data";

	public static boolean isDbOutOfDate(Context ctx) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		File f = new File(DB_PATH, DB_NAME);
		int dbVersion = preferences.getInt("databaseVersion", -1);
		if (!f.exists() || f.length() < 1048576 || dbVersion < CardDbAdapter.DATABASE_VERSION) {
			return true;
		}
		return false;
	}

	public static void copyDB(Context ctx) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();

		try {
			File folder = new File(DB_PATH);
			if (!folder.exists()) {
				folder.mkdir();
			}
			File db = new File(folder, DB_NAME);
			if (db.exists()) {
				db.delete();
				editor.putString("lastUpdate", "");
				editor.putInt("databaseVersion", -1);
				editor.commit();
			}
			if (!db.exists()) {

				GZIPInputStream gis = new GZIPInputStream(ctx.getResources().openRawResource(R.raw.db));
				FileOutputStream fos = new FileOutputStream(db);

				byte[] buffer = new byte[1024];
				int length;
				while ((length = gis.read(buffer)) > 0) {
					fos.write(buffer, 0, length);
				}

				editor.putInt("databaseVersion", CardDbAdapter.DATABASE_VERSION);
				editor.commit();

				// Close the streams
				fos.flush();
				fos.close();
				gis.close();
			}
		}
		catch (NotFoundException e) {
		}
		catch (IOException e) {
		}
		catch (Exception e) {
		}
	}

	public String getNameFromId(long id) {
		Cursor mCursor = null;
		String name = null;
		String statement = "(" + KEY_ID + " = " + id + ")";
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS, new String[] { KEY_ID, KEY_NAME }, statement, null, null, null,
					KEY_ID, null);
			mCursor.moveToFirst();
			name = mCursor.getString(mCursor.getColumnIndex(KEY_NAME));
		}
		catch (SQLiteException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}
		catch (IllegalStateException e) {
			Toast.makeText(mCtx, mCtx.getString(R.string.dberror), Toast.LENGTH_LONG).show();
		}

		return name;
	}
}
