package com.gelakinetic.mtgfam.helpers;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;

import com.gelakinetic.mtgfam.helpers.TCGPlayerXMLHandler.FetchPriceTask;

public class TradeListHelpers {

	public static final String	card_not_found		= "Card Not Found";
	public static final String	mangled_url				= "Mangled URL";
	public static final String	database_busy			= "Database Busy";
	public static final String	fetch_failed			= "Fetch Failed";
	public static final String	familiarDbException		= "FamiliarDbException";
	
	public static CardData FetchCardData(CardData _data, CardDbAdapter mDbHelper) throws FamiliarDbException {
		CardData data = _data;
		try {
			Cursor card;
			boolean opened = false;
			if(!mDbHelper.mDb.isOpen()) {
				mDbHelper.openReadable();
				opened = true;
			}

			if (data.setCode == null || data.setCode.equals(""))
				card = mDbHelper.fetchCardByName(data.name, CardDbAdapter.allData);
			else
				card = mDbHelper.fetchCardByNameAndSet(data.name, data.setCode);

			if(opened){
				mDbHelper.close();
			}
			if (card.moveToFirst()) {
				data.name = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NAME));
				data.setCode = card.getString(card.getColumnIndex(CardDbAdapter.KEY_SET));
				data.tcgName = mDbHelper.getTCGname(data.setCode);
				data.type = card.getString(card.getColumnIndex(CardDbAdapter.KEY_TYPE));
				data.cost = card.getString(card.getColumnIndex(CardDbAdapter.KEY_MANACOST));
				data.ability = card.getString(card.getColumnIndex(CardDbAdapter.KEY_ABILITY));
				data.power = card.getString(card.getColumnIndex(CardDbAdapter.KEY_POWER));
				data.toughness = card.getString(card.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
				data.loyalty = card.getInt(card.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
				data.rarity = card.getInt(card.getColumnIndex(CardDbAdapter.KEY_RARITY));
				data.cardNumber = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NUMBER));
			}
			card.close();
		}
		catch (SQLiteException e) {
			data.message = card_not_found;
		}
		catch (IllegalStateException e) {
			data.message = database_busy;
		}
		return data;
	}

	public static final int MAX_SIMULTANEOUS_THREADS = 9; // could be 10, but leaves space for when one is winding down while the next is starting
	public static LinkedBlockingQueue<FetchPriceTask> pendingTasks = new LinkedBlockingQueue<FetchPriceTask>();
	public static ArrayBlockingQueue<FetchPriceTask> currentExecutingTasks = new ArrayBlockingQueue<FetchPriceTask>(MAX_SIMULTANEOUS_THREADS);
	
	public static void addTaskAndExecute(FetchPriceTask fpt){
		pendingTasks.add(fpt);
		executeIfAvailableSpace();
	}
	
	@SuppressLint("NewApi")
	public static void executeIfAvailableSpace()
	{
		if(currentExecutingTasks.size() < MAX_SIMULTANEOUS_THREADS)
		{
			FetchPriceTask toExecute = pendingTasks.poll();
			if(toExecute != null){
				currentExecutingTasks.add(toExecute);
				

				boolean API_LEVEL_11 = android.os.Build.VERSION.SDK_INT > 11;

				if(API_LEVEL_11) {
					toExecute.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
				}
				else {
					toExecute.execute((Void[])null);
				}
			}
		}
	}
	
	public static void cancelAllTasks(){
		pendingTasks.clear();
		for(FetchPriceTask fpa : currentExecutingTasks){
			fpa.cancel(true);
		}
	}

	public class CardData implements Cloneable {

		public String	name;
		public String	cardNumber;
		public String	tcgName;
		public String	setCode;
		public int		numberOf;
		public int		price;			// In cents
		public String	message;
		public String	type;
		public String	cost;
		public String	ability;
		public String	power;
		public String	toughness;
		public int		loyalty;
		public int		rarity;

		public CardData(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, String type, String cost,
				String ability, String p, String t, int loyalty, int rarity) {
			this.name = name;
			this.cardNumber = number;
			this.setCode = setCode;
			this.tcgName = tcgName;
			this.numberOf = numberOf;
			this.price = price;
			this.message = message;
			this.type = type;
			this.cost = cost;
			this.ability = ability;
			this.power = p;
			this.toughness = t;
			this.loyalty = loyalty;
			this.rarity = rarity;
		}

		public CardData(String name, String tcgName, String setCode, int numberOf, int price, String message, String number, int rarity) {
			this.name = name;
			this.cardNumber = number;
			this.setCode = setCode;
			this.tcgName = tcgName;
			this.numberOf = numberOf;
			this.price = price;
			this.message = message;
			this.rarity = rarity;
		}

		public CardData(String cardName, String cardSet, int numberOf, String number, int rarity) {
			this.name = cardName;
			this.numberOf = numberOf;
			this.setCode = cardSet;
			this.cardNumber = number;
			this.rarity = rarity;
		}

		public String getPriceString() {
			return "$" + String.valueOf(this.price / 100) + "." + String.format("%02d", this.price % 100);
		}

		public boolean hasPrice() {
			return this.message == null || this.message.length() == 0;
		}

		public static final String	delimiter	= "%";

		public String toString() {
			return this.name + delimiter + this.setCode + delimiter + this.numberOf + delimiter + this.cardNumber + delimiter + this.rarity + '\n';
		}

		public String toString(int side) {
			return side + delimiter + this.name + delimiter + this.setCode + delimiter + this.numberOf + '\n';
		}

		public String toReadableString(boolean includeTcgName) {
			return String.valueOf(this.numberOf) + ' ' + this.name + (includeTcgName?" (" + this.tcgName + ')':"") + '\n';
		}

		public Object clone() { 
	        try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			} 
		} 
	}
}
