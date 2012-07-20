/**
Copyright 2011 Adam Feinstein and Michael Shick

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
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.GatheringsIO;
import com.gelakinetic.mtgfam.helpers.GatheringsPlayerData;

public class NPlayerLifeActivity extends FamiliarActivity implements OnInitListener {

	private static final int								DIALOG_RESET_CONFIRM	= 0;
	private static final int								DIALOG_REMOVE_PLAYER	= 1;
	private static final int								DIALOG_SET_NAME				= 2;
	private static final int								LIFE									= 0;
	private static final int								POISON								= 1;
	public static final int									INITIAL_LIFE					= 20;
	public static final int									INITIAL_POISON				= 0;
	public static final int									TERMINAL_LIFE					= 0;
	public static final int									TERMINAL_POISON				= 10;
	private static final String							PLAYER_DATA						= "player_data";
	protected static final int							EVERYTHING						= 0;
	protected static final int							JUST_TOTALS						= 1;

	private int															playerWidth;
	private int															playerHeight;
	private LinearLayout										mainLayout;
	private LinearLayout										doublePlayer;
	private int															playersInRow;

	private int															orientation;
	private boolean													canGetLock;
	private boolean													compactMode;
	private Editor													editor;
	private ImageView												lifeButton;
	private ImageView												poisonButton;
	private ImageView												dieButton;
	private ImageView												poolButton;
	private ImageView												resetButton;
	private NPlayerLifeActivity							anchor;
	private int															activeType;
	private int															timerStart;
	private int															timerTick;
	private int															timerValue;
	private Object													timerLock;
	private Handler													handler;
	private Runnable												runnable;
	private final ScheduledExecutorService	scheduler							= Executors.newScheduledThreadPool(1);

	private static final int								LANDSCAPE							= Configuration.ORIENTATION_LANDSCAPE;
	private static final int								PORTRAIT							= Configuration.ORIENTATION_PORTRAIT;

	private Player													playerToHaveNameChanged;

	private ArrayList<Player>								players;
	private EditText												nameInput;
	private PowerManager										pm;
	private WakeLock												wl;
	private boolean													resetting;
	private int															numPlayers						= 0;
	private int															listSizeHeight;
	private int															listSizeWidth;

	private TextToSpeech										tts;
	private boolean													ttsInitialized				= false;

	private GatheringsIO										gIO;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.n_player_life_activity);

		gIO = new GatheringsIO(getApplicationContext());

		players = new ArrayList<Player>(2);

		orientation = getResources().getConfiguration().orientation;

		listSizeHeight = -10;
		listSizeWidth = -10;

		canGetLock = preferences.getBoolean("wakelock", true);
		compactMode = preferences.getBoolean("compactMode", false);
		editor = preferences.edit();

		poisonButton = (ImageView) findViewById(R.id.poison_button);
		lifeButton = (ImageView) findViewById(R.id.life_button);
		dieButton = (ImageView) findViewById(R.id.die_button);
		poolButton = (ImageView) findViewById(R.id.pool_button);
		resetButton = (ImageView) findViewById(R.id.reset_button);

		anchor = this;

		poisonButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setType(POISON);
				update();
			}
		});
		lifeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setType(LIFE);
				update();
			}
		});
		dieButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent nextActivity = new Intent(anchor, DiceActivity.class);
				startActivity(nextActivity);
			}
		});
		poolButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent nextActivity = new Intent(anchor, ManaPoolActivity.class);
				startActivity(nextActivity);
			}
		});
		resetButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				showDialog(DIALOG_RESET_CONFIRM);
			}
		});

		timerStart = 1000;
		timerTick = 100;
		timerValue = 0;
		timerLock = new Object();
		handler = new Handler();

		runnable = new Runnable() {
			public void run() {
				boolean doCommit = false;
				synchronized (timerLock) {
					if (timerValue > 0) {
						timerValue -= timerTick;
						if (timerValue <= 0) {
							// This is used instead of having the commit loop here so I don't
							// have to think about deadlock
							doCommit = true;
						}
					}
				}

				if (doCommit) {
					handler.post(new Runnable() {
						public void run() {
							for (Player p : players) {
								synchronized (p.lifeAdapter) {
									p.lifeAdapter.commit();
								}
								synchronized (p.poisonAdapter) {
									p.poisonAdapter.commit();
								}
							}
						}
					});
				}
			}
		};

		scheduler.scheduleWithFixedDelay(runnable, timerTick, timerTick, TimeUnit.MILLISECONDS);

		setType(LIFE);

		if (preferences.getBoolean("hasTts", false)) {
			tts = new TextToSpeech(this, this);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		scheduler.shutdown();
		if (tts != null) {
			tts.shutdown();
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		if (canGetLock) {
			wl.release();
		}

		if (!resetting) {
			String playerData = "";

			for (Player p : players) {
				playerData += p.toString();
			}
			editor.putString(PLAYER_DATA, playerData);
			editor.commit();
		}
		resetting = false;
	}

	@Override
	public void onResume() {
		super.onResume();

		try {
			dismissDialog(DIALOG_RESET_CONFIRM);
		}
		catch (IllegalArgumentException e) {
		}
		try {
			dismissDialog(DIALOG_REMOVE_PLAYER);
		}
		catch (IllegalArgumentException e) {
		}
		try {
			dismissDialog(DIALOG_SET_NAME);
		}
		catch (IllegalArgumentException e) {
		}

		mainLayout = (LinearLayout) findViewById(R.id.playerList);
		playersInRow = 0;

		if (canGetLock) {
			pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			wl.acquire();
		}

		String lifeData = preferences.getString(PLAYER_DATA, null);

		if (lifeData == null || lifeData.length() == 0) {
			boolean addedPlayers = false;

			try {
				ArrayList<GatheringsPlayerData> dGatherings = gIO.getDefaultGathering();
				players = new ArrayList<Player>(dGatherings.size());
				for (GatheringsPlayerData player : dGatherings) {
					addPlayer(player.getName(), player.getStartingLife(), INITIAL_POISON, null, null, (Context) this);
					addedPlayers = true;
				}
			}
			catch (Exception e) {
				players.clear();
				players = new ArrayList<Player>(2);
				addedPlayers = false;
			}

			if (addedPlayers == false) {
				addPlayer(null, INITIAL_LIFE, INITIAL_POISON, null, null, (Context) this);
				addPlayer(null, INITIAL_LIFE, INITIAL_POISON, null, null, (Context) this);
			}
		}
		else if (players.size() == 0) {
			numPlayers = 0;
			String[] playerLines = lifeData.split("\n");
			for (String line : playerLines) {
				String[] data = line.split(";");
				String[] lifehist = data[2].split(",");
				int[] lhist = new int[lifehist.length];
				try {
					for (int i = 0; i < lifehist.length; i++) {
						lhist[i] = Integer.parseInt(lifehist[i]);
					}
				}
				catch (NumberFormatException e) {
					lhist = null;
				}

				int[] phist;
				try {
					String[] poisonhist = data[4].split(",");
					phist = new int[poisonhist.length];
					for (int i = 0; i < poisonhist.length; i++) {
						phist[i] = Integer.parseInt(poisonhist[i]);
					}
				}
				catch (NumberFormatException e) {
					phist = null;
				}
				catch (ArrayIndexOutOfBoundsException e) {
					phist = null;
				}

				addPlayer(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[3]), lhist, phist, (Context) this);
				numPlayers++;
			}
			String lastName = players.get(players.size() - 1).name;
			try {
				numPlayers = Integer.parseInt("" + lastName.charAt(lastName.length() - 1));
			}
			catch (NumberFormatException e) {
			}
			catch (StringIndexOutOfBoundsException e) {
			}
		}

		setType(activeType);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {

		if (!hasFocus) {
			return;
		}

		if (listSizeWidth != -10) {
			return;
		}

		FrameLayout fl = (FrameLayout) findViewById(R.id.playerScrollView);
		listSizeWidth = fl.getWidth();
		listSizeHeight = fl.getHeight();

		if (orientation == LANDSCAPE) {
			listSizeHeight = LayoutParams.FILL_PARENT;
			switch (players.size()) {
				case 1:
					break;
				case 2:
					listSizeWidth /= 2;
					break;
				default:
					listSizeWidth /= 2;
					break;
			}
		}
		else if (orientation == PORTRAIT) {
			listSizeWidth = LayoutParams.FILL_PARENT;
			switch (players.size()) {
				case 1:
					break;
				case 2:
					listSizeHeight /= 2;
					break;
				default:
					listSizeHeight /= 2;
					break;
			}
			if (listSizeHeight < 256) {
				listSizeHeight = 192;
			}

		}

		for (Player p : players) {
			p.setLayoutSize(listSizeWidth, listSizeHeight);
		}
	}

	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog) {
		switch (id) {
			case DIALOG_SET_NAME:
				if (playerToHaveNameChanged != null) {
					nameInput.setText(playerToHaveNameChanged.name);
					nameInput.selectAll();
				}
				break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final Context context = (Context) this;
		Dialog dialog;
		String[] names;
		switch (id) {
			case DIALOG_RESET_CONFIRM:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder
						.setMessage(getString(R.string.life_counter_clear_dialog_text))
						.setCancelable(true)
						.setPositiveButton(getString(R.string.dialog_both),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										ManaPoolActivity.reset(context);
										reset(EVERYTHING);
									}
								})
						.setNeutralButton(getString(R.string.dialog_life), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								ManaPoolActivity.reset(context);
								reset(JUST_TOTALS);
							}
						}).setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

				dialog = builder.create();
				break;
			case DIALOG_REMOVE_PLAYER:
				names = new String[players.size()];
				for (int i = 0; i < players.size(); i++) {
					names[i] = players.get(i).name;
				}

				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.life_counter_remove_player));

				builder.setItems(names, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						removePlayer(item);
						listSizeWidth = -10;
						onWindowFocusChanged(true);
						removeDialog(DIALOG_REMOVE_PLAYER);
					}
				});

				dialog = builder.create();
				break;
			case DIALOG_SET_NAME:
				LayoutInflater factory = LayoutInflater.from(this);
				final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
				nameInput = (EditText) textEntryView.findViewById(R.id.player_name);
				dialog = new AlertDialog.Builder(this).setTitle("Enter Name").setView(textEntryView)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								if (playerToHaveNameChanged == null) {
									return;
								}
								if (nameInput == null) {
									return;
								}
								String newName = nameInput.getText().toString();
								if (newName.equals("")) {
									return;
								}
								playerToHaveNameChanged.setName(newName);
							}
						}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
							}
						}).create();

				break;
			default:
				dialog = null;
		}
		return dialog;
	}

	private void update() {
		switch (activeType) {
			case LIFE:
				for (Player p : players) {
					if (p.TVlife != null) {
						p.TVlife.setTextColor(0xFFFFFFFF);
						p.TVlife.setText("" + p.life);
					}
				}
				break;
			case POISON:
				for (Player p : players) {
					if (p.TVlife != null) {
						p.TVlife.setTextColor(0xFF009000);
						p.TVlife.setText("" + p.poison);
					}
				}
				break;
		}
	}

	private void setType(int type) {
		activeType = type;

		switch (activeType) {
			case LIFE:
				lifeButton.setImageResource(R.drawable.life_button_highlighted);
				poisonButton.setImageResource(R.drawable.poison_button);
				for (Player p : players) {
					p.setAdapter(type);
				}
				break;
			case POISON:
				lifeButton.setImageResource(R.drawable.life_button);
				poisonButton.setImageResource(R.drawable.poison_button_highlighted);
				for (Player p : players) {
					p.setAdapter(type);
				}
				break;
		}
	}

	private void addPlayer(String name, int initialLife, int initialPoison, int[] lhist, int[] phist, Context context) {
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout;
		if (orientation == LANDSCAPE) {
			layout = (LinearLayout) inflater.inflate(R.layout.life_counter_player_col, null);
		}
		else {
			layout = (LinearLayout) inflater.inflate(R.layout.life_counter_player_row, null);
		}

		numPlayers++;
		if (name == null) {
			name = "Player " + numPlayers;
		}
		Player p = new Player(name, initialLife, initialPoison, lhist, phist, context);
		p.addButtons((Button) layout.findViewById(R.id.player_minus1), (Button) layout.findViewById(R.id.player_plus1),
				(Button) layout.findViewById(R.id.player_minus5), (Button) layout.findViewById(R.id.player_plus5));
		p.addOutputViews((TextView) layout.findViewById(R.id.player_name),
				(TextView) layout.findViewById(R.id.player_readout), (ListView) layout.findViewById(R.id.player_history));
		p.addLayout(layout);

		players.add(p);

		if (compactMode && orientation != LANDSCAPE) {
			p.hideHistory();

			if (playersInRow == 0) {
				doublePlayer = (LinearLayout) inflater.inflate(R.layout.life_counter_player_double_row, null);
				LinearLayout playerPlacement = (LinearLayout) doublePlayer.findViewById(R.id.playerLeft);
				playerPlacement.addView(layout);

				mainLayout.addView(doublePlayer);
				playersInRow++;
			}
			else if (playersInRow == 1) {
				LinearLayout playerPlacement = (LinearLayout) doublePlayer.findViewById(R.id.playerRight);
				playerPlacement.addView(layout);

				doublePlayer = null;
				playersInRow = 0;
			}
		}
		else {
			mainLayout.addView(layout, new LinearLayout.LayoutParams(playerWidth, playerHeight));
		}
	}

	private void removePlayer(int index) {
		mainLayout.removeView(players.get(index).layout);
		players.remove(index);

		// Rather then go through all the logic of finding the removed player, and
		// adjusting the locations
		// of all the player views, just restart the activity when in compact mode,
		// this will adjust the
		// layouts to not have a blank area when players are removed.
		if (compactMode) {
			Intent intent = getIntent();
			finish();
			startActivity(intent);
		}
	}

	private void reset(int type) {

		resetting = true;
		setType(LIFE);

		if (type == EVERYTHING) {
			editor.putString(PLAYER_DATA, null);
		}
		else if (type == JUST_TOTALS) {
			String data = "";
			for (Player p : players) {
				data += p.toFreshString();
			}
			editor.putString(PLAYER_DATA, data);
		}
		editor.commit();

		Intent intent = getIntent();
		// intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		finish();

		startActivity(intent);
	}

	private class HistoryAdapter extends BaseAdapter {
		private int													count, initialValue, delta;
		private ArrayList<Vector<Integer>>	list;
		private Context											context;

		public static final int							ABSOLUTE				= 0, RELATIVE = 1;
		public static final int							CHANGING				= 2;

		public static final int							NOTAREALUPDATE	= 1;

		public HistoryAdapter(Context context, int initialValue) {
			this.context = context;
			list = new ArrayList<Vector<Integer>>();
			count = 0;
			delta = 0;
			this.initialValue = initialValue;
		}

		public void addHistory(int[] hist, int initial_val) {
			try {
				for (int i = 0; i < hist.length; i++) {
					Vector<Integer> vi = new Vector<Integer>();
					vi.add(hist[i]);
					try {
						vi.add(hist[i] - hist[i + 1]);
					}
					catch (Exception e) {
						vi.add(hist[i] - initial_val);
					}
					list.add(vi);
					count++;
					delta = 0;
				}
			}
			catch (NullPointerException e) {
			}
			notifyDataSetChanged();
		}

		public void update(int magnitude) {
			delta += magnitude;

			Vector<Integer> vi = new Vector<Integer>();
			Boolean addNewVi = true;
			for (Vector<Integer> testvi : list) {
				try {
					if (testvi.get(CHANGING).intValue() == NOTAREALUPDATE) {
						vi = testvi;
						addNewVi = false;
						vi.clear();
						break; // Short circuit, we have the changing row already no need to
										// search the rest of the list.
					}
				}
				catch (ArrayIndexOutOfBoundsException e) {
					continue;
				}
			}

			int lastLifeTotal = initialValue;
			if (list.size() != 0) {
				// I know this is a weird code block, but its basically finding the
				// first non-changing history row.
				// It does this by searching for the changing value, when the changing
				// value doesn't exist, use that value.
				for (Vector<Integer> testvi : list) {
					try {
						if (testvi.get(CHANGING).intValue() == NOTAREALUPDATE) {
							continue;
						}
					}
					catch (ArrayIndexOutOfBoundsException e) {
						try {
							lastLifeTotal = testvi.get(ABSOLUTE).intValue();
						}
						catch (ArrayIndexOutOfBoundsException innerE) {
							continue;
						}
						break; // Short circuit, we have the life total from the last
										// committed update.
					}
				}
			}

			vi.add(lastLifeTotal + delta);
			vi.add(delta);
			vi.add(NOTAREALUPDATE);

			if (addNewVi == true) {
				count++;
				list.add(0, vi);
			}
			// if the change is 0, remove the row from the list.
			if (vi.get(RELATIVE).intValue() == 0) {
				if (list.get(0).get(RELATIVE).intValue() == 0) {
					list.remove(0);
					count--;
				}
			}

			notifyDataSetChanged();
		}

		public void commit() {
			Iterator<Vector<Integer>> iterate = list.iterator();
			while (iterate.hasNext()) {
				try {
					if (iterate.next().get(CHANGING).intValue() == NOTAREALUPDATE) {
						iterate.remove();
						count--;
					}
				}
				catch (ArrayIndexOutOfBoundsException e) {
					continue;
				}
			}

			int lastValue = initialValue;
			if (delta == 0) {
				return;
			}
			if (count > 0) {
				lastValue = list.get(0).get(ABSOLUTE).intValue();
			}
			Vector<Integer> v = new Vector<Integer>();
			v.add(Integer.valueOf(lastValue + delta));
			v.add(Integer.valueOf(delta));
			list.add(0, v);
			count++;
			delta = 0;
			notifyDataSetChanged();
		}

		public int getCount() {
			return count;
		}

		public Object getItem(int position) {
			if (position < 0 || position >= count) {
				return null;
			}
			return list.get(position);
		}

		public long getItemId(int position) {
			if (position < 0 || position >= count) {
				return -1l;
			}
			return (long) position;
		}

		public int clearToPosition(int position) {
			int returnValue = list.get(position).get(ABSOLUTE).intValue();
			for (int idx = position - 1; idx >= 0; idx--) {
				list.remove(idx);
				count--;
			}
			notifyDataSetChanged();
			return returnValue;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView relative, absolute;
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = vi.inflate(R.layout.history_adapter_row, null);
			Vector<Integer> row = list.get(position);
			absolute = (TextView) v.findViewById(R.id.absolute);
			relative = (TextView) v.findViewById(R.id.relative);
			if (relative == null || absolute == null) {
				TextView error = new TextView(context);
				error.setText("ERROR!");
				return error;
			}
			absolute.setText("" + row.get(ABSOLUTE).intValue());
			String relativeString = "";
			int relativeValue = row.get(RELATIVE).intValue();
			if (relativeValue > 0) {
				relativeString += "+";
			}
			relativeString += relativeValue;
			relative.setText(relativeString);

			int color;
			switch (activeType) {
				case POISON:
					// Positive poison is bad, so display red; otherwise show green
					color = (relativeValue > 0) ? context.getResources().getInteger(R.color.red) : context.getResources()
							.getInteger(R.color.green);
					break;
				case LIFE:
				default:
					// Negative life is bad, so display red; otherwise show green
					color = (relativeValue < 0) ? context.getResources().getInteger(R.color.red) : context.getResources()
							.getInteger(R.color.green);
					break;
			}

			try {
				if (row.get(CHANGING).intValue() == NOTAREALUPDATE) {
					relative.setTextColor(color);
					absolute.setTextColor(color);
				}
			}
			catch (Exception e) {
				// No changes needed.
			}

			return v;
		}
	}

	private class Player {

		public String			name;
		public int				life;
		public int				defaultLife;
		public int				poison;
		public Player			me;

		public TextView		TVname;
		public TextView		TVlife;

		public Button			minusButton1;
		public Button			plusButton1;
		private Button		minusButton5;
		private Button		plusButton5;
		private ListView	history;
		private HistoryAdapter	lifeAdapter, poisonAdapter;
		private LinearLayout		layout;

		public static final int	CONSTRAINT_POISON	= 0, CONSTRAINT_LIFE = Integer.MAX_VALUE - 1;

		public Player(String n, int l, int p, int[] lhist, int[] phist, Context context) {
			name = n;
			life = l;
			defaultLife = l;
			poison = p;
			this.lifeAdapter = new HistoryAdapter(context, life);
			this.poisonAdapter = new HistoryAdapter(context, poison);
			me = this;

			if (lhist != null) {
				lifeAdapter.addHistory(lhist, INITIAL_LIFE);
			}
			else {
				this.lifeAdapter = new HistoryAdapter(context, life);
			}
			if (phist != null) {
				poisonAdapter.addHistory(phist, INITIAL_POISON);
			}
			else {
				this.poisonAdapter = new HistoryAdapter(context, poison);
			}
		}

		public void setLayoutSize(int listSizeWidth, int listSizeHeight) {
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(listSizeWidth, listSizeHeight);
			layout.setLayoutParams(layoutParams);
		}

		public void setName(String text) {
			name = text;
			TVname.setText(text);
		}

		public void setAdapter(int TYPE) {
			if (history == null) {
				return;
			}
			switch (TYPE) {
				case LIFE:
					history.setAdapter(this.lifeAdapter);
					break;
				case POISON:
					history.setAdapter(this.poisonAdapter);
					break;
			}
			history.invalidate();
		}

		public void hideHistory() {
			((View) history.getParent()).setVisibility(View.GONE);
		}

		public void addOutputViews(TextView n, TextView l, ListView lv) {
			TVname = n;
			TVlife = l;
			history = lv;

			switch (activeType) {
				case LIFE:
					history.setAdapter(this.lifeAdapter);
					break;
				case POISON:
					history.setAdapter(this.poisonAdapter);
					break;
			}
			refreshTextViews();

			history.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

				public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

					int totalAtPosition;
					switch (activeType) {
						case LIFE:
							totalAtPosition = lifeAdapter.clearToPosition(arg2);
							life = totalAtPosition;
							break;
						case POISON:
							totalAtPosition = poisonAdapter.clearToPosition(arg2);
							poison = totalAtPosition;
							break;
					}
					refreshTextViews();
					return false;
				}
			});

			TVname.setOnClickListener(new View.OnClickListener() {

				public void onClick(View v) {
					playerToHaveNameChanged = me;
					showDialog(DIALOG_SET_NAME);
				}

			});
		}

		public void refreshTextViews() {
			TVname.setText(name);
			if (activeType == LIFE) {
				TVlife.setText("" + life);
				TVlife.setTextColor(0xFFFFFFFF);
			}
			else if (activeType == POISON) {
				TVlife.setText("" + poison);
				TVlife.setTextColor(0xFF009000);
			}
		}

		private void setValue(int type, int value) {
			switch (type) {
				case LIFE:
					if (value > CONSTRAINT_LIFE) {
						value = CONSTRAINT_LIFE;
					}
					life = value;
					break;
				case POISON:
					if (value < CONSTRAINT_POISON) {
						value = CONSTRAINT_POISON;
					}
					poison = value;
			}
		}

		private void incrementValue(int type, int delta) {
			int value = 0;
			switch (type) {
				case LIFE:
					value = life;
					if (value + delta > CONSTRAINT_LIFE) {
						delta = CONSTRAINT_LIFE - value;
					}
					lifeAdapter.update(delta);
					break;
				case POISON:
					value = poison;
					if (value + delta < CONSTRAINT_POISON) {
						delta = CONSTRAINT_POISON - value;
					}
					poisonAdapter.update(delta);
					break;
			}
			setValue(type, value + delta);
		}

		public void addButtons(Button minus1, Button plus1, Button minus5, Button plus5) {
			minusButton1 = minus1;
			plusButton1 = plus1;
			minusButton5 = minus5;
			plusButton5 = plus5;

			minusButton1.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					synchronized (timerLock) {
						timerValue = timerStart;
					}
					me.incrementValue(activeType, -1);
					refreshTextViews();
				}
			});

			plusButton1.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					synchronized (timerLock) {
						timerValue = timerStart;
					}

					me.incrementValue(activeType, 1);
					refreshTextViews();
				}
			});

			minusButton5.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					synchronized (timerLock) {
						timerValue = timerStart;
					}
					me.incrementValue(activeType, -5);
					refreshTextViews();
				}
			});

			plusButton5.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					synchronized (timerLock) {
						timerValue = timerStart;
					}
					me.incrementValue(activeType, 5);
					refreshTextViews();
				}
			});
		}

		// returns all persistent data associated with a player
		public String toString() {
			String data = this.name + ";";

			boolean first = true;
			data += life + ";";
			for (Vector<Integer> i : lifeAdapter.list) {
				if (first) {
					first = false;
					data += i.get(0);
				}
				else {
					data += "," + i.get(0);
				}
			}

			data += ";";

			first = true;
			data += poison + ";";
			for (Vector<Integer> i : poisonAdapter.list) {
				if (first) {
					first = false;
					data += i.get(0);
				}
				else {
					data += "," + i.get(0);
				}
			}

			return data + ";\n";
		}

		public String toFreshString() {
			String data = this.name + ";";

			data += defaultLife + ";";
			data += ";";

			data += INITIAL_POISON + ";";
			return data + ";\n";
		}

		public void addLayout(LinearLayout layout) {
			this.layout = layout;
		}
	}

	private void announceLifeTotals() {
		if (ttsInitialized) {
			boolean first = true;
			for (Player p : players) {
				// Format: "{name} has {quantity} {life | poison counter(s)}", depending
				// on the current mode
				String sentence = "";
				sentence += p.name;
				sentence += " has ";

				if (activeType == LIFE) {
					sentence += String.valueOf(p.life);
					sentence += " life";
				}
				else {
					sentence += String.valueOf(p.poison);
					sentence += " poison counter";
					if (p.poison != 1) {
						sentence += "s";
					}
				}

				if (first) {
					// Flush on the first one, so we interrupt if it's already going
					tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null);
					first = false;
				}
				else {
					// Otherwise, add to the queue
					tts.speak(sentence, TextToSpeech.QUEUE_ADD, null);
				}
			}
		}
		else {
			Toast.makeText(this, "You do not have text-to-speech installed", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.add_player:
				addPlayer(null, INITIAL_LIFE, INITIAL_POISON, null, null, anchor);
				listSizeWidth = -10;
				onWindowFocusChanged(true);
				return true;
			case R.id.remove_player:
				showDialog(DIALOG_REMOVE_PLAYER);
				return true;
			case R.id.announce_life:
				announceLifeTotals();
				return true;
			case R.id.change_gathering:
				Intent i = new Intent(this, GatheringCreateActivity.class);
				finish();
				startActivity(i);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.announce_life).setVisible(ttsInitialized);
		return true;
	}

	public void onInit(int status) {
		ttsInitialized = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.life_counter_menu, menu);
		return true;
	}
}
