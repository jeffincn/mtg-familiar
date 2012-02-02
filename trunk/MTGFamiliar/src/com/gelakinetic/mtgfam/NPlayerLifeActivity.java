package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NPlayerLifeActivity extends Activity {

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

	private int															orientation;
	private SharedPreferences								preferences;
	private boolean													canGetLock;
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.n_player_life_activity);

		players = new ArrayList<Player>(2);

		orientation = getResources().getConfiguration().orientation;

		listSizeHeight = -10;
		listSizeWidth = -10;

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		canGetLock = preferences.getBoolean("wakelock", true);
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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		scheduler.shutdown();
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

		mainLayout = (LinearLayout) findViewById(R.id.playerList);

		MyApp appState = ((MyApp) getApplicationContext());
		appState.setState(0);

		if (canGetLock) {
			pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			wl.acquire();
		}

		String lifeData = preferences.getString(PLAYER_DATA, null);

		if (lifeData == null || lifeData.length() == 0) {
			addPlayer(null, INITIAL_LIFE, INITIAL_POISON, null, null, (Context) this);
			addPlayer(null, INITIAL_LIFE, INITIAL_POISON, null, null, (Context) this);
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
		}

		for (Player p : players) {
			p.setLayoutSize(listSizeWidth, listSizeHeight);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.life_counter_menu, menu);
		return true;
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
			default:
				return super.onOptionsItemSelected(item);
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
				builder.setMessage("Reset counters and pool?").setCancelable(true)
						.setPositiveButton("Players and Totals", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								ManaPoolActivity.reset(context);
								dialog.cancel();
								reset(EVERYTHING);
							}
						}).setNeutralButton("Just Totals", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								ManaPoolActivity.reset(context);
								dialog.cancel();
								reset(JUST_TOTALS);
							}
						}).setNegativeButton("No", new DialogInterface.OnClickListener() {
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
				builder.setTitle(getString(R.string.removeplayer));

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
				nameInput = (EditText) textEntryView.findViewById(R.id.editText1);
				dialog = new AlertDialog.Builder(this).setTitle("Enter Name").setView(textEntryView)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								playerToHaveNameChanged.setName(nameInput.getText().toString());
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

		mainLayout.addView(layout, new LinearLayout.LayoutParams(playerWidth, playerHeight));
	}

	private void removePlayer(int index) {
		mainLayout.removeView(players.get(index).layout);
		players.remove(index);
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
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		finish();

		startActivity(intent);
	}

	private class HistoryAdapter extends BaseAdapter {
		private int													count, initialValue, delta;
		private ArrayList<Vector<Integer>>	list;
		private Context											context;

		public static final int							ABSOLUTE	= 0, RELATIVE = 1;

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
		}

		public void commit() {
			int lastValue = initialValue;
			if (delta == 0) {
				return;
			}
			if (count > 0) {
				lastValue = list.get(0).get(ABSOLUTE).intValue();
			}
			Vector<Integer> v = new Vector<Integer>();
			v.add(new Integer(lastValue + delta));
			v.add(new Integer(delta));
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
			return v;
		}
	}

	private class Player {

		public String			name;
		public int				life;
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

			data += INITIAL_LIFE + ";";
			data += ";";

			data += INITIAL_POISON + ";";
			return data + ";\n";
		}

		public void addLayout(LinearLayout layout) {
			this.layout = layout;
		}
	}
}
