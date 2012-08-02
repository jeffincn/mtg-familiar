/**
Copyright 2012 Jonathan Bettger

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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.GatheringsPlayerData;

public class GatheringCreateActivity extends FamiliarActivity {
	private static final int								DIALOG_SET_NAME				= 0;
	private static final int								DIALOG_GATHERING_EXIST		= 1;
	private static final String								NO_GATHERINGS_EXIST			= "No Gatherings exist.";
	private static final String								TOAST_NO_NAME				= "Please include a name for the Gathering.";
															

	private String							proposedGathering;

	private Context							mCtx;
	private GatheringsIO				gIO;
	private LinearLayout				mainLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gathering_create_activity);

		mainLayout = (LinearLayout) findViewById(R.id.gathering_player_list);

		mCtx = this;
		gIO = new GatheringsIO(mCtx);
		
		AddPlayerRowFromData(new GatheringsPlayerData("Player 1", 20));
		AddPlayerRowFromData(new GatheringsPlayerData("Player 2", 20));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
			case DIALOG_SET_NAME:
				LayoutInflater factory = LayoutInflater.from(this);
				final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
				final EditText nameInput = (EditText) textEntryView.findViewById(R.id.player_name);
				nameInput.setText(proposedGathering);
				dialog = new AlertDialog.Builder(this).setTitle("Enter Gathering's Name").setView(textEntryView)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								String gatheringName = nameInput.getText().toString().trim();
								if (gatheringName.length() <= 0){
									Toast.makeText(mCtx, TOAST_NO_NAME, Toast.LENGTH_LONG).show();
									return;
								}
								
								
								ArrayList<String> existingGatheringsFiles = gIO.getGatheringFileList();
								
								boolean existing = false;
								for (String existingGatheringFile : existingGatheringsFiles){
									String givenName = gIO.ReadGatheringNameFromXML(existingGatheringFile);
									
									if (gatheringName.equals(givenName)){
										//throw existing dialog
										existing = true;
										proposedGathering = gatheringName;
										showDialog(DIALOG_GATHERING_EXIST);
										break;
									}
								}
								
								if (existingGatheringsFiles.size() <= 0 || existing == false){
									SaveGathering(gatheringName);
								}
							}
						}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
							}
						}).create();

				break;
			case DIALOG_GATHERING_EXIST:
				LayoutInflater factory2 = LayoutInflater.from(this);
				final View textEntryView2 = factory2.inflate(R.layout.corruption_layout, null);
				final TextView text = (TextView) textEntryView2.findViewById(R.id.corruption_message);
				text.setText("This Gathering already exists, overwrite existing file?");
				dialog = new AlertDialog.Builder(this).setTitle("Overwrite Existing Gathering?").setView(textEntryView2)
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								gIO.DeleteGatheringByName(proposedGathering);
								SaveGathering(proposedGathering);
							}
						}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
							}
						}).create();

				break;
			default:
				dialog = null;
		}
		return dialog;
	}

	private void SaveGathering(String _gatheringName){
		if (_gatheringName.length() <= 0){
			Toast.makeText(mCtx, TOAST_NO_NAME, Toast.LENGTH_LONG).show();
			return;
		}
		
		int playersCount = mainLayout.getChildCount();
		ArrayList<GatheringsPlayerData> players = new ArrayList<GatheringsPlayerData>(playersCount);
		
		for (int idx = 0; idx < playersCount; idx++) {
            View player = mainLayout.getChildAt(idx);
            
            EditText customName = (EditText) player.findViewById(R.id.custom_name);
            String name = customName.getText().toString().trim();
            
            EditText startingLife = (EditText) player.findViewById(R.id.starting_life);
            int life = Integer.parseInt(startingLife.getText().toString());
            
            players.add(new GatheringsPlayerData(name, life));
		}
		
		gIO.writeGatheringXML(players, _gatheringName);
	}
	
	private void AddPlayerRowFromData(GatheringsPlayerData _player) {
		LayoutInflater inf = getLayoutInflater();
		View v = inf.inflate(R.layout.gathering_create_player_row, null);
		
		TextView name = (TextView) v.findViewById(R.id.custom_name);
		name.setText(_player.getName());
		
		TextView life = (TextView) v.findViewById(R.id.starting_life);
		life.setText(String.valueOf(_player.getStartingLife()));

		mainLayout.addView(v);
	}

	private void RemoveAllPlayerRows() {
		mainLayout.removeAllViews();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.gdelete_gathering:
				if (gIO.getNumberOfGatherings() <= 0){
					Toast.makeText(this, NO_GATHERINGS_EXIST, Toast.LENGTH_LONG).show();
					return true;
				}
				
				ArrayList<String> dGatherings = gIO.getGatheringFileList();
				final String[] dfGatherings = dGatherings.toArray(new String[dGatherings.size()]);
				final String[] dProperNames = new String[dGatherings.size()];
				for (int idx = 0; idx < dGatherings.size(); idx++) {
					dProperNames[idx] = gIO.ReadGatheringNameFromXML(dGatherings.get(idx));
				}

				AlertDialog.Builder dbuilder = new AlertDialog.Builder(mCtx);
				dbuilder.setTitle("Delete a Gathering");
				dbuilder.setItems(dProperNames, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int item) {
						gIO.DeleteGathering(dfGatherings[item]);
						return;
					}
				});
				dbuilder.create().show();
				return true;
			case R.id.gremove_player:
				ArrayList<String> names = new ArrayList<String>();
				for (int idx = 0; idx < mainLayout.getChildCount(); idx++){
		            View player = mainLayout.getChildAt(idx);
		            
		            EditText customName = (EditText) player.findViewById(R.id.custom_name);
		            names.add(customName.getText().toString().trim());
				}
				final String[] aNames = names.toArray(new String[names.size()]);
				
				AlertDialog.Builder builderRemove = new AlertDialog.Builder(mCtx);
				builderRemove.setTitle("Load a Gathering");
				builderRemove.setItems(aNames, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int item) {
						mainLayout.removeViewAt(item);
						return;
					}
				});
				builderRemove.create().show();
				return true;
			case R.id.gadd_player:
				int playersCount = mainLayout.getChildCount();
				AddPlayerRowFromData(new GatheringsPlayerData("Player " + String.valueOf(playersCount + 1), 20));
				return true;
			case R.id.gload_gathering:
				if (gIO.getNumberOfGatherings() <= 0){
					Toast.makeText(this, NO_GATHERINGS_EXIST, Toast.LENGTH_LONG).show();
					return true;
				}
				
				ArrayList<String> gatherings = gIO.getGatheringFileList();
				final String[] fGatherings = gatherings.toArray(new String[gatherings.size()]);
				final String[] properNames = new String[gatherings.size()];
				for (int idx = 0; idx < gatherings.size(); idx++) {
					properNames[idx] = gIO.ReadGatheringNameFromXML(gatherings.get(idx));
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
				builder.setTitle("Load a Gathering");
				builder.setItems(properNames, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int item) {
						RemoveAllPlayerRows();

						ArrayList<GatheringsPlayerData> players = gIO.ReadGatheringXML(fGatherings[item]);
						for (GatheringsPlayerData player : players) {
							AddPlayerRowFromData(player);
						}
						return;
					}
				});
				builder.create().show();
				return true;
			case R.id.gsave_gathering:
				
				showDialog(DIALOG_SET_NAME);
				
//				Intent i = new Intent(this, NPlayerLifeActivity.class);
//				finish();
//				startActivity(i);

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.gathering_menu, menu);
		return true;
	}
}