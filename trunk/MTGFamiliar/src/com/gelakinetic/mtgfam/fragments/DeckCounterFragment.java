package com.gelakinetic.mtgfam.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.gelakinetic.mtgfam.R;

public class DeckCounterFragment extends FamiliarFragment implements ViewFactory {

	private ArrayList<Integer> sequence = new ArrayList<Integer>();
	private int deckCount = 0;
	private TextSwitcher deckCountText;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View myFragmentView = inflater.inflate(R.layout.deck_counter_frag, container, false);

		deckCountText = (TextSwitcher) myFragmentView.findViewById(R.id.deck_counter_count);
		deckCountText.setFactory(this);

		Animation in = AnimationUtils.loadAnimation(this.getActivity(), android.R.anim.slide_in_left);
		Animation out = AnimationUtils.loadAnimation(this.getActivity(), android.R.anim.slide_out_right);
		deckCountText.setInAnimation(in);
		deckCountText.setOutAnimation(out);

		deckCountText.setText("" + deckCount);

		Button b1 = (Button) myFragmentView.findViewById(R.id.deck_counter_1);
		b1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				deckCount += 1;
				sequence.add(1);
				deckCountText.setText("" + deckCount);
			}
		});
		Button b2 = (Button) myFragmentView.findViewById(R.id.deck_counter_2);
		b2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				deckCount += 2;
				sequence.add(2);
				deckCountText.setText("" + deckCount);
			}
		});
		Button b3 = (Button) myFragmentView.findViewById(R.id.deck_counter_3);
		b3.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				deckCount += 3;
				sequence.add(3);
				deckCountText.setText("" + deckCount);
			}
		});
		Button b4 = (Button) myFragmentView.findViewById(R.id.deck_counter_4);
		b4.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				deckCount += 4;
				sequence.add(4);
				deckCountText.setText("" + deckCount);
			}
		});

		Button undo = (Button) myFragmentView.findViewById(R.id.deck_counter_undo);
		undo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (sequence.size() > 0) {
					sequence.remove(sequence.size() - 1);
					deckCount = 0;
					for (Integer i : sequence) {
						deckCount += i;
					}
					deckCountText.setText("" + deckCount);
				}
			}
		});

		Button reset = (Button) myFragmentView.findViewById(R.id.deck_counter_reset);
		reset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sequence.clear();
				deckCount = 0;
				deckCountText.setText("" + deckCount);
			}
		});
		return myFragmentView;
	}

	@Override
	public View makeView() {
		TextView t = new TextView(this.getActivity());
		t.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL);
		t.setTextAppearance(this.getActivity(), R.style.text);
		t.setTextSize(70);
		return t;
	}
}
