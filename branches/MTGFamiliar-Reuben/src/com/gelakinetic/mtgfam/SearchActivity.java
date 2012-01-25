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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

public class SearchActivity extends Activity {
	protected static final String	NAME				= "name";
	protected static final String	TEXT				= "text";
	protected static final String	TYPE				= "type";
	protected static final String	COLOR				= "color";
	protected static final String	COLORLOGIC	= "colorlogic";
	protected static final String	SET					= "set";
	protected static final String	POW_CHOICE	= "pow_choice";
	protected static final String	POW_LOGIC		= "pow_logic";
	protected static final String	TOU_CHOICE	= "tou_choice";
	protected static final String	TOU_LOGIC		= "tou_logic";
	protected static final String	CMC					= "cmc";
	protected static final String	CMC_LOGIC		= "cmc_logic";
	protected static final String	FORMAT			= "format";
	protected static final String	RARITY			= "rarity";
	protected static final String	ARTIST			= "artist";
	protected static final String	FLAVOR			= "flavor";
	protected static final String	RANDOM			= "random";
	//lines below added by Reuben Kriegel
	protected static final String	TEXTLOGIC		= "textlogic";
	//End addition
	
	protected static final int		SETLIST			= 0;
	protected static final int		FORMATLIST	= 1;
	protected static final int		RARITYLIST	= 2;

	private CardDbAdapter					mDbHelper;
	private Button								searchbutton;
	private AutoCompleteTextView	namefield;
	private EditText							textfield;
	private EditText							typefield;
	private CheckBox							checkboxW;
	private CheckBox							checkboxU;
	private CheckBox							checkboxB;
	private CheckBox							checkboxR;
	private CheckBox							checkboxG;
	private CheckBox							checkboxL;
	private Spinner								colorspinner;
	private Button								setButton;
	private String[]							setNames;
	private boolean[]							setChecked;
	private String[]							setSymbols;
	private Context								mCtx;
	private Spinner								powLogic;
	private Spinner								powChoice;
	private Spinner								touLogic;
	private Spinner								touChoice;
	private Spinner								cmcLogic;
	private Spinner								cmcChoice;
	private Button								formatButton;
	private String[]							formatNames;
	private Button								rarityButton;
	private String[]							rarityNames;
	private boolean[]							rarityChecked;
	private Dialog								setDialog;
	private Dialog								formatDialog;
	private Dialog								rarityDialog;
	private EditText							flavorfield;
	private EditText							artistfield;
	//Variable below added by Reuben Kriegel
	private Spinner								textspinner;
	
	private int	selectedFormat;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_activity);

		mCtx = this;

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();

		namefield = (AutoCompleteTextView) findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(mCtx, null));
		textfield = (EditText) findViewById(R.id.textsearch);
		typefield = (EditText) findViewById(R.id.typesearch);
		flavorfield = (EditText) findViewById(R.id.flavorsearch);
		artistfield = (EditText) findViewById(R.id.artistsearch);
		
		
		// So pressing enter does the search
		namefield.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		namefield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
					searchbutton.performClick();
					return true;
				}
				return false;
			}
		});

		textfield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
					searchbutton.performClick();
					return true;
				}
				return false;
			}
		});

		typefield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
					searchbutton.performClick();
					return true;
				}
				return false;
			}
		});

		flavorfield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
					searchbutton.performClick();
					return true;
				}
				return false;
			}
		});

		artistfield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
					searchbutton.performClick();
					return true;
				}
				return false;
			}
		});

		searchbutton = (Button) findViewById(R.id.searchbutton);
		// randombutton = (Button) findViewById(R.id.s);

		checkboxW = (CheckBox) findViewById(R.id.checkBoxW);
		checkboxU = (CheckBox) findViewById(R.id.checkBoxU);
		checkboxB = (CheckBox) findViewById(R.id.checkBoxB);
		checkboxR = (CheckBox) findViewById(R.id.checkBoxR);
		checkboxG = (CheckBox) findViewById(R.id.checkBoxG);
		checkboxL = (CheckBox) findViewById(R.id.checkBoxL);

		colorspinner = (Spinner) findViewById(R.id.colorlogic);
		textspinner = (Spinner) findViewById(R.id.textlogic);

		setButton = (Button) findViewById(R.id.setsearch);
		formatButton = (Button) findViewById(R.id.formatsearch);
		rarityButton = (Button) findViewById(R.id.raritysearch);

		powLogic = (Spinner) findViewById(R.id.powLogic);
		powChoice = (Spinner) findViewById(R.id.powChoice);
		touLogic = (Spinner) findViewById(R.id.touLogic);
		touChoice = (Spinner) findViewById(R.id.touChoice);
		cmcLogic = (Spinner) findViewById(R.id.cmcLogic);
		cmcChoice = (Spinner) findViewById(R.id.cmcChoice);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.logic_spinner,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		powLogic.setAdapter(adapter);

		ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this, R.array.pt_spinner,
				android.R.layout.simple_spinner_item);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		powChoice.setAdapter(adapter1);

		ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.logic_spinner,
				android.R.layout.simple_spinner_item);
		adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		touLogic.setAdapter(adapter2);

		ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this, R.array.pt_spinner,
				android.R.layout.simple_spinner_item);
		adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		touChoice.setAdapter(adapter3);

		ArrayAdapter<CharSequence> adapter4 = ArrayAdapter.createFromResource(this, R.array.logic_spinner,
				android.R.layout.simple_spinner_item);
		adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cmcLogic.setAdapter(adapter4);
		cmcLogic.setSelection(1); // CMC should default to <

		ArrayAdapter<CharSequence> adapter5 = ArrayAdapter.createFromResource(this, R.array.cmc_spinner,
				android.R.layout.simple_spinner_item);
		adapter5.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cmcChoice.setAdapter(adapter5);

		ArrayAdapter<CharSequence> adapter6 = ArrayAdapter.createFromResource(this, R.array.color_spinner,
				android.R.layout.simple_spinner_item);
		adapter6.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		colorspinner.setAdapter(adapter6);

		ArrayAdapter<CharSequence> adapter7 = ArrayAdapter.createFromResource(this, R.array.text_spinner, 
				android.R.layout.simple_spinner_item);
		adapter7.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		textspinner.setAdapter(adapter7);

		setButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(SETLIST);
			}
		});

		formatButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(FORMATLIST);
			}
		});

		rarityButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(RARITYLIST);
			}
		});

		searchbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doSearch(false);
			}
		});

		Cursor setCursor = mDbHelper.fetchAllSets();
		setCursor.moveToFirst();

		setNames = new String[setCursor.getCount()];
		setSymbols = new String[setCursor.getCount()];
		setChecked = new boolean[setCursor.getCount()];

		for (int i = 0; i < setCursor.getCount(); i++) {
			setSymbols[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_CODE));
			setNames[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
			setChecked[i] = false;
			setCursor.moveToNext();
		}

		setCursor.deactivate();
		setCursor.close();

		Cursor c = mDbHelper.fetchAllFormats();
		if (c != null) {
			formatNames = new String[c.getCount()];
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++) {
				formatNames[i] = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
				c.moveToNext();
			}
			c.deactivate();
			c.close();
		}
		else {
			formatNames = new String[0];
		}
		Resources res = getResources();
		rarityNames = res.getStringArray(R.array.rarities);
		rarityChecked = new boolean[rarityNames.length];
		
		selectedFormat = -1;

		setDialog = new AlertDialog.Builder(this).setTitle("Sets")
				.setMultiChoiceItems(setNames, setChecked, new DialogSelectionClickHandler())
				.setPositiveButton("OK", new DialogButtonClickHandler()).create();
		formatDialog = new AlertDialog.Builder(this).setTitle("Formats")
				.setSingleChoiceItems(formatNames, selectedFormat, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						selectedFormat = which;
					}
				})
				.setPositiveButton("OK", new DialogButtonClickHandler()).create();
		rarityDialog = new AlertDialog.Builder(this).setTitle("Rarities")
				.setMultiChoiceItems(rarityNames, rarityChecked, new DialogSelectionClickHandler())
				.setPositiveButton("OK", new DialogButtonClickHandler()).create();

		setDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface arg0) {
				setButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
				for (int i = 0; i < setChecked.length; i++) {
					if (setChecked[i]) {
						setButton.getBackground().setColorFilter(0xFFFFB942, Mode.MULTIPLY);
					}
				}
			}
		});

		formatDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface arg0) {
				formatButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
				if (selectedFormat != -1) {
					formatButton.getBackground().setColorFilter(0xFFFFB942, Mode.MULTIPLY);
				}
			}
		});

		rarityDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface arg0) {
				rarityButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
				for (int i = 0; i < rarityChecked.length; i++) {
					if (rarityChecked[i]) {
						rarityButton.getBackground().setColorFilter(0xFFFFB942, Mode.MULTIPLY);
					}
				}
			}
		});
	}

	@Override
	protected void onResume(){
		super.onResume();
		MyApp appState = ((MyApp)getApplicationContext());
		appState.setState(0);
	}
	
	private void doSearch(boolean isRandom) {
		String name = namefield.getText().toString();
		String text = textfield.getText().toString();
		String type = typefield.getText().toString();
		String flavor = flavorfield.getText().toString();
		String artist = artistfield.getText().toString();

		if (name.length() == 0) {
			name = null;
		}
		if (text.length() == 0) {
			text = null;
		}
		if (type.length() == 0) {
			type = null;
		}
		if (flavor.length() == 0) {
			flavor = null;
		}
		if (artist.length() == 0) {
			artist = null;
		}

		String color = null;

		if (checkboxW.isChecked()) {
			color = "W";
		}
		else {
			color = "w";
		}

		if (checkboxU.isChecked()) {
			color += "U";
		}
		else {
			color += "u";
		}
		if (checkboxB.isChecked()) {
			color += "B";
		}
		else {
			color += "b";
		}
		if (checkboxR.isChecked()) {
			color += "R";
		}
		else {
			color += "r";
		}
		if (checkboxG.isChecked()) {
			color += "G";
		}
		else {
			color += "g";
		}
		if (checkboxL.isChecked()) {
			color += "L";
		}
		else {
			color += "l";
		}

		String sets = null;

		for (int i = 0; i < setChecked.length; i++) {
			if (setChecked[i]) {
				if (sets == null) {
					sets = setSymbols[i];
				}
				else {
					sets += "-" + setSymbols[i];
				}
			}
		}

		String fmt = null;
		if(selectedFormat != -1){
			fmt = formatNames[selectedFormat];
		}

		String rarity = null;
		for (int i = 0; i < rarityChecked.length; i++) {
			if (rarityChecked[i]) {
				if (rarity == null) {
					rarity = rarityNames[i].charAt(0) + "";
				}
				else {
					rarity += rarityNames[i].charAt(0);
				}
			}
		}

		String[] logicChoices = getResources().getStringArray(R.array.logic_spinner);
		String power = getResources().getStringArray(R.array.pt_spinner)[powChoice.getSelectedItemPosition()];
		String toughness = getResources().getStringArray(R.array.pt_spinner)[touChoice.getSelectedItemPosition()];

		float pow = CardDbAdapter.NOONECARES;
		try {
			pow = Float.parseFloat(power);
		}
		catch (NumberFormatException e) {
			if (power.equals("*")) {
				pow = CardDbAdapter.STAR;
			}
			else if (power.equals("1+*")) {
				pow = CardDbAdapter.ONEPLUSSTAR;
			}
			else if (power.equals("2+*")) {
				pow = CardDbAdapter.TWOPLUSSTAR;
			}
			else if (power.equals("7-*")) {
				pow = CardDbAdapter.SEVENMINUSSTAR;
			}
			else if (power.equals("*^2")) {
				pow = CardDbAdapter.STARSQUARED;
			}
		}

		float tou = CardDbAdapter.NOONECARES;
		try {
			tou = Float.parseFloat(toughness);
		}
		catch (NumberFormatException e) {
			if (toughness.equals("*")) {
				tou = CardDbAdapter.STAR;
			}
			else if (toughness.equals("1+*")) {
				tou = CardDbAdapter.ONEPLUSSTAR;
			}
			else if (toughness.equals("2+*")) {
				tou = CardDbAdapter.TWOPLUSSTAR;
			}
			else if (toughness.equals("7-*")) {
				tou = CardDbAdapter.SEVENMINUSSTAR;
			}
			else if (toughness.equals("*^2")) {
				tou = CardDbAdapter.STARSQUARED;
			}
		}

		String[] cmcChoices = getResources().getStringArray(R.array.cmc_spinner);
		int cmc;
		try {
			cmc = Integer.parseInt(cmcChoices[cmcChoice.getSelectedItemPosition()]);
		}
		catch (NumberFormatException e) {
			cmc = -1;
		}
		Intent i = new Intent(mCtx, ResultListActivity.class);
		i.putExtra(NAME, name);
		i.putExtra(TEXT, text);
		i.putExtra(TYPE, type);
		i.putExtra(COLOR, color);
		i.putExtra(COLORLOGIC, colorspinner.getSelectedItemPosition());// logicbutton.isChecked());
		i.putExtra(SET, sets);
		i.putExtra(FORMAT, fmt);
		i.putExtra(POW_CHOICE, pow);
		i.putExtra(POW_LOGIC, logicChoices[powLogic.getSelectedItemPosition()]);
		i.putExtra(TOU_CHOICE, tou);
		i.putExtra(TOU_LOGIC, logicChoices[touLogic.getSelectedItemPosition()]);
		i.putExtra(CMC, cmc);
		i.putExtra(CMC_LOGIC, logicChoices[cmcLogic.getSelectedItemPosition()]);
		i.putExtra(RARITY, rarity);
		i.putExtra(ARTIST, artist);
		i.putExtra(FLAVOR, flavor);
		i.putExtra(RANDOM, isRandom);
		// Line below added by Reuben Kriegel
		i.putExtra(TEXTLOGIC, textspinner.getSelectedItemPosition());
		// End addition
		startActivityForResult(i, 0);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			doSearch(false);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == SETLIST) {
			return setDialog;
		}
		else if (id == FORMATLIST) {
			return formatDialog;
		}
		else if (id == RARITYLIST) {
			return rarityDialog;
		}
		return null;
	}

	public class DialogSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener {
		public void onClick(DialogInterface dialog, int clicked, boolean selected) {

			// called when something checked;
		}
	}

	public class DialogButtonClickHandler implements DialogInterface.OnClickListener {
		public void onClick(DialogInterface dialog, int clicked) {
			switch (clicked) {
				case DialogInterface.BUTTON_POSITIVE:
					break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.search_menu_random_search:
				doSearch(true);
				return true;
			case R.id.search_menu_search:
				doSearch(false);
				return true;

			case R.id.search_menu_clear:
				namefield.setText("");
				typefield.setText("");
				textfield.setText("");
				artistfield.setText("");
				flavorfield.setText("");

				checkboxW.setChecked(false);
				checkboxU.setChecked(false);
				checkboxB.setChecked(false);
				checkboxR.setChecked(false);
				checkboxG.setChecked(false);
				checkboxL.setChecked(false);
				colorspinner.setSelection(0);

				powLogic.setSelection(0);
				powChoice.setSelection(0);
				touLogic.setSelection(0);
				touChoice.setSelection(0);
				cmcLogic.setSelection(0);
				cmcLogic.setSelection(1); // CMC should default to <
				cmcChoice.setSelection(0);

				for (int i = 0; i < setChecked.length; i++) {
					setChecked[i] = false;
				}
				selectedFormat = -1;
				for (int i = 0; i < rarityChecked.length; i++) {
					rarityChecked[i] = false;
				}
				this.removeDialog(SETLIST);
				this.removeDialog(FORMATLIST);
				this.removeDialog(RARITYLIST);

				setDialog = new AlertDialog.Builder(this).setTitle("Sets")
						.setMultiChoiceItems(setNames, setChecked, new DialogSelectionClickHandler())
						.setPositiveButton("OK", new DialogButtonClickHandler()).create();
				formatDialog = new AlertDialog.Builder(this).setTitle("Formats")
				.setSingleChoiceItems(formatNames, selectedFormat, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						selectedFormat = which;
					}
				})
				.setPositiveButton("OK", new DialogButtonClickHandler()).create();
				rarityDialog = new AlertDialog.Builder(this).setTitle("Rarities")
						.setMultiChoiceItems(rarityNames, rarityChecked, new DialogSelectionClickHandler())
						.setPositiveButton("OK", new DialogButtonClickHandler()).create();

				setDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface arg0) {
						setButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
						for (int i = 0; i < setChecked.length; i++) {
							if (setChecked[i]) {
								setButton.getBackground().setColorFilter(0xFFFFB942, Mode.MULTIPLY);
							}
						}
					}
				});

				formatDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface arg0) {
						formatButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
						if(selectedFormat != -1){
							formatButton.getBackground().setColorFilter(0xFFFFB942, Mode.MULTIPLY);
						}
					}
				});

				rarityDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface arg0) {
						rarityButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
						for (int i = 0; i < rarityChecked.length; i++) {
							if (rarityChecked[i]) {
								rarityButton.getBackground().setColorFilter(0xFFFFB942, Mode.MULTIPLY);
							}
						}
					}
				});

				setButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
				formatButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
				rarityButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			if (resultCode == ResultListActivity.NO_RESULT) {
				;
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
