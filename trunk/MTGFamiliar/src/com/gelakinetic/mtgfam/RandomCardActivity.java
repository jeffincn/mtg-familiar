package com.gelakinetic.mtgfam;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.view.View;

public class RandomCardActivity extends Activity {
	private CardDbAdapter		mDbAdapter;
	private Random					rand;
	private String					name;
	private Spinner					momirCmcChoice;
	private String[]				cmcChoices;
	private Button					momirButton;
	private Context					mCtx;
	private Button	stonehewerButton;
	private Spinner	stonehewerCmcChoice;
	private Button	jhoiraInstantButton;
	private Button	jhoiraSorceryButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.random_card_activity);

		momirButton = (Button) findViewById(R.id.momir_button);
		stonehewerButton = (Button) findViewById(R.id.stonehewer_button);
		jhoiraInstantButton = (Button)findViewById(R.id.jhorira_instant_button);
		jhoiraSorceryButton = (Button)findViewById(R.id.jhorira_sorcery_button);
		
		mCtx = (Context) this;

		momirButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				int cmc;
				try {
					cmc = Integer.parseInt(cmcChoices[momirCmcChoice.getSelectedItemPosition()]);
				}
				catch (NumberFormatException e) {
					cmc = -1;
				}
				
				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };
				
				Cursor doods = mDbAdapter.Search(null, null, "Creature", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
						CardDbAdapter.NOONECARES, null, cmc, "=", null, null, null, null, false, returnTypes);

				int pos = rand.nextInt(doods.getCount());
				doods.moveToPosition(pos);
				name = doods.getString(doods.getColumnIndex(CardDbAdapter.KEY_NAME));

				Intent i = new Intent(mCtx, ResultListActivity.class);
				i.putExtra("id", mDbAdapter.fetchIdByName(name));
				startActivityForResult(i, 0);
			}
		});
		
		stonehewerButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				int cmc;
				try {
					cmc = Integer.parseInt(cmcChoices[stonehewerCmcChoice.getSelectedItemPosition()]);
				}
				catch (NumberFormatException e) {
					cmc = -1;
				}

				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };
				
				Cursor equipment = mDbAdapter.Search(null, null, "Equipment", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
						CardDbAdapter.NOONECARES, null, cmc+1, "<", null, null, null, null, false, returnTypes);
				
				int pos = rand.nextInt(equipment.getCount());
				equipment.moveToPosition(pos);
				name = equipment.getString(equipment.getColumnIndex(CardDbAdapter.KEY_NAME));

				Intent i = new Intent(mCtx, ResultListActivity.class);
				i.putExtra("id", mDbAdapter.fetchIdByName(name));
				startActivityForResult(i, 0);
			}
		});

		jhoiraInstantButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };
				
				Cursor instants = mDbAdapter.Search(null, null, "instant", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
						CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, false, returnTypes);

				// Get 3 random, distinct numbers
				int pos[] = new int[3];
				pos[0] = rand.nextInt(instants.getCount());
				pos[1] = rand.nextInt(instants.getCount());
				while(pos[0] == pos[1]){
					pos[1] = rand.nextInt(instants.getCount());					
				}
				pos[2] = rand.nextInt(instants.getCount());
				while(pos[0] == pos[2] || pos[1] == pos[2]){
					pos[2] = rand.nextInt(instants.getCount());					
				}
				
				String names[] = new String[3];
				Intent intent = new Intent(mCtx, ResultListActivity.class);
				for(int i=0; i < 3; i++){
					instants.moveToPosition(pos[i]);
	 				names[i] = instants.getString(instants.getColumnIndex(CardDbAdapter.KEY_NAME));
	 				intent.putExtra("id" + i, mDbAdapter.fetchIdByName(names[i]));
				}
				
				startActivityForResult(intent, 0);
			}
		});
		
		jhoiraSorceryButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };
				
				Cursor sorceries = mDbAdapter.Search(null, null, "sorcery", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
						CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, false, returnTypes);

				// Get 3 random, distinct numbers
				int pos[] = new int[3];
				pos[0] = rand.nextInt(sorceries.getCount());
				pos[1] = rand.nextInt(sorceries.getCount());
				while(pos[0] == pos[1]){
					pos[1] = rand.nextInt(sorceries.getCount());					
				}
				pos[2] = rand.nextInt(sorceries.getCount());
				while(pos[0] == pos[2] || pos[1] == pos[2]){
					pos[2] = rand.nextInt(sorceries.getCount());					
				}
				
				String names[] = new String[3];
				Intent intent = new Intent(mCtx, ResultListActivity.class);
				for(int i=0; i < 3; i++){
					sorceries.moveToPosition(pos[i]);
	 				names[i] = sorceries.getString(sorceries.getColumnIndex(CardDbAdapter.KEY_NAME));
	 				intent.putExtra("id" + i, mDbAdapter.fetchIdByName(names[i]));
				}
				
				startActivityForResult(intent, 0);
			}
		});
		
		momirCmcChoice = (Spinner) findViewById(R.id.momir_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.momir_spinner,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		momirCmcChoice.setAdapter(adapter);

		stonehewerCmcChoice = (Spinner) findViewById(R.id.stonehewer_spinner);
		ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this, R.array.momir_spinner,
				android.R.layout.simple_spinner_item);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		stonehewerCmcChoice.setAdapter(adapter1);

		mDbAdapter = new CardDbAdapter(this);
		mDbAdapter.open();

		cmcChoices = getResources().getStringArray(R.array.momir_spinner);

		rand = new Random(System.currentTimeMillis());
		/*
		 * // implements http://en.wikipedia.org/wiki/Fisher-Yates_shuffle a = new
		 * int[numChoices]; int temp, i, j; for (i = 0; i < numChoices; i++) { a[i]
		 * = i; } for (i = numChoices - 1; i > 0; i--) { j = rand.nextInt(i + 1);//
		 * j = random integer with 0 <= j <= i temp = a[j]; a[j] = a[i]; a[i] =
		 * temp; } index = 0;
		 */
	}
}
