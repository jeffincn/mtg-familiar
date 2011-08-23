package com.mtg.fam;

public class MtgCard {
	public String name;
	public String set;
	public String type;
	public char rarity;
	public String manacost;
	public int cmc;
	public int power;
	public int toughness;
	public int loyalty;
	public String ability;
	public String flavor;
	public String artist;
	public int number;
	public String color;
	
	public MtgCard(){
		name = "";
		set = "";
		type = "";
		rarity = '\0';
		manacost = "";
		cmc = 0;
		power = 0;
		toughness = 0;
		loyalty = 0;
		ability = "";
		flavor = "";
		artist = "";
		number = 0;
		color = "";
	}
}
