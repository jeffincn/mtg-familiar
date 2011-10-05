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

public class DeckManagement extends ListActivity {
	private SharedPreferences	preferences;
	private String						path;
	private File							SDcard;
	private File							deck_folder;
	private File[]						decks;
	private ListView					list;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.deckmanagement);

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
			Intent i = new Intent(DeckManagement.this, DeckView.class);
			i.putExtra("file_name", decks[(int) id].getName());
			startActivity(i);
		}
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }
}
