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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html.ImageGetter;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

public class RulesActivity extends FragmentActivity {

	public static String CATEGORY_KEY = "category";
	public static String SUBCATEGORY_KEY = "subcategory";
	public static String POSITION_KEY = "position";
	public static String KEYWORD_KEY = "keyword";
	
	private static final int SEARCH = 0;
	private static final int RESULT_NORMAL = 1;
	private static final int RESULT_QUIT_TO_MAIN = 2;
	private static final int ARBITRARY_REQUEST_CODE = 23;
	
	private CardDbAdapter mDbHelper;
	private ImageGetter imgGetter;
	private ListView list;
	private RulesListAdapter adapter;
	private ArrayList<RuleItem> rules;
	private String keyword;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rules_activity);
		
		MenuFragmentCompat.init(this, R.menu.rules_menu, "rules_menu_fragment");
		
		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();
		
		imgGetter = ImageGetterHelper.GlyphGetter(getResources());
		
		Bundle extras = getIntent().getExtras();
		int category, subcategory, position;
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
		rules = new ArrayList<RuleItem>();
		boolean clickable;
		Cursor c;
		
		if(keyword == null) {
			c = mDbHelper.getRules(category, subcategory);
			clickable = subcategory == -1;
		}
		else {
			c = mDbHelper.getRulesByKeyword(keyword);
			clickable = false;
		}
		if(c != null && c.getCount() > 0) {
			c.moveToFirst();
			while(!c.isAfterLast()) {
				rules.add(new RuleItem(c.getInt(c.getColumnIndex(CardDbAdapter.KEY_CATEGORY)), c.getInt(c.getColumnIndex(CardDbAdapter.KEY_SUBCATEGORY)),
						c.getString(c.getColumnIndex(CardDbAdapter.KEY_ENTRY)), c.getString(c.getColumnIndex(CardDbAdapter.KEY_RULE_TEXT))));
				c.moveToNext();
			}
			c.close();
			adapter = new RulesListAdapter(this, R.layout.rules_list_item, rules);
			list.setAdapter(adapter);
			
			if(clickable) {
				list.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						RuleItem item = rules.get(position);
						Intent i = new Intent(RulesActivity.this, RulesActivity.class);
						i.putExtra(CATEGORY_KEY, item.getCategory());
						i.putExtra(SUBCATEGORY_KEY, item.getSubcategory());
						startActivityForResult(i, ARBITRARY_REQUEST_CODE);
					}
				});
			}
		}
		else {
			Toast.makeText(this, "No results found.", Toast.LENGTH_LONG).show();
			this.finish();
		}
		
		list.setSelection(position);
		
		setResult(RESULT_NORMAL);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MyApp appState = ((MyApp) getApplicationContext());
		appState.setState(0);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
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
					String keyword = input.getText().toString().trim();
					if(keyword.length() < 3) {
						Toast.makeText(RulesActivity.this, "Your search term must be at least 3 characters long.", Toast.LENGTH_LONG).show();
					}
					else {
						Intent i = new Intent(RulesActivity.this, RulesActivity.class);
						i.putExtra(KEYWORD_KEY, keyword);
						startActivityForResult(i, ARBITRARY_REQUEST_CODE);
					}
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == ARBITRARY_REQUEST_CODE) {
			if(resultCode == RESULT_QUIT_TO_MAIN) {
				setResult(RESULT_QUIT_TO_MAIN);
				finish();
			}
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.rules_menu_search:
				showDialog(SEARCH);
				return true;
			case R.id.rules_menu_exit:
				setResult(RESULT_QUIT_TO_MAIN);
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private SpannableString formatText(String input) {
		CharSequence cs = Html.fromHtml(input.replace("_", "").replace("{", "<img src=\"").replace("}", "\"/>"), imgGetter, null); 
		SpannableString result = new SpannableString(cs);
		int index;
		
		//First, handle italicizing any words/phrases surrounded by underscores
		ArrayList<Integer> starts = new ArrayList<Integer>();
		ArrayList<Integer> ends = new ArrayList<Integer>();
		boolean start = true;
		index = input.indexOf("_");
		while(index != -1) {
			if(start) {
				starts.add(index);
				start = false;
			}
			else {
				ends.add(index - 1);
				start = true;
			}
			index = input.indexOf("_", index + 1);
		}
		
		if(starts.size() == ends.size()) {
			//In case we had some weirdness and not all pairs match, we won't bother italicizing
			for(int i = 0; i < starts.size(); i++) {
				result.setSpan(new StyleSpan(Typeface.ITALIC), starts.get(i) - 2 * i, ends.get(i) - 2 * i, 0);
			}
		}
		
		//Next, handle the keyword highlighting (if applicable)
		//Don't do it if it's null or if it contains { or } (they get replaced by images)
		if(keyword != null && !keyword.contains("{") && !keyword.contains("}")) {
			String loweredInput = input.replace("_", "").toLowerCase();
			String loweredKeyword = keyword.toLowerCase();
			index = loweredInput.indexOf(loweredKeyword);
			while(index != -1) {
				int end = index + keyword.length();
				result.setSpan(new ForegroundColorSpan(Color.YELLOW), index, end, 0);
				index = loweredInput.indexOf(loweredKeyword, end);
			}
		}
		
		//Finally, handle hyperlinking
		//TODO - Actually implement this
		
		return result;
	}
	
	private class RuleItem {
		private int category;
		private int subcategory;
		private String entry;
		private String rulesText;
		
		public RuleItem(int category, int subcategory, String entry, String rulesText) {
			this.category = category;
			this.subcategory = subcategory;
			this.entry = entry;
			this.rulesText = rulesText;
		}
		
		public int getCategory() {
			return this.category;
		}
		
		public int getSubcategory() {
			return this.subcategory;
		}
		
		public String getRulesText() {
			return this.rulesText;
		}
		
		public String getHeader() {
			if(this.subcategory == -1) {
				return String.valueOf(this.category) + ".";
			}
			else if(this.entry == null) {
				return String.valueOf((this.category * 100) + this.subcategory) + ".";
			}
			else {
				return String.valueOf((this.category * 100 + this.subcategory)) + "." + this.entry;
			}
		}
	}
	
	private class RulesListAdapter extends ArrayAdapter<RuleItem> {
		private int layoutResourceId;
		private ArrayList<RuleItem> items;

		public RulesListAdapter(Context context, int textViewResourceId, ArrayList<RuleItem> items) {
			super(context, textViewResourceId, items);

			this.layoutResourceId = textViewResourceId;
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inf = getLayoutInflater();
				v = inf.inflate(layoutResourceId, null);
			}
			RuleItem data = items.get(position);
			if (data != null) {
				TextView rulesHeader = (TextView)v.findViewById(R.id.rules_item_header);
				TextView rulesText = (TextView)v.findViewById(R.id.rules_item_text);

				rulesHeader.setText(data.getHeader());
				rulesText.setText(formatText(data.getRulesText()), BufferType.SPANNABLE);
			}
			return v;
		}
	}
}
