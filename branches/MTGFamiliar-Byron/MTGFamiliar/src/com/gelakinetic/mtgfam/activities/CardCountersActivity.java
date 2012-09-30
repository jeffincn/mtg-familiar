package com.gelakinetic.mtgfam.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
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
    private MtgCountersCard selectedCard;

    
    private class MtgCountersCard extends MtgCard {
        public int id;
        public ArrayList<String> counters;
        public MtgCountersCard() {
            counters = new ArrayList<String>();
        }
        private void addCounter(String type) {
            counters.add(type);
        }
    }

    
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
			        addCardToLayoutByName(cardName);
			        view.setText("");
					return true;
				}
				return false;
			}
		});
	}


    @Override
    public void onDestroy() {
        super.onDestroy();
        scheduler.shutdown();
    }


    @Override
    public void onPause() {
        super.onPause();

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
            // TODO - \n isn't used in the serialization of the data...remove outer for loop
            String[] counterLines = counterData.split("\n");
            for (String line : counterLines) {
                String[] cards = line.split(";");

                for (String card : cards) {
                    String[] cardData = card.split("=");
                    int cardId = Integer.parseInt(cardData[0]);

                    // Get the card by the card_id that's been deserialized
                    String[] dbFields = new String[] { CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_POWER, CardDbAdapter.KEY_TOUGHNESS };
                    Cursor c = mDbHelper.fetchCard(cardId, dbFields);
                    String name = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
                    float power = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_POWER));
                    float toughness = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));

                    // Create an MtgCard instance for the new card
                    MtgCountersCard mtgCard = new MtgCountersCard();
                    mtgCard.id = cardId;
                    mtgCard.name = name;
                    mtgCard.power = power;
                    mtgCard.toughness = toughness;

                    // Deserialize the counters
                    if (! cardData[1].contains("[]")) {
                        String cardCounterData = cardData[1].substring(1, cardData[1].length()-1);
                        String[] counters = cardCounterData.split(", ");

                        // Add the counters to the card instance
                        for (String counter : counters) {
                            mtgCard.addCounter(counter);
                        }

                        this.cards.add(mtgCard);
                    }
                    //String countersList = TextUtils.join(", ", counters);

                    addCardToLayout(mtgCard);
                }
            }
        }
    }


    // Add the card with the given name to this activity's layout
	private void addCardToLayoutByName(String cardName) {
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


    // Add the given card to this activit's layout
	private void addCardToLayout(MtgCountersCard cardToAdd) {
	    //final MtgCountersCard card = cardToAdd;
        LayoutInflater inflater = getLayoutInflater();

        TableLayout table = (TableLayout) findViewById(R.id.cardcounterstable);
        TableRow row = (TableRow) inflater.inflate(R.layout.card_counters_row, table, false);
        TextView nameView = (TextView) row.findViewById(R.id.cardname);
        nameView.setText(cardToAdd.name);
        TextView ptView = (TextView) row.findViewById(R.id.cardpowertougness);
        ptView.setText((int)cardToAdd.power + "/" + (int)cardToAdd.toughness);

        row.setTag(cardToAdd);

        row.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                selectedCard = (MtgCountersCard) v.getTag(); // get the card associated with the clicked row

                // No counters? Show the add counters dialog
                if (selectedCard.counters.isEmpty()) {
                    showAddCounterDialog();
                    return;
                }

                // There are counters, so view them and optionally add or remove
                // View, Add or Remove Counters Dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
                builder.setMessage("View, Remove or Add Counters?")
                       .setCancelable(false)
                       .setNegativeButton("View", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.dismiss();
                               showCountersDialog(selectedCard.counters);
                           }
                       })
                       .setNeutralButton("Remove", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.dismiss();
                               showRemoveCountersDialog();
                           }
                       })
                       .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               showAddCounterDialog();
                           }
                       });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        
        table.addView(row);
	}
	

	public void reset(View v) {
        resetting = true;

        editor.putString(COUNTER_DATA, "");
        editor.commit();

        Intent intent = getIntent();
        finish();

        startActivity(intent);
	}

	
	private void addCounterToSelectedCard(String counter) {
	    selectedCard.addCounter(counter);
	}


	private void removeCounterFromSelectedCard(int counterIndex) {
        selectedCard.counters.remove(counterIndex);
    }

    
    private void showRemoveCountersDialog() {
        final CharSequence[] items = selectedCard.counters.toArray(new CharSequence[selectedCard.counters.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove counter");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                //Toast.makeText(mCtx, items[item], Toast.LENGTH_SHORT).show();
                removeCounterFromSelectedCard(item);
            }
        });
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    private void showAddCounterDialog() {
        LayoutInflater inflater = (LayoutInflater) mCtx.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.card_counters_dialog, (ViewGroup) findViewById(R.id.cardcounters_dialog));

        AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
        builder.setView(layout);
        builder.setTitle("Add Counter");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // get the radio selected
                RadioButton customCounterRadio = (RadioButton) layout.findViewById(R.id.card_counters_customradio);
                RadioButton predefinedRadio = (RadioButton) layout.findViewById(R.id.card_counters_predefined);

                String counter = "";
                if (customCounterRadio.isChecked()) {
                    EditText counterText = (EditText) layout.findViewById(R.id.card_counters_customedit);
                    counter = counterText.getText().toString();
                    // Custom counter was blank/empty, show error
                    if (counter.length() == 0) {
                        dialog.cancel();
                        Toast.makeText(mCtx, "Tried to add a blank counter", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                else if (predefinedRadio.isChecked()) {
                    Spinner counterText = (Spinner) layout.findViewById(R.id.card_counters_spinner);
                    TextView counterView = (TextView) counterText.getSelectedView();
                    counter = counterView.getText().toString();
                }
                addCounterToSelectedCard(counter);
                dialog.dismiss();
            }
        });
        AlertDialog addCounterDialog = builder.create();
        addCounterDialog.show();
    }


    private void showCountersDialog(List<String> counters) {
        final CharSequence[] items = counters.toArray(new CharSequence[counters.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Counters");
        
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Toast.makeText(mCtx, items[item], Toast.LENGTH_SHORT).show();
            }
        });
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
