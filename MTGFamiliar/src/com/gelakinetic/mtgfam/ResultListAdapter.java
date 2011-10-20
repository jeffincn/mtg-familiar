/**
Copyright 2011 Adam Feinstein

This file is part of MTG Familiar.

MTG Familiar is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MTG Familiar is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ResultListAdapter extends SimpleCursorAdapter implements SectionIndexer {

	private int					layout;
	String[]						from;
	int[]								to;
	private Resources		resources;
	private ImageGetter	imgGetter;
	AlphabetIndexer			alphaIndexer;

	public ResultListAdapter(Context context, int layout, Cursor c, String[] f, int[] t, Resources r) {
		super(context, layout, c, f, t);
		from = f;
		to = t;
		resources = r;
		this.layout = layout;
		imgGetter = ImageGetterHelper.GlyphGetter(r);
		alphaIndexer = new AlphabetIndexer(c, c.getColumnIndex(CardDbAdapter.KEY_NAME), "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		Cursor c = getCursor();

		final LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(layout, parent, false);

		int nameCol = c.getColumnIndex(CardDbAdapter.KEY_NAME);

		String name = c.getString(nameCol);

		/**
		 * Next set the name of the entry.
		 */
		TextView name_text = (TextView) v.findViewById(R.id.format);
		if (name_text != null) {
			name_text.setText("custom-" + name);
		}

		return v;
	}

	@Override
	public void bindView(View v, Context context, Cursor c) {

		float p = CardDbAdapter.NOONECARES;
		float t = CardDbAdapter.NOONECARES;
		float l = CardDbAdapter.NOONECARES;
		
		boolean hideCost = true;
		boolean hideSet = true;
		boolean hideType = true;
		boolean hideAbility = true;

		for (int i = 0; i < from.length; i++) {
			String name = c.getString(c.getColumnIndex(from[i]));

			TextView textfield = null;
			if (i < to.length) {
				textfield = (TextView) v.findViewById(to[i]);
			}
			
			if (CardDbAdapter.KEY_NAME.equals(from[i])) {
				textfield.setText(name);
			}
			else if (CardDbAdapter.KEY_MANACOST.equals(from[i])) {
				hideCost = false;
				name = name.replace("{", "<img src=\"").replace("}", "\"/>");
				CharSequence csq = Html.fromHtml(name, imgGetter, null);
				textfield.setText(csq);
			}
			else if (CardDbAdapter.KEY_SET.equals(from[i])) {
				hideSet = false;
				textfield.setText(name);
				char rarity = (char) c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY));
				switch (rarity) {
					case 'C':
						textfield.setTextColor(resources.getColor(R.color.common));
						break;
					case 'U':
						textfield.setTextColor(resources.getColor(R.color.uncommon));
						break;
					case 'R':
						textfield.setTextColor(resources.getColor(R.color.rare));
						break;
					case 'M':
						textfield.setTextColor(resources.getColor(R.color.mythic));
						break;
				}
			}
			else if (CardDbAdapter.KEY_TYPE.equals(from[i])) {
				hideType = false;
				textfield.setText(name);
			}
			else if (CardDbAdapter.KEY_ABILITY.equals(from[i])) {
				hideAbility = false;
				name = name.replace("{", "<img src=\"").replace("}", "\"/>");
				CharSequence csq = Html.fromHtml(name, imgGetter, null);
				textfield.setText(csq);
			}
			else if (CardDbAdapter.KEY_POWER.equals(from[i])) {
				p = Float.parseFloat(name);
			}
			else if (CardDbAdapter.KEY_TOUGHNESS.equals(from[i])) {
				t = Float.parseFloat(name);
			}
			else if (CardDbAdapter.KEY_LOYALTY.equals(from[i])) {
				l = Float.parseFloat(name);
			}
		}

		TextView pt = (TextView) v.findViewById(R.id.cardpt);

		if (l != CardDbAdapter.NOONECARES) {
			pt.setText(new Integer((int)l).toString());
		}
		else if (p != CardDbAdapter.NOONECARES && t != CardDbAdapter.NOONECARES) {

			String spt = "";

			if (p == CardDbAdapter.STAR)
				spt += "*";
			else if (p == CardDbAdapter.ONEPLUSSTAR)
				spt += "1+*";
			else if (p == CardDbAdapter.TWOPLUSSTAR)
				spt += "2+*";
			else if (p == CardDbAdapter.SEVENMINUSSTAR)
				spt += "7-*";
			else if (p == CardDbAdapter.STARSQUARED)
				spt += "*^2";
			else {
				if (p == (int) p) {
					spt += (int) p;
				}
				else {
					spt += p;
				}
			}

			spt += "/";

			if (t == CardDbAdapter.STAR)
				spt += "*";
			else if (t == CardDbAdapter.ONEPLUSSTAR)
				spt += "1+*";
			else if (t == CardDbAdapter.TWOPLUSSTAR)
				spt += "2+*";
			else if (t == CardDbAdapter.SEVENMINUSSTAR)
				spt += "7-*";
			else if (t == CardDbAdapter.STARSQUARED)
				spt += "*^2";
			else {
				if (t == (int) t) {
					spt += (int) t;
				}
				else {
					spt += t;
				}
			}

			pt.setText(spt);
		}
		else {
			pt.setVisibility(View.GONE);
		}
		
		if(hideCost){
			((TextView) v.findViewById(R.id.cardcost)).setVisibility(View.GONE);
		}
		if(hideSet){
			((TextView) v.findViewById(R.id.cardset)).setVisibility(View.GONE);
		}
		if(hideType){
			((TextView) v.findViewById(R.id.cardtype)).setVisibility(View.GONE);
		}
		if(hideAbility){
			((TextView) v.findViewById(R.id.cardability)).setVisibility(View.GONE);
		}

	}

	public int getPositionForSection(int section) {
		return alphaIndexer.getPositionForSection(section); // use the indexer
	}

	public int getSectionForPosition(int position) {
		return alphaIndexer.getSectionForPosition(position); // use the indexer
	}

	public Object[] getSections() {
		return alphaIndexer.getSections(); // use the indexer
	}
}
