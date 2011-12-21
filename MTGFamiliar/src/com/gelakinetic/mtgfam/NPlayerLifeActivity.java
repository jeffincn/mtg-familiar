//TODO EDH

package com.gelakinetic.mtgfam;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.devsmart.android.ui.HorizontalListView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class NPlayerLifeActivity extends Activity {

	private ImageView												poisonButton;
	private ImageView												lifeButton;
	private ImageView												dieButton;
	private ImageView												poolButton;
	private ImageView												resetButton;
	private Activity												anchor;
	private static final int								DIALOG_RESET_CONFIRM	= 0;
	private static final int								DIALOG_REMOVE_PLAYER	= 1;
	private static final int								DIALOG_SET_NAME				= 2;
	private static final int								LIFE									= 0;
	private static final int								POISON								= 1;

	private int															timerTick, timerValue, timerStart;
	private Object													timerLock;
	private final ScheduledExecutorService	scheduler							= Executors.newScheduledThreadPool(1);
	private Handler													handler;
	private static int											activeType						= -1;
	private PlayerAdapter										rla;
	private int															orientation;
	private ArrayList<Player>								players;
	private boolean													resetting							= false;
	private SharedPreferences								preferences;
	private boolean													canGetLock;
	private PowerManager										pm;
	private WakeLock												wl;
	private Editor													editor;
	private int															numPlayers						= 0;
	private String[]												names;
	private Runnable												runnable;

	public static final int									INITIAL_LIFE					= 20;
	public static final int									INITIAL_POISON				= 0;
	public static final int									TERMINAL_LIFE					= 0;
	public static final int									TERMINAL_POISON				= 10;
	private static final String							PLAYER_DATA						= "player_data";
	protected static final int							EVERYTHING						= 0;
	protected static final int							JUST_TOTALS						= 1;

	private Player													playerToHaveNameChanged;
	private EditText												nameInput;
	AdapterView<ListAdapter> lv;
	private int	listSizePx;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Android 1.5 (API3) isnt compatable with horizontal listview. just lock it
		// in portrait
		int APIlevel = Integer.parseInt(Build.VERSION.SDK);
		if (APIlevel < 4) {
			setRequestedOrientation(1);
		}

		setContentView(R.layout.n_player_life_activity);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		canGetLock = preferences.getBoolean("wakelock", true);
		editor = preferences.edit();

		poisonButton = (ImageView) findViewById(R.id.poison_button);
		lifeButton = (ImageView) findViewById(R.id.life_button);
		dieButton = (ImageView) findViewById(R.id.die_button);
		poolButton = (ImageView) findViewById(R.id.pool_button);
		resetButton = (ImageView) findViewById(R.id.reset_button);

		anchor = this;

		if (activeType == -1) {
			activeType = LIFE;
		}

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
							for (Player p : rla.players) {
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

			for (Player p : rla.players) {
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

		MyApp appState = ((MyApp) getApplicationContext());
		appState.setState(0);

		if (canGetLock) {
			pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			wl.acquire();
		}

		orientation = getResources().getConfiguration().orientation;

		String lifeData = preferences.getString(PLAYER_DATA, null);

		players = new ArrayList<Player>(2);

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			rla = new PlayerAdapter(this, R.layout.life_counter_player_col, players);
		}
		else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			rla = new PlayerAdapter(this, R.layout.life_counter_player_row, players);
		}
		
		if (lifeData == null || lifeData.length() == 0) {
			rla.players.add(new Player("Player 1", INITIAL_LIFE, INITIAL_POISON, (Context) this));
			rla.players.add(new Player("Player 2", INITIAL_LIFE, INITIAL_POISON, (Context) this));
			numPlayers = 2;
		}
		else {
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

				rla.players.add(new Player(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[3]), lhist, phist,
						(Context) this));
				numPlayers++;
			}
			String lastName = rla.players.get(rla.players.size() - 1).name;
			try {
				numPlayers = Integer.parseInt("" + lastName.charAt(lastName.length() - 1));
			}
			catch (NumberFormatException e) {

			}
		}

		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			lv = (HorizontalListView) findViewById(R.id.h_list);
			lv.setAdapter(rla);
		}
		else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			lv = (ListView) findViewById(R.id.v_list);
			lv.setAdapter(rla);
		}
		
		setType(activeType);
	}

	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			listSizePx = lv.getWidth();
		}
		else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			listSizePx = lv.getHeight();
		}
		if(rla.players.size() < 3){
			for(Player p : rla.players){
				p.setLayoutSize(listSizePx/2);
			}
		}
		lv.invalidate();
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
				numPlayers++;
				rla.players.add(new Player("Player " + numPlayers, INITIAL_LIFE, INITIAL_POISON, (Context) this));
				rla.notifyDataSetChanged();
				return true;
			case R.id.remove_player:
				showDialog(DIALOG_REMOVE_PLAYER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
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
			for (Player p : rla.players) {
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

	private void update() {
		switch (activeType) {
			case LIFE:
				for (Player p : rla.players) {
					if (p.TVlife != null) {
						p.TVlife.setTextColor(0xFFFFFFFF);
						p.TVlife.setText("" + p.life);
					}
				}
				break;
			case POISON:
				for (Player p : rla.players) {
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
				if (rla != null && rla.players != null) {
					for (Player p : rla.players) {
						p.setAdapter(type);
					}
				}
				break;
			case POISON:
				lifeButton.setImageResource(R.drawable.life_button);
				poisonButton.setImageResource(R.drawable.poison_button_highlighted);
				if (rla != null && rla.players != null) {
					for (Player p : rla.players) {
						p.setAdapter(type);
					}
				}
				break;
		}
	}

	public class PlayerAdapter extends ArrayAdapter<Player> {

		public ArrayList<Player>	players;
		private int								textViewResourceId;

		public PlayerAdapter(Context context, int textViewResourceId, ArrayList<Player> ps) {
			super(context, textViewResourceId, ps);
			players = ps;
			this.textViewResourceId = textViewResourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(textViewResourceId, parent, false);


			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				players.get(position).addOutputViews((TextView) row.findViewById(R.id.player_name),
						(TextView) row.findViewById(R.id.player_readout), (ListView) row.findViewById(R.id.player_history), (LinearLayout)row.findViewById(R.id.nplayer_col));
			}
			else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				players.get(position).addOutputViews((TextView) row.findViewById(R.id.player_name),
						(TextView) row.findViewById(R.id.player_readout), (ListView) row.findViewById(R.id.player_history), (LinearLayout)row.findViewById(R.id.nplayer_row));
			}
			players.get(position).addButtons((Button) row.findViewById(R.id.player_minus1),
					(Button) row.findViewById(R.id.player_plus1), (Button) row.findViewById(R.id.player_minus5),
					(Button) row.findViewById(R.id.player_plus5));

			players.get(position).refreshTextViews();

			return row;
		}
	}

	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog) {
		switch (id) {
			case DIALOG_SET_NAME:
				nameInput.setText("");
				break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final Context context = (Context) this;
		Dialog dialog;
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
				names = new String[rla.players.size()];
				for (int i = 0; i < rla.players.size(); i++) {
					names[i] = rla.players.get(i).name;
				}

				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.removeplayer));

				builder.setItems(names, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						rla.players.remove(item);
						rla.notifyDataSetChanged();
						removeDialog(DIALOG_REMOVE_PLAYER);
					}
				});

				dialog = builder.create();
				break;
			case DIALOG_SET_NAME:

				// This example shows how to add a custom layout to an AlertDialog
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

	public class Player {

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
		public HistoryAdapter	lifeAdapter, poisonAdapter;
		private int	orien;
		private LinearLayout layout;

		public static final int	CONSTRAINT_POISON	= 0, CONSTRAINT_LIFE = Integer.MAX_VALUE - 1;

		public Player(String n, int l, int p, Context context) {
			name = n;
			life = l;
			poison = p;
			this.lifeAdapter = new HistoryAdapter(context, life);
			this.poisonAdapter = new HistoryAdapter(context, poison);
			me = this;
			this.orien = orientation;
		}

		public Player(String n, int l, int p, int[] lhist, int[] phist, Context context) {
			name = n;
			life = l;
			poison = p;
			this.lifeAdapter = new HistoryAdapter(context, life);
			this.poisonAdapter = new HistoryAdapter(context, poison);
			me = this;
			this.orien = orientation;

			lifeAdapter.addHistory(lhist, INITIAL_LIFE);
			poisonAdapter.addHistory(phist, INITIAL_POISON);
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

		public void addOutputViews(TextView n, TextView l, ListView lv, LinearLayout ll) {
			TVname = n;
			TVlife = l;
			history = lv;
			layout = ll;
			
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

		public void setLayoutSize(int i) {
			
			// Gets the layout params that will allow you to resize the layout
			LayoutParams params = layout.getLayoutParams();
			
			if (orien == Configuration.ORIENTATION_LANDSCAPE) {
				// Changes the height and width to the specified *pixels*
				params.height = LayoutParams.FILL_PARENT;
				params.width = i;
			}
			else if (orien == Configuration.ORIENTATION_PORTRAIT) {
				// Changes the height and width to the specified *pixels*
				params.height = i;
				params.width = LayoutParams.FILL_PARENT;
			}
			
			layout.setLayoutParams(params);
			// listView holding each row is invalidated after this is called, rather than invalidating each row
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
	}
}