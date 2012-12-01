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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class DeckManagementActivity extends ListActivity {
	private SharedPreferences	preferences;
	private String						path;
	private File							SDcard;
	private File							deck_folder;
	private File[]						decks;
	private ListView					list;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.deck_management_activity);

		list = getListView();

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		SDcard = Environment.getExternalStorageDirectory();
		path = preferences.getString(getString(R.string.default_deck_path_key), getString(R.string.defaultdeckpath));

		deck_folder = new File(SDcard, path);
		if (!deck_folder.exists()) {
			deck_folder.mkdir();
		}

		decks = deck_folder.listFiles();

		String[] from = new String[] { "name" };
		int[] to = new int[] { R.id.deckname };

		List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();

		for (int i = 0; i < decks.length; i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("name", decks[i].getName());
			fillMaps.add(map);
		}

		SimpleAdapter adapter = new SimpleAdapter(this, fillMaps, R.layout.deck_row, from, to);
		list.setAdapter(adapter);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (decks[(int) id].isFile()) {
			Intent i = new Intent(DeckManagementActivity.this, DeckViewActivity.class);
			i.putExtra("file_name", decks[(int) id].getName());
			startActivity(i);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
