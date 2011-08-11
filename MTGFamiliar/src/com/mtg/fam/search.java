package com.mtg.fam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.ToggleButton;

public class search extends Activity{
	protected static final String NAME = "name";
	protected static final String TEXT = "text";
	protected static final String TYPE = "type";
	protected static final String COLOR = "color";
	protected static final String COLORLOGIC = "colorlogic";
	protected static final String SET = "set";
	protected static final String POW_CHOICE = "pow_choice";
	protected static final String POW_LOGIC = "pow_logic";
	protected static final String TOU_CHOICE = "tou_choice";
	protected static final String TOU_LOGIC = "tou_logic";
	protected static final String CMC = "cmc";
	protected static final String CMC_LOGIC = "cmc_logic";
	protected static final String FORMAT = "format";
	protected static final String RARITY = "rarity";

	protected static final int SETLIST = 0;
	protected static final int FORMATLIST = 1;
	protected static final int RARITYLIST = 2;
	
	private CardDbAdapter mDbHelper;
	private Button searchbutton;
	AutoCompleteTextView namefield;
	AutoCompleteTextView textfield;
	AutoCompleteTextView typefield;
	private CheckBox checkboxW;
	private CheckBox checkboxU;
	private CheckBox checkboxB;
	private CheckBox checkboxR;
	private CheckBox checkboxG;
	private CheckBox checkboxL;
	private ToggleButton logicbutton;
	private Button setButton;
	private String[] setNames;
	private boolean[] setChecked;
	private String[] setSymbols;
	private Context mCtx;
	private Spinner powLogic;
	private Spinner powChoice;
	private Spinner touLogic;
	private Spinner touChoice;
	private Spinner cmcLogic;
	private Spinner cmcChoice;
	private Button formatButton;
	private String[] formatNames;
	private boolean[] formatChecked;
	private Button	rarityButton;
	private String[]	rarityNames;
	private boolean[]	rarityChecked;
	private Dialog	setDialog;
	private Dialog	formatDialog;
	private Dialog	rarityDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();

		namefield = (AutoCompleteTextView)findViewById(R.id.namesearch);
		textfield = (AutoCompleteTextView)findViewById(R.id.textsearch);
		typefield = (AutoCompleteTextView)findViewById(R.id.typesearch);
		searchbutton = (Button)findViewById(R.id.searchbutton);

		checkboxW = (CheckBox)findViewById(R.id.checkBoxW);
		checkboxU = (CheckBox)findViewById(R.id.checkBoxU);
		checkboxB = (CheckBox)findViewById(R.id.checkBoxB);
		checkboxR = (CheckBox)findViewById(R.id.checkBoxR);
		checkboxG = (CheckBox)findViewById(R.id.checkBoxG);
		checkboxL = (CheckBox)findViewById(R.id.checkBoxL);
	
    logicbutton = (ToggleButton) findViewById(R.id.logicToggle);
    
    setButton = (Button)findViewById(R.id.setsearch);
    formatButton = (Button)findViewById(R.id.formatsearch);
    rarityButton = (Button)findViewById(R.id.raritysearch);
    
    powLogic = (Spinner)findViewById(R.id.powLogic);
    powChoice= (Spinner)findViewById(R.id.powChoice);
    touLogic = (Spinner)findViewById(R.id.touLogic);
    touChoice= (Spinner)findViewById(R.id.touChoice);
    cmcLogic = (Spinner)findViewById(R.id.cmcLogic);
    cmcChoice= (Spinner)findViewById(R.id.cmcChoice);
    
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource( this, R.array.logic_spinner, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    powLogic.setAdapter(adapter);

    ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource( this, R.array.pt_spinner, android.R.layout.simple_spinner_item);
    adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    powChoice.setAdapter(adapter1);

    ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource( this, R.array.logic_spinner, android.R.layout.simple_spinner_item);
    adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    touLogic.setAdapter(adapter2);

    ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource( this, R.array.pt_spinner, android.R.layout.simple_spinner_item);
    adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    touChoice.setAdapter(adapter3);

    ArrayAdapter<CharSequence> adapter4 = ArrayAdapter.createFromResource( this, R.array.logic_spinner, android.R.layout.simple_spinner_item);
    adapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    cmcLogic.setAdapter(adapter4);

    ArrayAdapter<CharSequence> adapter5 = ArrayAdapter.createFromResource( this, R.array.cmc_spinner, android.R.layout.simple_spinner_item);
    adapter5.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    cmcChoice.setAdapter(adapter5);

		setButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog( SETLIST );
			}
		});
		
		formatButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog( FORMATLIST );
			}
		});
		
		rarityButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog( RARITYLIST );
			}
		});
		
		mCtx = this;
		
		searchbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String name = namefield.getText().toString();
				String text = textfield.getText().toString();
				String type = typefield.getText().toString();
				
				if(name.length() == 0){
					name = null;
				}
				if(text.length() == 0){
					text = null;
				}
				if(type.length() == 0){
					type = null;
				}
				
				String color = null;
				
				if(checkboxW.isChecked()){
					color = "W";						
				}
				else{
					color = "w";
				}
				
				if(checkboxU.isChecked()){
					color += "U";
				}
				else{
					color += "u";
				}
				if(checkboxB.isChecked()){
					color += "B";
				}
				else{
					color += "b";
				}
				if(checkboxR.isChecked()){
					color += "R";
				}
				else{
					color += "r";
				}
				if(checkboxG.isChecked()){
					color += "G";
				}
				else{
					color += "g";
				}
				if(checkboxL.isChecked()){
					color += "L";
				}
				else{
					color += "l";
				}
				
				String sets = null;
				
				for(int i=0; i < setChecked.length; i++){
					if(setChecked[i]){
						if(sets==null){
							sets = setSymbols[i];
						}
						else{
							sets += "-" + setSymbols[i];							
						}
					}
				}
				
				String fmt = null;
				for(int i=0; i < formatChecked.length; i++){
					if(formatChecked[i]){
						if(fmt==null){
							fmt = formatNames[i];
						}
						else{
							fmt += "-" + formatNames[i];							
						}
					}
				}
				
				String rarity = null;
				for(int i=0; i < rarityChecked.length; i++){
					if(rarityChecked[i]){
						if(rarity == null){
							rarity = rarityNames[i].charAt(0)+"";
						}
						else{
							rarity += rarityNames[i].charAt(0);
						}
					}
				}
				
				String[] ptChoices = getResources().getStringArray(R.array.pt_spinner);
				String[] logicChoices = getResources().getStringArray(R.array.logic_spinner);
				String[] cmcChoices = getResources().getStringArray(R.array.cmc_spinner);
				int cmc;
				try{
					cmc = Integer.parseInt(cmcChoices[cmcChoice.getSelectedItemPosition()]);
				}
				catch(NumberFormatException e){
					cmc = -1;
				}
				Intent i = new Intent(mCtx, resultlist.class);
				i.putExtra(NAME, name);
				i.putExtra(TEXT, text);
				i.putExtra(TYPE, type);
				i.putExtra(COLOR, color);
				i.putExtra(COLORLOGIC, logicbutton.isChecked());
				i.putExtra(SET, sets);
				i.putExtra(FORMAT, fmt);
				i.putExtra(POW_CHOICE, ptChoices[powChoice.getSelectedItemPosition()]);
				i.putExtra(POW_LOGIC, logicChoices[powLogic.getSelectedItemPosition()]);
				i.putExtra(TOU_CHOICE, ptChoices[touChoice.getSelectedItemPosition()]);
				i.putExtra(TOU_LOGIC, logicChoices[touLogic.getSelectedItemPosition()]);
				i.putExtra(CMC, cmc);
				i.putExtra(CMC_LOGIC, logicChoices[cmcLogic.getSelectedItemPosition()]);
				i.putExtra(RARITY, rarity);
        startActivity(i);
			}
		});
		
		Cursor setCursor = mDbHelper.fetchAllSets();
		setCursor.moveToFirst();
		
		setNames = new String[setCursor.getCount()];
		setSymbols = new String[setCursor.getCount()];
		setChecked = new boolean[setCursor.getCount()];
		
		for(int i=0; i < setCursor.getCount(); i++){
			setSymbols[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_CODE));
			setNames[i] = setCursor.getString(setCursor.getColumnIndex(CardDbAdapter.KEY_NAME));
			setChecked[i] = false;
			setCursor.moveToNext();
		}
		
		Resources res = getResources();
		formatNames = res.getStringArray(R.array.format_names);
		formatChecked = new boolean[formatNames.length];
		
		rarityNames = res.getStringArray(R.array.rarities);
		rarityChecked = new boolean[rarityNames.length];
	}

	@Override
	protected Dialog onCreateDialog( int id ) 
	{
		if(id == SETLIST){
			setDialog = new AlertDialog.Builder( this )
	        	.setTitle( "Sets" )
	        	.setMultiChoiceItems( setNames, setChecked, new DialogSelectionClickHandler() )
	        	.setPositiveButton( "OK", new DialogButtonClickHandler() )
	        	.create();
			return setDialog;
		}
		else if(id==FORMATLIST){
			formatDialog = new AlertDialog.Builder( this )
	        	.setTitle( "Formats" )
	        	.setMultiChoiceItems( formatNames, formatChecked, new DialogSelectionClickHandler() )
	        	.setPositiveButton( "OK", new DialogButtonClickHandler() )
	        	.create();
			return formatDialog;
		}
		else if(id==RARITYLIST){
			rarityDialog = new AlertDialog.Builder( this )
	        	.setTitle( "Rarities" )
	        	.setMultiChoiceItems( rarityNames, rarityChecked, new DialogSelectionClickHandler() )
	        	.setPositiveButton( "OK", new DialogButtonClickHandler() )
	        	.create();
			return rarityDialog;
		}
		return null;
	}
	
	public class DialogSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener
	{
		public void onClick( DialogInterface dialog, int clicked, boolean selected )
		{
			
			// called when something checked;
		}
	}
	

	public class DialogButtonClickHandler implements DialogInterface.OnClickListener
	{
		public void onClick( DialogInterface dialog, int clicked )
		{
			switch( clicked )
			{
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
			case R.id.clear:
				namefield.setText("");
				typefield.setText("");
				textfield.setText("");
				
				checkboxW.setChecked(false);
				checkboxU.setChecked(false);
				checkboxB.setChecked(false);
				checkboxR.setChecked(false);
				checkboxG.setChecked(false);
				checkboxL.setChecked(false);
				logicbutton.setChecked(false);
				
				powLogic.setSelection(0);
				powChoice.setSelection(0);
				touLogic.setSelection(0);
				touChoice.setSelection(0);
				cmcLogic.setSelection(0);
				cmcChoice.setSelection(0);
				
				for(int i=0; i < setChecked.length; i++){
					setChecked[i] = false;
				}
				for(int i=0; i < formatChecked.length; i++){
					formatChecked[i] = false;
				}
				for(int i=0; i < rarityChecked.length; i++){
					rarityChecked[i] = false;
				}
				this.removeDialog(SETLIST);
				this.removeDialog(FORMATLIST);
				this.removeDialog(RARITYLIST);
/*
				onCreateDialog(SETLIST);
				onCreateDialog(FORMATLIST);
				onCreateDialog(RARITYLIST);
*/				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}