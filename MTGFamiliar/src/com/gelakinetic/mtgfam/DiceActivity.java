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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DiceActivity extends Activity {
	private Random													r;
	private ImageView												d2, d4, d6, d8, d10, d12, d20, d100;
	private TextView												dieOutput;
	private boolean													d2AsCoin;
	private final ScheduledExecutorService	scheduler		= Executors.newScheduledThreadPool(1);
	private Handler													handler;

	public static final int									updateDelay	= 150;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dice_activity);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		d2AsCoin = settings.getBoolean("d2AsCoin", true);

		final DiceActivity anchor = this;
		r = new Random();
		handler = new Handler();

		dieOutput = (TextView) findViewById(R.id.die_output);

		d2 = (ImageView) findViewById(R.id.d2);
		d4 = (ImageView) findViewById(R.id.d4);
		d6 = (ImageView) findViewById(R.id.d6);
		d8 = (ImageView) findViewById(R.id.d8);
		d10 = (ImageView) findViewById(R.id.d10);
		d12 = (ImageView) findViewById(R.id.d12);
		d20 = (ImageView) findViewById(R.id.d20);
		d100 = (ImageView) findViewById(R.id.d100);

		if (d2 != null) {
			if (d2AsCoin) {
				d2.setImageResource(R.drawable.dcoin);
			}
			else {
				d2.setImageResource(R.drawable.d2);
			}
			if (d2AsCoin) {
				d2.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						anchor.flipCoin();
					}
				});
			}
			else {
				d2.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						anchor.rollDie(2);
					}
				});
			}
		}
		if (d4 != null) {
			d4.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					anchor.rollDie(4);
				}
			});
		}
		if (d6 != null) {
			d6.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					anchor.rollDie(6);
				}
			});
		}
		if (d8 != null) {
			d8.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					anchor.rollDie(8);
				}
			});
		}
		if (d10 != null) {
			d10.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					anchor.rollDie(10);
				}
			});
		}
		if (d12 != null) {
			d12.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					anchor.rollDie(12);
				}
			});
		}
		if (d20 != null) {
			d20.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					anchor.rollDie(20);
				}
			});
		}
		if (d100 != null) {
			d100.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					anchor.rollDie(100);
				}
			});
		}
	}

	public void rollDie(int d) {
		final int f_d = d;
		if (dieOutput != null) {
			dieOutput.setText("");
			scheduler.schedule(new Runnable() {
				public void run() {
					handler.post(new Runnable() {
						public void run() {
							dieOutput.setText("" + (r.nextInt(f_d) + 1));
						}
					});
				}
			}, updateDelay, TimeUnit.MILLISECONDS);
		}
	}

	public void flipCoin() {
		if (dieOutput != null) {
			String output = "heads";
			dieOutput.setText("");
			if (r.nextInt(2) == 0) {
				output = "tails";
			}
			final String f_output = output;
			scheduler.schedule(new Runnable() {
				public void run() {
					handler.post(new Runnable() {
						public void run() {
							dieOutput.setText(f_output);
						}
					});
				}
			}, updateDelay, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
