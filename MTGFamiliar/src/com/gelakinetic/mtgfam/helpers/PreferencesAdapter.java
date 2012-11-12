package com.gelakinetic.mtgfam.helpers;

import com.gelakinetic.mtgfam.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.RingtoneManager;
import android.preference.PreferenceManager;

/**
 * Brief guide to adding new preferences to this app:
 * 1. Choose a name for the preference and add it to the list of static final Strings
 * 2. Add getter/setter methods to this class for the new preference
 * 3. Channel all accesses to the preference through the new getter/setter methods
 * 4. ???
 * 5. Profit (or at least sanity)!
 */

public class PreferencesAdapter {

	private Context context;
	private SharedPreferences prefs;
	private Editor edit;
		
	private static final String LAST_VERSION = "lastVersion"; //int, default 0
	private static final String LAST_LEGALITY_UPDATE = "lastLegalityUpdate"; //int, default 0
	private static final String WHITE_MANA = "whiteMana"; //int, default 0
	private static final String BLUE_MANA = "blueMana"; //int, default 0
	private static final String BLACK_MANA = "blackMana"; //int, default 0
	private static final String RED_MANA = "redMana"; //int, default 0
	private static final String GREEN_MANA = "greenMana"; //int, default 0
	private static final String COLORLESS_MANA = "colorlessMana"; //int, default 0
	private static final String SPELL_COUNT = "spellCount"; //int, default 0
	
	private static final String LAST_RULES_UPDATE = "lastRulesUpdate"; //long, default BuildDate.get(context).getTime()
	
	private static final String TTS_SHOW_DIALOG = "ttsShowDialog"; //boolean, default true
	private static final String AUTO_UPDATE = "autoupdate"; //boolean, default true
	private static final String CONSOLIDATE_SEARCH = "consolidateSearch"; //boolean, default true
	private static final String PIC_FIRST = "picFirst"; //boolean, default false
	private static final String SCROLL_RESULTS = "scrollresults"; //boolean, default false
	private static final String D2_AS_COIN = "d2AsCoin"; //boolean, default true
	private static final String WAKELOCK = "wakelock"; //boolean, default true
	private static final String SET_PREF = "setPref"; //boolean, default true
	private static final String MANA_COST_PREF = "manacostPref"; //boolean, default true
	private static final String TYPE_PREF = "typePref"; //boolean, default true
	private static final String ABILITY_PREF = "abilityPref"; //boolean, default true
	private static final String PT_PREF = "ptPref"; //boolean, default true
	private static final String FIFTEEN_MINUTE_PREF = "fifteenMinutePref"; //boolean, default false
	private static final String TEN_MINUTE_PREF = "tenMinutePref"; //boolean, default false
	private static final String FIVE_MINUTE_PREF = "fiveMinutePref"; //boolean, default false
	private static final String SHOW_TOTAL_WISHLIST_PRICE = "showTotalPriceWishlistPref"; //boolean, default false
	private static final String SHOW_INDIVIDUAL_WISHLIST_PRICES = "showIndividualPricesWishlistPref"; //boolean, default true
	private static final String VERBOSE_WISHLIST = "verboseWishlistPref"; //boolean, default false
	private static final String MOJHOSTO_FIRST_TIME = "mojhostoFirstTime"; //boolean, default true
	
	private static final String UPDATE_FREQUENCY = "updatefrequency"; //String, default "3"
	private static final String DEFAULT_FRAGMENT = "defaultFragment"; //String, default R.string.main_card_search
	private static final String CARD_LANGUAGE = "cardlanguage"; //String, default "en"
	private static final String DISPLAY_MODE = "displayMode"; //String, default "0"
	private static final String PLAYER_DATA = "player_data"; //String, default null
	private static final String ROUND_LENGTH = "roundLength"; //String, default "50"
	private static final String TIMER_SOUND = "timerSound"; //String, default RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
	private static final String TRADE_PRICE = "tradePrice"; //String, default "1"
	private static final String LEGALITY_DATE = "date"; //String, default null
	private static final String LAST_UPDATE = "lastUpdate"; //String, default ""
	private static final String LAST_TCGNAME_UPDATE = "lastTCGNameUpdate"; //String, default ""
	
	public PreferencesAdapter(Context context) {
		this.context = context;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.edit = this.prefs.edit();
	}
	
	/* Int preferences */
	//Last version
	private boolean lastVersionLoaded = false;
	private int lastVersion;
	
	public synchronized int getLastVersion() {
		if(!this.lastVersionLoaded) {
			this.lastVersion = this.prefs.getInt(LAST_VERSION, 0);
			this.lastVersionLoaded = true;
		}
		return this.lastVersion;
	}
	
	public synchronized void setLastVersion(int lastVersion) {
		this.lastVersion = lastVersion;
		this.lastVersionLoaded = true;
		
		this.edit.putInt(LAST_VERSION, lastVersion);
		this.edit.commit();
	}
	
	//Last legality update
	private boolean lastLegalityUpdateLoaded = false;
	private int lastLegalityUpdate;
	
	public synchronized int getLastLegalityUpdate() {
		if(!this.lastLegalityUpdateLoaded) {
			this.lastLegalityUpdate = this.prefs.getInt(LAST_LEGALITY_UPDATE, 0);
			this.lastLegalityUpdateLoaded = true;
		}
		return this.lastLegalityUpdate;
	}
	
	public synchronized void setLastLegalityUpdate(int lastLegalityUpdate) {
		this.lastLegalityUpdate = lastLegalityUpdate;
		this.lastLegalityUpdateLoaded = true;
		
		this.edit.putInt(LAST_LEGALITY_UPDATE, lastLegalityUpdate);
		this.edit.commit();
	}
	
	//White mana
	private boolean whiteManaLoaded = false;
	private int whiteMana;
	
	public synchronized int getWhiteMana() {
		if(!this.whiteManaLoaded) {
			this.whiteMana = this.prefs.getInt(WHITE_MANA, 0);
			this.whiteManaLoaded = true;
		}
		return this.whiteMana;
	}
	
	public synchronized void setWhiteMana(int whiteMana) {
		this.whiteMana = whiteMana;
		this.whiteManaLoaded = true;
		
		this.edit.putInt(WHITE_MANA, whiteMana);
		this.edit.commit();
	}
	
	//Blue mana
	private boolean blueManaLoaded = false;
	private int blueMana;
	
	public synchronized int getBlueMana() {
		if(!this.blueManaLoaded) {
			this.blueMana = this.prefs.getInt(BLUE_MANA, 0);
			this.blueManaLoaded = true;
		}
		return this.blueMana;
	}
	
	public synchronized void setBlueMana(int blueMana) {
		this.blueMana = blueMana;
		this.blueManaLoaded = true;
		
		this.edit.putInt(BLUE_MANA, blueMana);
		this.edit.commit();
	}
	
	//Black mana
	private boolean blackManaLoaded = false;
	private int blackMana;
	
	public synchronized int getBlackMana() {
		if(!this.blackManaLoaded) {
			this.blackMana = this.prefs.getInt(BLACK_MANA, 0);
			this.blackManaLoaded = true;
		}
		return this.blackMana;
	}
	
	public synchronized void setBlackMana(int blackMana) {
		this.blackMana = blackMana;
		this.blackManaLoaded = true;
		
		this.edit.putInt(BLACK_MANA, blackMana);
		this.edit.commit();
	}
	
	//Red mana
	private boolean redManaLoaded = false;
	private int redMana;
	
	public synchronized int getRedMana() {
		if(!this.redManaLoaded) {
			this.redMana = this.prefs.getInt(RED_MANA, 0);
			this.redManaLoaded = true;
		}
		return this.redMana;
	}
	
	public synchronized void setRedMana(int redMana) {
		this.redMana = redMana;
		this.redManaLoaded = true;
		
		this.edit.putInt(RED_MANA, redMana);
		this.edit.commit();
	}
	
	//Green mana
	private boolean greenManaLoaded = false;
	private int greenMana;
	
	public synchronized int getGreenMana() {
		if(!this.greenManaLoaded) {
			this.greenMana = this.prefs.getInt(GREEN_MANA, 0);
			this.greenManaLoaded = true;
		}
		return this.greenMana;
	}
	
	public synchronized void setGreenMana(int greenMana) {
		this.greenMana = greenMana;
		this.greenManaLoaded = true;
		
		this.edit.putInt(GREEN_MANA, greenMana);
		this.edit.commit();
	}
	
	//Colorless mana
	private boolean colorlessManaLoaded = false;
	private int colorlessMana;
	
	public synchronized int getColorlessMana() {
		if(!this.colorlessManaLoaded) {
			this.colorlessMana = this.prefs.getInt(COLORLESS_MANA, 0);
			this.colorlessManaLoaded = true;
		}
		return this.colorlessMana;
	}
	
	public synchronized void setColorlessMana(int colorlessMana) {
		this.colorlessMana = colorlessMana;
		this.colorlessManaLoaded = true;
		
		this.edit.putInt(COLORLESS_MANA, colorlessMana);
		this.edit.commit();
	}
	
	//Spell count
	private boolean spellCountLoaded = false;
	private int spellCount;
	
	public synchronized int getSpellCount() {
		if(!this.spellCountLoaded) {
			this.spellCount = this.prefs.getInt(SPELL_COUNT, 0);
			this.spellCountLoaded = true;
		}
		return this.spellCount;
	}
	
	public synchronized void setSpellCount(int spellCount) {
		this.spellCount = spellCount;
		this.spellCountLoaded = true;
		
		this.edit.putInt(SPELL_COUNT, spellCount);
		this.edit.commit();
	}
	
	/* Long preferences */
	//Last rules update
	private boolean lastRulesUpdateLoaded = false;
	private long lastRulesUpdate;
	
	public synchronized long getLastRulesUpdate() {
		if(!this.lastRulesUpdateLoaded) {
			this.lastRulesUpdate = this.prefs.getLong(LAST_RULES_UPDATE, BuildDate.get(this.context).getTime());
			this.lastRulesUpdateLoaded = true;
		}
		return this.lastRulesUpdate;
	}
	
	public synchronized void setLastRulesUpdate(long lastRulesUpdate) {
		this.lastRulesUpdate = lastRulesUpdate;
		this.lastRulesUpdateLoaded = true;
		
		this.edit.putLong(LAST_RULES_UPDATE, lastRulesUpdate);
		this.edit.commit();
	}
	
	
	/* Boolean preferences */
	//TTS show dialog
	private boolean ttsShowDialogLoaded = false;
	private boolean ttsShowDialog;
	
	public synchronized boolean getTtsShowDialog() {
		if(!this.ttsShowDialogLoaded) {
			this.ttsShowDialog = this.prefs.getBoolean(TTS_SHOW_DIALOG, true);
			this.ttsShowDialogLoaded = true;
		}
		return this.ttsShowDialog;
	}
	
	public synchronized void setTtsShowDialog(boolean ttsShowDialog) {
		this.ttsShowDialog = ttsShowDialog;
		this.ttsShowDialogLoaded = true;
		
		this.edit.putBoolean(TTS_SHOW_DIALOG, ttsShowDialog);
		this.edit.commit();
	}
	
	//Auto-update
	private boolean autoUpdateLoaded = false;
	private boolean autoUpdate;
	
	public synchronized boolean getAutoUpdate() {
		if(!this.autoUpdateLoaded) {
			this.autoUpdate = this.prefs.getBoolean(AUTO_UPDATE, true);
			this.autoUpdateLoaded = true;
		}
		return this.autoUpdate;
	}
	
	public synchronized void setAutoUpdate(boolean autoUpdate) {
		this.autoUpdate = autoUpdate;
		this.autoUpdateLoaded = true;
		
		this.edit.putBoolean(AUTO_UPDATE, autoUpdate);
		this.edit.commit();
	}
	
	//Consolidate search
	private boolean consolidateSearchLoaded = false;
	private boolean consolidateSearch;
	
	public synchronized boolean getConsolidateSearch() {
		if(!this.consolidateSearchLoaded) {
			this.consolidateSearch = this.prefs.getBoolean(CONSOLIDATE_SEARCH, true);
			this.consolidateSearchLoaded = true;
		}
		return this.consolidateSearch;
	}
	
	public synchronized void setConsolidateSearch(boolean consolidateSearch) {
		this.consolidateSearch = consolidateSearch;
		this.consolidateSearchLoaded = true;
		
		this.edit.putBoolean(CONSOLIDATE_SEARCH, consolidateSearch);
		this.edit.commit();
	}
	
	//Pic first
	private boolean picFirstLoaded = false;
	private boolean picFirst;
	
	public synchronized boolean getPicFirst() {
		if(!this.picFirstLoaded) {
			this.picFirst = this.prefs.getBoolean(PIC_FIRST, false);
			this.picFirstLoaded = true;
		}
		return this.picFirst;
	}
	
	public synchronized void setPicFirst(boolean picFirst) {
		this.picFirst = picFirst;
		this.picFirstLoaded = true;
		
		this.edit.putBoolean(PIC_FIRST, picFirst);
		this.edit.commit();
	}
	
	//Scroll results
	private boolean scrollResultsLoaded = false;
	private boolean scrollResults;
	
	public synchronized boolean getScrollResults() {
		if(!this.scrollResultsLoaded) {
			this.scrollResults = this.prefs.getBoolean(SCROLL_RESULTS, false);
			this.scrollResultsLoaded = true;
		}
		return this.scrollResults;
	}
	
	public synchronized void setScrollResults(boolean scrollResults) {
		this.scrollResults = scrollResults;
		this.scrollResultsLoaded = true;
		
		this.edit.putBoolean(SCROLL_RESULTS, scrollResults);
		this.edit.commit();
	}
	
	//D2 as coin
	private boolean d2AsCoinLoaded = false;
	private boolean d2AsCoin;
	
	public synchronized boolean getD2AsCoin() {
		if(!this.d2AsCoinLoaded) {
			this.d2AsCoin = this.prefs.getBoolean(D2_AS_COIN, true);
			this.d2AsCoinLoaded = true;
		}
		return this.d2AsCoin;
	}
	
	public synchronized void setD2AsCoin(boolean d2AsCoin) {
		this.d2AsCoin = d2AsCoin;
		this.d2AsCoinLoaded = true;
		
		this.edit.putBoolean(D2_AS_COIN, d2AsCoin);
		this.edit.commit();
	}
	
	//Wakelock
	private boolean wakelockLoaded = false;
	private boolean wakelock;
	
	public synchronized boolean getWakelock() {
		if(!this.wakelockLoaded) {
			this.wakelock = this.prefs.getBoolean(WAKELOCK, true);
			this.wakelockLoaded = true;
		}
		return this.wakelock;
	}
	
	public synchronized void setWakelock(boolean wakelock) {
		this.wakelock = wakelock;
		this.wakelockLoaded = true;
		
		this.edit.putBoolean(WAKELOCK, wakelock);
		this.edit.commit();
	}
	
	//Set pref
	private boolean setPrefLoaded = false;
	private boolean setPref;
	
	public synchronized boolean getSetPref() {
		if(!this.setPrefLoaded) {
			this.setPref = this.prefs.getBoolean(SET_PREF, true);
			this.setPrefLoaded = true;
		}
		return this.setPref;
	}
	
	public synchronized void setSetPref(boolean setPref) {
		this.setPref = setPref;
		this.setPrefLoaded = true;
		
		this.edit.putBoolean(SET_PREF, setPref);
		this.edit.commit();
	}
	
	//Mana cost pref
	private boolean manaCostPrefLoaded = false;
	private boolean manaCostPref;
	
	public synchronized boolean getManaCostPref() {
		if(!this.manaCostPrefLoaded) {
			this.manaCostPref = this.prefs.getBoolean(MANA_COST_PREF, true);
			this.manaCostPrefLoaded = true;
		}
		return this.manaCostPref;
	}
	
	public synchronized void setManaCostPref(boolean manaCostPref) {
		this.manaCostPref = manaCostPref;
		this.manaCostPrefLoaded = true;
		
		this.edit.putBoolean(MANA_COST_PREF, manaCostPref);
		this.edit.commit();
	}
	
	//Type pref
	private boolean typePrefLoaded = false;
	private boolean typePref;
	
	public synchronized boolean getTypePref() {
		if(!this.typePrefLoaded) {
			this.typePref = this.prefs.getBoolean(TYPE_PREF, true);
			this.typePrefLoaded = true;
		}
		return this.typePref;
	}
	
	public synchronized void setTypePref(boolean typePref) {
		this.typePref = typePref;
		this.typePrefLoaded = true;
		
		this.edit.putBoolean(TYPE_PREF, typePref);
		this.edit.commit();
	}
	
	//Ability pref
	private boolean abilityPrefLoaded = false;
	private boolean abilityPref;
	
	public synchronized boolean getAbilityPref() {
		if(!this.abilityPrefLoaded) {
			this.abilityPref = this.prefs.getBoolean(ABILITY_PREF, true);
			this.abilityPrefLoaded = true;
		}
		return this.abilityPref;
	}
	
	public synchronized void setAbilityPref(boolean abilityPref) {
		this.abilityPref = abilityPref;
		this.abilityPrefLoaded = true;
		
		this.edit.putBoolean(ABILITY_PREF, abilityPref);
		this.edit.commit();
	}
	
	//P/T pref
	private boolean ptPrefLoaded = false;
	private boolean ptPref;
	
	public synchronized boolean getPTPref() {
		if(!this.ptPrefLoaded) {
			this.ptPref = this.prefs.getBoolean(PT_PREF, true);
			this.ptPrefLoaded = true;
		}
		return this.ptPref;
	}
	
	public synchronized void setPTPref(boolean ptPref) {
		this.ptPref = ptPref;
		this.ptPrefLoaded = true;
		
		this.edit.putBoolean(PT_PREF, ptPref);
		this.edit.commit();
	}
	
	//15-minute warning pref
	private boolean fifteenMinutePrefLoaded = false;
	private boolean fifteenMinutePref;
	
	public synchronized boolean getFifteenMinutePref() {
		if(!this.fifteenMinutePrefLoaded) {
			this.fifteenMinutePref = this.prefs.getBoolean(FIFTEEN_MINUTE_PREF, false);
			this.fifteenMinutePrefLoaded = true;
		}
		return this.fifteenMinutePref;
	}
	
	public synchronized void setFifteenMinutePref(boolean fifteenMinutePref) {
		this.fifteenMinutePref = fifteenMinutePref;
		this.fifteenMinutePrefLoaded = true;
		
		this.edit.putBoolean(FIFTEEN_MINUTE_PREF, fifteenMinutePref);
		this.edit.commit();
	}
	
	//10-minute warning pref
	private boolean tenMinutePrefLoaded = false;
	private boolean tenMinutePref;
	
	public synchronized boolean getTenMinutePref() {
		if(!this.tenMinutePrefLoaded) {
			this.tenMinutePref = this.prefs.getBoolean(TEN_MINUTE_PREF, false);
			this.tenMinutePrefLoaded = true;
		}
		return this.tenMinutePref;
	}
	
	public synchronized void setTenMinutePref(boolean tenMinutePref) {
		this.tenMinutePref = tenMinutePref;
		this.tenMinutePrefLoaded = true;
		
		this.edit.putBoolean(TEN_MINUTE_PREF, tenMinutePref);
		this.edit.commit();
	}
	
	//5-minute warning pref
	private boolean fiveMinutePrefLoaded = false;
	private boolean fiveMinutePref;
	
	public synchronized boolean getFiveMinutePref() {
		if(!this.fiveMinutePrefLoaded) {
			this.fiveMinutePref = this.prefs.getBoolean(FIVE_MINUTE_PREF, false);
			this.fiveMinutePrefLoaded = true;
		}
		return this.fiveMinutePref;
	}
	
	public synchronized void setFiveMinutePref(boolean fiveMinutePref) {
		this.fiveMinutePref = fiveMinutePref;
		this.fiveMinutePrefLoaded = true;
		
		this.edit.putBoolean(FIVE_MINUTE_PREF, fiveMinutePref);
		this.edit.commit();
	}
	
	//Show total wishlist price
	private boolean showTotalWishlistPriceLoaded = false;
	private boolean showTotalWishlistPrice;
	
	public synchronized boolean getShowTotalWishlistPrice() {
		if(!this.showTotalWishlistPriceLoaded) {
			this.showTotalWishlistPrice = this.prefs.getBoolean(SHOW_TOTAL_WISHLIST_PRICE, false);
			this.showTotalWishlistPriceLoaded = true;
		}
		return this.showTotalWishlistPrice;
	}
	
	public synchronized void setShowTotalWishlistPrice(boolean showTotalWishlistPrice) {
		this.showTotalWishlistPrice = showTotalWishlistPrice;
		this.showTotalWishlistPriceLoaded = true;
		
		this.edit.putBoolean(SHOW_TOTAL_WISHLIST_PRICE, showTotalWishlistPrice);
		this.edit.commit();
	}
	
	//Show individual wishlist prices
	private boolean showIndividualWishlistPricesLoaded = false;
	private boolean showIndividualWishlistPrices;
	
	public synchronized boolean getShowIndividualWishlistPrices() {
		if(!this.showIndividualWishlistPricesLoaded) {
			this.showIndividualWishlistPrices = this.prefs.getBoolean(SHOW_INDIVIDUAL_WISHLIST_PRICES, true);
			this.showIndividualWishlistPricesLoaded = true;
		}
		return this.showIndividualWishlistPrices;
	}
	
	public synchronized void setShowIndividualWishlistPrices(boolean showIndividualWishlistPrices) {
		this.showIndividualWishlistPrices = showIndividualWishlistPrices;
		this.showIndividualWishlistPricesLoaded = true;
		
		this.edit.putBoolean(SHOW_INDIVIDUAL_WISHLIST_PRICES, showIndividualWishlistPrices);
		this.edit.commit();
	}
	
	//Verbose wishlist
	private boolean verboseWishlistLoaded = false;
	private boolean verboseWishlist;
	
	public synchronized boolean getVerboseWishlist() {
		if(!this.verboseWishlistLoaded) {
			this.verboseWishlist = this.prefs.getBoolean(VERBOSE_WISHLIST, false);
			this.verboseWishlistLoaded = true;
		}
		return this.verboseWishlist;
	}
	
	public synchronized void setVerboseWishlist(boolean verboseWishlist) {
		this.verboseWishlist = verboseWishlist;
		this.verboseWishlistLoaded = true;
		
		this.edit.putBoolean(VERBOSE_WISHLIST, verboseWishlist);
		this.edit.commit();
	}
	
	//MoJhoSto first time
	private boolean mojhostoFirstTimeLoaded = false;
	private boolean mojhostoFirstTime;
	
	public synchronized boolean getMojhostoFirstTime() {
		if(!this.mojhostoFirstTimeLoaded) {
			this.mojhostoFirstTime = this.prefs.getBoolean(MOJHOSTO_FIRST_TIME, true);
			this.mojhostoFirstTimeLoaded = true;
		}
		return this.mojhostoFirstTime;
	}
	
	public synchronized void setMojhostoFirstTime(boolean mojhostoFirstTime) {
		this.mojhostoFirstTime = mojhostoFirstTime;
		this.mojhostoFirstTimeLoaded = true;
		
		this.edit.putBoolean(MOJHOSTO_FIRST_TIME, mojhostoFirstTime);
		this.edit.commit();
	}
	
	
	/* String preferences */
	//Update frequency
	private boolean updateFrequencyLoaded = false;
	private String updateFrequency;
	
	public synchronized String getUpdateFrequency() {
		if(!this.updateFrequencyLoaded) {
			this.updateFrequency = this.prefs.getString(UPDATE_FREQUENCY, "3");
			this.updateFrequencyLoaded = true;
		}
		return this.updateFrequency;
	}
	
	public synchronized void setUpdateFrequency(String updateFrequency) {
		this.updateFrequency = updateFrequency;
		this.updateFrequencyLoaded = true;
		
		this.edit.putString(UPDATE_FREQUENCY, updateFrequency);
		this.edit.commit();
	}
	
	//Card language
	private boolean cardLanguageLoaded = false;
	private String cardLanguage;
	
	public synchronized String getCardLanguage() {
		if(!this.cardLanguageLoaded) {
			this.cardLanguage = this.prefs.getString(CARD_LANGUAGE, "en");
			this.cardLanguageLoaded = true;
		}
		return this.cardLanguage;
	}
	
	public synchronized void setCardLanguage(String cardLanguage) {
		this.cardLanguage = cardLanguage;
		this.cardLanguageLoaded = true;
		
		this.edit.putString(CARD_LANGUAGE, cardLanguage);
		this.edit.commit();
	}

	//Default fragment
	private boolean defaultFragmentLoaded = false;
	private String defaultFragment;
	
	public synchronized String getDefaultFragment() {
		if(!this.defaultFragmentLoaded) {
			this.defaultFragment = this.prefs.getString(DEFAULT_FRAGMENT, this.context.getString(R.string.main_card_search));
			this.defaultFragmentLoaded = true;
		}
		return this.defaultFragment;
	}
	
	public synchronized void setDefaultFragment(String defaultFragment) {
		this.defaultFragment = defaultFragment;
		this.defaultFragmentLoaded = true;
		
		this.edit.putString(DEFAULT_FRAGMENT, defaultFragment);
		this.edit.commit();
	}
	
	//Display mode
	private boolean displayModeLoaded = false;
	private String displayMode;
	
	public synchronized String getDisplayMode() {
		if(!this.displayModeLoaded) {
			this.displayMode = this.prefs.getString(DISPLAY_MODE, "0");
			this.displayModeLoaded = true;
		}
		return this.displayMode;
	}
	
	public synchronized void setDisplayMode(String displayMode) {
		this.displayMode = displayMode;
		this.displayModeLoaded = true;
		
		this.edit.putString(DISPLAY_MODE, displayMode);
		this.edit.commit();
	}
	
	//Player data
	private boolean playerDataLoaded = false;
	private String playerData;
	
	public synchronized String getPlayerData() {
		if(!this.playerDataLoaded) {
			this.playerData = this.prefs.getString(PLAYER_DATA, null);
			this.playerDataLoaded = true;
		}
		return this.playerData;
	}
	
	public synchronized void setPlayerData(String playerData) {
		this.playerData = playerData;
		this.playerDataLoaded = true;
		
		this.edit.putString(PLAYER_DATA, playerData);
		this.edit.commit();
	}
	
	//Round length
	private boolean roundLengthLoaded = false;
	private String roundLength;
	
	public synchronized String getRoundLength() {
		if(!this.roundLengthLoaded) {
			this.roundLength = this.prefs.getString(ROUND_LENGTH, "50");
			this.roundLengthLoaded = true;
		}
		return this.roundLength;
	}
	
	public synchronized void setRoundLength(String roundLength) {
		this.roundLength = roundLength;
		this.roundLengthLoaded = true;
		
		this.edit.putString(ROUND_LENGTH, roundLength);
		this.edit.commit();
	}
	
	//Timer sound
	private boolean timerSoundLoaded = false;
	private String timerSound;
	
	public synchronized String getTimerSound() {
		if(!this.timerSoundLoaded) {
			this.timerSound = this.prefs.getString(TIMER_SOUND, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
			this.timerSoundLoaded = true;
		}
		return this.timerSound;
	}
	
	public synchronized void setTimerSound(String timerSound) {
		this.timerSound = timerSound;
		this.timerSoundLoaded = true;
		
		this.edit.putString(TIMER_SOUND, timerSound);
		this.edit.commit();
	}
	
	//Trade price
	private boolean tradePriceLoaded = false;
	private String tradePrice;
	
	public synchronized String getTradePrice() {
		if(!this.tradePriceLoaded) {
			this.tradePrice = this.prefs.getString(TRADE_PRICE, "50");
			this.tradePriceLoaded = true;
		}
		return this.tradePrice;
	}
	
	public synchronized void setTradePrice(String tradePrice) {
		this.tradePrice = tradePrice;
		this.tradePriceLoaded = true;
		
		this.edit.putString(TRADE_PRICE, tradePrice);
		this.edit.commit();
	}
	
	//Date
	private boolean legalityDateLoaded = false;
	private String legalityDate;
	
	public synchronized String getLegalityDate() {
		if(!this.legalityDateLoaded) {
			this.legalityDate = this.prefs.getString(LEGALITY_DATE, null);
			this.legalityDateLoaded = true;
		}
		return this.legalityDate;
	}
	
	public synchronized void setLegalityDate(String date) {
		this.legalityDate = date;
		this.legalityDateLoaded = true;
		
		this.edit.putString(LEGALITY_DATE, date);
		this.edit.commit();
	}
	
	//Last update
	private boolean lastUpdateLoaded = false;
	private String lastUpdate;
	
	public synchronized String getLastUpdate() {
		if(!this.lastUpdateLoaded) {
			this.lastUpdate = this.prefs.getString(LAST_UPDATE, "");
			this.lastUpdateLoaded = true;
		}
		return this.lastUpdate;
	}
	
	public synchronized void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
		this.lastUpdateLoaded = true;
		
		this.edit.putString(LAST_UPDATE, lastUpdate);
		this.edit.commit();
	}
	
	//Last TCG name update
	private boolean lastTCGNameUpdateLoaded = false;
	private String lastTCGNameUpdate;
	
	public synchronized String getLastTCGNameUpdate() {
		if(!this.lastTCGNameUpdateLoaded) {
			this.lastTCGNameUpdate = this.prefs.getString(LAST_TCGNAME_UPDATE, "");
			this.lastTCGNameUpdateLoaded = true;
		}
		return this.lastTCGNameUpdate;
	}
	
	public synchronized void setLastTCGNameUpdate(String lastTCGNameUpdate) {
		this.lastTCGNameUpdate = lastTCGNameUpdate;
		this.lastTCGNameUpdateLoaded = true;
		
		this.edit.putString(LAST_TCGNAME_UPDATE, lastTCGNameUpdate);
		this.edit.commit();
	}
}
