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

package com.gelakinetic.mtgfam.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Bundle;
import android.text.Html.ImageGetter;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;

public class RulesActivity extends FamiliarActivity {

	public static String						CATEGORY_KEY						= "category";
	public static String						SUBCATEGORY_KEY					= "subcategory";
	public static String						POSITION_KEY						= "position";
	public static String						KEYWORD_KEY							= "keyword";
	public static String						GLOSSARY_KEY						= "glossary";

	private static final int				SEARCH									= 1;
	private static final int				RESULT_NORMAL						= 2;
	private static final int				RESULT_QUIT_TO_MAIN			= 3;
	private static final int				CORRUPTION							= 4;
	private static final int				ARBITRARY_REQUEST_CODE	= 23;

	private ImageGetter							imgGetter;
	private ListView								list;
	private RulesListAdapter				adapter;
	private ArrayList<DisplayItem>	rules;
	private int											category;
	private int											subcategory;
	private String									keyword;

	private Pattern									underscorePattern;
	private Pattern									examplePattern;
	private Pattern									glyphPattern;
	private Pattern									keywordPattern;
	private Pattern									hyperlinkPattern;
	private Pattern									linkPattern;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rules_activity);

		imgGetter = ImageGetterHelper.GlyphGetter(getResources());

		Bundle extras = getIntent().getExtras();
		int position;
		boolean isGlossary;
		if (extras == null) {
			category = -1;
			subcategory = -1;
			position = 0;
			keyword = null;
			isGlossary = false;
		}
		else {
			category = extras.getInt(CATEGORY_KEY, -1);
			subcategory = extras.getInt(SUBCATEGORY_KEY, -1);
			position = extras.getInt(POSITION_KEY, 0);
			keyword = extras.getString(KEYWORD_KEY);
			isGlossary = extras.getBoolean(GLOSSARY_KEY, false);
		}

		list = (ListView) findViewById(R.id.rules_list);
		rules = new ArrayList<DisplayItem>();
		boolean clickable;
		Cursor c;

		if (isGlossary) {
			c = mDbHelper.getGlossaryTerms();
			clickable = false;
		}
		else if (keyword == null) {
			c = mDbHelper.getRules(category, subcategory);
			clickable = subcategory == -1;
		}
		else {
			c = mDbHelper.getRulesByKeyword(keyword, category, subcategory);
			clickable = false;
		}
		if (c != null) {
			try {
				if (c.getCount() > 0) {
					c.moveToFirst();
					// throw this exception to test the dialog
					// throw(new SQLiteDatabaseCorruptException("seriously"));
					while (!c.isAfterLast()) {
						if (isGlossary) {
							rules.add(new GlossaryItem(c.getString(c.getColumnIndex(CardDbAdapter.KEY_TERM)), c.getString(c
									.getColumnIndex(CardDbAdapter.KEY_DEFINITION))));
						}
						else {
							rules
									.add(new RuleItem(c.getInt(c.getColumnIndex(CardDbAdapter.KEY_CATEGORY)), c.getInt(c
											.getColumnIndex(CardDbAdapter.KEY_SUBCATEGORY)), c.getString(c
											.getColumnIndex(CardDbAdapter.KEY_ENTRY)), c.getString(c
											.getColumnIndex(CardDbAdapter.KEY_RULE_TEXT))));
						}
						c.moveToNext();
					}
					c.close();
					if (!isGlossary && category == -1 && keyword == null) {
						// If it's the initial rules page, add a Glossary link to the end
						rules.add(new GlossaryItem("Glossary", "", true));
					}
					int listItemResource = R.layout.rules_list_item;
					if (category >= 0 && subcategory < 0) {
						listItemResource = R.layout.rules_list_subcategory_item;
					}
					// These cases can't be exclusive; otherwise keyword search from
					// anything but a subcategory will use the wrong layout
					if (isGlossary || subcategory >= 0 || keyword != null) {
						listItemResource = R.layout.rules_list_detail_item;
					}
					adapter = new RulesListAdapter(this, listItemResource, rules);
					list.setAdapter(adapter);

					if (clickable) {
						// This only happens for rule items with no subcategory, so the cast
						// should be safe
						list.setOnItemClickListener(new OnItemClickListener() {
							public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
								DisplayItem item = rules.get(position);
								Intent i = new Intent(RulesActivity.this, RulesActivity.class);
								if (RuleItem.class.isInstance(item)) {
									RuleItem ri = (RuleItem) item;
									i.putExtra(CATEGORY_KEY, ri.getCategory());
									i.putExtra(SUBCATEGORY_KEY, ri.getSubcategory());
								}
								else if (GlossaryItem.class.isInstance(item)) {
									i.putExtra(GLOSSARY_KEY, true);
								}
								// The else case shouldn't happen, but meh
								startActivityForResult(i, ARBITRARY_REQUEST_CODE);
							}
						});
					}
				}
				else {
					c.close();
					Toast.makeText(this, "No results found.", Toast.LENGTH_SHORT).show();
					this.finish();
				}
			}
			catch (SQLiteDatabaseCorruptException e) {
				showDialog(CORRUPTION);
				return;
			}
		}
		else {
			Toast.makeText(this, "No results found.", Toast.LENGTH_SHORT).show();
			this.finish();
		}

		list.setSelection(position);

		// Explanations for these regexes are available upon request.
		// - Alex
		underscorePattern = Pattern.compile("_(.+?)_");
		examplePattern = Pattern.compile("(Example:.+)$");
		glyphPattern = Pattern.compile("\\{([a-zA-Z0-9/]{1,3})\\}");
		if (keyword != null && !keyword.contains("{") && !keyword.contains("}")) {
			keywordPattern = Pattern.compile("(" + Pattern.quote(keyword) + ")", Pattern.CASE_INSENSITIVE);
		}
		else {
			keywordPattern = null;
		}
		hyperlinkPattern = Pattern.compile("\\<(http://)?(www|gatherer)(.+?)\\>");

		/*
		 * Regex breakdown for Adam: [1-9]{1}: first character is between 1 and 9
		 * [0-9]{2}: followed by two characters between 0 and 9 (i.e. a 3-digit
		 * number) (...)?: maybe followed by the group: \\.: period
		 * ([a-z0-9]{1,3}(-[a-z]{1})?)?: maybe followed by one to three alphanumeric
		 * characters, which are maybe followed by a hyphen and an alphabetical
		 * character \\.?: maybe followed by another period
		 * 
		 * I realize this isn't completely easy to read, but it might at least help
		 * make some sense of the regex so I'm not just waving my hands and shouting
		 * "WIZAAAAAARDS!". I still reserve the right to do that, though. - Alex
		 */
		linkPattern = Pattern.compile("([1-9]{1}[0-9]{2}(\\.([a-z0-9]{1,3}(-[a-z]{1})?)?\\.?)?)");

		setResult(RESULT_NORMAL);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case SEARCH: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.rules_search_title);
				String header;
				if (this.category == -1) {
					header = getString(R.string.rules_search_all);
				}
				else {
					header = String
							.format(getString(R.string.rules_search_cat), mDbHelper.getCategoryName(category, subcategory));
				}
				View v = getLayoutInflater().inflate(R.layout.rules_search_dialog, null);
				((TextView) v.findViewById(R.id.keyword_search_desc)).setText(header);
				final EditText input = (EditText) v.findViewById(R.id.keyword_search_field);
				builder.setView(v);
				builder.setPositiveButton(R.string.dialog_ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String keyword = input.getText().toString().trim();
						if (keyword.length() < 3) {
							Toast.makeText(RulesActivity.this, "Your search term must be at least 3 characters long.",
									Toast.LENGTH_LONG).show();
						}
						else {
							Intent i = new Intent(RulesActivity.this, RulesActivity.class);
							i.putExtra(KEYWORD_KEY, keyword);
							i.putExtra(CATEGORY_KEY, category);
							i.putExtra(SUBCATEGORY_KEY, subcategory);
							startActivityForResult(i, ARBITRARY_REQUEST_CODE);
						}
					}
				});
				builder.setNegativeButton(R.string.dialog_cancel, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Do nothing
					}
				});
				return builder.create();
			}
			case CORRUPTION: {
				View dialogLayout = getLayoutInflater().inflate(R.layout.simple_message_layout, null);
				TextView text = (TextView) dialogLayout.findViewById(R.id.message);
				text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.error_corruption)));
				text.setMovementMethod(LinkMovementMethod.getInstance());

				return new AlertDialog.Builder(this).setTitle(R.string.error).setView(dialogLayout)
						.setPositiveButton(R.string.dialog_ok, new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								finish();
							}
						}).setCancelable(false).create();
			}
			default: {
				return super.onCreateDialog(id);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ARBITRARY_REQUEST_CODE) {
			if (resultCode == RESULT_QUIT_TO_MAIN) {
				setResult(RESULT_QUIT_TO_MAIN);
				finish();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.rules_menu_exit:
				setResult(RESULT_QUIT_TO_MAIN);
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			showDialog(SEARCH);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private SpannableString formatText(String input, boolean linkify) {
		String encodedInput = input;
		encodedInput = underscorePattern.matcher(encodedInput).replaceAll("\\<i\\>$1\\</i\\>");
		encodedInput = examplePattern.matcher(encodedInput).replaceAll("\\<i\\>$1\\</i\\>");
		encodedInput = glyphPattern.matcher(encodedInput).replaceAll("\\<img src=\"$1\"/\\>");
		if (keywordPattern != null) {
			encodedInput = keywordPattern.matcher(encodedInput).replaceAll("\\<font color=\"yellow\"\\>$1\\</font\\>");
		}
		encodedInput = hyperlinkPattern.matcher(encodedInput).replaceAll("\\<a href=\"http://$2$3\"\\>$2$3\\</a\\>");
		encodedInput = encodedInput.replace("{", "").replace("}", "");

		CharSequence cs = ImageGetterHelper.jellyBeanHack(encodedInput, imgGetter, null);
		SpannableString result = new SpannableString(cs);

		if (linkify) {
			Matcher m = linkPattern.matcher(cs);
			while (m.find()) {
				try {
					String[] tokens = cs.subSequence(m.start(), m.end()).toString().split("(\\.)");
					int firstInt = Integer.parseInt(tokens[0]);
					final int linkCat = firstInt / 100;
					final int linkSub = firstInt % 100;
					int position = 0;
					if (tokens.length > 1) {
						String entry = tokens[1];
						int dashIndex = entry.indexOf("-");
						if (dashIndex >= 0) {
							entry = entry.substring(0, dashIndex);
						}
						position = mDbHelper.getRulePosition(linkCat, linkSub, entry);
					}
					final int linkPosition = position;
					result.setSpan(new ClickableSpan() {
						@Override
						public void onClick(View widget) {
							// Open a new activity instance
							Intent i = new Intent(RulesActivity.this, RulesActivity.class);
							i.putExtra(CATEGORY_KEY, linkCat);
							i.putExtra(SUBCATEGORY_KEY, linkSub);
							i.putExtra(POSITION_KEY, linkPosition);
							startActivityForResult(i, ARBITRARY_REQUEST_CODE);
						}
					}, m.start(), m.end(), 0);
				}
				catch (Exception e) {
					// Eat any exceptions; they'll just cause the link to not appear
				}
			}
		}

		return result;
	}

	private abstract class DisplayItem {
		public abstract String getText();

		public abstract String getHeader();

		public abstract boolean isClickable();
	}

	private class RuleItem extends DisplayItem {
		private int			category;
		private int			subcategory;
		private String	entry;
		private String	rulesText;

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

		public String getText() {
			return this.rulesText;
		}

		public String getHeader() {
			if (this.subcategory == -1) {
				return String.valueOf(this.category) + ".";
			}
			else if (this.entry == null) {
				return String.valueOf((this.category * 100) + this.subcategory) + ".";
			}
			else {
				return String.valueOf((this.category * 100 + this.subcategory)) + "." + this.entry;
			}
		}

		public boolean isClickable() {
			return this.entry == null || this.entry.length() == 0;
		}
	}

	private class GlossaryItem extends DisplayItem {
		private String	term;
		private String	definition;
		private boolean	clickable;

		public GlossaryItem(String term, String definition) {
			this.term = term;
			this.definition = definition;
			this.clickable = false;
		}

		public GlossaryItem(String term, String definition, boolean clickable) {
			this.term = term;
			this.definition = definition;
			this.clickable = clickable;
		}

		public String getText() {
			return this.definition;
		}

		public String getHeader() {
			return this.term;
		}

		public boolean isClickable() {
			return this.clickable;
		}
	}

	private class RulesListAdapter extends ArrayAdapter<DisplayItem> implements SectionIndexer {
		private int												layoutResourceId;
		private ArrayList<DisplayItem>		items;
		private HashMap<String, Integer>	alphaIndex;
		private String[]									sections;

		public RulesListAdapter(Context context, int textViewResourceId, ArrayList<DisplayItem> items) {
			super(context, textViewResourceId, items);

			this.layoutResourceId = textViewResourceId;
			this.items = items;

			boolean isGlossary = true;
			for (DisplayItem item : items) {
				if (RuleItem.class.isInstance(item)) {
					isGlossary = false;
					break;
				}
			}

			if (isGlossary) {
				this.alphaIndex = new HashMap<String, Integer>();
				for (int i = 0; i < items.size(); i++) {
					String first = items.get(i).getHeader().substring(0, 1).toUpperCase();
					if (!this.alphaIndex.containsKey(first)) {
						this.alphaIndex.put(first, i);
					}
				}

				ArrayList<String> letters = new ArrayList<String>(this.alphaIndex.keySet());
				Collections.sort(letters); // This should do nothing in practice, but
																		// just to be safe

				sections = new String[letters.size()];
				letters.toArray(sections);
			}
			else {
				this.alphaIndex = null;
				this.sections = null;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inf = getLayoutInflater();
				v = inf.inflate(layoutResourceId, null);
			}
			DisplayItem data = items.get(position);
			if (data != null) {
				TextView rulesHeader = (TextView) v.findViewById(R.id.rules_item_header);
				TextView rulesText = (TextView) v.findViewById(R.id.rules_item_text);

				String header = data.getHeader();
				String text = data.getText();

				rulesHeader.setText(formatText(header, false), BufferType.SPANNABLE);
				if (text.equals("")) {
					rulesText.setVisibility(View.GONE);
				}
				else {
					rulesText.setVisibility(View.VISIBLE);
					rulesText.setText(formatText(text, true), BufferType.SPANNABLE);
				}
				if (!data.isClickable()) {
					rulesText.setMovementMethod(LinkMovementMethod.getInstance());
					rulesText.setClickable(false);
					rulesText.setLongClickable(false);
				}
			}
			return v;
		}

		public int getPositionForSection(int section) {
			if (this.alphaIndex == null) {
				return 0;
			}
			else {
				return this.alphaIndex.get(this.sections[section]);
			}
		}

		public int getSectionForPosition(int position) {
			if (this.alphaIndex == null) {
				return 0;
			}
			else {
				return 1;
			}
		}

		public Object[] getSections() {
			if (this.alphaIndex == null) {
				return null;
			}
			else {
				return sections;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.rules_menu, menu);
		return true;
	}
}
