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
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ResultListAdapter extends SimpleCursorAdapter {

	private int						layout;
	String[] from;
	int[] to;
	private Resources	resources;
	private ImageGetter	imgGetter;

	public ResultListAdapter(Context context, int layout, Cursor c, String[] f, int[] t, Resources r) {
		super(context, layout, c, f, t);
		from = f;
		to = t;
		resources = r;
		this.layout = layout;
		imgGetter = ImageGetterHelper.GlyphGetter(r);
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
		
		for(int i=0; i < from.length; i++){
			String name = c.getString(c.getColumnIndex(from[i]));

			TextView textfield = (TextView) v.findViewById(to[i]);
			if (textfield != null) {
				if(CardDbAdapter.KEY_NAME.equals(from[i])){
					textfield.setText(name);
				}
				else if(CardDbAdapter.KEY_MANACOST.equals(from[i])){
					name = name.replace("{", "<img src=\"").replace("}", "\"/>");
					CharSequence csq = Html.fromHtml(name, imgGetter, null);
					textfield.setText(csq);						
				}
				else if(CardDbAdapter.KEY_SET.equals(from[i])){
					textfield.setText(name);
					char rarity = (char)c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY));
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
			}
		}
	}
}
