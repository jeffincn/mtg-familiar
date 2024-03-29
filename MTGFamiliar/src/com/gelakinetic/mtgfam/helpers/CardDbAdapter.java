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

package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.gelakinetic.mtgfam.R;

/**
 * Simple Cards database access helper class. Defines the basic CRUD operations
 * and gives the ability to list all Cards as well as retrieve or modify a
 * specific Card.
 */
public class CardDbAdapter {

	public static final int STAR = -1000;
	public static final int ONEPLUSSTAR = -1001;
	public static final int TWOPLUSSTAR = -1002;
	public static final int SEVENMINUSSTAR = -1003;
	public static final int STARSQUARED = -1004;
	public static final int NOONECARES = -1005;

	public static final int MOSTRECENTPRINTING = 0;
	public static final int FIRSTPRINTING = 1;
	public static final int ALLPRINTINGS = 2;

	public static final String DATABASE_NAME = "data";
	public static final String DATABASE_TABLE_CARDS = "cards";
	public static final String DATABASE_TABLE_SETS = "sets";
	public static final String DATABASE_TABLE_FORMATS = "formats";
	private static final String DATABASE_TABLE_LEGAL_SETS = "legal_sets";
	private static final String DATABASE_TABLE_BANNED_CARDS = "banned_cards";
	private static final String DATABASE_TABLE_RULES = "rules";
	private static final String DATABASE_TABLE_GLOSSARY = "glossary";

	public static final int DATABASE_VERSION = 42;

	public static final String KEY_ID = "_id";
	public static final String KEY_NAME = SearchManager.SUGGEST_COLUMN_TEXT_1; // "name";
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
	public static final String KEY_MULTIVERSEID = "multiverseID";
	public static final String KEY_RULINGS = "rulings";

	public static final String KEY_CODE = "code";
	public static final String KEY_CODE_MTGI = "code_mtgi";
	public static final String KEY_NAME_TCGPLAYER = "name_tcgplayer";
	public static final String KEY_DATE = "date";

	public static final String KEY_FORMAT = "format";
	public static final String KEY_LEGALITY = "legality";

	public static final int LEFT = 0;
	public static final int RIGHT = 1;

	public static final String KEY_CATEGORY = "category";
	public static final String KEY_SUBCATEGORY = "subcategory";
	public static final String KEY_ENTRY = "entry";
	public static final String KEY_RULE_TEXT = "rule_text";
	public static final String KEY_POSITION = "position";

	public static final String KEY_TERM = "term";
	public static final String KEY_DEFINITION = "definition";

	private DatabaseHelper mDbHelper;
	public SQLiteDatabase mDb;

	public static final String[] allData = { CardDbAdapter.KEY_ID,
			CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_SET,
			CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_TYPE,
			CardDbAdapter.KEY_MANACOST, CardDbAdapter.KEY_ABILITY,
			CardDbAdapter.KEY_POWER, CardDbAdapter.KEY_TOUGHNESS,
			CardDbAdapter.KEY_LOYALTY, CardDbAdapter.KEY_RARITY };
	
	private static final String DATABASE_CREATE_CARDS = "create table "
			+ DATABASE_TABLE_CARDS + "(" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_NAME
			+ " text not null, " + KEY_SET + " text not null, " + KEY_TYPE
			+ " text not null, " + KEY_RARITY + " integer, " + KEY_MANACOST
			+ " text, " + KEY_CMC + " integer not null, " + KEY_POWER
			+ " real, " + KEY_TOUGHNESS + " real, " + KEY_LOYALTY
			+ " integer, " + KEY_ABILITY + " text, " + KEY_FLAVOR + " text, "
			+ KEY_ARTIST + " text, " + KEY_NUMBER + " text, "
			+ KEY_MULTIVERSEID + " integer not null, " + KEY_COLOR
			+ " text not null, " + KEY_RULINGS + " text);";

	private static final String DATABASE_CREATE_SETS = "create table "
			+ DATABASE_TABLE_SETS + "(" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_NAME
			+ " text not null, " + KEY_CODE + " text not null unique, "
			+ KEY_CODE_MTGI + " text not null, " + KEY_NAME_TCGPLAYER
			+ " text, " + KEY_DATE + " integer);";

	private static final String DATABASE_CREATE_FORMATS = "create table "
			+ DATABASE_TABLE_FORMATS + "(" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_NAME
			+ " text not null);";

	private static final String DATABASE_CREATE_LEGAL_SETS = "create table "
			+ DATABASE_TABLE_LEGAL_SETS + "(" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_SET
			+ " text not null, " + KEY_FORMAT + " text not null);";

	private static final String DATABASE_CREATE_BANNED_CARDS = "create table "
			+ DATABASE_TABLE_BANNED_CARDS + "(" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_NAME
			+ " text not null, " + KEY_LEGALITY + " integer not null, "
			+ KEY_FORMAT + " text not null);";

	private static final String DATABASE_CREATE_RULES = "create table "
			+ DATABASE_TABLE_RULES + "(" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_CATEGORY
			+ " integer not null, " + KEY_SUBCATEGORY + " integer not null, "
			+ KEY_ENTRY + " text null, " + KEY_RULE_TEXT + " text not null, "
			+ KEY_POSITION + " integer null);";

	private static final String DATABASE_CREATE_GLOSSARY = "create table "
			+ DATABASE_TABLE_GLOSSARY + "(" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_TERM
			+ " text not null, " + KEY_DEFINITION + " text not null);";

	private final Context mCtx;

	public static final String EXCLUDE_TOKEN = "!";
	public static final int EXCLUDE_TOKEN_START = 1;

	public static final int LEGAL = 0;
	public static final int BANNED = 1;
	public static final int RESTRICTED = 2;

	// use a hash map for performance
	private static final HashMap<String, String> mColumnMap = buildColumnMap();

	public static final String DB_PATH = "/data/data/com.gelakinetic.mtgfam/databases/";
	public static final String DB_NAME = "data";
	
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
		}
	}

	public CardDbAdapter(Context ctx) throws FamiliarDbException {

		if (CardDbAdapter.isDbOutOfDate(ctx)) {
			CardDbAdapter.copyDB(ctx);
		}
		
		this.mCtx = ctx;

		// Always open the database
		try {
			this.mDbHelper = new DatabaseHelper(ctx);
			this.mDb = this.mDbHelper.getReadableDatabase();
		} catch (SQLException e) {
			throw new FamiliarDbException(e);
		}
	}

	public void openReadable() throws FamiliarDbException {
		if(mDb != null && mDb.isOpen()) {
			// its already open, silly!
			mDb.close();
		}
		try {
			mDbHelper = new DatabaseHelper(mCtx);
			mDb = mDbHelper.getReadableDatabase();
		} catch (SQLException e) {
			throw new FamiliarDbException(e);
		}
	}

	public void openTransactional() throws FamiliarDbException {
		try {
			mDbHelper = new DatabaseHelper(mCtx);
			mDb = mDbHelper.getWritableDatabase();
			mDb.execSQL("BEGIN DEFERRED TRANSACTION");
		} catch (SQLException e) {
			throw new FamiliarDbException(e);
		}
	}

	public void closeTransactional() throws FamiliarDbException {
		try {
			mDb.execSQL("COMMIT");
			mDbHelper.close();
		} catch (SQLException e) {
			throw new FamiliarDbException(e);
		}
	}

	public void close() {
		try {
			mDbHelper.close();
		}
		catch (Exception e) {
			// This threw an exception at the GTC prerelease. Something to do with price fetching, not really sure
			// Doing it pokemon style and hoping to remember to dig deeper later
		}
	}

	public void dropCreateDB() throws FamiliarDbException {
		try{
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARDS);
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_SETS);
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_FORMATS);
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_LEGAL_SETS);
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_BANNED_CARDS);
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RULES);
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_GLOSSARY);
	
			mDb.execSQL(DATABASE_CREATE_CARDS);
			mDb.execSQL(DATABASE_CREATE_SETS);
			mDb.execSQL(DATABASE_CREATE_FORMATS);
			mDb.execSQL(DATABASE_CREATE_LEGAL_SETS);
			mDb.execSQL(DATABASE_CREATE_BANNED_CARDS);
			mDb.execSQL(DATABASE_CREATE_RULES);
			mDb.execSQL(DATABASE_CREATE_GLOSSARY);
		}
		catch(SQLiteException e) {
			throw new FamiliarDbException(e);
		}
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

	public long createSet(MtgSet set) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_CODE, set.code);
		initialValues.put(KEY_NAME, set.name);
		initialValues.put(KEY_CODE_MTGI, set.code_magiccards);
		initialValues.put(KEY_DATE, set.date);

		return mDb.insert(DATABASE_TABLE_SETS, null, initialValues);
	}

	public boolean addTCGname(String name, String code)
			throws FamiliarDbException {
		ContentValues args = new ContentValues();

		args.put(KEY_NAME_TCGPLAYER, name);

		boolean wasSuccess = mDb.update(DATABASE_TABLE_SETS, args, KEY_CODE
				+ " = '" + code + "'", null) > 0;

		return wasSuccess;
	}

	public Cursor fetchAllSets() throws FamiliarDbException {

		Cursor c = null;
		try {
			if(mDb == null) {
				this.openReadable();
			}
			c = mDb.query(DATABASE_TABLE_SETS, new String[] { KEY_ID, KEY_NAME,
					KEY_CODE, KEY_CODE_MTGI }, null, null, null, null, KEY_DATE
					+ " DESC");
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		} catch (NullPointerException e) {
			throw new FamiliarDbException(e);
		}

		return c;
	}

	public boolean doesSetExist(String code) throws FamiliarDbException {

		String statement = "(" + KEY_CODE + " LIKE '%" + code + "%')";

		Cursor c = null;
		int count = 0;
		try {
			c = mDb.query(true, DATABASE_TABLE_SETS, new String[] { KEY_ID },
					statement, null, null, null, KEY_NAME, null);
			count = c.getCount();
			c.close();
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		return count > 0;
	}

	public String getCodeMtgi(String code) throws FamiliarDbException {
		Cursor c = null;
		try {
			c = mDb.query(DATABASE_TABLE_SETS, new String[] { KEY_CODE_MTGI },
					KEY_CODE + "=\"" + code + "\"", null, null, null, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		c.moveToFirst();
		String retval = c.getString(c.getColumnIndex(KEY_CODE_MTGI));
		c.close();
		return retval;
	}

	public Cursor fetchCard(long id, String[] columns)
			throws FamiliarDbException {

		if (columns == null) {
			columns = new String[] { KEY_ID, KEY_NAME, KEY_SET, KEY_TYPE,
					KEY_RARITY, KEY_MANACOST, KEY_CMC, KEY_POWER,
					KEY_TOUGHNESS, KEY_LOYALTY, KEY_ABILITY, KEY_FLAVOR,
					KEY_ARTIST, KEY_NUMBER, KEY_COLOR, KEY_MULTIVERSEID };
		}
		Cursor mCursor = null;
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS, columns, KEY_ID
					+ "=" + id, null, null, null, KEY_NAME, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	public Cursor fetchCardByName(String name, String[] fields)
			throws FamiliarDbException {
		// replace lowercase ae with Ae
		name = name.replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]);
		String sql = "SELECT ";
		boolean first = true;
		for (String field : fields) {
			if (first) {
				first = false;
			} else {
				sql += ", ";
			}
			sql += DATABASE_TABLE_CARDS + "." + field;
		}
		sql += " FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS
				+ " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = "
				+ DATABASE_TABLE_CARDS + "." + KEY_SET + " WHERE "
				+ DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + DatabaseUtils.sqlEscapeString(name)
				+ " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE
				+ " DESC";
		Cursor mCursor = null;

		try {
			mCursor = mDb.rawQuery(sql, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public Cursor fetchLatestCardByName(String name, String[] fields)
			throws FamiliarDbException {
		// replace lowercase ae with Ae
		name = name.replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]);
		String sql = "SELECT ";
		boolean first = true;
		for (String field : fields) {
			if (first) {
				first = false;
			} else {
				sql += ", ";
			}
			sql += DATABASE_TABLE_CARDS + "." + field;
		}
		sql += " FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS
				+ " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = "
				+ DATABASE_TABLE_CARDS + "." + KEY_SET + " WHERE "
				+ DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + DatabaseUtils.sqlEscapeString(name)
				+ " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE
				+ " DESC LIMIT 1";
		Cursor mCursor = null;

		try {
			mCursor = mDb.rawQuery(sql, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public Cursor fetchCardByNameAndSet(String name, String setCode)
			throws FamiliarDbException {
		// replace lowercase ae with Ae
		name = name.replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]);
		String sql = "SELECT " + DATABASE_TABLE_CARDS + "." + KEY_ID + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_NAME + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_SET + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_NUMBER + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_TYPE + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_MANACOST + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_ABILITY + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_POWER + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_LOYALTY + ", "
				+ DATABASE_TABLE_CARDS + "." + KEY_RARITY + " FROM "
				+ DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS
				+ " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = "
				+ DATABASE_TABLE_CARDS + "." + KEY_SET + " WHERE "
				+ DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + DatabaseUtils.sqlEscapeString(name)
				+ " AND " + DATABASE_TABLE_CARDS + "." + KEY_SET + " = '"
				+ setCode + "' ORDER BY " + DATABASE_TABLE_SETS + "."
				+ KEY_DATE + " DESC";
		Cursor mCursor = null;

		try {
			mCursor = mDb.rawQuery(sql, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public long fetchIdByName(String name) throws FamiliarDbException {
		// replace lowercase ae with Ae
		name = name.replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]);
		
		String sql = "SELECT " + DATABASE_TABLE_CARDS + "." + KEY_ID + ", " + DATABASE_TABLE_CARDS + "." + KEY_SET + ", " + DATABASE_TABLE_SETS + "." + KEY_DATE +
				" FROM (" + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_CARDS + "." + KEY_SET + "=" + DATABASE_TABLE_SETS + "." + KEY_CODE + ")" +
				" WHERE " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " = " + DatabaseUtils.sqlEscapeString(name) + " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE + " DESC";
		
		Cursor mCursor = null;
		try {
			mCursor = mDb.rawQuery(sql, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		if (mCursor != null) {
			mCursor.moveToFirst();
			long id = mCursor.getLong(mCursor
					.getColumnIndex(CardDbAdapter.KEY_ID));
			mCursor.close();
			return id;
		}
		return -1;
	}

	public Cursor autoComplete(String cardname) throws FamiliarDbException {
		Cursor mCursor = null;
		String convertName = null;
		
		if (cardname != null){
			cardname = cardname.replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]).trim();
			convertName = cardname.toLowerCase().replace("ae", String.valueOf(Character.toChars(0xC6)[0]));
		}

		String sql = "SELECT MIN(" + KEY_ID + ") AS " + KEY_ID + ", "
				+ KEY_NAME + " FROM " + DATABASE_TABLE_CARDS + " WHERE "
				+ KEY_NAME + " LIKE " + DatabaseUtils.sqlEscapeString(cardname + "%") 
				+ " OR " + KEY_NAME + " LIKE " + DatabaseUtils.sqlEscapeString(convertName + "%")
				+ "GROUP BY " + KEY_NAME + " ORDER BY " + KEY_NAME + " COLLATE UNICODE";
		try {
			mCursor = mDb.rawQuery(sql, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		if (mCursor != null) {
			mCursor.moveToFirst();
		}

		return mCursor;
	}

	public Cursor Search(String cardname, String cardtext, String cardtype,
			String color, int colorlogic, String sets, float pow_choice,
			String pow_logic, float tou_choice, String tou_logic, int cmc,
			String cmcLogic, String format, String rarity, String flavor,
			String artist, int type_logic, int text_logic, int set_logic,
			boolean backface, String[] returnTypes, boolean consolidate)
			throws FamiliarDbException {
		Cursor mCursor = null;

		if (cardname != null)
			cardname = cardname.replace("'", "''").replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]).trim();
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
				statement += " AND (" +
						DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '%" + s + "%' OR " +
						DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '%" + s.toLowerCase().replace("ae", String.valueOf(Character.toChars(0xC6)[0])) + "%')";
			}
		}

		/*************************************************************************************/
		/**
		 * Reuben's version Differences: Original code is verbose only, but mine
		 * allows for matching exact text, all words, or just any one word.
		 */
		if (cardtext != null) {
			String[] cardTextParts = cardtext.split(" "); // Separate each
															// individual

			/**
			 * The following switch statement tests to see which text search
			 * logic was chosen by the user. If they chose the first option (0),
			 * then look for cards with text that includes all words, but not
			 * necessarily the exact phrase. The second option (1) finds cards
			 * that have 1 or more of the chosen words in their text. The third
			 * option (2) searches for the exact phrase as entered by the user.
			 * The 'default' option is impossible via the way the code is
			 * written, but I believe it's also mandatory to include it in case
			 * someone else is perhaps fussing with the code and breaks it. The
			 * if statement at the end is theorhetically unnecessary, because
			 * once we've entered the current if statement, there is no way to
			 * NOT change the statement variable. However, you never really know
			 * who's going to break open your code and fuss around with it, so
			 * it's always good to leave some small safety measures.
			 */
			switch (text_logic) {
			case 0:
				for (String s : cardTextParts) {
					if (s.contains(EXCLUDE_TOKEN))
						statement += " AND (" + DATABASE_TABLE_CARDS + "."
								+ KEY_ABILITY + " NOT LIKE '%"
								+ s.substring(EXCLUDE_TOKEN_START) + "%')";
					else
						statement += " AND (" + DATABASE_TABLE_CARDS + "."
								+ KEY_ABILITY + " LIKE '%" + s + "%')";
				}
				break;
			case 1:
				boolean firstRun = true;
				for (String s : cardTextParts) {
					if (firstRun) {
						firstRun = false;
						if (s.contains(EXCLUDE_TOKEN))
							statement += " AND ((" + DATABASE_TABLE_CARDS + "."
									+ KEY_ABILITY + " NOT LIKE '%"
									+ s.substring(EXCLUDE_TOKEN_START) + "%')";
						else
							statement += " AND ((" + DATABASE_TABLE_CARDS + "."
									+ KEY_ABILITY + " LIKE '%" + s + "%')";
					} else {
						if (s.contains(EXCLUDE_TOKEN))
							statement += " AND (" + DATABASE_TABLE_CARDS + "."
									+ KEY_ABILITY + " NOT LIKE '%"
									+ s.substring(EXCLUDE_TOKEN_START) + "%')";
						else
							statement += " OR (" + DATABASE_TABLE_CARDS + "."
									+ KEY_ABILITY + " LIKE '%" + s + "%')";
					}
				}
				statement += ")";
				break;
			case 2:
				statement += " AND (" + DATABASE_TABLE_CARDS + "."
						+ KEY_ABILITY + " LIKE '%" + cardtext + "%')";
				break;
			default:
				break;
			}
		}
		/** End Reuben's version */

		/**
		 * Reuben's version Differences: Original version only allowed for
		 * including all types, not any of the types or excluding the given
		 * types.
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
			} else if (containsSupertype) {
				supertypes = cardtype.replace(" -", "");
			} else {
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
						statement += " AND (" + DATABASE_TABLE_CARDS + "."
								+ KEY_TYPE + " NOT LIKE '%" + s.substring(1)
								+ "%')";
					else
						statement += " AND (" + DATABASE_TABLE_CARDS + "."
								+ KEY_TYPE + " LIKE '%" + s + "%')";
				}
				break;
			case 1:
				boolean firstRun = true;
				for (String s : supertypesParts) {
					if (firstRun) {
						firstRun = false;

						if (s.contains(EXCLUDE_TOKEN))
							statement += " AND ((" + DATABASE_TABLE_CARDS + "."
									+ KEY_TYPE + " NOT LIKE '%"
									+ s.substring(1) + "%')";
						else
							statement += " AND ((" + DATABASE_TABLE_CARDS + "."
									+ KEY_TYPE + " LIKE '%" + s + "%')";
					} else if (s.contains(EXCLUDE_TOKEN))
						statement += " AND (" + DATABASE_TABLE_CARDS + "."
								+ KEY_TYPE + " NOT LIKE '%" + s.substring(1)
								+ "%')";
					else
						statement += " OR (" + DATABASE_TABLE_CARDS + "."
								+ KEY_TYPE + " LIKE '%" + s + "%')";
				}
				statement += ")";
				break;
			case 2:
				for (String s : supertypesParts) {
					statement += " AND (" + DATABASE_TABLE_CARDS + "."
							+ KEY_TYPE + " NOT LIKE '%" + s + "%')";
				}
				break;
			default:
				break;
			}
		}

		if (subtypes != null) {
			String[] subtypesParts = subtypes.split(" "); // Separate each
															// individual

			switch (type_logic) {
			case 0:
				for (String s : subtypesParts) {
					if (s.contains(EXCLUDE_TOKEN))
						statement += " AND (" + DATABASE_TABLE_CARDS + "."
								+ KEY_TYPE + " NOT LIKE '%" + s.substring(1)
								+ "%')";
					else
						statement += " AND (" + DATABASE_TABLE_CARDS + "."
								+ KEY_TYPE + " LIKE '%" + s + "%')";
				}
				break;
			case 1:
				boolean firstRun = true;
				for (String s : subtypesParts) {
					if (firstRun) {
						firstRun = false;
						if (s.contains(EXCLUDE_TOKEN))
							statement += " AND ((" + DATABASE_TABLE_CARDS + "."
									+ KEY_TYPE + " NOT LIKE '%"
									+ s.substring(1) + "%')";
						else
							statement += " AND ((" + DATABASE_TABLE_CARDS + "."
									+ KEY_TYPE + " LIKE '%" + s + "%')";
					} else if (s.contains(EXCLUDE_TOKEN))
						statement += " AND (" + DATABASE_TABLE_CARDS + "."
								+ KEY_TYPE + " NOT LIKE '%" + s.substring(1)
								+ "%')";
					else
						statement += " OR (" + DATABASE_TABLE_CARDS + "."
								+ KEY_TYPE + " LIKE '%" + s + "%')";
				}
				statement += ")";
				break;
			case 2:
				for (String s : subtypesParts) {
					statement += " AND (" + DATABASE_TABLE_CARDS + "."
							+ KEY_TYPE + " NOT LIKE '%" + s + "%')";
				}
				break;
			default:
				break;
			}
		}
		/** End Reuben's version */
		/*************************************************************************************/

		if (flavor != null) {
			statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_FLAVOR
					+ " LIKE '%" + flavor + "%')";
		}

		if (artist != null) {
			statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_ARTIST
					+ " LIKE '%" + artist + "%')";
		}

		/*************************************************************************************/
		/**
		 * Code below added/modified by Reuben. Differences: Original version
		 * only had 'Any' and 'All' options and lacked 'Exclusive' and 'Exact'
		 * matching. In addition, original programming only provided exclusive
		 * results.
		 */
		if (!(color.equals("wubrgl") || (color.equals("WUBRGL") && colorlogic == 0))) {
			boolean firstPrint = true;

			// Can't contain these colors
			/**
			 * ...if the chosen color logic was exactly (2) or none (3) of the
			 * selected colors
			 */
			if (colorlogic > 1) // if colorlogic is 2 or 3 it will be greater
								// than 1
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
							statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR
									+ " NOT GLOB '[CLA]'";
						else
							statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR
									+ " NOT LIKE '%" + Character.toUpperCase(c)
									+ "%'";
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
						statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR
								+ " GLOB '[CLA]'";
					else
						statement += DATABASE_TABLE_CARDS + "." + KEY_COLOR
								+ " LIKE '%" + c + "%'";
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
				} else {
					statement += " OR ";
				}
				statement += DATABASE_TABLE_CARDS + "." + KEY_SET + " = '" + s
						+ "'";
			}

			statement += ")";
		}

		if (pow_choice != NOONECARES) {
			statement += " AND (";

			if (pow_choice > STAR) {
				statement += DATABASE_TABLE_CARDS + "." + KEY_POWER + " "
						+ pow_logic + " " + pow_choice;
				if (pow_logic.equals("<")) {
					statement += " AND " + DATABASE_TABLE_CARDS + "."
							+ KEY_POWER + " > " + STAR;
				}
			} else if (pow_logic.equals("=")) {
				statement += DATABASE_TABLE_CARDS + "." + KEY_POWER + " "
						+ pow_logic + " " + pow_choice;
			}
			statement += ")";
		}

		if (tou_choice != NOONECARES) {
			statement += " AND (";

			if (tou_choice > STAR) {
				statement += DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " "
						+ tou_logic + " " + tou_choice;
				if (tou_logic.equals("<")) {
					statement += " AND " + DATABASE_TABLE_CARDS + "."
							+ KEY_TOUGHNESS + " > " + STAR;
				}
			} else if (tou_logic.equals("=")) {
				statement += DATABASE_TABLE_CARDS + "." + KEY_TOUGHNESS + " "
						+ tou_logic + " " + tou_choice;
			}
			statement += ")";
		}

		if (cmc != -1) {
			statement += " AND (";

			statement += DATABASE_TABLE_CARDS + "." + KEY_CMC + " " + cmcLogic
					+ " " + cmc + ")";
		}

		if (rarity != null) {
			statement += " AND (";

			boolean firstPrint = true;
			for (int i = 0; i < rarity.length(); i++) {
				if (firstPrint) {
					firstPrint = false;
				} else {
					statement += " OR ";
				}
				statement += DATABASE_TABLE_CARDS + "." + KEY_RARITY + " = "
						+ (int) rarity.toUpperCase().charAt(i) + "";
			}
			statement += ")";
		}

		String tbl = DATABASE_TABLE_CARDS;
		if (format != null) {
			if (!(format.equals("Legacy") || format.equals("Vintage"))) {
				tbl = "(" + DATABASE_TABLE_CARDS + " JOIN "
						+ DATABASE_TABLE_LEGAL_SETS + " ON "
						+ DATABASE_TABLE_CARDS + "." + KEY_SET + "="
						+ DATABASE_TABLE_LEGAL_SETS + "." + KEY_SET + " AND "
						+ DATABASE_TABLE_LEGAL_SETS + "." + KEY_FORMAT + "='"
						+ format + "')";
			} else {
				statement += " AND NOT " + KEY_SET + "= 'UNH' AND NOT "
						+ KEY_SET + "= 'UG'";
			}
			statement += " AND NOT EXISTS (SELECT * FROM "
					+ DATABASE_TABLE_BANNED_CARDS + " WHERE "
					+ DATABASE_TABLE_CARDS + "." + KEY_NAME + " = "
					+ DATABASE_TABLE_BANNED_CARDS + "." + KEY_NAME + " AND "
					+ DATABASE_TABLE_BANNED_CARDS + "." + KEY_FORMAT + " = '"
					+ format + "' AND " + DATABASE_TABLE_BANNED_CARDS + "."
					+ KEY_LEGALITY + " = " + BANNED + ")";
		}

		if (!backface) {
			statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_NUMBER
					+ " NOT LIKE '%b%')";
		}

		if (set_logic != MOSTRECENTPRINTING && set_logic != ALLPRINTINGS) {
			statement = " JOIN (SELECT iT" + DATABASE_TABLE_CARDS + "."
					+ KEY_NAME + ", MIN(" + DATABASE_TABLE_SETS + "."
					+ KEY_DATE + ") AS " + KEY_DATE + " FROM "
					+ DATABASE_TABLE_CARDS + " AS iT" + DATABASE_TABLE_CARDS
					+ " JOIN " + DATABASE_TABLE_SETS + " ON iT"
					+ DATABASE_TABLE_CARDS + "." + KEY_SET + " = "
					+ DATABASE_TABLE_SETS + "." + KEY_CODE + " GROUP BY iT"
					+ DATABASE_TABLE_CARDS + "." + KEY_NAME
					+ ") AS FirstPrints" + " ON " + DATABASE_TABLE_CARDS + "."
					+ KEY_NAME + " = FirstPrints." + KEY_NAME + statement;
			if (set_logic == FIRSTPRINTING)
				statement = " AND " + DATABASE_TABLE_SETS + "." + KEY_DATE
						+ " = FirstPrints." + KEY_DATE + statement;
			else
				statement = " AND " + DATABASE_TABLE_SETS + "." + KEY_DATE
						+ " <> FirstPrints." + KEY_DATE + statement;
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
				} else {
					sel += ", " + DATABASE_TABLE_CARDS + "." + s + " AS " + s;
				}
			}
			sel += ", " + DATABASE_TABLE_SETS + "." + KEY_DATE;

			String sql = "SELECT * FROM (SELECT " + sel + " FROM " + tbl
					+ " JOIN " + DATABASE_TABLE_SETS + " ON "
					+ DATABASE_TABLE_CARDS + "." + KEY_SET + " = "
					+ DATABASE_TABLE_SETS + "." + KEY_CODE + statement;

			if (consolidate) {
				sql += " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE
						+ ") GROUP BY " + KEY_NAME + " ORDER BY " + KEY_NAME + " COLLATE UNICODE";
			} else {
				sql += " ORDER BY " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " COLLATE UNICODE"
						+ ", " + DATABASE_TABLE_SETS + "." + KEY_DATE
						+ " DESC)";
			}
			mCursor = mDb.rawQuery(sql, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public Cursor PrefixSearch(String cardname, String[] returnTypes)
			throws FamiliarDbException {
		Cursor mCursor = null;
		String convertName = null;
		
		if (cardname != null) {
			cardname = cardname.replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]).trim();
			convertName = cardname.toLowerCase().replace("ae", String.valueOf(Character.toChars(0xC6)[0]));
		}

		String statement = " WHERE 1=1";

		statement += " AND (" + DATABASE_TABLE_CARDS + "." + KEY_NAME
				+ " LIKE " + DatabaseUtils.sqlEscapeString(cardname + "%") 
				+ " OR " + DATABASE_TABLE_CARDS + "." + KEY_NAME
				+ " LIKE " + DatabaseUtils.sqlEscapeString(convertName + "%") 
				+ ")";

		try {
			String sel = null;
			for (String s : returnTypes) {
				if (sel == null) {
					sel = DATABASE_TABLE_CARDS + "." + s + " AS " + s;
				} else {
					sel += ", " + DATABASE_TABLE_CARDS + "." + s + " AS " + s;
				}
			}
			sel += ", " + DATABASE_TABLE_SETS + "." + KEY_DATE;

			String sql = "SELECT * FROM (SELECT " + sel + " FROM "
					+ DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS
					+ " ON " + DATABASE_TABLE_CARDS + "." + KEY_SET + " = "
					+ DATABASE_TABLE_SETS + "." + KEY_CODE + statement;

			sql += " ORDER BY " + DATABASE_TABLE_SETS + "." + KEY_DATE
					+ ") GROUP BY " + KEY_NAME + " ORDER BY " + KEY_NAME + " COLLATE UNICODE";
			mCursor = mDb.rawQuery(sql, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public int getTransform(String set, String number)
			throws FamiliarDbException {
		Cursor mCursor = null;
		int ID = -1;
		String statement = "(" + KEY_NUMBER + " = '" + number + "') AND ("
				+ KEY_SET + " = '" + set + "')";
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS,
					new String[] { KEY_ID }, statement, null, null, null,
					KEY_ID, null);
			mCursor.moveToFirst();
			ID = mCursor.getInt(mCursor.getColumnIndex(KEY_ID));
			mCursor.close();
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
		return ID;
	}

	public String getTransformName(String set, String number)
			throws FamiliarDbException {
		Cursor mCursor = null;
		String name = null;
		String statement = "(" + KEY_NUMBER + " = '" + number + "') AND ("
				+ KEY_SET + " = '" + set + "')";
		try {
			mCursor = mDb.query(true, DATABASE_TABLE_CARDS,
					new String[] { KEY_NAME }, statement, null, null, null,
					KEY_NAME, null);
			mCursor.moveToFirst();
			name = mCursor.getString(mCursor.getColumnIndex(KEY_NAME));
			mCursor.close();
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		return name;
	}

	public void createLegalTables() throws FamiliarDbException {
		try {
			mDb.execSQL(DATABASE_CREATE_FORMATS);
			mDb.execSQL(DATABASE_CREATE_LEGAL_SETS);
			mDb.execSQL(DATABASE_CREATE_BANNED_CARDS);
		}
		catch(SQLiteException e) {
			throw new FamiliarDbException(e);
		}
	}

	public void dropLegalTables() throws FamiliarDbException {
		try{
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_FORMATS);
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_LEGAL_SETS);
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_BANNED_CARDS);
		}
		catch(SQLiteException e) {
			throw new FamiliarDbException(e);
		}
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

	public Cursor fetchAllFormats() throws FamiliarDbException {
		try {
			return mDb.query(DATABASE_TABLE_FORMATS, new String[] { KEY_ID,
					KEY_NAME, }, null, null, null, null, KEY_NAME);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
	}

	public int checkLegality(String mCardName, String format)
			throws FamiliarDbException {
		mCardName = mCardName.replace("'", "''").replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]);
		format = format.replace("'", "''"); // Just to be safe; remember Bobby
		// Tables
		try {
			// The new way (single query per type, should be much faster) - Alex
			String sql = "SELECT COALESCE(CASE (SELECT "
					+ KEY_SET
					+ " FROM "
					+ DATABASE_TABLE_CARDS
					+ " WHERE "
					+ KEY_NAME
					+ " = '"
					+ mCardName
					+ "') WHEN 'UG' THEN 1 WHEN 'UNH' THEN 1 WHEN 'ARS' THEN 1 WHEN 'PCP' THEN 1 "
					+ "WHEN 'PP2' THEN 1 ELSE NULL END, "
					+ "CASE (SELECT 1 FROM " + DATABASE_TABLE_CARDS
					+ " c INNER JOIN " + DATABASE_TABLE_LEGAL_SETS
					+ " ls ON ls." + KEY_SET + " = c." + KEY_SET + " WHERE ls."
					+ KEY_FORMAT + " = '" + format + "' AND c." + KEY_NAME
					+ " = '" + mCardName
					+ "') WHEN 1 THEN NULL ELSE CASE WHEN '" + format
					+ "' = 'Legacy' " + "THEN NULL WHEN '" + format
					+ "' = 'Vintage' THEN NULL ELSE 1 END END, (SELECT "
					+ KEY_LEGALITY + " from " + DATABASE_TABLE_BANNED_CARDS
					+ " WHERE " + KEY_NAME + " = '" + mCardName + "' AND "
					+ KEY_FORMAT + " = '" + format + "'), 0) AS "
					+ KEY_LEGALITY;

			Cursor c = null;
			c = mDb.rawQuery(sql, null);

			c.moveToFirst();
			int legality = c.getInt(c.getColumnIndex(KEY_LEGALITY));
			c.close();
			return legality;
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
	}

	public String getTCGname(String setCode) throws FamiliarDbException {
		try {
			String sql = "SELECT " + KEY_NAME_TCGPLAYER + " FROM " + DATABASE_TABLE_SETS + " WHERE " + KEY_CODE + " = '" + setCode.replace("'", "''") + "';";
			Cursor c = mDb.rawQuery(sql, null);
			c.moveToFirst();
			String TCGname = c.getString(c.getColumnIndex(KEY_NAME_TCGPLAYER));
			return TCGname;
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
	}

	public String getSetCode(String TCGname) throws FamiliarDbException {
		try {
			String sql = "SELECT " + KEY_CODE + " FROM " + DATABASE_TABLE_SETS + " WHERE " + KEY_NAME_TCGPLAYER + " = '" + TCGname.replace("'", "''") + "';";
			Cursor c = mDb.rawQuery(sql, null);
			c.moveToFirst();
			String setCode = c.getString(c.getColumnIndex(KEY_CODE));
			return setCode;
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
	}

	public Cursor fetchAllTcgNames() throws FamiliarDbException {

		Cursor c = null;
		try {
			c = mDb.query(DATABASE_TABLE_SETS, new String[] {
					KEY_NAME_TCGPLAYER, KEY_CODE }, null, null, null, null,
					KEY_DATE + " DESC");
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}

		return c;
	}
	
	public boolean isModernLegalSet(String setName) throws FamiliarDbException {
		try {
			String sql = "SELECT " + KEY_SET + " FROM " + DATABASE_TABLE_LEGAL_SETS + " WHERE " + KEY_SET + " = '" + setName.replace("'", "''") + "';";
			Cursor c = mDb.rawQuery(sql, null);
			if (c.getCount() >= 1)
				return true;
			else
				return false;
			
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
	}

	public Cursor getRules(int category, int subcategory)
			throws FamiliarDbException {
		try {
			if (category == -1) {
				// No category specified; return the main categories
				String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
						+ " WHERE " + KEY_SUBCATEGORY + " = -1";
				return mDb.rawQuery(sql, null);
			} else if (subcategory == -1) {
				// No subcategory specified; return the subcategories under the
				// given
				// category
				String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
						+ " WHERE " + KEY_CATEGORY + " = "
						+ String.valueOf(category) + " AND " + KEY_SUBCATEGORY
						+ " > -1 AND " + KEY_ENTRY + " IS NULL";
				return mDb.rawQuery(sql, null);
			} else {
				// Both specified; return the rules under the given subcategory
				String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
						+ " WHERE " + KEY_CATEGORY + " = "
						+ String.valueOf(category) + " AND " + KEY_SUBCATEGORY
						+ " = " + String.valueOf(subcategory) + " AND "
						+ KEY_ENTRY + " IS NOT NULL";
				return mDb.rawQuery(sql, null);
			}
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
	}
	
	public Cursor getRulesByKeyword(String keyword, int category,
			int subcategory) throws FamiliarDbException {
		try {
			// Don't let them pass in an empty string; it'll return ALL the
			// rules
			if (keyword != null && !keyword.trim().equals("")) {
				keyword = "'%" + keyword.replace("'", "''") + "%'";

				if (category == -1) {
					// No category; we're searching from the main page, so no
					// restrictions
					String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
							+ " WHERE " + KEY_RULE_TEXT + " LIKE " + keyword
							+ " AND " + KEY_ENTRY + " IS NOT NULL";
					return mDb.rawQuery(sql, null);
				} else if (subcategory == -1) {
					// No subcategory; we're searching from a category page, so
					// restrict
					// within that
					String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
							+ " WHERE " + KEY_RULE_TEXT + " LIKE " + keyword
							+ " AND " + KEY_ENTRY + " IS NOT NULL AND "
							+ KEY_CATEGORY + " = " + String.valueOf(category);
					return mDb.rawQuery(sql, null);
				} else {
					// We're searching within a subcategory, so restrict within
					// that
					String sql = "SELECT * FROM " + DATABASE_TABLE_RULES
							+ " WHERE " + KEY_RULE_TEXT + " LIKE " + keyword
							+ " AND " + KEY_ENTRY + " IS NOT NULL AND "
							+ KEY_CATEGORY + " = " + String.valueOf(category)
							+ " AND " + KEY_SUBCATEGORY + " = "
							+ String.valueOf(subcategory);
					return mDb.rawQuery(sql, null);
				}
			}
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
		return null;
	}

	public int getRulePosition(int category, int subcategory, String entry)
			throws FamiliarDbException {
		try {
			if (entry != null) {
				String sql = "SELECT " + KEY_POSITION + " FROM "
						+ DATABASE_TABLE_RULES + " WHERE " + KEY_CATEGORY
						+ " = " + String.valueOf(category) + " AND "
						+ KEY_SUBCATEGORY + " = " + String.valueOf(subcategory)
						+ " AND " + KEY_ENTRY + " = '"
						+ entry.replace("'", "''") + "'";
				Cursor c = mDb.rawQuery(sql, null);
				if (c != null) {
					c.moveToFirst();
					int result = c.getInt(c.getColumnIndex(KEY_POSITION));
					c.close();
					return result;
				}
			}
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
		return 0;
	}

	public String getCategoryName(int category, int subcategory)
			throws FamiliarDbException {
		try {
			String sql = "SELECT " + KEY_RULE_TEXT + " FROM "
					+ DATABASE_TABLE_RULES + " WHERE " + KEY_CATEGORY + " = "
					+ String.valueOf(category) + " AND " + KEY_SUBCATEGORY
					+ " = " + String.valueOf(subcategory) + " AND " + KEY_ENTRY
					+ " IS NULL";
			Cursor c = mDb.rawQuery(sql, null);
			if (c != null) {
				c.moveToFirst();
				String result = c.getString(c.getColumnIndex(KEY_RULE_TEXT));
				c.close();
				return result;
			}
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
		return "";
	}

	public Cursor getGlossaryTerms() throws FamiliarDbException {
		try {
			String sql = "SELECT * FROM " + DATABASE_TABLE_GLOSSARY;
			return mDb.rawQuery(sql, null);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
	}

	public void dropRulesTables() throws FamiliarDbException {
		try{
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RULES);
			mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_GLOSSARY);
		}
		catch(SQLiteException e) {
			throw new FamiliarDbException(e);
		}
	}

	public void createRulesTables() throws FamiliarDbException {
		try{
			mDb.execSQL(DATABASE_CREATE_RULES);
			mDb.execSQL(DATABASE_CREATE_GLOSSARY);
		}
		catch(SQLiteException e) {
			throw new FamiliarDbException(e);
		}
	}

	public void insertRule(int category, int subcategory, String entry,
			String text, int position) throws FamiliarDbException {
		if (entry == null) {
			entry = "NULL";
		} else {
			entry = "'" + entry.replace("'", "''") + "'";
		}
		text = "'" + text.replace("'", "''") + "'";
		String positionStr;
		if (position < 0) {
			positionStr = "NULL";
		} else {
			positionStr = String.valueOf(position);
		}
		String sql = "INSERT INTO " + DATABASE_TABLE_RULES + " ("
				+ KEY_CATEGORY + ", " + KEY_SUBCATEGORY + ", " + KEY_ENTRY
				+ ", " + KEY_RULE_TEXT + ", " + KEY_POSITION + ") VALUES ("
				+ String.valueOf(category) + ", " + String.valueOf(subcategory)
				+ ", " + entry + ", " + text + ", " + positionStr + ");";
		try{
			mDb.execSQL(sql);
		}
		catch(SQLiteException e) {
			throw new FamiliarDbException(e);
		}
	}

	public void insertGlossaryTerm(String term, String definition) throws FamiliarDbException {
		term = "'" + term.replace("'", "''") + "'";
		definition = "'" + definition.replace("'", "''") + "'";
		String sql = "INSERT INTO " + DATABASE_TABLE_GLOSSARY + " (" + KEY_TERM
				+ ", " + KEY_DEFINITION + ") VALUES (" + term + ", "
				+ definition + ");";
		try{
			mDb.execSQL(sql);
		}
		catch(SQLiteException e) {
			throw new FamiliarDbException(e);
		}
	}

	/**
	 * Builds a map for all columns that may be requested, which will be given
	 * to the SQLiteQueryBuilder. This is a good way to define aliases for
	 * column names, but must include all columns, even if the value is the key.
	 * This allows the ContentProvider to request columns w/o the need to know
	 * real column names and create the alias itself.
	 */
	private static HashMap<String, String> buildColumnMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(KEY_NAME, KEY_NAME);
		map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS "
				+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS "
				+ SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
		return map;
	}

	/**
	 * Performs a database query.
	 * 
	 * @param selection
	 *            The selection clause
	 * @param selectionArgs
	 *            Selection arguments for "?" components in the selection
	 * @param columns
	 *            The columns to return
	 * @return A Cursor over all rows matching the query
	 * @throws FamiliarDbException
	 */
	private Cursor query(String selection, String[] selectionArgs,
			String[] columns) throws FamiliarDbException {
		/*
		 * The SQLiteBuilder provides a map for all possible columns requested
		 * to actual columns in the database, creating a simple column alias
		 * mechanism by which the ContentProvider does not need to know the real
		 * column names
		 */
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(DATABASE_TABLE_CARDS);
		builder.setProjectionMap(mColumnMap);

		Cursor cursor = null;
		try {
			cursor = builder.query(mDb, columns, selection, selectionArgs,
					KEY_NAME, null, KEY_NAME);
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
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
	 *            id of word to retrieve
	 * @param columns
	 *            The columns to include, if null then all are included
	 * @return Cursor positioned to matching word, or null if not found.
	 * @throws FamiliarDbException
	 */
	public Cursor getWord(String rowId, String[] columns)
			throws FamiliarDbException {
		String selection = "rowid = ?";
		String[] selectionArgs = new String[] { rowId };

		return query(selection, selectionArgs, columns);

		/*
		 * This builds a query that looks like: SELECT <columns> FROM <table>
		 * WHERE rowid = <rowId>
		 */
	}

	/**
	 * Returns a Cursor over all words that match the given query
	 * 
	 * @param query
	 *            The string to search for
	 * @param columns
	 *            The columns to include, if null then all are included
	 * @return Cursor over all words that match, or null if none found.
	 * @throws FamiliarDbException
	 */
	public Cursor getWordMatches(String query, String[] columns)
			throws FamiliarDbException {
		
		query = query.replace("'", "''").replace(Character.toChars(0xE6)[0], Character.toChars(0xC6)[0]).trim();
		String convert = query.toLowerCase().replace("ae", String.valueOf(Character.toChars(0xC6)[0]));

		if(query.length() < 2) {
			return null;
		}
		
		String sql =
				"SELECT * FROM (" + 
				"SELECT " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " AS " + KEY_NAME + ", " + DATABASE_TABLE_CARDS + "." + KEY_ID + " AS " + KEY_ID + ", " + DATABASE_TABLE_CARDS + "." + KEY_ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID +
				" FROM " + DATABASE_TABLE_CARDS + " JOIN " + DATABASE_TABLE_SETS + " ON " + DATABASE_TABLE_SETS + "." + KEY_CODE + " = " + DATABASE_TABLE_CARDS + "." + KEY_SET +
				" WHERE " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '" + query + "%'"
				+ " OR " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " LIKE '" + convert + "%'" +
				" ORDER BY " + DATABASE_TABLE_CARDS + "." + KEY_NAME + " COLLATE UNICODE, " + DATABASE_TABLE_SETS + "." + KEY_DATE + " ASC " +
				") GROUP BY " + KEY_NAME;
		return mDb.rawQuery(sql, null);
	}

	public static boolean isDbOutOfDate(Context ctx) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		File f = new File(DB_PATH, DB_NAME);
		int dbVersion = preferences.getInt("databaseVersion", -1);
		if (!f.exists() || f.length() < 1048576
				|| dbVersion < CardDbAdapter.DATABASE_VERSION) {
			return true;
		}
		return false;
	}

	public static void copyDB(Context ctx) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(ctx);
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

				GZIPInputStream gis = new GZIPInputStream(ctx.getResources()
						.openRawResource(R.raw.db));
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
		} catch (NotFoundException e) {
		} catch (IOException e) {
		} catch (Exception e) {
		}
	}

	public boolean isSplitCard(long multiverseId) throws FamiliarDbException {
		Cursor mCursor = null;
		String statement = "SELECT " + KEY_NAME + " from "
				+ DATABASE_TABLE_CARDS + " WHERE " + KEY_MULTIVERSEID + " = "
				+ multiverseId;

		try {
			mCursor = mDb.rawQuery(statement, null);
			int numRows = mCursor.getCount();
			mCursor.close();

			if (numRows == 1) {
				return false;
			} else if (numRows == 2) {
				return true;
			}
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
		return false;
	}
	
	public int getSplitMultiverseID(String name) throws FamiliarDbException {
		Cursor mCursor = null;
		String statement = "SELECT " + KEY_MULTIVERSEID + " from "
				+ DATABASE_TABLE_CARDS + " WHERE " + KEY_NAME + " = '"
				+ name +"'";

		try {
			mCursor = mDb.rawQuery(statement, null);

			if (mCursor.getCount() == 1) {
				mCursor.moveToFirst();
				int retVal = mCursor.getInt(mCursor
						.getColumnIndex(KEY_MULTIVERSEID));
				mCursor.close();
				return retVal;
			} else {
				mCursor.close();
				return -1;
			}
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
	}

	public String getSplitName(int multiverseId) throws FamiliarDbException {
		Cursor mCursor = null;
		String statement = "SELECT " + KEY_NAME + ", " + KEY_NUMBER + " from "
				+ DATABASE_TABLE_CARDS + " WHERE " + KEY_MULTIVERSEID + " = "
				+ multiverseId + " ORDER BY " + KEY_NUMBER + " ASC";

		try {
			mCursor = mDb.rawQuery(statement, null);

			if (mCursor.getCount() == 2) {
				mCursor.moveToFirst();
				String retVal = mCursor.getString(mCursor
						.getColumnIndex(KEY_NAME));
				retVal += " // ";
				mCursor.moveToNext();
				retVal += mCursor.getString(mCursor.getColumnIndex(KEY_NAME));
				mCursor.close();
				return retVal;
			} else {
				mCursor.close();
				return null;
			}
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
	}
	
    public static String removeAccentMarks(String s) {
        return s.replace(Character.toChars(0xC0)[0]+"", "A")
        		.replace(Character.toChars(0xC1)[0]+"", "A")
        		.replace(Character.toChars(0xC2)[0]+"", "A")
        		.replace(Character.toChars(0xC3)[0]+"", "A")
        		.replace(Character.toChars(0xC4)[0]+"", "A")
        		.replace(Character.toChars(0xC5)[0]+"", "A")
        		.replace(Character.toChars(0xC6)[0]+"", "Ae")
        		.replace(Character.toChars(0xC7)[0]+"", "C")
        		.replace(Character.toChars(0xC8)[0]+"", "E")
        		.replace(Character.toChars(0xC9)[0]+"", "E")
        		.replace(Character.toChars(0xCA)[0]+"", "E")
        		.replace(Character.toChars(0xCB)[0]+"", "E")
        		.replace(Character.toChars(0xCC)[0]+"", "I")
        		.replace(Character.toChars(0xCD)[0]+"", "I")
        		.replace(Character.toChars(0xCE)[0]+"", "I")
        		.replace(Character.toChars(0xCF)[0]+"", "I")
        		.replace(Character.toChars(0xD0)[0]+"", "D")
        		.replace(Character.toChars(0xD1)[0]+"", "N")
        		.replace(Character.toChars(0xD2)[0]+"", "O")
        		.replace(Character.toChars(0xD3)[0]+"", "O")
        		.replace(Character.toChars(0xD4)[0]+"", "O")
        		.replace(Character.toChars(0xD5)[0]+"", "O")
        		.replace(Character.toChars(0xD6)[0]+"", "O")
        		.replace(Character.toChars(0xD7)[0]+"", "x")
        		.replace(Character.toChars(0xD8)[0]+"", "O")
        		.replace(Character.toChars(0xD9)[0]+"", "U")
        		.replace(Character.toChars(0xDA)[0]+"", "U")
        		.replace(Character.toChars(0xDB)[0]+"", "U")
        		.replace(Character.toChars(0xDC)[0]+"", "U")
        		.replace(Character.toChars(0xDD)[0]+"", "Y")
        		.replace(Character.toChars(0xE0)[0]+"", "a")
        		.replace(Character.toChars(0xE1)[0]+"", "a")
        		.replace(Character.toChars(0xE2)[0]+"", "a")
        		.replace(Character.toChars(0xE3)[0]+"", "a")
        		.replace(Character.toChars(0xE4)[0]+"", "a")
        		.replace(Character.toChars(0xE5)[0]+"", "a")
        		.replace(Character.toChars(0xE6)[0]+"", "ae")
        		.replace(Character.toChars(0xE7)[0]+"", "c")
        		.replace(Character.toChars(0xE8)[0]+"", "e")
        		.replace(Character.toChars(0xE9)[0]+"", "e")
        		.replace(Character.toChars(0xEA)[0]+"", "e")
        		.replace(Character.toChars(0xEB)[0]+"", "e")
        		.replace(Character.toChars(0xEC)[0]+"", "i")
        		.replace(Character.toChars(0xED)[0]+"", "i")
        		.replace(Character.toChars(0xEE)[0]+"", "i")
        		.replace(Character.toChars(0xEF)[0]+"", "i")
        		.replace(Character.toChars(0xF1)[0]+"", "n")
        		.replace(Character.toChars(0xF2)[0]+"", "o")
        		.replace(Character.toChars(0xF3)[0]+"", "o")
        		.replace(Character.toChars(0xF4)[0]+"", "o")
        		.replace(Character.toChars(0xF5)[0]+"", "o")
        		.replace(Character.toChars(0xF6)[0]+"", "o")
        		.replace(Character.toChars(0xF8)[0]+"", "o")
        		.replace(Character.toChars(0xF9)[0]+"", "u")
        		.replace(Character.toChars(0xFA)[0]+"", "u")
        		.replace(Character.toChars(0xFB)[0]+"", "u")
        		.replace(Character.toChars(0xFC)[0]+"", "u")
        		.replace(Character.toChars(0xFD)[0]+"", "y")
        		.replace(Character.toChars(0xFF)[0]+"", "y");
    }

	public int fetchMultiverseId(String name, String setCode) throws FamiliarDbException{
		name = name.replace("'", "''"); // Sanitization
		setCode = setCode.replace("'", "''"); // Sanitization
		String sql = "SELECT " + KEY_MULTIVERSEID + " FROM " + DATABASE_TABLE_CARDS + " WHERE " +
				KEY_NAME + " = '" + name + "' AND " +
				KEY_SET +" = '" + setCode + "'";
		try {
			Cursor mCursor = mDb.rawQuery(sql, null);
			if(mCursor.getCount() > 0) {
				mCursor.moveToFirst();
				return mCursor.getInt(mCursor.getColumnIndex(KEY_MULTIVERSEID));
			}
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		}
		return -1;
	}
	
	public static final int NOPE = 0;
	public static final int TRANSFORM = 1;
	public static final int FUSE = 2;
	public static final int SPLIT = 3;

	public static int isMulticard(String number, String setCode) {
		if (number.contains("a") || number.contains("b")) {
			if (setCode.compareTo("ISD") == 0 || setCode.compareTo("DKA") == 0) {
				return TRANSFORM;
			}
			else if (setCode.compareTo("DGM") == 0) {
				return FUSE;
			}
			else {
				return SPLIT;
			}
		}
		return NOPE;
	}
	
	public String getImageSearchNameFromMultiverseID(long multiverseID) throws FamiliarDbException {
				
		Cursor mCursor = null;
		String statement = "SELECT " + KEY_NAME + " from "
				+ DATABASE_TABLE_CARDS + " WHERE " + KEY_MULTIVERSEID + " = "
				+ multiverseID;

		try {
			mCursor = mDb.rawQuery(statement, null);
			
			if(mCursor.getCount() > 0) {
				mCursor.moveToFirst();
				String name = mCursor.getString(mCursor.getColumnIndex(KEY_NAME));
				mCursor.close();
				return name;
			}
			else {
				URL url = new URL("http://93.103.149.115/card/" + multiverseID);
				InputStream is = url.openStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(is)));
	
				String json = "", line;
				while ((line = br.readLine()) != null) {
					json += line;
				}
				JSONObject jo = new JSONObject(json);
				return jo.getString("name");
			}			
		} catch (SQLiteException e) {
			throw new FamiliarDbException(e);
		} catch (IllegalStateException e) {
			throw new FamiliarDbException(e);
		} catch (CursorIndexOutOfBoundsException e) {
			throw new FamiliarDbException(e);
		} catch (MalformedURLException e) {
			throw new FamiliarDbException(e);
		} catch (IOException e) {
			throw new FamiliarDbException(e);
		} catch (JSONException e) {
			throw new FamiliarDbException(e);
		}
	}
}
