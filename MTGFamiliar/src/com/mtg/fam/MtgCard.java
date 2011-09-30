package com.mtg.fam;

public class MtgCard {
	public String name;
	public String set;
	public String type;
	public char rarity;
	public String manacost;
	public int cmc;
	public float power;
	public float toughness;
	public int loyalty;
	public String ability;
	public String flavor;
	public String artist;
	public String number;
	public String color;
	
	public MtgCard(){
		name = "";
		set = "";
		type = "";
		rarity = '\0';
		manacost = "";
		cmc = 0;
		power = CardDbAdapter.NOONECARES;
		toughness = CardDbAdapter.NOONECARES;
		loyalty = CardDbAdapter.NOONECARES;
		ability = "";
		flavor = "";
		artist = "";
		number = "";
		color = "";
	}
}