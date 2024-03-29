package com.gelakinetic.mtgfam.fragments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.AutocompleteCursorAdapter;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers.CardData;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

public class TradeFragment extends FamiliarFragment {
	private final static int			DIALOG_UPDATE_CARD		= 1;
	private final static int			DIALOG_PRICE_SETTING	= 2;
	private final static int			DIALOG_SAVE_TRADE			= 3;
	private final static int			DIALOG_LOAD_TRADE			= 4;
	private final static int			DIALOG_DELETE_TRADE		= 5;
	private final static int			DIALOG_CONFIRMATION		= 6;
	private String								sideForDialog;
	private int										positionForDialog;

	private AutoCompleteTextView	namefield;
	private ImageButton				camerabutton;

	private Button					bAddTradeLeft;
	private TextView				tradePriceLeft;
	private ListView				lvTradeLeft;
	private TradeListAdapter		aaTradeLeft;
	private ArrayList<CardData>		lTradeLeft;

	private Button					bAddTradeRight;
	private TextView				tradePriceRight;
	private ListView				lvTradeRight;
	private TradeListAdapter		aaTradeRight;
	private ArrayList<CardData>		lTradeRight;

	private EditText				numberfield;
	private CheckBox				checkboxFoil;

	private int						priceSetting;

	private String					currentTrade;
	private TradeListHelpers		mTradeListHelper;

	public static final String		card_not_found				= "Card Not Found";
	public static final String		mangled_url						= "Mangled URL";
	public static final String		database_busy					= "Database Busy";
	public static final String		fetch_failed					= "Fetch Failed";
	public static final String		number_of_invalid			= "Number of Cards Invalid";
	public static final String		card_corrupted					= "Card Data corrupted, discarding.";
	public static final String		cannot_be_foil				= 	"Removing Foil - Card set does not match those printed for foil.";

	private static final String		autosaveName					= "autosave";
	private static final String		tradeExtension				= ".trade";
	private boolean					doneLoading				= false;

	private static final int	LOW_PRICE	= 0;
	private static final int	AVG_PRICE	= 1;
	private static final int	HIGH_PRICE	= 2;
	private static final int	FOIL_PRICE	= 3;
	
	public TradeFragment() {
		/* http://developer.android.com/reference/android/app/Fragment.html
		 * All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// This is necessary to keep the image search task running through rotation
		this.setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View myFragmentView = inflater.inflate(R.layout.trader_activity, container, false);

		mTradeListHelper = new TradeListHelpers();

		namefield = (AutoCompleteTextView) myFragmentView.findViewById(R.id.namesearch);
		namefield.setAdapter(new AutocompleteCursorAdapter(this.getActivity(), null));

		setKeyboardFocus(savedInstanceState, namefield, false, 100);
		
		camerabutton = (ImageButton) myFragmentView.findViewById(R.id.cameraButton);

		numberfield = (EditText) myFragmentView.findViewById(R.id.numberInput);
		numberfield.setText("1");
		
		checkboxFoil = (CheckBox) myFragmentView.findViewById(R.id.trader_foil);

		lTradeLeft = new ArrayList<CardData>();
		bAddTradeLeft = (Button) myFragmentView.findViewById(R.id.addCardLeft);
		tradePriceLeft = (TextView) myFragmentView.findViewById(R.id.priceTextLeft);
		lvTradeLeft = (ListView) myFragmentView.findViewById(R.id.tradeListLeft);
		aaTradeLeft = new TradeListAdapter(this.getActivity(), R.layout.trader_row, lTradeLeft);
		lvTradeLeft.setAdapter(aaTradeLeft);

		lTradeRight = new ArrayList<CardData>();
		bAddTradeRight = (Button) myFragmentView.findViewById(R.id.addCardRight);
		tradePriceRight = (TextView) myFragmentView.findViewById(R.id.priceTextRight);
		lvTradeRight = (ListView) myFragmentView.findViewById(R.id.tradeListRight);
		aaTradeRight = new TradeListAdapter(this.getActivity(), R.layout.trader_row, lTradeRight);
		lvTradeRight.setAdapter(aaTradeRight);

		camerabutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				takePictureAndSearchImageIntent();
			}
		});
		camerabutton.setVisibility(View.GONE);
		
		bAddTradeLeft.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().toString().length() > 0) {
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					int numberOf = Integer.parseInt(numberOfFromField);
					boolean foil = checkboxFoil.isChecked();

					String cardName = "", 
							setCode = "", 
							tcgName = "",
							cardNumber="";
					try {
						cardName = namefield.getText().toString();
						Cursor cards = mDbHelper.fetchCardByName(cardName, new String[]{CardDbAdapter.KEY_SET, CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_RARITY});
						setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
						tcgName = mDbHelper.getTCGname(setCode);
						cardNumber = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));
						
						cards.close();
					}
					catch (Exception e) {
						Toast.makeText(getActivity(), card_not_found, Toast.LENGTH_SHORT).show();
						namefield.setText("");
						numberfield.setText("1");
						return;
					}
					try {
						if (foil && !TradeListHelpers.canBeFoil(setCode, mDbHelper)){
							Toast.makeText(getActivity(), cannot_be_foil, Toast.LENGTH_LONG).show();
							foil = false;
						}
					} catch (FamiliarDbException e) {}
					
					final CardData data = mTradeListHelper.new CardData(cardName, tcgName, setCode, numberOf, 0, "loading", cardNumber, '-', false, foil);
					
					lTradeLeft.add(0, data);
					aaTradeLeft.notifyDataSetChanged();
					loadPrice(data, aaTradeLeft);
					namefield.setText("");
					numberfield.setText("1");
					checkboxFoil.setChecked(false);
				}
				else {
					Toast.makeText(getActivity(), getString(R.string.trader_toast_select_card), Toast.LENGTH_SHORT).show();
				}
			}
		});

		bAddTradeRight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (namefield.getText().toString().length() > 0) {
					String numberOfFromField = numberfield.getText().toString();
					if (numberOfFromField.length() == 0) {
						numberOfFromField = "1";
					}
					int numberOf = Integer.parseInt(numberOfFromField);
					boolean foil = checkboxFoil.isChecked();

					String cardName = "", 
							setCode = "", 
							tcgName = "",
							cardNumber = "";
					try {
						cardName = namefield.getText().toString();
						Cursor cards = mDbHelper.fetchCardByName(cardName, new String[]{CardDbAdapter.KEY_SET, CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_RARITY});
						setCode = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET));
						tcgName = mDbHelper.getTCGname(setCode);
						cardNumber = cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_NUMBER));
						
						cards.close();
					}
					catch (Exception e) {
						Toast.makeText(getActivity(), card_not_found, Toast.LENGTH_SHORT).show();
						namefield.setText("");
						numberfield.setText("1");
						return;
					}
					try {
						if (foil && !TradeListHelpers.canBeFoil(setCode, mDbHelper)){
							Toast.makeText(getActivity(), cannot_be_foil, Toast.LENGTH_LONG).show();
							foil = false;
						}
					} catch (FamiliarDbException e) {}
					
					CardData data = mTradeListHelper.new CardData(cardName, tcgName, setCode, numberOf, 0, "loading", cardNumber,'-', false, foil);

					lTradeRight.add(0, data);
					aaTradeRight.notifyDataSetChanged();
					loadPrice(data, aaTradeRight);
					namefield.setText("");
					numberfield.setText("1");
					checkboxFoil.setChecked(false);
				}
				else {
					Toast.makeText(getActivity(), getString(R.string.trader_toast_select_card), Toast.LENGTH_SHORT).show();
				}
			}
		});

		lvTradeLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				sideForDialog = "left";
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});
		lvTradeRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				sideForDialog = "right";
				positionForDialog = arg2;
				showDialog(DIALOG_UPDATE_CARD);
			}
		});

		// Give this a default value so we don't get the null pointer-induced FC. It
		// shouldn't matter what we set it to, as long as we set it, since we
		// dismiss the dialog if it's showing in onResume().
		sideForDialog = "left";

		// Default this to an empty string so we never get NPEs from it
		currentTrade = "";
		
		return myFragmentView;
	}

	@Override
    protected void onImageSearchSuccess(long multiverseId, String name) {
    	// this method must be overridden by each class calling takePictureAndSearchImageIntent
		super.onImageSearchSuccess(multiverseId, name);
		namefield.setText(name);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		priceSetting = Integer.parseInt(getMainActivity().getPreferencesAdapter().getTradePrice());

		try {
			// Test to see if the autosave file exist, then load the trade it if does.
			this.getActivity().openFileInput(autosaveName + tradeExtension);
			LoadTrade(autosaveName + tradeExtension);
		}
		catch (FileNotFoundException e) {
			// Do nothing if the file doesn't exist, but mark it as loaded otherwise prices won't update
			doneLoading = true;
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		SaveTrade(autosaveName + tradeExtension);
		getMainActivity().getSpiceManager().cancelAllRequests();
	}
		
	private void showDialog(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}

		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {
			
			@Override
			public void onDestroy() {
				super.onDestroy();
				removeDialog();
			}
			
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				setShowsDialog(true); // We're setting this to false if we return null, so we should reset it every time to be safe
				switch (id) {
					case DIALOG_UPDATE_CARD: {
						final int position = positionForDialog;
						final String side = (sideForDialog.equals("left") ? "left" : "right");
						final ArrayList<CardData> lSide = (sideForDialog.equals("left") ? lTradeLeft : lTradeRight);
						final TradeListAdapter aaSide = (sideForDialog.equals("left") ? aaTradeLeft : aaTradeRight);
						final int numberOfCards = lSide.get(position).numberOf;
						final String priceOfCard = lSide.get(position).getPriceString();
						final boolean foil = lSide.get(position).foil;
		
						View view = LayoutInflater.from(getActivity()).inflate(R.layout.trader_card_click_dialog, null);
						Button removeAll = (Button) view.findViewById(R.id.traderDialogRemove);
						Button resetPrice = (Button) view.findViewById(R.id.traderDialogResetPrice);
						Button info = (Button) view.findViewById(R.id.traderDialogInfo);
						Button changeSet = (Button) view.findViewById(R.id.traderDialogChangeSet);
						Button cancelbtn = (Button) view.findViewById(R.id.traderDialogCancel);
						Button donebtn = (Button) view.findViewById(R.id.traderDialogDone);
						final CheckBox foilbtn = (CheckBox) view.findViewById(R.id.traderDialogFoil);
						final EditText numberOf = (EditText) view.findViewById(R.id.traderDialogNumber);
						final EditText priceText = (EditText) view.findViewById(R.id.traderDialogPrice);
		
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
						builder.setTitle(lSide.get(position).name).setView(view);
		
						Dialog dialog = builder.create();
		
						String numberOfStr = String.valueOf(numberOfCards);
						numberOf.setText(numberOfStr);
						numberOf.setSelection(numberOfStr.length());
						foilbtn.setChecked(foil);
		
						final String priceNumberStr = lSide.get(position).hasPrice() ? priceOfCard.substring(1) : "";
						priceText.setText(priceNumberStr);
						priceText.setSelection(priceNumberStr.length());
		
						removeAll.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								lSide.remove(position);
								aaSide.notifyDataSetChanged();
								UpdateTotalPrices(side);
								removeDialog();
							}
						});
						
						if(!lSide.get(position).customPrice) {
							resetPrice.setVisibility(View.GONE);
						}
						else {
						resetPrice.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								lSide.get(position).customPrice = false;
								loadPrice(lSide.get(position), aaSide);
								aaSide.notifyDataSetChanged();
								UpdateTotalPrices(side);
								removeDialog();
							}
						});
						}
						
						info.setOnClickListener(new OnClickListener() {
							
							@Override
							public void onClick(View v) {
								try {
									Bundle args = new Bundle();
									Cursor c = mDbHelper.fetchCardByNameAndSet(lSide.get(position).name, lSide.get(position).setCode);
									args.putLong("id", c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID)));
									args.putBoolean(SearchViewFragment.RANDOM, false);
									args.putBoolean("isSingle", true);
									c.close();
									CardViewFragment cvFrag = new CardViewFragment();
									startNewFragment(cvFrag, args);
								} catch (FamiliarDbException e) {
								}
							}
						});
		
						changeSet.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								removeDialog();
								try {
									ChangeSet(side, position);
								} catch (FamiliarDbException e) {
									getMainActivity().showDbErrorToast();
									getMainActivity().getSupportFragmentManager().popBackStack();
									return;
								}
							}
						});
		
						cancelbtn.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								removeDialog();
							}
						});
		
						donebtn.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
		
								// validate number of cards text
								if (numberOf.length() == 0) {
									Toast.makeText(getActivity(), number_of_invalid, Toast.LENGTH_LONG).show();
									return;
								}
		
								// validate the price text
								String userInputPrice = priceText.getText().toString();
								
								if (!userInputPrice.equals(priceNumberStr)){
									lSide.get(position).SetIsCustomPrice();
								}
								
								//Hack to regrab price, if the set price is blank.
								if (userInputPrice.length() == 0){
									lSide.get(position).customPrice = false;
									loadPrice(lSide.get(position), aaSide);
								}
								
								lSide.get(position).foil = foilbtn.isChecked();
								try {
									if (lSide.get(position).foil && !TradeListHelpers.canBeFoil(lSide.get(position).setCode, mDbHelper)){
										Toast.makeText(getActivity(), cannot_be_foil, Toast.LENGTH_LONG).show();
										lSide.get(position).foil = false;
									}
								} catch (FamiliarDbException e) {}
								if (foil != foilbtn.isChecked()){
									loadPrice(lSide.get(position), aaSide);
								}
									
								double uIP;
								try {
									uIP = Double.parseDouble(userInputPrice);
									// Clear the message so the user's specified price will display
									lSide.get(position).message = "";
								}
								catch (NumberFormatException e) {
									uIP = 0;
								}
		
								lSide.get(position).numberOf = (Integer.parseInt(numberOf.getEditableText().toString()));
								lSide.get(position).price = ((int) Math.round(uIP * 100));
								aaSide.notifyDataSetChanged();
								UpdateTotalPrices(side);
		
								removeDialog();
							}
						});
						return dialog;
					}
					case DIALOG_PRICE_SETTING: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		
						builder.setTitle(R.string.trader_pricing_dialog_title);
						builder.setSingleChoiceItems(new String[] { getString(R.string.trader_Low), getString(R.string.trader_Average), getString(R.string.trader_High) }, priceSetting,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										priceSetting = which;
										dialog.dismiss();
		
										// Update ALL the prices!
										for (CardData data : lTradeLeft) {
											if (data.customPrice == false) {
												data.message = "loading";
												loadPrice(data, aaTradeLeft);
											}
										}
										aaTradeLeft.notifyDataSetChanged();
		
										for (CardData data : lTradeRight) {
											if (data.customPrice == false) {
												data.message = "loading";
												loadPrice(data, aaTradeRight);
											}
										}
										aaTradeRight.notifyDataSetChanged();
		
										// And also update the preference
										getMainActivity().getPreferencesAdapter().setTradePrice(String.valueOf(priceSetting));
		
										removeDialog();
									}
								});
		
						Dialog dialog = builder.create();
		
						return dialog;
					}
					case DIALOG_SAVE_TRADE: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		
						builder.setTitle(R.string.trader_save_dialog_title);
						builder.setMessage(R.string.trader_save_dialog_text);
		
						// Set an EditText view to get user input
						final EditText input = new EditText(this.getActivity());
						input.setText(currentTrade);
						input.setSingleLine(true);
						builder.setView(input);
		
						builder.setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String tradeName = input.getText().toString();
		
								String FILENAME = tradeName + tradeExtension;
								SaveTrade(FILENAME);
		
								currentTrade = tradeName;
							}
						});
		
						builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Canceled.
							}
						});
		
						Dialog dialog = builder.create();
						dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
						return dialog;
					}
					case DIALOG_LOAD_TRADE: {
						String[] files = this.getActivity().fileList();
						ArrayList<String> validFiles = new ArrayList<String>();
						for (String fileName : files) {
							if (fileName.endsWith(tradeExtension)) {
								validFiles.add(fileName.substring(0, fileName.indexOf(tradeExtension)));
							}
						}
		
						Dialog dialog;
						if (validFiles.size() == 0) {
							Toast.makeText(this.getActivity(), R.string.trader_toast_no_trades, Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}
						
						final String[] tradeNames = new String[validFiles.size()];
						validFiles.toArray(tradeNames);
		
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
						builder.setTitle(R.string.trader_select_dialog_title);
						builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Canceled.
							}
						});
						builder.setItems(tradeNames, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface di, int which) {
		
								LoadTrade(tradeNames[which] + tradeExtension);
		
								currentTrade = tradeNames[which];
		
								aaTradeLeft.notifyDataSetChanged();
								aaTradeRight.notifyDataSetChanged();
							}
						});
		
						dialog = builder.create();
						return dialog;
					}
					case DIALOG_DELETE_TRADE: {
						String[] files = getActivity().fileList();
						ArrayList<String> validFiles = new ArrayList<String>();
						for (String fileName : files) {
							if (fileName.endsWith(tradeExtension)) {
								validFiles.add(fileName.substring(0, fileName.indexOf(tradeExtension)));
							}
						}
		
						Dialog dialog;
						if (validFiles.size() == 0) {
							Toast.makeText(this.getActivity(), R.string.trader_toast_no_trades, Toast.LENGTH_LONG).show();
							setShowsDialog(false);
							return null;
						}
		
						final String[] tradeNamesD = new String[validFiles.size()];
						validFiles.toArray(tradeNamesD);
		
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
						builder.setTitle(R.string.trader_delete_dialog_title);
						builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Canceled.
							}
						});
						builder.setItems(tradeNamesD, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface di, int which) {
								File toDelete = new File(getActivity().getFilesDir(), tradeNamesD[which] + tradeExtension);
								toDelete.delete();
							}
						});
		
						dialog = builder.create();
						return dialog;
					}
					case DIALOG_CONFIRMATION: {
						View dialogLayout = this.getActivity().getLayoutInflater().inflate(R.layout.simple_message_layout, null);
						TextView text = (TextView) dialogLayout.findViewById(R.id.message);
						text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.trader_clear_dialog_text)));
						text.setMovementMethod(LinkMovementMethod.getInstance());
		
						Dialog dialog = new AlertDialog.Builder(this.getActivity())
							.setTitle(R.string.trader_clear_dialog_title)
							.setView(dialogLayout)
								.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										lTradeRight.clear();
										aaTradeRight.notifyDataSetChanged();
										lTradeLeft.clear();
										aaTradeLeft.notifyDataSetChanged();
										UpdateTotalPrices("both");
										removeDialog();
									}
								}).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										removeDialog();
									}
								}).setCancelable(true).create();
						return dialog;
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(ft, "dialog");
	}
			
			
		
	protected void SaveTrade(String _tradeName) {
		FileOutputStream fos;

		try {
			// MODE_PRIVATE will create the file (or replace a file of the
			// same name)
			fos = this.getActivity().openFileOutput(_tradeName, Context.MODE_PRIVATE);

			for (CardData cd : lTradeLeft) {
				fos.write(cd.toString(CardDbAdapter.LEFT).getBytes());
			}
			for (CardData cd : lTradeRight) {
				fos.write(cd.toString(CardDbAdapter.RIGHT).getBytes());
			}

			fos.close();
		}
		catch (FileNotFoundException e) {
			Toast.makeText(this.getActivity(), R.string.trader_toast_save_error, Toast.LENGTH_LONG).show();
		}
		catch (IOException e) {
			Toast.makeText(this.getActivity(), R.string.trader_toast_save_error, Toast.LENGTH_LONG).show();
		}
		catch (IllegalArgumentException e) {
			Toast.makeText(this.getActivity(), R.string.trader_toast_invalid_chars, Toast.LENGTH_LONG).show();
		}
	}

	protected void LoadTrade(String _tradeName) {
		try {
			doneLoading = false;
			BufferedReader br = new BufferedReader(new InputStreamReader(this.getActivity().openFileInput(_tradeName)));

			lTradeLeft.clear();
			lTradeRight.clear();

			String line;
			String[] parts;
			while ((line = br.readLine()) != null) {
				parts = line.split(CardData.delimiter);

				String cardName = "";
				
				try {
					cardName = parts[1];
					String cardSet = parts[2];
					String tcgName = mDbHelper.getTCGname(cardSet);
					int side = Integer.parseInt(parts[0]);
					int numberOf = Integer.parseInt(parts[3]);
					boolean customPrice = false;
					int price = 0;
					String message = "loading";
					boolean foil = false;
					
					CardData cd = null;
					
					try {
						customPrice = Boolean.parseBoolean(parts[4]);
						price = Integer.parseInt(parts[5]);
						foil = Boolean.parseBoolean(parts[6]);
						message = "";
					}
					catch (Exception e)	{
						customPrice = false;
						price = 0;
						message = "loading";
						foil = false;
					}
					finally {
						cd = mTradeListHelper.new CardData(cardName, tcgName, cardSet, numberOf, price, message, null, '-', customPrice, foil);
					}
					
					if (side == CardDbAdapter.LEFT) {
						lTradeLeft.add(cd);
						if (customPrice == false)
							loadPrice(cd, aaTradeLeft);
					}
					else if (side == CardDbAdapter.RIGHT) {
						lTradeRight.add(cd);
						if (customPrice == false)
							loadPrice(cd, aaTradeRight);
					}
				}
				catch (Exception e) {
					if (cardName != null && cardName.length() != 0)
						Toast.makeText(this.getActivity(), cardName + ": " + card_corrupted, Toast.LENGTH_LONG).show();
					else
						Toast.makeText(this.getActivity(), card_corrupted, Toast.LENGTH_SHORT).show();
					continue;
				}
			}
		}
		catch (NumberFormatException e) {
			Toast.makeText(this.getActivity(), "NumberFormatException", Toast.LENGTH_LONG).show();
		}
		catch (IOException e) {
			Toast.makeText(this.getActivity(), "IOException", Toast.LENGTH_LONG).show();
		}
		doneLoading = true;
		UpdateTotalPrices(); // this is for custom prices
	}

	protected void ChangeSet(final String _side, final int _position) throws FamiliarDbException {
		CardData data = (_side.equals("left") ? lTradeLeft.get(_position) : lTradeRight.get(_position));
		String name = data.name;

		Cursor cards = mDbHelper.fetchCardByName(name, new String[]{CardDbAdapter.KEY_SET});
		Set<String> sets = new LinkedHashSet<String>();
		Set<String> setCodes = new LinkedHashSet<String>();
		while (!cards.isAfterLast()) {
			if (sets.add(mDbHelper.getTCGname(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET))))) {
				setCodes.add(cards.getString(cards.getColumnIndex(CardDbAdapter.KEY_SET)));
			}
			cards.moveToNext();
		}
		cards.close();

		final String[] aSets = sets.toArray(new String[sets.size()]);
		final String[] aSetCodes = setCodes.toArray(new String[setCodes.size()]);
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setTitle(R.string.card_view_set_dialog_title);
		builder.setItems(aSets, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int item) {
				if (_side.equals("left")) {
					lTradeLeft.get(_position).setCode = (aSetCodes[item]);
					lTradeLeft.get(_position).tcgName = (aSets[item]);
					lTradeLeft.get(_position).message = ("loading");
					aaTradeLeft.notifyDataSetChanged();
					loadPrice(lTradeLeft.get(_position), aaTradeLeft);
				}
				else if (_side.equals("right")) {
					lTradeRight.get(_position).setCode = (aSetCodes[item]);
					lTradeRight.get(_position).tcgName = (aSets[item]);
					lTradeRight.get(_position).message = ("loading");
					aaTradeRight.notifyDataSetChanged();
					loadPrice(lTradeRight.get(_position), aaTradeRight);
				}
				return;
			}
		});
		builder.create().show();
	}

	public void UpdateTotalPrices() {
		UpdateTotalPrices("both");
	}

	private void UpdateTotalPrices(String _side) {
		if(doneLoading){
			if (_side.equals("left") || _side.equals("both")) {
				int totalPriceLeft = GetPricesFromTradeList(lTradeLeft);
				int color = PriceListHasBadValues(lTradeLeft) ? this.getActivity().getResources().getColor(R.color.red) : this.getActivity().getResources().getColor(R.color.white);
				String sTotalLeft = "$" + (totalPriceLeft / 100) + "." + String.format("%02d", (totalPriceLeft % 100));
				tradePriceLeft.setText(sTotalLeft);
				tradePriceLeft.setTextColor(color);
			}
			if (_side.equals("right") || _side.equals("both")) {
				int totalPriceRight = GetPricesFromTradeList(lTradeRight);
				int color = PriceListHasBadValues(lTradeRight) ? this.getActivity().getResources().getColor(R.color.red) : this.getActivity().getResources().getColor(R.color.white);
				String sTotalRight = "$" + (totalPriceRight / 100) + "." + String.format("%02d", (totalPriceRight % 100));
				tradePriceRight.setText(sTotalRight);
				tradePriceRight.setTextColor(color);
			}
		}
	}

	private boolean PriceListHasBadValues(ArrayList<CardData> trade) {
		for (CardData data : trade) {
			if (!data.hasPrice()) {
				return true;
			}
		}
		return false;
	}

	private int GetPricesFromTradeList(ArrayList<CardData> _trade) {
		int totalPrice = 0;

		for (int i = 0; i < _trade.size(); i++) {// CardData data : _trade) {
			CardData data = _trade.get(i);
			if (data.hasPrice()) {
				totalPrice += data.numberOf * data.price;
			}
			else {
				String message = data.message;

				// Remove the card from the list, unless it was just a fetch failed.
				// Otherwise, the card does not exist, or there is a database problem

				if (message.compareTo(card_not_found) == 0) {
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(this.getActivity(), data.name + ": " + card_not_found, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(mangled_url) == 0) {
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(this.getActivity(), data.name + ": " + mangled_url, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(database_busy) == 0) {
					_trade.remove(data);
					i--;
					aaTradeRight.notifyDataSetChanged();
					aaTradeLeft.notifyDataSetChanged();
					Toast.makeText(this.getActivity(), data.name + ": " + database_busy, Toast.LENGTH_LONG).show();
				}
				else if (message.compareTo(fetch_failed) == 0) {

				}
			}
		}
		return totalPrice;
	}
	
	 @Override
     public boolean onOptionsItemSelected(MenuItem item) {
             // Handle item selection
             switch (item.getItemId()) {
                     case R.id.trader_menu_clear:
                             showDialog(DIALOG_CONFIRMATION);
                             return true;
                     case R.id.trader_menu_settings:
                             showDialog(DIALOG_PRICE_SETTING);
                             return true;
                     case R.id.trader_menu_save:
                             showDialog(DIALOG_SAVE_TRADE);
                             return true;
                     case R.id.trader_menu_load:
                             showDialog(DIALOG_LOAD_TRADE);
                             return true;
                     case R.id.trader_menu_delete:
                             showDialog(DIALOG_DELETE_TRADE);
                             return true;
                     default:
                             return super.onOptionsItemSelected(item);
             }
     }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.trader_menu, menu);
	}
	
	private class TradeListAdapter extends ArrayAdapter<CardData> {

		private int									layoutResourceId;
		private ArrayList<CardData>	items;

		public TradeListAdapter(Context context, int textViewResourceId, ArrayList<CardData> items) {
			super(context, textViewResourceId, items);

			this.layoutResourceId = textViewResourceId;
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inf = getActivity().getLayoutInflater();
				v = inf.inflate(layoutResourceId, null);
			}
			CardData data = items.get(position);
			if (data != null) {
				TextView nameField = (TextView) v.findViewById(R.id.traderRowName);
				TextView setField = (TextView) v.findViewById(R.id.traderRowSet);
				TextView numberField = (TextView) v.findViewById(R.id.traderNumber);
				TextView priceField = (TextView) v.findViewById(R.id.traderRowPrice);
				ImageView foilField = (ImageView) v.findViewById(R.id.traderRowFoil);

				nameField.setText(data.name);
				setField.setText(data.tcgName);
				numberField.setText(data.hasPrice() ? data.numberOf + "x" : "");
				priceField.setText(data.hasPrice() ? data.getPriceString() : data.message);
				foilField.setVisibility((data.foil ? View.VISIBLE : View.GONE));

				if (data.hasPrice()) {
					if (data.customPrice == true) {
						priceField.setTextColor(getActivity().getResources().getColor(R.color.green));
					}
					else {
						priceField.setTextColor(getActivity().getResources().getColor(R.color.light_gray));
					}
				}
				else {
					priceField.setTextColor(getActivity().getResources().getColor(R.color.red));
				}
			}
			return v;
		}
	}
	
	private void loadPrice(final CardData data, final TradeListAdapter adapter) {
		PriceFetchRequest priceRequest = new PriceFetchRequest(data.name, data.setCode, data.cardNumber, -1, mDbHelper);
		final boolean foilOverride = data.foil;
		getMainActivity().getSpiceManager().execute( priceRequest, data.name + "-" + data.setCode, DurationInMillis.ONE_DAY, new RequestListener< PriceInfo >(){
	        @Override
	        public void onRequestFailure( SpiceException spiceException ) {
				data.message = spiceException.getMessage();
	        }

	        @Override
	        public void onRequestSuccess( final PriceInfo result ) {
	        	if (result != null) {
	        		int cardPrice = (foilOverride ? FOIL_PRICE : priceSetting);
	        		
	        		switch(cardPrice) {
		        		case LOW_PRICE:
		        		{
		        			data.price = (int) (result.low * 100);
		        			break;
		        		}
		        		default:
		        		case AVG_PRICE:
		        		{
		        			data.price = (int) (result.average * 100);
		        			break;
		        		}
		        		case HIGH_PRICE:
		        		{
		        			data.price = (int) (result.high * 100);
		        			break;
		        		}
		        		case FOIL_PRICE:
		        		{
		        			data.price = (int) (result.foil_average * 100);
		        			break;
		        		}
	        		}
	        		data.message = null;
				}
	        	else {
	        		data.message = getString(R.string.trader_no_price);
	        	}
				
				UpdateTotalPrices();
				adapter.notifyDataSetChanged();
	        }
		} );
	}
}