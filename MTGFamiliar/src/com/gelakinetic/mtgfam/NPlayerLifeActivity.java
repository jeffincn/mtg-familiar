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
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class NPlayerLifeActivity extends Activity {

	// private ListView lv;
	private ImageView												poisonButton;
	private ImageView												lifeButton;
	private ImageView												dieButton;
	private ImageView												poolButton;
	private ImageView												resetButton;
	private Activity												anchor;
	public static final int									DIALOG_RESET_CONFIRM	= 0;
	private static final int								LIFE									= 0;
	private static final int								POISON								= 1;

	private int															timerTick, timerValue, timerStart;
	private Object													timerLock;
	private final ScheduledExecutorService	scheduler							= Executors.newScheduledThreadPool(1);
	private Handler													handler;
	private int															activeType;
	private PlayerAdapter									rla;
	private int															orientation;

	public static final int									INITIAL_LIFE					= 20, INITIAL_POISON = 0, TERMINAL_LIFE = 0,
			TERMINAL_POISON = 10;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.n_player_life_activity);

		Player[] players = new Player[3];


		orientation = getResources().getConfiguration().orientation;
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			rla = new PlayerAdapter(this, R.layout.life_counter_player_col, players);

			rla.players[0] = new Player("Adam", INITIAL_LIFE, INITIAL_POISON, (Context) this);
			rla.players[1] = new Player("Mike", INITIAL_LIFE, INITIAL_POISON, (Context) this);
			rla.players[2] = new Player("April", INITIAL_LIFE, INITIAL_POISON, (Context) this);

			HorizontalListView lv = (HorizontalListView) findViewById(R.id.h_list);
			registerForContextMenu(lv);
			lv.setAdapter(rla);
		}
		else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			rla = new PlayerAdapter(this, R.layout.life_counter_player_row, players);

			rla.players[0] = new Player("Adam", INITIAL_LIFE, INITIAL_POISON, (Context) this);
			rla.players[1] = new Player("Mike", INITIAL_LIFE, INITIAL_POISON, (Context) this);
			rla.players[2] = new Player("April", INITIAL_LIFE, INITIAL_POISON, (Context) this);

			ListView lv = (ListView) findViewById(R.id.v_list);
			registerForContextMenu(lv);
			lv.setAdapter(rla);
		}

		// setListAdapter(rla);

		poisonButton = (ImageView) findViewById(R.id.poison_button);
		lifeButton = (ImageView) findViewById(R.id.life_button);
		dieButton = (ImageView) findViewById(R.id.die_button);
		poolButton = (ImageView) findViewById(R.id.pool_button);
		resetButton = (ImageView) findViewById(R.id.reset_button);

		anchor = this;

		setType(LIFE);

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

		scheduler.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				boolean doCommit = false;
				synchronized (timerLock) {
					if (timerValue > 0) {
						timerValue -= timerTick;
						if (timerValue <= 0) {
							/*
							 * This is used instead of having the commit loop here so I don't
							 * have to think about deadlock
							 */
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
		}, timerTick, timerTick, TimeUnit.MILLISECONDS);
	}

	private void reset() {
		setType(LIFE);

		for (Player p : rla.players) {
			p.setValue(LIFE, INITIAL_LIFE);
			p.setValue(POISON, INITIAL_POISON);
			p.lifeAdapter = new HistoryAdapter(this, INITIAL_LIFE);
			p.poisonAdapter = new HistoryAdapter(this, INITIAL_POISON);
			p.history.setAdapter(p.lifeAdapter);
		}
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
				for (Player p : rla.players) {
					if (p.history != null) {
						p.history.setAdapter(p.lifeAdapter);
					}
				}
				break;
			case POISON:
				lifeButton.setImageResource(R.drawable.life_button);
				poisonButton.setImageResource(R.drawable.poison_button_highlighted);
				for (Player p : rla.players) {
					if (p.history != null) {
						p.history.setAdapter(p.poisonAdapter);
					}
				}
				break;
		}
	}

	public class PlayerAdapter extends ArrayAdapter<Player> {

		public Player[]	players;
		private int			textViewResourceId;

		public PlayerAdapter(Context context, int textViewResourceId, Player[] ps) {
			super(context, textViewResourceId, ps);
			players = ps;
			this.textViewResourceId = textViewResourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(textViewResourceId, parent, false);

			players[position].addOutputViews((TextView) row.findViewById(R.id.player_name),
					(TextView) row.findViewById(R.id.player_readout), (ListView) row.findViewById(R.id.player_history));
			players[position].addButtons((Button) row.findViewById(R.id.player_minus1),
					(Button) row.findViewById(R.id.player_plus1), (Button) row.findViewById(R.id.player_minus5),
					(Button) row.findViewById(R.id.player_plus5));

			players[position].refreshTextViews();

			return row;
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
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								reset();
								ManaPoolActivity.reset(context);
								for (Player p : rla.players) {
									p.refreshTextViews();
								}
							}
						}).setNegativeButton("No", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

				dialog = builder.create();
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
				// Log.e("Life Counter",
				// "failed to inflate history adapter row view correctly");
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

		public static final int	CONSTRAINT_POISON	= 0, CONSTRAINT_LIFE = Integer.MAX_VALUE - 1;

		public Player(String n, int l, int p, Context context) {
			name = n;
			life = l;
			poison = p;
			this.lifeAdapter = new HistoryAdapter(context, life);
			this.poisonAdapter = new HistoryAdapter(context, poison);
			me = this;
		}

		public void addOutputViews(TextView n, TextView l, ListView lv) {
			TVname = n;
			TVlife = l;
			history = lv;
			history.setAdapter(this.lifeAdapter);
			refreshTextViews();
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
	}

}
