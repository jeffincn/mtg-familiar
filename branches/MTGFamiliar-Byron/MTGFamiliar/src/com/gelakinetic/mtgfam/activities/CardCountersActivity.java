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
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.MtgCard;

public class CardCountersActivity extends FamiliarActivity {
    private static final String COUNTER_DATA = "counter_data";

    private AutoCompleteTextView namefield;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); 

    private ArrayList<MtgCountersCard> cards;
    private Editor editor;
    private boolean resetting;
    private int numCards = 0;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.card_counters);
		
		cards = new ArrayList<MtgCountersCard>();
        editor = preferences.edit();

		namefield = (AutoCompleteTextView) findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(mCtx, null));

		// So pressing enter does the search
		namefield.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		namefield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView view, int arg1, KeyEvent arg2) {
				if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
				    String cardName = view.getText().toString();
			        addCardByName(cardName);
			        view.setText("");
					return true;
				}
				return false;
			}
		});
	}

	
	private void addCardByName(String cardName) {
        // Get the P/T for the card in the text field
        String[] dbFields = new String[] { CardDbAdapter.KEY_ID, CardDbAdapter.KEY_POWER, CardDbAdapter.KEY_TOUGHNESS };
        Cursor c = this.mDbHelper.fetchCardByName(cardName, dbFields);
        int cardId = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_ID));
        float power = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_POWER));
        float toughness = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
	    
        MtgCountersCard card = new MtgCountersCard();
        card.id = cardId;
        card.name = cardName;
        card.power = power;
        card.toughness = toughness;
        cards.add(card);
        addCardToLayout(card);
	}

   private void addCardById(int cardId) {
       // Get the P/T for the card in the text field
       String[] dbFields = new String[] { CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_POWER, CardDbAdapter.KEY_TOUGHNESS };
       Cursor c = this.mDbHelper.fetchCard(cardId, dbFields);
       String name = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
       float power = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_POWER));
       float toughness = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
        
        MtgCountersCard card = new MtgCountersCard();
        card.id = cardId;
        card.name = name;
        card.power = power;
        card.toughness = toughness;
        cards.add(card);
        addCardToLayout(card);
    }

	
    // Add the selected card to the layout with relevant information (Name, P/T, etc)
	private void addCardToLayout(MtgCountersCard card) {
        LayoutInflater inflater = getLayoutInflater();
        
        TableLayout table = (TableLayout) findViewById(R.id.cardcounterstable);
        TableRow row = (TableRow) inflater.inflate(R.layout.card_counters_row, table, false);
        TextView nameView = (TextView) row.findViewById(R.id.cardname);
        nameView.setText(card.name);
        TextView ptView = (TextView) row.findViewById(R.id.cardpowertougness);
        ptView.setText((int)card.power + "/" + (int)card.toughness);
        
        table.addView(row);
	}


	@Override
    public void onDestroy() {
        super.onDestroy();
        scheduler.shutdown();
    }

    @Override
    public void onPause() {
        super.onPause();

        /*
        if (canGetLock) {
                wl.release();
        }
        */

        if (!resetting) {
            String counterData = "";

            for (MtgCountersCard card : cards) {
                counterData += Integer.toString(card.id);
                counterData += "=";
                counterData += card.counters.toString();
                counterData += ";";
            }
            editor.putString(COUNTER_DATA, counterData);
            editor.commit();
        }
        resetting = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        String counterData = preferences.getString(COUNTER_DATA, "");

        if ((counterData.length() > 0) && (cards.size() == 0)) {
            numCards = 0;
            // TODO - \n isn't used in the serialization of the data...remove outer for loop
            String[] counterLines = counterData.split("\n");
            for (String line : counterLines) {
                String[] cards = line.split(";");

                for (String card : cards) {
                    String[] cardData = card.split("=");
                    int cardId = Integer.parseInt(cardData[0]);
                    Log.w("CARD COUNTERS RESUMING: cardId", Integer.toString(cardId));
                    Log.w("CARD COUNTERS RESUMING: cardData[1]", cardData[1]);
                    if (! cardData[1].contains("[]")) {
                        String cardCounterData = cardData[1].substring(1, cardData[1].length()-2);
                        Log.w("CARD COUNTERS RESUMING: cardCounterData", cardCounterData);
                        String[] counters = cardCounterData.split(", ");
                        ArrayList<Integer> counterTotals = new ArrayList<Integer>();
                        for (String counter : counters) {
                            counterTotals.add(Integer.parseInt(counter));
                        }
                    }
                    addCardById(cardId);
                    numCards++;
                }
            }
        }
    }

    private class MtgCountersCard extends MtgCard {
        public int id;
        public ArrayList<Integer> counters;
        public MtgCountersCard() {
            counters = new ArrayList<Integer>();
        }
    }

}
