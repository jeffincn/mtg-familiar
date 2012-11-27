package com.gelakinetic.mtgfam.fragments;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;

public class SearchViewFragment extends FamiliarFragment {

	public static class SearchCriteria implements Serializable {
		private static final long	serialVersionUID	= 4712329695735151964L;
		public String							Name;
		public String							Text;
		public String							Type;
		public String							Color							= "wubrgl";
		public int								Color_Logic				= 0;
		public String							Set;
		public Float							Pow_Choice				= (float) CardDbAdapter.NOONECARES;
		public String							Pow_Logic;
		public Float							Tou_Choice				= (float) CardDbAdapter.NOONECARES;
		public String							Tou_Logic;
		public int								Cmc								= -1;
		public String							Cmc_Logic;
		public String							Format;
		public String							Rarity;
		public String							Flavor;
		public String							Artist;
		public int								Type_Logic				= 0;
		public int								Text_Logic				= 0;
		public int								Set_Logic;
	}

	protected static final int		SETLIST								= 1;
	protected static final int		FORMATLIST						= 2;
	protected static final int		RARITYLIST						= 3;

	public static final String		CRITERIA							= "criteria";
	public static final String		RANDOM								= "random";
	private static final String		DEFAULT_CRITERIA_FILE	= "defaultSearchCriteria.ser";

	private Button								searchbutton;
	private AutoCompleteTextView	namefield;
	private ImageButton							camerabutton;
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Cursor setCursor;
		try {
			setCursor = mDbHelper.fetchAllSets();
			setCursor.moveToFirst();
		}
		catch (FamiliarDbException e) {
			mDbHelper.showDbErrorToast(this.getActivity());
			this.getMainActivity().getSupportFragmentManager().popBackStack();
			return;
		}
		catch (SQLiteDatabaseCorruptException e) {
			mDbHelper.showDbErrorToast(this.getActivity());
			this.getMainActivity().getSupportFragmentManager().popBackStack();
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

		Cursor c;
		try {
			c = mDbHelper.fetchAllFormats();
		} catch (FamiliarDbException e) {
			mDbHelper.showDbErrorToast(this.getActivity());
			this.getMainActivity().getSupportFragmentManager().popBackStack();
			return;
		}
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

		setDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_sets)
				.setMultiChoiceItems(setNames, setChecked, new DialogSelectionClickHandler())
				.setPositiveButton(R.string.dialog_ok, new DialogButtonClickHandler()).create();
		formatDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_formats)
				.setSingleChoiceItems(formatNames, selectedFormat, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						selectedFormat = which;
					}
				}).setPositiveButton(R.string.dialog_ok, new DialogButtonClickHandler()).create();
		rarityDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_rarities)
				.setMultiChoiceItems(rarityNames, rarityChecked, new DialogSelectionClickHandler())
				.setPositiveButton(R.string.dialog_ok, new DialogButtonClickHandler()).create();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View myFragmentView = inflater.inflate(R.layout.search_frag, container, false);

		namefield = (AutoCompleteTextView) myFragmentView.findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(this.getActivity(), null));
		textfield = (EditText) myFragmentView.findViewById(R.id.textsearch);
		supertypefield = (AutoCompleteTextView) myFragmentView.findViewById(R.id.supertypesearch);
		subtypefield = (EditText) myFragmentView.findViewById(R.id.subtypesearch);
		flavorfield = (EditText) myFragmentView.findViewById(R.id.flavorsearch);
		artistfield = (EditText) myFragmentView.findViewById(R.id.artistsearch);

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
		ArrayAdapter<String> supertypeadapter = new ArrayAdapter<String>(this.getActivity(), R.layout.supertype_list_item,
				supertypes);
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

		searchbutton = (Button) myFragmentView.findViewById(R.id.searchbutton);
		camerabutton = (ImageButton) myFragmentView.findViewById(R.id.cameraButton);
		// randombutton = (Button) myFragmentView.findViewById(R.id.s);

		checkboxW = (CheckBox) myFragmentView.findViewById(R.id.checkBoxW);
		checkboxU = (CheckBox) myFragmentView.findViewById(R.id.checkBoxU);
		checkboxB = (CheckBox) myFragmentView.findViewById(R.id.checkBoxB);
		checkboxR = (CheckBox) myFragmentView.findViewById(R.id.checkBoxR);
		checkboxG = (CheckBox) myFragmentView.findViewById(R.id.checkBoxG);
		checkboxL = (CheckBox) myFragmentView.findViewById(R.id.checkBoxL);

		colorspinner = (Spinner) myFragmentView.findViewById(R.id.colorlogic);
		textspinner = (Spinner) myFragmentView.findViewById(R.id.textlogic);
		typespinner = (Spinner) myFragmentView.findViewById(R.id.typelogic);
		setspinner = (Spinner) myFragmentView.findViewById(R.id.setlogic);

		setButton = (Button) myFragmentView.findViewById(R.id.setsearch);
		formatButton = (Button) myFragmentView.findViewById(R.id.formatsearch);
		rarityButton = (Button) myFragmentView.findViewById(R.id.raritysearch);

		powLogic = (Spinner) myFragmentView.findViewById(R.id.powLogic);
		powChoice = (Spinner) myFragmentView.findViewById(R.id.powChoice);
		touLogic = (Spinner) myFragmentView.findViewById(R.id.touLogic);
		touChoice = (Spinner) myFragmentView.findViewById(R.id.touChoice);
		cmcLogic = (Spinner) myFragmentView.findViewById(R.id.cmcLogic);
		cmcChoice = (Spinner) myFragmentView.findViewById(R.id.cmcChoice);

		ArrayAdapter<CharSequence> powerLogicAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.logic_spinner, android.R.layout.simple_spinner_item);
		powerLogicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		powLogic.setAdapter(powerLogicAdapter);

		ArrayAdapter<CharSequence> powChoiceAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.pt_spinner, android.R.layout.simple_spinner_item);
		powChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		powChoice.setAdapter(powChoiceAdapter);

		ArrayAdapter<CharSequence> touLogicAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.logic_spinner, android.R.layout.simple_spinner_item);
		touLogicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		touLogic.setAdapter(touLogicAdapter);

		ArrayAdapter<CharSequence> touChoiceAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.pt_spinner, android.R.layout.simple_spinner_item);
		touChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		touChoice.setAdapter(touChoiceAdapter);

		ArrayAdapter<CharSequence> CMCLogicAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.logic_spinner, android.R.layout.simple_spinner_item);
		CMCLogicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cmcLogic.setAdapter(CMCLogicAdapter);
		cmcLogic.setSelection(1); // CMC should default to <

		ArrayAdapter<CharSequence> CMCChoiceAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.cmc_spinner, android.R.layout.simple_spinner_item);
		CMCChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cmcChoice.setAdapter(CMCChoiceAdapter);

		ArrayAdapter<CharSequence> colorSpinnerAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.color_spinner, android.R.layout.simple_spinner_item);
		colorSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		colorspinner.setAdapter(colorSpinnerAdapter);
		colorspinner.setSelection(2);

		// Lines Below added by Reuben Kriegel
		ArrayAdapter<CharSequence> rulesTextAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.text_spinner, android.R.layout.simple_spinner_item);
		rulesTextAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		textspinner.setAdapter(rulesTextAdapter);

		ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.type_spinner,
				android.R.layout.simple_spinner_item);
		typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		typespinner.setAdapter(typeAdapter);
		// End addition

		ArrayAdapter<CharSequence> printingsAdapter = ArrayAdapter.createFromResource(this.getActivity(),
				R.array.set_spinner, android.R.layout.simple_spinner_item);
		printingsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		setspinner.setAdapter(printingsAdapter);

		boolean consolidate = true;
		consolidate = getMainActivity().getPreferencesAdapter().getConsolidateSearch();
		setspinner.setSelection(consolidate ? CardDbAdapter.MOSTRECENTPRINTING : CardDbAdapter.ALLPRINTINGS);

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

		camerabutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				takePictureAndSearchGoogleGogglesIntent();
			}
		});
		
		checkDialogButtonColors();

		return myFragmentView;
	}
	
	@Override
    protected void onGoogleGogglesSuccess(String cardName) {
    	// this method must be overridden by each class calling takePictureAndSearchGoogleGogglesIntent
		namefield.setText(cardName);
		doSearch(false);
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

		// add a fragment
		Bundle args = new Bundle();
		args.putBoolean(RANDOM, isRandom);
		args.putSerializable(CRITERIA, searchCriteria);
		ResultListFragment rlFrag = new ResultListFragment();
		anchor.startNewFragment(rlFrag, args);
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
		this.removeDialog();

		setDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_sets)
				.setMultiChoiceItems(setNames, setChecked, new DialogSelectionClickHandler())
				.setPositiveButton(R.string.dialog_ok, new DialogButtonClickHandler()).create();
		formatDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_formats)
				.setSingleChoiceItems(formatNames, selectedFormat, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						selectedFormat = which;
					}
				}).setPositiveButton(R.string.dialog_ok, new DialogButtonClickHandler()).create();
		rarityDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_rarities)
				.setMultiChoiceItems(rarityNames, rarityChecked, new DialogSelectionClickHandler())
				.setPositiveButton(R.string.dialog_ok, new DialogButtonClickHandler()).create();

		setButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
		formatButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
		rarityButton.getBackground().setColorFilter(0xFFFFFFFF, Mode.DST);
	}

	private void persistOptions() {
		try {
			SearchCriteria searchCriteria = parseForm();
			FileOutputStream fileStream = this.getActivity().openFileOutput(DEFAULT_CRITERIA_FILE, Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(fileStream);
			os.writeObject(searchCriteria);
			os.close();
		}
		catch (IOException e) {
			Toast.makeText(this.getActivity(), R.string.search_toast_cannot_save, Toast.LENGTH_LONG).show();
		}
	}

	private void fetchPersistedOptions() {
		try {
			FileInputStream fileInputStream = this.getActivity().openFileInput(DEFAULT_CRITERIA_FILE);
			ObjectInputStream oInputStream = new ObjectInputStream(fileInputStream);
			SearchCriteria criteria = (SearchCriteria) oInputStream.readObject();
			oInputStream.close();

			namefield.setText(criteria.Name);
			String[] type = criteria.Type.split(" - ");
			try {
				supertypefield.setText(type[0]);
				subtypefield.setText(type[1]);
			}
			catch (Exception e) {
			}
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
			if (p != CardDbAdapter.NOONECARES) {
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
						powChoice.setSelection(ptlist.indexOf(((int) p) + ""));
					}
					else {
						powChoice.setSelection(ptlist.indexOf(p + ""));
					}
				}
			}
			touLogic.setSelection(logicChoices.indexOf(criteria.Tou_Logic));
			float t = criteria.Tou_Choice;
			if (t != CardDbAdapter.NOONECARES) {
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
						touChoice.setSelection(ptlist.indexOf(((int) t) + ""));
					}
					else {
						touChoice.setSelection(ptlist.indexOf(t + ""));
					}
				}
			}
			cmcLogic.setSelection(logicChoices.indexOf(criteria.Cmc_Logic));
			try {
				cmcChoice.setSelection(Arrays.asList(getResources().getStringArray(R.array.cmc_spinner)).indexOf(
						String.valueOf(criteria.Cmc)));
			}
			catch (Exception e) {
			}

			if (criteria.Set != null) {
				List<String> sets = Arrays.asList(criteria.Set.split("-"));
				for (int i = 0; i < setChecked.length; i++)
					setChecked[i] = sets.contains(setSymbols[i]);
			}
			else
				for (int i = 0; i < setChecked.length; i++)
					setChecked[i] = false;

			selectedFormat = Arrays.asList(formatNames).indexOf(criteria.Format);
			for (int i = 0; i < rarityChecked.length; i++) {
				rarityChecked[i] = (criteria.Rarity != null && criteria.Rarity.contains(rarityNames[i].charAt(0) + ""));
			}

			this.removeDialog();

			setDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_sets)
					.setMultiChoiceItems(setNames, setChecked, new DialogSelectionClickHandler())
					.setPositiveButton(R.string.dialog_ok, new DialogButtonClickHandler()).create();
			formatDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_formats)
					.setSingleChoiceItems(formatNames, selectedFormat, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							selectedFormat = which;
						}
					}).setPositiveButton(R.string.dialog_ok, new DialogButtonClickHandler()).create();
			rarityDialog = new AlertDialog.Builder(this.getActivity()).setTitle(R.string.search_rarities)
					.setMultiChoiceItems(rarityNames, rarityChecked, new DialogSelectionClickHandler())
					.setPositiveButton(R.string.dialog_ok, new DialogButtonClickHandler()).create();

			checkDialogButtonColors();

		}
		catch (IOException e) {
			Toast.makeText(this.getActivity(), R.string.search_toast_cannot_load, Toast.LENGTH_LONG).show();
		}
		catch (ClassNotFoundException e) {
			Toast.makeText(this.getActivity(), R.string.search_toast_cannot_load, Toast.LENGTH_LONG).show();
		}
	}

	private void checkDialogButtonColors() {
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
	}

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
			case R.id.search_menu_save_defaults:
				persistOptions();
				return true;
			case R.id.search_menu_load_defaults:
				fetchPersistedOptions();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.search_menu, menu);
	}

	protected void showDialog(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}

		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				super.onDismiss(dialog);
				checkDialogButtonColors();
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				switch (id) {
					case SETLIST: {
						return setDialog;
					}
					case FORMATLIST: {
						return formatDialog;
					}
					case RARITYLIST: {
						return rarityDialog;
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(ft, DIALOG_TAG);
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
	public boolean onInterceptSearchKey() {
		doSearch(false);
		return true;
	}
}
