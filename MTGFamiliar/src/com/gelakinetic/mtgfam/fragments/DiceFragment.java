package com.gelakinetic.mtgfam.fragments;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.gelakinetic.mtgfam.R;

public class DiceFragment extends FamiliarFragment {
	private Random													r;
	private ImageView												d2, d4, d6, d8, d10, d12, d20, d100;
	private TextView												dieOutput;
	private final ScheduledExecutorService	scheduler		= Executors.newScheduledThreadPool(1);
	private Handler													handler;
	private final DiceFragment							anchor			= this;

	public static final int									updateDelay	= 150;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View myFragmentView = inflater.inflate(R.layout.dice_activity, container, false);

		r = new Random();
		handler = new Handler();

		dieOutput = (TextView) myFragmentView.findViewById(R.id.die_output);

		d2 = (ImageView) myFragmentView.findViewById(R.id.d2);
		d4 = (ImageView) myFragmentView.findViewById(R.id.d4);
		d6 = (ImageView) myFragmentView.findViewById(R.id.d6);
		d8 = (ImageView) myFragmentView.findViewById(R.id.d8);
		d10 = (ImageView) myFragmentView.findViewById(R.id.d10);
		d12 = (ImageView) myFragmentView.findViewById(R.id.d12);
		d20 = (ImageView) myFragmentView.findViewById(R.id.d20);
		d100 = (ImageView) myFragmentView.findViewById(R.id.d100);

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
		return myFragmentView;
	}

	@Override
	public void onResume() {
		super.onResume();
		boolean d2AsCoin = this.getMainActivity().preferences.getBoolean("d2AsCoin", true);

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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.dice_menu, menu);
	}
}
