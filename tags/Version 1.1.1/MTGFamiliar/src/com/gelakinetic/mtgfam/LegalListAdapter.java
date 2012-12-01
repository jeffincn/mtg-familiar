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
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class LegalListAdapter extends SimpleCursorAdapter {

	private int						layout;

	private long					mCardID;

	private CardDbAdapter	mDbHelper;

	private String				setCode;

	public LegalListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, long cardID,
			CardDbAdapter cda, String sc) {
		super(context, layout, c, from, to);
		this.layout = layout;
		mCardID = cardID;
		mDbHelper = cda;
		setCode = sc;
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
		mDbHelper.open();
		TextView status_text = (TextView) v.findViewById(R.id.status);
		TextView name_text = (TextView) v.findViewById(R.id.format);
		int nameCol = c.getColumnIndex(CardDbAdapter.KEY_NAME);
		String name = c.getString(nameCol);

		if (setCode.equalsIgnoreCase("PCP") || setCode.equalsIgnoreCase("ARS") || setCode.equalsIgnoreCase("UG")
				|| setCode.equalsIgnoreCase("UNH")) {
			if (name_text != null) {
				name_text.setText(name + ":");
			}
			if (status_text != null) {
				status_text.setText("Banned");
			}
		}
		else {
			int legality = mDbHelper.checkLegality(mCardID, name);

			/**
			 * Next set the name of the entry.
			 */
			if (name_text != null) {
				name_text.setText(name + ":");
			}

			if (status_text != null) {
				if (legality == CardDbAdapter.LEGAL) {
					status_text.setText("Legal");
				}
				else if (legality == CardDbAdapter.BANNED) {
					status_text.setText("Banned");
				}
				else if (legality == CardDbAdapter.RESTRICTED) {
					status_text.setText("Restricted");
				}
			}
		}
		mDbHelper.close();
	}
}
