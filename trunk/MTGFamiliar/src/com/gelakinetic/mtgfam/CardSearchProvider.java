/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gelakinetic.mtgfam;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Provides access to the dictionary database.
 */
public class CardSearchProvider extends ContentProvider {
	String													TAG										= "CardSearchProvider";

	public static String						AUTHORITY							= "com.gelakinetic.mtgfam.CardSearchProvider";
	public static final Uri					CONTENT_URI						= Uri.parse("content://" + AUTHORITY + "/cards");

	// MIME types used for searching words or looking up a single definition
	public static final String			WORDS_MIME_TYPE				= ContentResolver.CURSOR_DIR_BASE_TYPE
																														+ "/vnd.gelakinetic.mtgfam";
	public static final String			DEFINITION_MIME_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE
																														+ "/vnd.gelakinetic.mtgfam";

	// private DictionaryDatabase mDictionary;

	private CardDbAdapter						mDbAdapter;

	// UriMatcher stuff
	private static final int				SEARCH_WORDS					= 0;
	private static final int				GET_WORD							= 1;
	private static final int				SEARCH_SUGGEST				= 2;
	private static final int				REFRESH_SHORTCUT			= 3;
	private static final UriMatcher	sURIMatcher						= buildUriMatcher();

	/**
	 * Builds up a UriMatcher for search suggestion and shortcut refresh queries.
	 */
	private static UriMatcher buildUriMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		// to get definitions...
		matcher.addURI(AUTHORITY, "cards", SEARCH_WORDS);
		matcher.addURI(AUTHORITY, "cards/#", GET_WORD);
		// to get suggestions...
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

		return matcher;
	}

	@Override
	public boolean onCreate() {
		// mDictionary = new DictionaryDatabase(getContext());
		mDbAdapter = new CardDbAdapter(getContext());
		mDbAdapter.open();
		return true;
	}

	/**
	 * Handles all the dictionary searches and suggestion queries from the Search
	 * Manager. When requesting a specific word, the uri alone is required. When
	 * searching all of the dictionary for matches, the selectionArgs argument
	 * must carry the search query as the first element. All other arguments are
	 * ignored.
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		// Use the UriMatcher to see what kind of query we have and format the db
		// query accordingly
		switch (sURIMatcher.match(uri)) {
			case SEARCH_SUGGEST:
				if (selectionArgs == null) {
					throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
				}
				return getSuggestions(selectionArgs[0]);
			case SEARCH_WORDS:
				if (selectionArgs == null) {
					throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
				}
				return search(selectionArgs[0]);
			case GET_WORD:
				return getWord(uri);
			case REFRESH_SHORTCUT:
				return refreshShortcut(uri);
			default:
				throw new IllegalArgumentException("Unknown Uri: " + uri);
		}
	}

	/*
	private Cursor getSuggestions(String query) {
		String[] columns = new String[] { BaseColumns._ID, CardDbAdapter.KEY_NAME };

		return mDbAdapter.Search(query, null, null, "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
				CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, true, columns, true);
	}

	private Cursor search(String query) {
		String[] columns = new String[] { BaseColumns._ID, CardDbAdapter.KEY_NAME };

		return mDbAdapter.Search(query, null, null, "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
				CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, true, columns, true);
	}
*/
	
  private Cursor getSuggestions(String query) {
    query = query.toLowerCase();
    String[] columns = new String[] {
        BaseColumns._ID,
        CardDbAdapter.KEY_NAME,
     /* SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
                      (only if you want to refresh shortcuts) */
        SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

    return mDbAdapter.getWordMatches(query, columns);
  }

  private Cursor search(String query) {
    query = query.toLowerCase();
    String[] columns = new String[] {
        BaseColumns._ID,
        CardDbAdapter.KEY_NAME};

    return mDbAdapter.getWordMatches(query, columns);
  }
	/*
	private Cursor getWord(Uri uri) {
		String rowId = uri.getLastPathSegment();
		String[] columns = new String[] { CardDbAdapter.KEY_NAME
		// DictionaryDatabase.KEY_WORD,
		// DictionaryDatabase.KEY_DEFINITION
		};

		return mDbAdapter.fetchCard(Long.parseLong(rowId), columns);// mDictionary.getWord(rowId,
																																// columns);
	}
*/
  private Cursor getWord(Uri uri) {
    String rowId = uri.getLastPathSegment();
    String[] columns = new String[] {
        CardDbAdapter.KEY_NAME};

    return mDbAdapter.getWord(rowId, columns);
  }
	
  /*
	 * This won't be called with the current implementation, but if we include
	 * {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID} as a column in our
	 * suggestions table, we could expect to receive refresh queries when a
	 * shortcutted suggestion is displayed in Quick Search Box. In which case,
	 * this method will query the table for the specific word, using the given
	 * item Uri and provide all the columns originally provided with the
	 * suggestion query.
	 */

  /*
	private Cursor refreshShortcut(Uri uri) {
		String rowId = uri.getLastPathSegment();
		String[] columns = new String[] { BaseColumns._ID,
				// DictionaryDatabase.KEY_WORD,
				// DictionaryDatabase.KEY_DEFINITION,
				CardDbAdapter.KEY_NAME, SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID };

		// return mDictionary.getWord(rowId, columns);
		return mDbAdapter.fetchCard(Long.parseLong(rowId), columns);
	}
*/

private Cursor refreshShortcut(Uri uri) {
  /* This won't be called with the current implementation, but if we include
   * {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID} as a column in our suggestions table, we
   * could expect to receive refresh queries when a shortcutted suggestion is displayed in
   * Quick Search Box. In which case, this method will query the table for the specific
   * word, using the given item Uri and provide all the columns originally provided with the
   * suggestion query.
   */
  String rowId = uri.getLastPathSegment();
  String[] columns = new String[] {
      BaseColumns._ID,
      CardDbAdapter.KEY_NAME,
      SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
      SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID};

  return mDbAdapter.getWord(rowId, columns);
}

	/**
	 * This method is required in order to query the supported types. It's also
	 * useful in our own query() method to determine the type of Uri received.
	 */
	@Override
	public String getType(Uri uri) {
		switch (sURIMatcher.match(uri)) {
			case SEARCH_WORDS:
				return WORDS_MIME_TYPE;
			case GET_WORD:
				return DEFINITION_MIME_TYPE;
			case SEARCH_SUGGEST:
				return SearchManager.SUGGEST_MIME_TYPE;
			case REFRESH_SHORTCUT:
				return SearchManager.SHORTCUT_MIME_TYPE;
			default:
				throw new IllegalArgumentException("Unknown URL " + uri);
		}
	}

	// Other required implementations...

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

}
