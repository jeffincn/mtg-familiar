package com.gelakinetic.mtgfam.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;

public class ManaPoolFragment extends FamiliarFragment {
	private Button	whiteMinus, blueMinus, blackMinus, redMinus, greenMinus, colorlessMinus, spellMinus;
	private Button	whitePlus, bluePlus, blackPlus, redPlus, greenPlus, colorlessPlus, spellPlus;
	private TextView	whiteReadout, blueReadout, blackReadout, redReadout, greenReadout, colorlessReadout, spellReadout;

	private int				white, blue, black, red, green, colorless, spell;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View myFragmentView = inflater.inflate(R.layout.mana_pool_activity, container, false);

		white = 0;
		blue = 0;
		black = 0;
		red = 0;
		green = 0;

		whiteMinus = (Button) myFragmentView.findViewById(R.id.white_minus);
		blueMinus = (Button) myFragmentView.findViewById(R.id.blue_minus);
		blackMinus = (Button) myFragmentView.findViewById(R.id.black_minus);
		redMinus = (Button) myFragmentView.findViewById(R.id.red_minus);
		greenMinus = (Button) myFragmentView.findViewById(R.id.green_minus);
		colorlessMinus = (Button) myFragmentView.findViewById(R.id.colorless_minus);
		spellMinus = (Button) myFragmentView.findViewById(R.id.spell_minus);

		whitePlus = (Button) myFragmentView.findViewById(R.id.white_plus);
		bluePlus = (Button) myFragmentView.findViewById(R.id.blue_plus);
		blackPlus = (Button) myFragmentView.findViewById(R.id.black_plus);
		redPlus = (Button) myFragmentView.findViewById(R.id.red_plus);
		greenPlus = (Button) myFragmentView.findViewById(R.id.green_plus);
		colorlessPlus = (Button) myFragmentView.findViewById(R.id.colorless_plus);
		spellPlus = (Button) myFragmentView.findViewById(R.id.spell_plus);

		whiteReadout = (TextView) myFragmentView.findViewById(R.id.white_readout);
		blueReadout = (TextView) myFragmentView.findViewById(R.id.blue_readout);
		blackReadout = (TextView) myFragmentView.findViewById(R.id.black_readout);
		redReadout = (TextView) myFragmentView.findViewById(R.id.red_readout);
		greenReadout = (TextView) myFragmentView.findViewById(R.id.green_readout);
		colorlessReadout = (TextView) myFragmentView.findViewById(R.id.colorless_readout);
		spellReadout = (TextView) myFragmentView.findViewById(R.id.spell_readout);

		boolean loadSuccessful = true;

		Button buttons[] = { whiteMinus, blueMinus, blackMinus, redMinus, greenMinus, colorlessMinus, spellMinus,
				whitePlus, bluePlus, blackPlus, redPlus, greenPlus, colorlessPlus, spellPlus };
		TextView readouts[] = { whiteReadout, blueReadout, blackReadout, redReadout, greenReadout, colorlessReadout,
				spellReadout };

		for (Button e : buttons) {
			if (e == null) {
				loadSuccessful = false;
			}
		}
		for (TextView e : readouts) {
			if (e == null) {
				loadSuccessful = false;
			}
		}
		if (!loadSuccessful) {
			Toast.makeText(this.getActivity(), R.string.mana_pool_error_toast, Toast.LENGTH_LONG).show();
			// this.finish();
			this.getMainActivity().mFragmentManager.popBackStack();
		}

		whiteMinus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				white--;
				if (white < 0) {
					white = 0;
				}
				update();
			}
		});
		blueMinus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				blue--;
				if (blue < 0) {
					blue = 0;
				}
				update();
			}
		});
		blackMinus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				black--;
				if (black < 0) {
					black = 0;
				}
				update();
			}
		});
		redMinus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				red--;
				if (red < 0) {
					red = 0;
				}
				update();
			}
		});
		greenMinus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				green--;
				if (green < 0) {
					green = 0;
				}
				update();
			}
		});
		colorlessMinus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				colorless--;
				if (colorless < 0) {
					colorless = 0;
				}
				update();
			}
		});
		spellMinus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				spell--;
				if (spell < 0) {
					spell = 0;
				}
				update();
			}
		});
		whiteMinus.setOnLongClickListener(new Button.OnLongClickListener() {
			public boolean onLongClick(View view) {
				white = 0;
				update();
				return true;
			}
		});
		blueMinus.setOnLongClickListener(new Button.OnLongClickListener() {
			public boolean onLongClick(View view) {
				blue = 0;
				update();
				return true;
			}
		});
		blackMinus.setOnLongClickListener(new Button.OnLongClickListener() {
			public boolean onLongClick(View view) {
				black = 0;
				update();
				return true;
			}
		});
		redMinus.setOnLongClickListener(new Button.OnLongClickListener() {
			public boolean onLongClick(View view) {
				red = 0;
				update();
				return true;
			}
		});
		greenMinus.setOnLongClickListener(new Button.OnLongClickListener() {
			public boolean onLongClick(View view) {
				green = 0;
				update();
				return true;
			}
		});
		colorlessMinus.setOnLongClickListener(new Button.OnLongClickListener() {
			public boolean onLongClick(View view) {
				colorless = 0;
				update();
				return true;
			}
		});
		spellMinus.setOnLongClickListener(new Button.OnLongClickListener() {
			public boolean onLongClick(View view) {
				spell = 0;
				update();
				return true;
			}
		});

		whitePlus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				white++;
				update();
			}
		});
		bluePlus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				blue++;
				update();
			}
		});
		blackPlus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				black++;
				update();
			}
		});
		redPlus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				red++;
				update();
			}
		});
		greenPlus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				green++;
				update();
			}
		});
		colorlessPlus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				colorless++;
				update();
			}
		});
		spellPlus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				spell++;
				update();
			}
		});

		return myFragmentView;
	}

	@Override
	public void onPause() {
		super.onPause();
		store();
	}

	@Override
	public void onResume() {
		super.onResume();
		load();
		update();
	}

	@Override
	public void onStop() {
		super.onStop();
		store();
	}

	private void load() {
		white = getMainActivity().getPreferencesAdapter().getWhiteMana();
		blue = getMainActivity().getPreferencesAdapter().getBlueMana();
		black = getMainActivity().getPreferencesAdapter().getBlackMana();
		red = getMainActivity().getPreferencesAdapter().getRedMana();
		green = getMainActivity().getPreferencesAdapter().getGreenMana();
		colorless = getMainActivity().getPreferencesAdapter().getColorlessMana();
		spell = getMainActivity().getPreferencesAdapter().getSpellCount();
	}

	private void store() {
		getMainActivity().getPreferencesAdapter().setWhiteMana(white);
		getMainActivity().getPreferencesAdapter().setBlueMana(blue);
		getMainActivity().getPreferencesAdapter().setBlackMana(black);
		getMainActivity().getPreferencesAdapter().setRedMana(red);
		getMainActivity().getPreferencesAdapter().setGreenMana(green);
		getMainActivity().getPreferencesAdapter().setColorlessMana(colorless);
		getMainActivity().getPreferencesAdapter().setSpellCount(spell);
	}

	private void update() {
		TextView readouts[] = { whiteReadout, blueReadout, blackReadout, redReadout, greenReadout, colorlessReadout,
				spellReadout };
		int values[] = { white, blue, black, red, green, colorless, spell };

		for (int ii = 0; ii < readouts.length; ii++) {
			readouts[ii].setText("" + values[ii]);
		}
	}

	public static void reset(MainActivity ma) {
		ma.getPreferencesAdapter().setWhiteMana(0);
		ma.getPreferencesAdapter().setBlueMana(0);
		ma.getPreferencesAdapter().setBlackMana(0);
		ma.getPreferencesAdapter().setRedMana(0);
		ma.getPreferencesAdapter().setGreenMana(0);
		ma.getPreferencesAdapter().setColorlessMana(0);
		ma.getPreferencesAdapter().setSpellCount(0);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.clear_all:
				reset(this.getMainActivity());
				load();
				update();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.mana_pool_menu, menu);
	}
}
