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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class DeckViewActivity extends ListActivity {
	private ListView					list;
	private String						fname;
	private SharedPreferences	preferences;
	private File							SDcard;
	private String						path;
	private Bundle						extras;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.deck_management_activity);
		list = getListView();

		extras = getIntent().getExtras(); // this appends the directory name to the
		// path
		if (extras != null) {
			fname = extras.getString("file_name");
		}

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		SDcard = Environment.getExternalStorageDirectory();
		path = preferences.getString(getString(R.string.default_deck_path_key), "");

		File deck = new File(new File(SDcard, path), fname);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(deck));
		}
		catch (FileNotFoundException e1) {
		}

		String[] from = new String[] { "name" };
		int[] to = new int[] { R.id.deckname };

		List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();

		String line;
		try {
			while ((line = br.readLine()) != null) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("name", line);
				fillMaps.add(map);
			}
		}
		catch (IOException e) {
		}

		SimpleAdapter adapter = new SimpleAdapter(this, fillMaps, R.layout.deck_row, from, to);
		list.setAdapter(adapter);

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
