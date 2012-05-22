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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class RulesActivity extends FragmentActivity {

	public static String CATEGORY_KEY = "category";
	public static String SUBCATEGORY_KEY = "subcategory";
	public static String POSITION_KEY = "position";
	
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
		if(extras == null) {
			category = -1;
			subcategory = -1;
			position = 0;
		}
		else {
			category = extras.getInt(CATEGORY_KEY, -1);
			subcategory = extras.getInt(SUBCATEGORY_KEY, -1);
			position = extras.getInt(POSITION_KEY, 0);
		}
		
		list = (ListView)findViewById(R.id.rules_list);
		
		ruleCursor = mDbHelper.getRules(category, subcategory);
		if(ruleCursor != null) {
			ruleCursor.moveToFirst();
			list.setAdapter(new SimpleCursorAdapter(this, R.layout.rules_list_item, ruleCursor, 
					new String[] { CardDbAdapter.KEY_RULE_TEXT, CardDbAdapter.KEY_CATEGORY, CardDbAdapter.KEY_SUBCATEGORY }, 
					new int[] { R.id.rules_item_text, R.id.rules_item_category, R.id.rules_item_subcategory }));
			
			if(subcategory == -1) {
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
		
		list.setSelection(position);
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
}
