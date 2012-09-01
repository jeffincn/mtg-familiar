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

package com.gelakinetic.mtgfam.activities;

import java.io.Serializable;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;

public class SearchActivity extends FamiliarActivity {
	public static class SearchCriteria implements Serializable {
		private static final long serialVersionUID = 4712329695735151964L;
		String Name; 
		String Text;
		String Type; 
		String Color = "wubrgl";
		int Color_Logic = 0; 
		String Set;
		Float Pow_Choice = (float) CardDbAdapter.NOONECARES; 
		String Pow_Logic;
		Float Tou_Choice = (float) CardDbAdapter.NOONECARES; 
		String Tou_Logic;
		int Cmc = -1; 
		String Cmc_Logic;
		String Format; 
		String Rarity;
		String Flavor; 
		String Artist;
		int Type_Logic = 0; 
		int Text_Logic = 0;
		int Set_Logic;
	}
	public static final String CRITERIA = "criteria";
//	private static final String DEFAULT_CRITERIA_FILE = "defaultSearchCriteria.ser";
//	public static final String		NAME				= "name";
//	public static final String		TEXT				= "text";
//	public static final String		TYPE				= "type";
//	public static final String		COLOR				= "color";
//	public static final String		COLORLOGIC	= "colorlogic";
//	public static final String		SET					= "set";
//	public static final String		POW_CHOICE	= "pow_choice";
//	public static final String		POW_LOGIC		= "pow_logic";
//	public static final String		TOU_CHOICE	= "tou_choice";
//	public static final String		TOU_LOGIC		= "tou_logic";
//	public static final String		CMC					= "cmc";
//	public static final String		CMC_LOGIC		= "cmc_logic";
//	public static final String		FORMAT			= "format";
//	public static final String		RARITY			= "rarity";
//	public static final String		ARTIST			= "artist";
//	public static final String		FLAVOR			= "flavor";
	public static final String		RANDOM			= "random";
//	// lines below added by Reuben Kriegel
//	public static final String		TYPELOGIC		= "typelogic";
//	public static final String		TEXTLOGIC		= "textlogic";
//	// End addition
//	public static final String		SETLOGIC		= "setlogic";

	protected static final int		SETLIST			= 0;
	protected static final int		FORMATLIST	= 1;
	protected static final int		RARITYLIST	= 2;
	protected static final int		CORRUPTION	= 3;

	private Button								searchbutton;
	private AutoCompleteTextView	namefield;
	private EditText							textfield;
	private AutoCompleteTextView	supertypefield;
	private EditText							subtypefield;
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

	// Variables below added by Reuben Kriegel
	private Spinner								textspinner;
	private Spinner								typespinner;
	private Spinner								setspinner;

	private int										selectedFormat;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_activity);

		namefield = (AutoCompleteTextView) findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(mCtx, null));
		textfield = (EditText) findViewById(R.id.textsearch);
		supertypefield = (AutoCompleteTextView) findViewById(R.id.supertypesearch);
		subtypefield = (EditText) findViewById(R.id.subtypesearch);
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

		supertypefield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
					searchbutton.performClick();
					return true;
				}
				return false;
			}
		});

		String[] supertypes = getResources().getStringArray(R.array.supertypes);
		ArrayAdapter<String> supertypeadapter = new ArrayAdapter<String>(this, R.layout.supertype_list_item, supertypes);
		supertypefield.setThreshold(1);
		supertypefield.setAdapter(supertypeadapter);

		subtypefield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
		typespinner = (Spinner) findViewById(R.id.typelogic);
		setspinner = (Spinner) findViewById(R.id.setlogic);

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
		colorspinner.setSelection(2);

		// Lines Below added by Reuben Kriegel
		ArrayAdapter<CharSequence> adapter7 = ArrayAdapter.createFromResource(this, R.array.text_spinner,
				android.R.layout.simple_spinner_item);
		adapter7.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		textspinner.setAdapter(adapter7);

		ArrayAdapter<CharSequence> adapter8 = ArrayAdapter.createFromResource(this, R.array.type_spinner,
				android.R.layout.simple_spinner_item);
		adapter8.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		typespinner.setAdapter(adapter8);
		// End addition

		ArrayAdapter<CharSequence> adapter9 = ArrayAdapter.createFromResource(this, R.array.text_spinner,
				android.R.layout.simple_spinner_item);
		adapter9.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		textspinner.setAdapter(adapter9);

		ArrayAdapter<CharSequence> adapter10 = ArrayAdapter.createFromResource(this, R.array.set_spinner,
				android.R.layout.simple_spinner_item);
		adapter10.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		setspinner.setAdapter(adapter10);

		boolean consolidate=true;
		consolidate = preferences.getBoolean("consolidateSearch", true);
		setspinner.setSelection(consolidate?CardDbAdapter.MOSTRECENTPRINTING:CardDbAdapter.ALLPRINTINGS);

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
		try {
			setCursor.moveToFirst();
		}
		catch (SQLiteDatabaseCorruptException e) {
			showDialog(CORRUPTION);
			return;
		}

		setNames = new String[setCursor.getCount()];
		setSymbols = new String[setCursor.getCount()];
		setChecked = new boolean[setCursor.getCount()];

		for (int i = 0; i < setCursor.getCount(); i++) {
			setSymbols[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_CODE));
			setNames[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
			setChecked[i] = false;
			setCursor.moveToNext();
		}

		setCursor.close();

		Cursor c = mDbHelper.fetchAllFormats();
		if (c != null) {
			formatNames = new String[c.getCount()];
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++) {
				formatNames[i] = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
				c.moveToNext();
			}
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
				}).setPositiveButton("OK", new DialogButtonClickHandler()).create();
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

	private SearchCriteria parseForm() {
		SearchCriteria searchCriteria = new SearchCriteria();

		searchCriteria.Name = namefield.getText().toString();
		searchCriteria.Text = textfield.getText().toString();
		String supertype = supertypefield.getText().toString();
		String subtype = subtypefield.getText().toString();
		searchCriteria.Type = supertype + " - " + subtype;
		searchCriteria.Flavor = flavorfield.getText().toString();
		searchCriteria.Artist = artistfield.getText().toString();

		if (searchCriteria.Name.length() == 0) {
			searchCriteria.Name = null;
		}
		if (searchCriteria.Text.length() == 0) {
			searchCriteria.Text = null;
		}
		if (searchCriteria.Type.length() == 0) {
			searchCriteria.Type = null;
		}
		if (searchCriteria.Flavor.length() == 0) {
			searchCriteria.Flavor = null;
		}
		if (searchCriteria.Artist.length() == 0) {
			searchCriteria.Artist = null;
		}

		searchCriteria.Color = null;

		if (checkboxW.isChecked()) {
			searchCriteria.Color = "W";
		}
		else {
			searchCriteria.Color = "w";
		}

		if (checkboxU.isChecked()) {
			searchCriteria.Color += "U";
		}
		else {
			searchCriteria.Color += "u";
		}
		if (checkboxB.isChecked()) {
			searchCriteria.Color += "B";
		}
		else {
			searchCriteria.Color += "b";
		}
		if (checkboxR.isChecked()) {
			searchCriteria.Color += "R";
		}
		else {
			searchCriteria.Color += "r";
		}
		if (checkboxG.isChecked()) {
			searchCriteria.Color += "G";
		}
		else {
			searchCriteria.Color += "g";
		}
		if (checkboxL.isChecked()) {
			searchCriteria.Color += "L";
		}
		else {
			searchCriteria.Color += "l";
		}
		searchCriteria.Color_Logic = colorspinner.getSelectedItemPosition();

		searchCriteria.Set = null;

		for (int i = 0; i < setChecked.length; i++) {
			if (setChecked[i]) {
				if (searchCriteria.Set == null) {
					searchCriteria.Set = setSymbols[i];
				}
				else {
					searchCriteria.Set += "-" + setSymbols[i];
				}
			}
		}

		searchCriteria.Format = null;
		if (selectedFormat != -1) {
			searchCriteria.Format = formatNames[selectedFormat];
		}

		searchCriteria.Rarity = null;
		for (int i = 0; i < rarityChecked.length; i++) {
			if (rarityChecked[i]) {
				if (searchCriteria.Rarity == null) {
					searchCriteria.Rarity = rarityNames[i].charAt(0) + "";
				}
				else {
					searchCriteria.Rarity += rarityNames[i].charAt(0);
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
		searchCriteria.Pow_Choice = pow;
		searchCriteria.Pow_Logic = logicChoices[powLogic.getSelectedItemPosition()];

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
		searchCriteria.Tou_Choice = tou;
		searchCriteria.Tou_Logic = logicChoices[touLogic.getSelectedItemPosition()];

		String[] cmcChoices = getResources().getStringArray(R.array.cmc_spinner);
		int cmc;
		try {
			cmc = Integer.parseInt(cmcChoices[cmcChoice.getSelectedItemPosition()]);
		}
		catch (NumberFormatException e) {
			cmc = -1;
		}
		searchCriteria.Cmc = cmc;
		searchCriteria.Cmc_Logic = logicChoices[cmcLogic.getSelectedItemPosition()];

		searchCriteria.Type_Logic = typespinner.getSelectedItemPosition();
		searchCriteria.Text_Logic = textspinner.getSelectedItemPosition();
		// End addition
		searchCriteria.Set_Logic = setspinner.getSelectedItemPosition();

		return searchCriteria;
	}
	private void doSearch(boolean isRandom) {
		SearchCriteria searchCriteria = parseForm();
		Intent i = new Intent(mCtx, ResultListActivity.class);
		i.putExtra(CRITERIA, searchCriteria);
		i.putExtra(RANDOM, isRandom);
		
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
		else if (id == CORRUPTION) {
			View dialogLayout = getLayoutInflater().inflate(R.layout.simple_message_layout, null);
			TextView text = (TextView) dialogLayout.findViewById(R.id.message);
			text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.error_corruption)));
			text.setMovementMethod(LinkMovementMethod.getInstance());

			AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.error)
					.setView(dialogLayout).setPositiveButton(R.string.dialog_ok, new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).setCancelable(false).create();
			return dialog;
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

	public void clear() {
		namefield.setText("");
		supertypefield.setText("");
		subtypefield.setText("");
		textfield.setText("");
		artistfield.setText("");
		flavorfield.setText("");

		checkboxW.setChecked(false);
		checkboxU.setChecked(false);
		checkboxB.setChecked(false);
		checkboxR.setChecked(false);
		checkboxG.setChecked(false);
		checkboxL.setChecked(false);
		colorspinner.setSelection(2);

		textspinner.setSelection(0);
		typespinner.setSelection(0);
		setspinner.setSelection(0);

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
				}).setPositiveButton("OK", new DialogButtonClickHandler()).create();
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

		setButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
		formatButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
		rarityButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
	}
/*
	private void persistOptions(){
		try {
			SearchCriteria searchCriteria = parseForm();
			FileOutputStream fileStream = mCtx.openFileOutput(DEFAULT_CRITERIA_FILE, Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(fileStream); 
			os.writeObject(searchCriteria); 
			os.close(); 
		} catch (IOException e) {
			Toast.makeText(mCtx, "Could not save search options.", Toast.LENGTH_LONG).show();
		} 
	}

	private void fetchPersistedOptions(){
		try {
			FileInputStream fileInputStream = mCtx.openFileInput(DEFAULT_CRITERIA_FILE); 
			ObjectInputStream oInputStream = new ObjectInputStream(fileInputStream); 
			SearchCriteria criteria = (SearchCriteria) oInputStream.readObject(); 
			oInputStream.close();
			
			namefield.setText(criteria.Name);
			String[] type = criteria.Type.split(" - ");
			try {
			supertypefield.setText(type[0]);
			subtypefield.setText(type[1]);
			} catch (Exception e) {}
			textfield.setText(criteria.Text);
			artistfield.setText(criteria.Artist);
			flavorfield.setText(criteria.Flavor);

			checkboxW.setChecked(criteria.Color.contains("W"));
			checkboxU.setChecked(criteria.Color.contains("U"));
			checkboxB.setChecked(criteria.Color.contains("B"));
			checkboxR.setChecked(criteria.Color.contains("R"));
			checkboxG.setChecked(criteria.Color.contains("G"));
			checkboxL.setChecked(criteria.Color.contains("L"));
			colorspinner.setSelection(criteria.Color_Logic);

			textspinner.setSelection(criteria.Text_Logic);
			typespinner.setSelection(criteria.Type_Logic);
			setspinner.setSelection(criteria.Set_Logic);

			List<String> logicChoices = Arrays.asList(getResources().getStringArray(R.array.logic_spinner));
			powLogic.setSelection(logicChoices.indexOf(criteria.Pow_Logic));
			List<String> ptlist = Arrays.asList(getResources().getStringArray(R.array.pt_spinner));
			float p = criteria.Pow_Choice;
			if(p != CardDbAdapter.NOONECARES){
				if (p == CardDbAdapter.STAR)
					powChoice.setSelection(ptlist.indexOf("*"));
				else if (p == CardDbAdapter.ONEPLUSSTAR)
					powChoice.setSelection(ptlist.indexOf("1+*"));
				else if (p == CardDbAdapter.TWOPLUSSTAR)
					powChoice.setSelection(ptlist.indexOf("2+*"));
				else if (p == CardDbAdapter.SEVENMINUSSTAR)
					powChoice.setSelection(ptlist.indexOf("7-*"));
				else if (p == CardDbAdapter.STARSQUARED)
					powChoice.setSelection(ptlist.indexOf("*^2"));
				else {
					if (p == (int) p) {
						powChoice.setSelection(ptlist.indexOf(((int) p)+""));
					} else {
						powChoice.setSelection(ptlist.indexOf(p+""));
					}
				}
			}
			touLogic.setSelection(logicChoices.indexOf(criteria.Tou_Logic));
			float t = criteria.Tou_Choice;
			if(t != CardDbAdapter.NOONECARES){
				if (t == CardDbAdapter.STAR)
					touChoice.setSelection(ptlist.indexOf("*"));
				else if (t == CardDbAdapter.ONEPLUSSTAR)
					touChoice.setSelection(ptlist.indexOf("1+*"));
				else if (t == CardDbAdapter.TWOPLUSSTAR)
					touChoice.setSelection(ptlist.indexOf("2+*"));
				else if (t == CardDbAdapter.SEVENMINUSSTAR)
					touChoice.setSelection(ptlist.indexOf("7-*"));
				else if (t == CardDbAdapter.STARSQUARED)
					touChoice.setSelection(ptlist.indexOf("*^2"));
				else {
					if (t == (int) t) {
						touChoice.setSelection(ptlist.indexOf(((int) t)+""));
					} else {
						touChoice.setSelection(ptlist.indexOf(t+""));
					}
				}
			}
			cmcLogic.setSelection(logicChoices.indexOf(criteria.Cmc_Logic));
			try {
				cmcChoice.setSelection(Arrays.asList(getResources().getStringArray(R.array.cmc_spinner)).indexOf(String.valueOf(criteria.Cmc)));
			}
			catch (Exception e) {
			}
			
			if(criteria.Set != null){
				List<String> sets = Arrays.asList(criteria.Set.split("-"));
				for (int i = 0; i < setChecked.length; i++) 
					setChecked[i] = sets.contains(setSymbols[i]);
			}else
				for (int i = 0; i < setChecked.length; i++) 
					setChecked[i] = false;
				
			selectedFormat = Arrays.asList(formatNames).indexOf(criteria.Format);
			for (int i = 0; i < rarityChecked.length; i++) {
				rarityChecked[i] = ( criteria.Rarity != null && criteria.Rarity.contains(rarityNames[i].charAt(0)+""));
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
					}).setPositiveButton("OK", new DialogButtonClickHandler()).create();
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

			
			setButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
			for (int i = 0; i < setChecked.length; i++) {
				if (setChecked[i]) {
					setButton.getBackground().setColorFilter(0xFFFFB942, Mode.MULTIPLY);
				}
			}
			formatButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
			if (selectedFormat != -1) {
				formatButton.getBackground().setColorFilter(0xFFFFB942, Mode.MULTIPLY);
			}
			rarityButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
			for (int i = 0; i < rarityChecked.length; i++) {
				if (rarityChecked[i]) {
					rarityButton.getBackground().setColorFilter(0xFFFFB942, Mode.MULTIPLY);
				}
			}
			
		} catch (IOException e) {
			Toast.makeText(mCtx, "Could not load search options.", Toast.LENGTH_LONG).show();
		} catch (ClassNotFoundException e) {
			Toast.makeText(mCtx, "Could not load search options.", Toast.LENGTH_LONG).show();
		} 
	}
*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.search_menu_random_search:
				doSearch(true);
				return true;
			case R.id.search_menu_clear:
				clear();
				return true;
			//To be uncommented for 1.9
//			case R.id.search_menu_save_defaults:
//				persistOptions();
//				return true;
//			case R.id.search_menu_load_defaults:
//				fetchPersistedOptions();
//				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.search_menu, menu);
		return true;
	}
}
