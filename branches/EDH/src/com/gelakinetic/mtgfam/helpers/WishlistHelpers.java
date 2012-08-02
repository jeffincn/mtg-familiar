package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.gelakinetic.mtgfam.activities.WishlistActivity;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers.CardData;
import com.gelakinetic.mtgfam.helpers.TradeListHelpers.FetchPriceTask;

public class WishlistHelpers {
	private static final String wishlistName = "card.wishlist";

	public static void WriteWishlist(Context mCtx, ArrayList<CardData> lWishlist){
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

	public static void AppendCard(Context mCtx, CardData card){
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

	public static void ReadWishlist(Context mCtx, WishlistActivity activity, CardDbAdapter mDbHelper, ArrayList<CardData> lWishlist, ArrayAdapter<CardData> _toNotify){
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
					String tcgName ="";
					try{
						tcgName = mDbHelper.getTCGname(cardSet);
					}catch(Exception e){}
					int numberOf = Integer.parseInt(parts[2]);

					CardData cd = tradeListHelper.new CardData(cardName, tcgName, cardSet, numberOf, 0, "loading", null, null, null, null, null, null,
							CardDbAdapter.NOONECARES, -1);
					lWishlist.add(0, cd);
					if(_toNotify != null){
						FetchPriceTask loadPrice = tradeListHelper.new FetchPriceTask(lWishlist.get(0), _toNotify, activity.priceSetting, null, activity);
						loadPrice.execute();
					}
				}
			}
			catch (NumberFormatException e) {
				Toast.makeText(mCtx, "NumberFormatException", Toast.LENGTH_LONG).show();
			}
			catch (IOException e) {
				Toast.makeText(mCtx, "IOException", Toast.LENGTH_LONG).show();
			}
			if(_toNotify != null)
				_toNotify.notifyDataSetChanged();
		}
	}
}
