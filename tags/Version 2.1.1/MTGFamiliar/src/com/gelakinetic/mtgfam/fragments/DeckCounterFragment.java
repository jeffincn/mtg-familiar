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
	private TextView deckCountHistory;
	
	private static final int COUNT_FLAG_UNDO = -1;
	private static final int COUNT_FLAG_RESET = -2;

	public DeckCounterFragment() {
		/* http://developer.android.com/reference/android/app/Fragment.html
		 * All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}
	
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
		
		deckCountHistory = (TextView) myFragmentView.findViewById(R.id.deck_counter_history);

		Button b1 = (Button) myFragmentView.findViewById(R.id.deck_counter_1);
		b1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateCardCount(1);
			}
		});
		Button b2 = (Button) myFragmentView.findViewById(R.id.deck_counter_2);
		b2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateCardCount(2);
			}
		});
		Button b3 = (Button) myFragmentView.findViewById(R.id.deck_counter_3);
		b3.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateCardCount(3);
			}
		});
		Button b4 = (Button) myFragmentView.findViewById(R.id.deck_counter_4);
		b4.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateCardCount(4);
			}
		});

		Button undo = (Button) myFragmentView.findViewById(R.id.deck_counter_undo);
		undo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateCardCount(COUNT_FLAG_UNDO);
			}
		});

		Button reset = (Button) myFragmentView.findViewById(R.id.deck_counter_reset);
		reset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateCardCount(COUNT_FLAG_RESET);
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
	
	
	private void updateCardCount(int count) {
		boolean updateUi = true;
		
		switch (count) {
			case COUNT_FLAG_UNDO:
				if (sequence.size() > 0) {
					deckCount -= sequence.remove(sequence.size() - 1);
				}
				else {
					updateUi = false;
				}
				break;
			case COUNT_FLAG_RESET:
				if (sequence.size() > 0) {
					deckCount = 0;
					sequence.clear();	
				}
				else {
					updateUi = false;
				}
				break;
			default:
				deckCount += count;
				sequence.add(count);
				break;
		}
		
		if (updateUi) {
			StringBuilder history = new StringBuilder();
			for (int i = 0; i < sequence.size(); i++) {
				history.append(sequence.get(i) + "  ");
			}
			deckCountHistory.setText(history.toString());
			deckCountText.setText("" + deckCount);
		}
	}
}
