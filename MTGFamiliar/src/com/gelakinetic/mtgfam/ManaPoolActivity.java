/**
Copyright 2011 Michael Shick

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

import android.content.Context;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ManaPoolActivity extends FragmentActivity {
	private Button	whiteMinus, blueMinus, blackMinus, redMinus, greenMinus, colorlessMinus, spellMinus;
	private Button	whitePlus, bluePlus, blackPlus, redPlus, greenPlus, colorlessPlus, spellPlus;
	private TextView	whiteReadout, blueReadout, blackReadout, redReadout, greenReadout, colorlessReadout, spellReadout;

	private int				white, blue, black, red, green, colorless, spell;
	private MenuFragment	mFragment1;

	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.mana_pool_activity);

		white = 0;
		blue = 0;
		black = 0;
		red = 0;
		green = 0;

		whiteMinus = (Button) findViewById(R.id.white_minus);
		blueMinus = (Button) findViewById(R.id.blue_minus);
		blackMinus = (Button) findViewById(R.id.black_minus);
		redMinus = (Button) findViewById(R.id.red_minus);
		greenMinus = (Button) findViewById(R.id.green_minus);
		colorlessMinus = (Button) findViewById(R.id.colorless_minus);
		spellMinus = (Button) findViewById(R.id.spell_minus);

		whitePlus = (Button) findViewById(R.id.white_plus);
		bluePlus = (Button) findViewById(R.id.blue_plus);
		blackPlus = (Button) findViewById(R.id.black_plus);
		redPlus = (Button) findViewById(R.id.red_plus);
		greenPlus = (Button) findViewById(R.id.green_plus);
		colorlessPlus = (Button) findViewById(R.id.colorless_plus);
		spellPlus = (Button) findViewById(R.id.spell_plus);

		whiteReadout = (TextView) findViewById(R.id.white_readout);
		blueReadout = (TextView) findViewById(R.id.blue_readout);
		blackReadout = (TextView) findViewById(R.id.black_readout);
		redReadout = (TextView) findViewById(R.id.red_readout);
		greenReadout = (TextView) findViewById(R.id.green_readout);
		colorlessReadout = (TextView) findViewById(R.id.colorless_readout);
		spellReadout = (TextView) findViewById(R.id.spell_readout);

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
			// Log.e("Mana Pool", "Failed to locate all views from inflated XML");
			Toast.makeText(this, "Mana pool failed to load!", Toast.LENGTH_LONG).show();
			this.finish();
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
		
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		mFragment1 = (MenuFragment) fm.findFragmentByTag("f1");
		if (mFragment1 == null) {
			mFragment1 = new MenuFragment(this, R.menu.mana_pool_menu);
			ft.add(mFragment1, "f1");
		}
		ft.commit();
	}

	@Override
	protected void onPause() {
		super.onPause();
		store();
	}

	@Override
	protected void onResume() {
		super.onResume();
		load();
		update();
		MyApp appState = ((MyApp)getApplicationContext());
		appState.setState(0);
	}

	@Override
	protected void onStop() {
		super.onStop();
		store();
	}

	private void load() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		white = settings.getInt("whiteMana", 0);
		blue = settings.getInt("blueMana", 0);
		black = settings.getInt("blackMana", 0);
		red = settings.getInt("redMana", 0);
		green = settings.getInt("greenMana", 0);
		colorless = settings.getInt("colorlessMana", 0);
		spell = settings.getInt("spellCount", 0);
	}

	private void store() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.edit().putInt("whiteMana", white).putInt("blueMana", blue).putInt("blackMana", black)
				.putInt("redMana", red).putInt("greenMana", green).putInt("colorlessMana", colorless)
				.putInt("spellCount", spell).commit();
	}

	private void update() {
		TextView readouts[] = { whiteReadout, blueReadout, blackReadout, redReadout, greenReadout, colorlessReadout,
				spellReadout };
		int values[] = { white, blue, black, red, green, colorless, spell };

		for (int ii = 0; ii < readouts.length; ii++) {
			readouts[ii].setText("" + values[ii]);
		}
	}

	public static void reset(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		settings.edit().putInt("whiteMana", 0).putInt("blueMana", 0).putInt("blackMana", 0).putInt("redMana", 0)
				.putInt("greenMana", 0).putInt("colorlessMana", 0).putInt("spellCount", 0).commit();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.clear_all:
				reset(this);
				load();
				update();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
