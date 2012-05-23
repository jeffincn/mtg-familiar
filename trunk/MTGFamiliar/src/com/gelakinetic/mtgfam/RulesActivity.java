/**
Copyright 2012 Alex Levine

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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class RulesActivity extends FragmentActivity {

	public static String CATEGORY_KEY = "category";
	public static String SUBCATEGORY_KEY = "subcategory";
	public static String POSITION_KEY = "position";
	public static String KEYWORD_KEY = "keyword";
	
	private static final int SEARCH = 0;
	private static final int QUIT_TO_MAIN = 1;
	
	private CardDbAdapter mDbHelper;
	private ListView list;
	private Cursor ruleCursor;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rules_activity);
		
		MenuFragmentCompat.init(this, R.menu.rules_menu, "rules_menu_fragment");
		
		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();
		
		Bundle extras = getIntent().getExtras();
		int category, subcategory, position;
		String keyword;
		if(extras == null) {
			category = -1;
			subcategory = -1;
			position = 0;
			keyword = null;
		}
		else {
			category = extras.getInt(CATEGORY_KEY, -1);
			subcategory = extras.getInt(SUBCATEGORY_KEY, -1);
			position = extras.getInt(POSITION_KEY, 0);
			keyword = extras.getString(KEYWORD_KEY);
		}
		
		list = (ListView)findViewById(R.id.rules_list);
		boolean clickable;
		
		if(keyword == null) {
			ruleCursor = mDbHelper.getRules(category, subcategory);
			clickable = subcategory == -1;
		}
		else {
			ruleCursor = mDbHelper.getRulesByKeyword(keyword);
			clickable = false;
		}
		if(ruleCursor != null && ruleCursor.getCount() > 0) {
			ruleCursor.moveToFirst();
			list.setAdapter(new SimpleCursorAdapter(this, R.layout.rules_list_item, ruleCursor, 
					new String[] { CardDbAdapter.KEY_RULE_TEXT, CardDbAdapter.KEY_CATEGORY, CardDbAdapter.KEY_SUBCATEGORY }, 
					new int[] { R.id.rules_item_text, R.id.rules_item_category, R.id.rules_item_subcategory }));
			
			if(clickable) {
				list.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						String category = ((TextView)view.findViewById(R.id.rules_item_category)).getText().toString();
						String subcategory = ((TextView)view.findViewById(R.id.rules_item_subcategory)).getText().toString();
						int catInt, subcatInt;
						try {
							catInt = Integer.parseInt(category);
						}
						catch (NumberFormatException e) {
							catInt = -1;
						}
						try {
							subcatInt = Integer.parseInt(subcategory);
						}
						catch (NumberFormatException e) {
							subcatInt = -1;
						}
						
						Intent i = new Intent(RulesActivity.this, RulesActivity.class);
						i.putExtra(CATEGORY_KEY, catInt);
						i.putExtra(SUBCATEGORY_KEY, subcatInt);
						startActivity(i);
					}
				});
			}
		}
		else {
			Toast.makeText(this, "No results found.", Toast.LENGTH_LONG).show();
			this.finish();
		}
		
		list.setSelection(position);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(((MyApp)getApplicationContext()).getState() == QUIT_TO_MAIN) {
			this.finish();
			return;
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(ruleCursor != null) {
			ruleCursor.close();
		}
		if(mDbHelper != null) {
			mDbHelper.close();
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog result = null;
		
		if(id == SEARCH) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.rules_search_title);
			final EditText input = new EditText(this);
			builder.setView(input);
			builder.setPositiveButton(R.string.dialog_ok, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					String keyword = input.getText().toString();
					Intent i = new Intent(RulesActivity.this, RulesActivity.class);
					i.putExtra(KEYWORD_KEY, keyword);
					startActivity(i);
				}
			});
			builder.setNegativeButton(R.string.dialog_cancel, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					//Do nothing
				}
			});
			result = builder.create();
		}
		
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.rules_menu_search:
				//Toast.makeText(this, "Search", Toast.LENGTH_LONG).show();
				showDialog(SEARCH);
				return true;
			case R.id.rules_menu_exit:
				((MyApp)getApplicationContext()).setState(QUIT_TO_MAIN);
				this.finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
