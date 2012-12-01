package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.FamiliarActivity;
import com.gelakinetic.mtgfam.activities.WishlistActivity;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers.CardData;

public class WishlistHelpers {
	private static final String	wishlistName	= "card.wishlist";

	private static final int		DONE					= 1;
	private static final int		CANCEL				= 2;

	public static void WriteWishlist(Context mCtx, ArrayList<CardData> lWishlist) {
		try {
			FileOutputStream fos = mCtx.openFileOutput(wishlistName, Context.MODE_PRIVATE);

			for (int i = lWishlist.size() - 1; i >= 0; i--) {
				fos.write(lWishlist.get(i).toString().getBytes());
			}
			fos.close();
		}
		catch (FileNotFoundException e) {
			Toast.makeText(mCtx, "FileNotFoundException", Toast.LENGTH_LONG).show();
		}
		catch (IOException e) {
			Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
		}
	}

	public static void AppendCard(Context mCtx, CardData card) {
		try {
			FileOutputStream fos = mCtx.openFileOutput(wishlistName, Context.MODE_APPEND);
			fos.write(card.toString().getBytes());
			fos.close();
		}
		catch (FileNotFoundException e) {
			Toast.makeText(mCtx, "FileNotFoundException", Toast.LENGTH_LONG).show();
		}
		catch (IOException e) {
			Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
		}
	}

	public static void ResetCards(Context mCtx, String newName, ArrayList<CardData> newCards) {

		ArrayList<CardData> wishlist = new ArrayList<CardData>();

		String[] files = mCtx.fileList();
		Boolean wishlistExists = false;
		for (String fileName : files) {
			if (fileName.equals(wishlistName)) {
				wishlistExists = true;
			}
		}

		if (wishlistExists) {
			try {

				BufferedReader br = new BufferedReader(new InputStreamReader(mCtx.openFileInput(wishlistName)));
				String line;
				String[] parts;
				TradeListHelpers tlh = new TradeListHelpers();

				while ((line = br.readLine()) != null) {
					parts = line.split(CardData.delimiter);

					String cardName = parts[0];
					String cardSet = parts[1];
					int numberOf = Integer.parseInt(parts[2]);

					// Build the wishlist, ignoring any cards we are currently updating
					if (!cardName.equalsIgnoreCase(newName)) {
						CardData cd = (tlh).new CardData(cardName, cardSet, numberOf);
						wishlist.add(cd);
					}
				}
			}
			catch (NumberFormatException e) {
				Toast.makeText(mCtx, "NumberFormatException", Toast.LENGTH_LONG).show();
			}
			catch (IOException e) {
				Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
			}
		}
		else {
			// wishlist doesnt exist
		}

		try {
			FileOutputStream fos = mCtx.openFileOutput(wishlistName, Context.MODE_PRIVATE);

			for (int i = newCards.size() - 1; i >= 0; i--) {
				fos.write(newCards.get(i).toString().getBytes());
			}
			for (int i = wishlist.size() - 1; i >= 0; i--) {
				fos.write(wishlist.get(i).toString().getBytes());
			}
			fos.close();
		}
		catch (FileNotFoundException e) {
			Toast.makeText(mCtx, "FileNotFoundException", Toast.LENGTH_LONG).show();
		}
		catch (IOException e) {
			Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
		}
	}

	public static void ReadWishlist(Context mCtx, WishlistActivity activity, CardDbAdapter mDbHelper, ArrayList<CardData> lWishlist) {
		String[] files = mCtx.fileList();
		Boolean wishlistExists = false;
		for (String fileName : files) {
			if (fileName.equals(wishlistName)) {
				wishlistExists = true;
			}
		}
		if (wishlistExists) {
			lWishlist.clear();
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(mCtx.openFileInput(wishlistName)));

				String line;
				String[] parts;
				TradeListHelpers tradeListHelper = new TradeListHelpers();
				while ((line = br.readLine()) != null) {
					parts = line.split(CardData.delimiter);

					String cardName = parts[0];
					String cardSet = parts[1];
					String tcgName = "";
					try {
						tcgName = mDbHelper.getTCGname(cardSet);
					}
					catch (Exception e) {
					}
					int numberOf = Integer.parseInt(parts[2]);

					CardData cd = tradeListHelper.new CardData(cardName, tcgName, cardSet, numberOf, 0, "loading", null);
					lWishlist.add(0, cd);
				}
			}
			catch (NumberFormatException e) {
				Toast.makeText(mCtx, "NumberFormatException", Toast.LENGTH_LONG).show();
			}
			catch (IOException e) {
				Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
			}
		}
	}

	// Variables for the dialog. Like the highlander, there should only be one
	public int									dismissReason	= 0;
	Dialog							dialog;
	Context							mCtx;
	public ArrayList<CardData>	lCardlist;
	String							cardName;
	FamiliarActivity		act;

	public Dialog getDialog(String cn, final FamiliarActivity fa) {
		return getDialog(cn, fa, null);
	}
	public Dialog getDialog(String cn, final FamiliarActivity fa, ArrayList<CardData> list) {
		
		act = fa;
		cardName = cn;
		mCtx = act;
		dialog = new Dialog(act);

		dialog.setTitle(cardName + " in the Wishlist");

		dialog.setContentView(R.layout.card_setwishlist_dialog);

		dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface di) {
				switch (dismissReason) {
					case CANCEL:
						// this will refill the dialog with the values in the wishlist
						// otherwise the changed values will persist in the dialog even if
						// they arent saved
						fillWishlistDialog();
						bindWishlistRows();
						break;
					case DONE:
					default:
						LinearLayout lvSets = (LinearLayout) dialog.findViewById(R.id.setList);
						ArrayList<CardData> newCards = new ArrayList<CardData>();

						for (int i = 0; i < lvSets.getChildCount(); i++) {
							View v = lvSets.getChildAt(i);
							int numberField;
							try{
								numberField = Integer.valueOf(((EditText) v.findViewById(R.id.numberInput)).getText().toString());
							}
							catch(NumberFormatException e){
								numberField = 0;
							}
							if (numberField != 0) {
								CardData cd = (CardData) lCardlist.get(i); // returns the
																														// CardData at that
																														// position
								cd.numberOf = numberField;
								newCards.add(cd);
							}
						}
						WishlistHelpers.ResetCards(mCtx, cardName, newCards);
						break;
				}
			}
		});

		Button done = (Button) dialog.findViewById(R.id.done);
		done.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dismissReason = DONE;
				dialog.dismiss();
			}
		});
		Button cancel = (Button) dialog.findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dismissReason = CANCEL;
				dialog.dismiss();
			}
		});

		if(list == null){
			lCardlist = new ArrayList<CardData>();
			fillWishlistDialog();
		}
		else lCardlist = (ArrayList<CardData>) list.clone();
		bindWishlistRows();

		return dialog;
	}

	public void fillWishlistDialog() {
		lCardlist.clear();
		Cursor c = act.mDbHelper.fetchCardByName(cardName);
		// make a place holder item for each version set of this card
		while (!c.isAfterLast()) {
			String setCode = c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET));
			String tcgName = act.mDbHelper.getTCGname(setCode);

			lCardlist.add(new TradeListHelpers().new CardData(cardName, tcgName, setCode, 0, 0, "loading", null));
			c.moveToNext();
		}
		c.deactivate();
		c.close();

		// Read the wishlist
		ArrayList<CardData> lWishlist = new ArrayList<CardData>();
		ReadWishlist(act, null, act.mDbHelper, lWishlist);

		// For each card in the wishlist
		for (int i = 0; i < lWishlist.size(); i++) {
			// Check each card to see if we're looking at it
			if (lWishlist.get(i).name.equalsIgnoreCase(cardName)) {
				// If we're looking at that card by name, find it's entry by setcode
				for (int j = 0; j < lCardlist.size(); j++) {
					if (lCardlist.get(j).setCode.equalsIgnoreCase(lWishlist.get(i).setCode)) {
						// set the number, but don't modify the wishlist
						lCardlist.get(j).numberOf = lWishlist.get(i).numberOf;
					}
				}
			}
		}
	}
	public void bindWishlistRows(){
		LinearLayout lvSets = (LinearLayout) dialog.findViewById(R.id.setList);
		lvSets.removeAllViews();

		for (CardData cd : lCardlist) {
			LayoutInflater inf = act.getLayoutInflater();
			View v = inf.inflate(R.layout.card_setwishlist_row, null);

			EditText numberField = (EditText) v.findViewById(R.id.numberInput);
			TextView setField = (TextView) v.findViewById(R.id.cardset);

			numberField.setText(cd.numberOf + "");
			setField.setText(cd.tcgName);

			lvSets.addView(v);
		}		
	}
}
