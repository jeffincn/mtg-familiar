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

import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

public class RandomCardActivity extends Activity {
	private static final int		RULESDIALOG				= 0;
	protected static final int	MOMIR_IMAGE				= 1;
	protected static final int	STONEHEWER_IMAGE	= 2;
	protected static final int	JHOIRA_IMAGE			= 3;
	private CardDbAdapter				mDbAdapter;
	private Random							rand;
	private String							name;
	private Spinner							momirCmcChoice;
	private String[]						cmcChoices;
	private Button							momirButton;
	private Context							mCtx;
	private Button							stonehewerButton;
	private Spinner							stonehewerCmcChoice;
	private Button							jhoiraInstantButton;
	private Button							jhoiraSorceryButton;
	private ImageView						stonehewerImage;
	private ImageView						momirImage;
	private ImageView						jhoiraImage;
	private SharedPreferences		preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.random_card_activity);

		momirImage = (ImageView) findViewById(R.id.imageViewMo);
		stonehewerImage = (ImageView) findViewById(R.id.imageViewSto);
		jhoiraImage = (ImageView) findViewById(R.id.imageViewJho);

		momirImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(MOMIR_IMAGE);
			}
		});
		stonehewerImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(STONEHEWER_IMAGE);
			}
		});
		jhoiraImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(JHOIRA_IMAGE);
			}
		});

		momirButton = (Button) findViewById(R.id.momir_button);
		stonehewerButton = (Button) findViewById(R.id.stonehewer_button);
		jhoiraInstantButton = (Button) findViewById(R.id.jhorira_instant_button);
		jhoiraSorceryButton = (Button) findViewById(R.id.jhorira_sorcery_button);

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
						CardDbAdapter.NOONECARES, null, cmc, "=", null, null, null, null, false, returnTypes, false, CardDbAdapter.KEY_NAME);

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

				Cursor equipment = mDbAdapter.Search(null, null, "Equipment", "wubrgl", 0, null, CardDbAdapter.NOONECARES,
						null, CardDbAdapter.NOONECARES, null, cmc + 1, "<", null, null, null, null, false, returnTypes, false, CardDbAdapter.KEY_NAME);

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
						CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, false, returnTypes, false, CardDbAdapter.KEY_NAME);

				// Get 3 random, distinct numbers
				int pos[] = new int[3];
				pos[0] = rand.nextInt(instants.getCount());
				pos[1] = rand.nextInt(instants.getCount());
				while (pos[0] == pos[1]) {
					pos[1] = rand.nextInt(instants.getCount());
				}
				pos[2] = rand.nextInt(instants.getCount());
				while (pos[0] == pos[2] || pos[1] == pos[2]) {
					pos[2] = rand.nextInt(instants.getCount());
				}

				String names[] = new String[3];
				Intent intent = new Intent(mCtx, ResultListActivity.class);
				for (int i = 0; i < 3; i++) {
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
						CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, false, returnTypes, false, CardDbAdapter.KEY_NAME);

				// Get 3 random, distinct numbers
				int pos[] = new int[3];
				pos[0] = rand.nextInt(sorceries.getCount());
				pos[1] = rand.nextInt(sorceries.getCount());
				while (pos[0] == pos[1]) {
					pos[1] = rand.nextInt(sorceries.getCount());
				}
				pos[2] = rand.nextInt(sorceries.getCount());
				while (pos[0] == pos[2] || pos[1] == pos[2]) {
					pos[2] = rand.nextInt(sorceries.getCount());
				}

				String names[] = new String[3];
				Intent intent = new Intent(mCtx, ResultListActivity.class);
				for (int i = 0; i < 3; i++) {
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

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		boolean b = preferences.getBoolean("mojhostoFirstTime", true);
		if (b) {
			showDialog(RULESDIALOG);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("mojhostoFirstTime", false);
			editor.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.random_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.random_rules:
				showDialog(RULESDIALOG);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		Dialog d = null;
		if (id == RULESDIALOG) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setNeutralButton("Lets Play!", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			builder.setMessage(Html.fromHtml(getString(R.string.mojhostorules)));
			builder.setTitle(R.string.random_rules);
			d = builder.create();
		}
		else if (id == MOMIR_IMAGE) {
			d = new Dialog(this);
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);

			d.setContentView(R.layout.image_dialog);

			ImageView image = (ImageView) d.findViewById(R.id.cardimage);
			image.setImageResource(R.drawable.momir_full);
		}
		else if (id == STONEHEWER_IMAGE) {
			d = new Dialog(this);
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);

			d.setContentView(R.layout.image_dialog);

			ImageView image = (ImageView) d.findViewById(R.id.cardimage);
			image.setImageResource(R.drawable.stonehewer_full);
		}
		else if (id == JHOIRA_IMAGE) {
			d = new Dialog(this);
			d.requestWindowFeature(Window.FEATURE_NO_TITLE);

			d.setContentView(R.layout.image_dialog);

			ImageView image = (ImageView) d.findViewById(R.id.cardimage);
			image.setImageResource(R.drawable.jhoira_full);
		}
		return d;
	}
}
