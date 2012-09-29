package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.fragments.FamiliarFragment;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers.CardData;

public class WishlistHelpers {
	private static final String wishlistName = "card.wishlist";

	public static final int DONE = 1;
	public static final int CANCEL = 2;
	
	public static void WriteWishlist(Context mCtx, ArrayList<CardData> lWishlist) {
		try {
			FileOutputStream fos = mCtx.openFileOutput(wishlistName,
					Context.MODE_PRIVATE);

			for (int i = lWishlist.size() - 1; i >= 0; i--) {
				fos.write(lWishlist.get(i).toString().getBytes());
			}
			fos.close();
		} catch (FileNotFoundException e) {
			Toast.makeText(mCtx, "FileNotFoundException", Toast.LENGTH_LONG)
					.show();
		} catch (IOException e) {
			Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
		}
	}

	public static String GetReadableWishlist(
			ArrayList<ArrayList<CardData>> cardSetWishlists,
			boolean includeTcgName) {
		StringBuilder readableWishlist = new StringBuilder();
		for (ArrayList<CardData> cardlist : cardSetWishlists)
			for (CardData card : cardlist)
				if (card.numberOf > 0)
					readableWishlist.append(card
							.toReadableString(includeTcgName));
		return readableWishlist.toString();
	}

	public static void AppendCard(Context mCtx, CardData card) {
		try {
			FileOutputStream fos = mCtx.openFileOutput(wishlistName,
					Context.MODE_APPEND);
			fos.write(card.toString().getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			Toast.makeText(mCtx, "FileNotFoundException", Toast.LENGTH_LONG)
					.show();
		} catch (IOException e) {
			Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
		}
	}

	public static void ResetCards(Context mCtx, String newName,
			ArrayList<CardData> newCards, CardDbAdapter mDbHelper) {

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

				BufferedReader br = new BufferedReader(new InputStreamReader(
						mCtx.openFileInput(wishlistName)));
				String line;
				String[] parts;
				TradeListHelpers tlh = new TradeListHelpers();

				while ((line = br.readLine()) != null) {
					parts = line.split(CardData.delimiter);

					String cardName = parts[0];
					String cardSet = parts[1];
					int numberOf = Integer.parseInt(parts[2]);
					String number = parts.length < 4 ? null : parts[3];
					int rarity = parts.length < 5 ? '-' : Integer
							.parseInt(parts[4]);

					// Build the wishlist, ignoring any cards we are currently
					// updating
					if (!cardName.equalsIgnoreCase(newName)) {
						CardData cd = (tlh).new CardData(cardName, cardSet,
								numberOf, number, rarity);
						if (rarity == '-' || number == null)
							cd = TradeListHelpers.FetchCardData(cd, mDbHelper);
						wishlist.add(cd);
					}
				}
			} catch (NumberFormatException e) {
				Toast.makeText(mCtx, "NumberFormatException", Toast.LENGTH_LONG)
						.show();
			} catch (IOException e) {
				Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
			}
		} else {
			// wishlist doesnt exist
		}

		try {
			FileOutputStream fos = mCtx.openFileOutput(wishlistName,
					Context.MODE_PRIVATE);

			for (int i = newCards.size() - 1; i >= 0; i--) {
				fos.write(newCards.get(i).toString().getBytes());
			}
			for (int i = wishlist.size() - 1; i >= 0; i--) {
				fos.write(wishlist.get(i).toString().getBytes());
			}
			fos.close();
		} catch (FileNotFoundException e) {
			Toast.makeText(mCtx, "FileNotFoundException", Toast.LENGTH_LONG)
					.show();
		} catch (IOException e) {
			Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
		}
	}

	public static void ReadWishlist(Context mCtx, CardDbAdapter mDbHelper, ArrayList<CardData> lWishlist) {
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
				BufferedReader br = new BufferedReader(new InputStreamReader(
						mCtx.openFileInput(wishlistName)));

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
					} catch (Exception e) {
					}
					int numberOf = Integer.parseInt(parts[2]);
					String number = parts.length < 4 ? null : parts[3];
					int rarity = parts.length < 5 ? '-' : Integer
							.parseInt(parts[4]);

					CardData cd = tradeListHelper.new CardData(cardName,
							tcgName, cardSet, numberOf, 0, "loading", number,
							rarity);
					if (rarity == '-' || number == null)
						cd = TradeListHelpers.FetchCardData(cd, mDbHelper);
					lWishlist.add(0, cd);
				}
			} catch (NumberFormatException e) {
				Toast.makeText(mCtx, "NumberFormatException", Toast.LENGTH_LONG)
						.show();
			} catch (IOException e) {
				Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
			}
		}
	}

	// Variables for the dialog. Like the highlander, there should only be one
	public int dismissReason = 0;
	LinearLayout lvSets;
	public ArrayList<CardData> lCardlist;
	String cardName;
	FamiliarFragment ff;
	MainActivity ma;

	public void onDialogDismissed(){
		switch (dismissReason) {
		case CANCEL:
			// this will refill the dialog with the values in the
			// wishlist otherwise the changed values will persist in the
			// dialog even if they aren't saved
			fillWishlistDialog(null);
			bindWishlistRows();
			break;
		case DONE:
		default:
			ArrayList<CardData> newCards = new ArrayList<CardData>();
	
			for (int i = 0; i < lvSets.getChildCount(); i++) {
				View v = lvSets.getChildAt(i);
				int numberField;
				try {
					numberField = Integer.valueOf(((EditText) v
							.findViewById(R.id.numberInput)).getText()
							.toString());
				} catch (NumberFormatException e) {
					numberField = 0;
				}
				if (numberField != 0) {
					// return the CardData at that position
					CardData cd = (CardData) lCardlist.get(i);
					cd.numberOf = numberField;
					newCards.add(cd);
				}
			}
			WishlistHelpers.ResetCards(ma, cardName, newCards, ff.mDbHelper);
			break;
		}
	}
	
	public Dialog getDialog(String cn, final FamiliarFragment ff, MainActivity ma) {
		return getDialog(cn, ff, ma, null);
	}

	public Dialog getDialog(String cn, final FamiliarFragment ff,
			MainActivity ma, ArrayList<CardData> list) {

		this.ff = ff;
		this.ma = ma;
		cardName = cn;
		AlertDialog.Builder b = new AlertDialog.Builder(ma);
		// dialog = new Dialog(act);

		b.setTitle(cardName + " in the Wishlist");

		View view = ma.getLayoutInflater().inflate(
				R.layout.card_setwishlist_dialog, null);
		lvSets = (LinearLayout) view.findViewById(R.id.setList);
		b.setView(view);

		b.setPositiveButton(R.string.dialog_done,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dismissReason = DONE;
						dialog.dismiss();
					}
				});

		b.setNegativeButton(R.string.dialog_cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dismissReason = CANCEL;
						dialog.dismiss();
					}
				});

		AlertDialog dialog = b.create();

		fillWishlistDialog(list);
		bindWishlistRows();

		return dialog;
	}

	public void fillWishlistDialog(ArrayList<CardData> list) {
		lCardlist = new ArrayList<CardData>();
		lCardlist.clear();
		if(!ff.mDbHelper.mDb.isOpen()) {
			// this is a rotation, db is closed
			return;			
		}
		Cursor c = ff.mDbHelper.fetchCardByName(cardName,
				new String[] { CardDbAdapter.KEY_SET, CardDbAdapter.KEY_NUMBER, CardDbAdapter.KEY_RARITY });
		// make a place holder item for each version set of this card
		while (!c.isAfterLast()) {
			String setCode = c.getString(c
					.getColumnIndex(CardDbAdapter.KEY_SET));
			String tcgName = ff.mDbHelper.getTCGname(setCode);
			String number = c.getString(c
					.getColumnIndex(CardDbAdapter.KEY_NUMBER));
			int rarity = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY));

			lCardlist.add(new TradeListHelpers().new CardData(cardName,
					tcgName, setCode, 0, 0, "loading", number, rarity));
			c.moveToNext();
		}
		c.close();

		ArrayList<CardData> lWishlist = new ArrayList<CardData>();
		if(list == null){
			// Read the wishlist
			ReadWishlist(ma, ff.mDbHelper, lWishlist);
		}
		else
			//lWishlist = (ArrayList<CardData>) list.clone();
			lWishlist = new ArrayList<CardData>(list);

		// For each card in the wishlist
		for (int i = 0; i < lWishlist.size(); i++) {
			// Check each card to see if we're looking at it
			if (lWishlist.get(i).name.equalsIgnoreCase(cardName)) {
				// If we're looking at that card by name, find it's entry by
				// setcode
				for (int j = 0; j < lCardlist.size(); j++) {
					if (lCardlist.get(j).setCode.equalsIgnoreCase(lWishlist
							.get(i).setCode)) {
						// set the number, but don't modify the wishlist
						lCardlist.get(j).numberOf = lWishlist.get(i).numberOf;
					}
				}
			}
		}
	}

	public void bindWishlistRows() {
		lvSets.removeAllViews();

		for (CardData cd : lCardlist) {
			LayoutInflater inf = ma.getLayoutInflater();
			View v = inf.inflate(R.layout.card_setwishlist_row, null);

			EditText numberField = (EditText) v.findViewById(R.id.numberInput);
			TextView setField = (TextView) v.findViewById(R.id.cardset);

			numberField.setText(cd.numberOf + "");
			setField.setText(cd.tcgName);

			lvSets.addView(v);
		}
	}
}
