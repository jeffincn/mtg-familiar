package com.gelakinetic.mtgfam.activities;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.MtgCard;

public class CardCountersActivity extends FamiliarActivity {
	private AutoCompleteTextView namefield;
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.card_counters);

		namefield = (AutoCompleteTextView) findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(mCtx, null));

		// So pressing enter does the search
		namefield.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		namefield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
					addCardToTrack();
					return true;
				}
				return false;
			}
		});
	}


	private void addCardToTrack() {
		String cardName = namefield.getText().toString();

		// Get the P/T for the card in the text field
		String[] dbFields = new String[] { CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_POWER, CardDbAdapter.KEY_TOUGHNESS };
		Cursor c = this.mDbHelper.fetchCardByName(cardName, dbFields);
		String name = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
		int power = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_POWER));
		int toughness = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));

		LayoutInflater inflater = getLayoutInflater();
		TableLayout table = (TableLayout) findViewById(R.id.cardcounterstable);
		TableRow row = (TableRow) inflater.inflate(R.layout.card_counters_row, table, false);
		TextView content1 = (TextView) row.findViewById(R.id.cardname);
		content1.setText(name);
		TextView content2 = (TextView) row.findViewById(R.id.cardpower);
		content2.setText(Integer.toString(power));
		TextView content3 = (TextView) row.findViewById(R.id.cardtoughness);
		content3.setText(Integer.toString(toughness));
		table.addView(row);
		
		namefield.setText("");
	}
}
