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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class RandomCardActivity extends FragmentActivity {
	private static final int		RULESDIALOG				= 0;
	protected static final int	MOMIR_IMAGE				= 1;
	protected static final int	STONEHEWER_IMAGE	= 2;
	protected static final int	JHOIRA_IMAGE			= 3;
	protected static final int	CORRUPTION = 4;
	private CardDbAdapter				mDbHelper;
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

				try {
					Cursor doods = mDbHelper.Search(null, null, "Creature", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
							CardDbAdapter.NOONECARES, null, cmc, "=", null, null, null, null, 0, 0, CardDbAdapter.ANYPRINTING, false, returnTypes, true);

					int pos = rand.nextInt(doods.getCount());
					doods.moveToPosition(pos);
					name = doods.getString(doods.getColumnIndex(CardDbAdapter.KEY_NAME));
					doods.close();
	
					Intent i = new Intent(mCtx, ResultListActivity.class);
					i.putExtra("id", mDbHelper.fetchIdByName(name));
					startActivityForResult(i, 0);
				}
				catch (SQLiteDatabaseCorruptException e) {
					showDialog(CORRUPTION);
					return;
				}
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

				try {
					Cursor equipment = mDbHelper.Search(null, null, "Equipment", "wubrgl", 0, null, CardDbAdapter.NOONECARES,
							null, CardDbAdapter.NOONECARES, null, cmc + 1, "<", null, null, null, null, 0, 0, CardDbAdapter.ANYPRINTING, false, returnTypes, true);

					int pos = rand.nextInt(equipment.getCount());
					equipment.moveToPosition(pos);
					name = equipment.getString(equipment.getColumnIndex(CardDbAdapter.KEY_NAME));
					equipment.close();
	
					Intent i = new Intent(mCtx, ResultListActivity.class);
					i.putExtra("id", mDbHelper.fetchIdByName(name));
					startActivityForResult(i, 0);
				}
				catch (SQLiteDatabaseCorruptException e) {
					showDialog(CORRUPTION);
					return;
				}
			}
		});

		jhoiraInstantButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };

				try {
					Cursor instants = mDbHelper.Search(null, null, "instant", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
							CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, 0, 0, CardDbAdapter.ANYPRINTING, false, returnTypes, true);

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
						intent.putExtra("id" + i, mDbHelper.fetchIdByName(names[i]));
					}
					instants.close();
	
					startActivityForResult(intent, 0);
				}
				catch (SQLiteDatabaseCorruptException e) {
					showDialog(CORRUPTION);
					return;
				}
			}
		});

		jhoiraSorceryButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };

				try {
					Cursor sorceries = mDbHelper.Search(null, null, "sorcery", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
							CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, 0, 0, CardDbAdapter.ANYPRINTING, false, returnTypes, true);
	
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
						intent.putExtra("id" + i, mDbHelper.fetchIdByName(names[i]));
					}
					sorceries.close();
	
					startActivityForResult(intent, 0);
				}
				catch (SQLiteDatabaseCorruptException e) {
					showDialog(CORRUPTION);
					return;
				}
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

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.openReadable();

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
		
		MenuFragmentCompat.init(this, R.menu.random_menu, "random_card_menu_fragment");
	}

	@Override
	protected void onResume(){
		super.onResume();
		MyApp appState = ((MyApp)getApplicationContext());
		appState.setState(0);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
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
		else if (id == CORRUPTION) {
			View dialogLayout = getLayoutInflater().inflate(R.layout.corruption_layout, null);
			TextView text = (TextView)dialogLayout.findViewById(R.id.corruption_message);
			text.setText(Html.fromHtml(getString(R.string.corruption_error)));
			text.setMovementMethod(LinkMovementMethod.getInstance());
			
			d = new AlertDialog.Builder(this)
				.setTitle(R.string.corruption_error_title)
				.setView(dialogLayout)
				.setPositiveButton(R.string.dialog_ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})
				.setCancelable(false)
				.create();
		}
		return d;
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
}
