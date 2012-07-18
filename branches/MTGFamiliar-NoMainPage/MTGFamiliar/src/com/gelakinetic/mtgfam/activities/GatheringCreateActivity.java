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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.xmlpull.v1.XmlSerializer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.GatheringsIO.PlayerData;

public class GatheringCreateActivity extends FamiliarActivity {
	final private static String	FOLDERPATH			= "Gatherings";
	final private static String	DEFAULTFILE			= "default.info";
	final private String				name_gathering	= "Please entering a name for the Gathering.";

	private String							loadedGathering;

	private Context							mCtx;
	private GatheringsIO				gIO;
	private LinearLayout				mainLayout;

	private TextView						gatheringName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gathering_create_activity);

		gatheringName = (TextView) findViewById(R.id.gathering_name);

		mainLayout = (LinearLayout) findViewById(R.id.gathering_player_list);

		mCtx = this;
		gIO = new GatheringsIO(mCtx);

		String defaultG = gIO.getDefaultGathering();
		if (!defaultG.equals("")) {
			String gName = gIO.ReadGatheringNameFromXML(defaultG);
			loadedGathering = defaultG;
			gatheringName.setText(gName);
			ArrayList<PlayerData> players = gIO.ReadGatheringXML(defaultG);
			for (PlayerData player : players) {
				AddPlayerRowFromData(player);
			}
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Read values from the "savedInstanceState"-object and put them in your
		// textview

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {

		// Save the values you need from your textview into "outState"-object
		super.onSaveInstanceState(outState);
	}

	private String writeGatheringXML() {
		int players = mainLayout.getChildCount();
		int defaultPlayers = 1;

		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);

			serializer.startTag("", "gathering");
			serializer.startTag("", "name");
			serializer.text(gatheringName.getText().toString().trim());
			serializer.endTag("", "name");

			serializer.startTag("", "players");
			// serializer.attribute("", "number", String.valueOf(players));

			for (int idx = 0; idx < players; idx++) {
				View player = mainLayout.getChildAt(idx);

				RadioGroup nameSelection = (RadioGroup) player.findViewById(R.id.radioGroupName);
				String defaultname = ((nameSelection.getCheckedRadioButtonId() == R.id.defaultNameRadio) ? "true" : "false");

				EditText customName = (EditText) player.findViewById(R.id.custom_name);
				String name = customName.getText().toString().trim();
				if (name == null)
					name = "";

				if (defaultname.equals("true")) {
					name = "Player " + defaultPlayers;
					defaultPlayers++;
				}

				EditText startingLife = (EditText) player.findViewById(R.id.starting_life);
				String life = startingLife.getText().toString();
				if (life == null || life == "")
					life = "0";

				serializer.startTag("", "player");
				serializer.startTag("", "name");
				serializer.attribute("", "default", defaultname);
				serializer.text(name);
				serializer.endTag("", "name");

				serializer.startTag("", "startinglife");
				serializer.text(String.valueOf(life));
				serializer.endTag("", "startinglife");

				serializer.endTag("", "player");
			}
			serializer.endTag("", "players");
			serializer.endTag("", "gathering");
			serializer.endDocument();

			Editor editor = preferences.edit();
			editor.putString("player_data", null);
			editor.commit();

			return writer.toString();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void AddPlayerRowFromData(PlayerData _player) {
		LayoutInflater inf = getLayoutInflater();
		View v = inf.inflate(R.layout.gathering_create_player_row, null);
		TextView name = (TextView) v.findViewById(R.id.custom_name);
		final RadioGroup nameGroup = (RadioGroup) v.findViewById(R.id.radioGroupName);

		if (!_player.getIsDefaultName()) {
			name.setText(_player.getName());

			nameGroup.check(R.id.customNameRadio);
		}
		name.setOnFocusChangeListener(new OnFocusChangeListener() {

			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					nameGroup.check(R.id.customNameRadio);
				}
			}
		});
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
				if (loadedGathering == null)
					return true;

				String gatheringNameInFile = gIO.ReadGatheringNameFromXML(loadedGathering);
				if (gatheringNameInFile.equals(gatheringName.getText().toString())) {
					gIO.DeleteGathering(loadedGathering);
					RemoveAllPlayerRows();
					gatheringName.setText("");
				}
				return true;
			case R.id.gremove_player:
				mainLayout.removeViewAt(mainLayout.getChildCount() - 1);
				return true;
			case R.id.gadd_player:
				AddPlayerRowFromData(gIO.new PlayerData());
				return true;
			case R.id.gload_gathering:
				ArrayList<String> gatherings = gIO.getGatheringFileList();
				final String[] fGatherings = gatherings.toArray(new String[gatherings.size()]);
				final String[] properNames = new String[gatherings.size()];
				for (int idx = 0; idx < gatherings.size(); idx++) {
					properNames[idx] = gIO.ReadGatheringNameFromXML(gatherings.get(idx));
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
				builder.setTitle("Load a Gathering");
				builder.setItems(properNames, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int item) {
						RemoveAllPlayerRows();

						loadedGathering = fGatherings[item];
						gatheringName.setText(properNames[item]);

						ArrayList<PlayerData> players = gIO.ReadGatheringXML(fGatherings[item]);
						for (PlayerData player : players) {
							AddPlayerRowFromData(player);
						}
						return;
					}
				});
				builder.create().show();
				return true;
			case R.id.gsave_gathering:
				if (gatheringName.getText().toString().trim().equals("")) {
					Toast.makeText(getApplicationContext(), name_gathering, Toast.LENGTH_LONG).show();
				}

				try {
					Date date = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddkkmmss");

					String gathering = sdf.format(date);

					File path = new File(getFilesDir(), FOLDERPATH);
					if (!path.exists())
						if (path.mkdirs() == false)
							throw new FileNotFoundException("Folders not made");

					File file = new File(path, gathering + ".xml");

					String string = writeGatheringXML();

					BufferedWriter out = new BufferedWriter(new FileWriter(file));

					out.write(string);
					out.close();

					// Write out the new default
					File namedDefault = new File(path, DEFAULTFILE);
					out = new BufferedWriter(new FileWriter(namedDefault));
					out.write(file.getName());
					out.close();
				}
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}

				Intent i = new Intent(this, NPlayerLifeActivity.class);
				finish();
				startActivity(i);

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