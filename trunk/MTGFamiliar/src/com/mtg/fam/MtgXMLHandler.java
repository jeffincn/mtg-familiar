package com.mtg.fam;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class MtgXMLHandler extends DefaultHandler {

	// ===========================================================
	// Fields
	// ===========================================================

	String								value;
	private boolean				parsing_sets	= true;
	private StringBuilder	buf						= new StringBuilder();

	// Set Variables
	String								code;
	String								name;

	// Card Variables, also name
	private int						power;
	private int						toughness;
	private String				ability;
	private String				type;
	private String				color;
	private String				manacost;
	private int						cmc;
	private int						loyalty;
	private char					rarity;
	private String				flavor;
	private int						number;
	private String				artist;

	ArrayList<String>			sets;
	ArrayList<String>			picURLs;

	private CardDbAdapter	mDbHelper;
	private String				set;
	private String				code_mtgi;
	private boolean				split = false;
	private String[]			names;
	private String[]			rarities;
	private String[]			manacosts;
	private String[]			cmcs;
	private String[]			abilities;
	private String[]			artists;
	private String[]			colors;
	private String[]	types;
	
	private main mMain;

	// ===========================================================
	// Methods
	// ===========================================================
	public void setDb(CardDbAdapter cda) {
		mDbHelper = cda;
	}

	public void setMain(main m) {
		mMain = m;
	}

	@Override
	public void startDocument() throws SAXException {
		// this.myParsedExampleDataSet = new ParsedExampleDataSet();
		sets = new ArrayList<String>();
		picURLs = new ArrayList<String>();
	}

	@Override
	public void endDocument() throws SAXException {
		// Nothing to do
	}

	/**
	 * Gets be called on opening tags like: <tag> Can provide attribute(s), when
	 * xml was like: <tag attribute="attributeValue">
	 */
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {

		buf.delete(0, buf.length());

		if (localName.equals("set") && !parsing_sets) {
			picURLs.add(atts.getValue("picURL"));
		}
	}

	/**
	 * Gets be called on closing tags like: </tag>
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

		value = buf.toString();

		if(localName.equals("numCards")){
			mMain.setNumCards(Integer.parseInt(value));
		}
		
		// for sets
		if (parsing_sets) {
			if (localName.equals("code_magiccards")) {
				code_mtgi = value;
			}
			if (localName.equals("code")) {
				code = value;
			}
			else if (localName.equals("name")) {
				name = value;
			}
			else if (localName.equals("set")) {
				mDbHelper.createSet(name, code, code_mtgi);// add set to DB
			}
			else if (localName.equals("sets")) {
				parsing_sets = false;
			}
		}
		else {
			// for cards
			if (localName.equals("name")) {
				if (value.contains(" // ")) {
					split = true;
					names = value.split(" // ");
				}
				else {
					name = value;
				}
			}
			else if (localName.equals("set")) { // extract picture URL here "picURL"
				set = value;
			}
			else if (localName.equals("type")) {
				if (split) {
					types = value.split(" // ");
				}
				else {
					type = value;
				}
			}
			else if (localName.equals("rarity")) {
				if (split) {
					rarities = value.split(" // ");
				}
				else {
					rarity = value.charAt(0);
				}
			}
			else if (localName.equals("manacost")) {
				if (split) {
					manacosts = value.split(" // ");
				}
				else {
					manacost = value;
				}
			}
			else if (localName.equals("converted_manacost")) {
				if (split) {
					cmcs = value.split(" // ");
				}
				else {
					try {
						cmc = Integer.parseInt(value);
					}
					catch (NumberFormatException e) {
						cmc = 0;
					}
				}
			}
			else if (localName.equals("power")) {
				try{
					power = Integer.parseInt(value);
				}
				catch(NumberFormatException e){
					if(value.equals("*")){
						power = CardDbAdapter.STAR;
					}
					else if(value.equals("1+*")){
						power = CardDbAdapter.ONEPLUSSTAR;
					}
					else if(value.equals("2+*")){
						power = CardDbAdapter.TWOPLUSSTAR;
					}
					else if(value.equals("7-*")){
						power = CardDbAdapter.SEVENMINUSSTAR;
					}
					else{
						Log.d("powerXML", value);
					}
				}
			}
			else if (localName.equals("toughness")) {
				try{
					toughness = Integer.parseInt(value);
				}
				catch(NumberFormatException e){
					if(value.equals("*")){
						toughness = CardDbAdapter.STAR;
					}
					else if(value.equals("1+*")){
						toughness = CardDbAdapter.ONEPLUSSTAR;
					}
					else if(value.equals("2+*")){
						toughness = CardDbAdapter.TWOPLUSSTAR;
					}
					else if(value.equals("7-*")){
						toughness = CardDbAdapter.SEVENMINUSSTAR;
					}
					else{
						Log.d("toughnessXML", value);
					}
				}
			}
			else if (localName.equals("loyalty")) {
				try {
					loyalty = Integer.parseInt(value);
				}
				catch (NumberFormatException e) {
					loyalty = 0;
				}
			}
			else if (localName.equals("ability")) {
				if (split) {
					abilities = value.split(" // ");
					;
				}
				else {
					ability = value;
				}
			}
			else if (localName.equals("flavor")) {
				flavor = value;
			}
			else if (localName.equals("artist")) {
				if (split) {
					artists = value.split(" // ");
					;
				}
				else {
					artist = value;
				}
			}
			else if (localName.equals("number")) {
				try {
					number = Integer.parseInt(value);
				}
				catch (NumberFormatException e) {
					number = 0;
				}

			}
			else if (localName.equals("color")) { // may not be set
				if (split) {
					colors = value.split(" // ");
					;
				}
				else {
					color = value;
				}
			}

			else if (localName.equals("card")) {
				// add card to DB
				if (split) {
					try{
						mDbHelper.createCard(names[0], set, types[0], rarities[0].charAt(0), manacosts[0], Integer.parseInt(cmcs[0]),
								power, toughness, loyalty, abilities[0], flavor, artists[0], number, colors[0]);
						mDbHelper.createCard(names[1], set, types[1], rarities[1].charAt(0), manacosts[1], Integer.parseInt(cmcs[1]),
								power, toughness, loyalty, abilities[1], flavor, artists[1], number, colors[1]);
					}
					catch(NumberFormatException e){
						e.toString();
					}
					names = null;
					sets.clear();
					types = null;
					rarities = null;
					manacosts = null;
					cmcs = null;
					power = 0;
					toughness = 0;
					loyalty = 0;
					abilities = null;
					flavor = null;
					number = 0;
					artists = null;
					colors = null;
					
					split = false;
				}
				else {
					mDbHelper.createCard(name, set, type, rarity, manacost, cmc, power, toughness, loyalty, ability, flavor,
							artist, number, color);

					name = null;
					sets.clear();
					type = null;
					rarity = '\0';
					manacost = null;
					cmc = 0;
					power = 0;
					toughness = 0;
					loyalty = 0;
					ability = null;
					flavor = null;
					number = 0;
					artist = null;
					color = null;
					
					mMain.cardAdded();
				}
			}
		}

	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		if (buf != null) {
			for (int i = start; i < start + length; i++) {
				buf.append(ch[i]);
			}
		}
	}
}
