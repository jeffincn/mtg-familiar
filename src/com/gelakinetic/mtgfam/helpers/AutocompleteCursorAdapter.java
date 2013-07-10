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

package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;

public class AutocompleteCursorAdapter extends CursorAdapter {

	private CardDbAdapter	mDbHelper;

	public AutocompleteCursorAdapter(Context context, Cursor c) {
		super(context, c);
		try {
			mDbHelper = new CardDbAdapter(context);
			mDbHelper.close(); // close the immediately opened db
		}
		catch(FamiliarDbException e) {
			// uhoh
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.autocomplete_row, null);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String keyword = cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME));
		TextView tv = (TextView) view.findViewById(R.id.autocomplete_name);
		tv.setText(keyword);
	}

	@Override
	public CharSequence convertToString(Cursor cursor) {
		return cursor.getString(cursor.getColumnIndex(CardDbAdapter.KEY_NAME));
	}

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		String filter = null;
		if (constraint != null) {
			filter = constraint.toString();
		}
		else {
			return null;
		}

		Cursor cursor = null;
		try {
			if(mDbHelper != null) {
				mDbHelper.openReadable();
				cursor = mDbHelper.autoComplete(filter);
				mDbHelper.close();
			}
		}
		catch (FamiliarDbException e) {
			return null;
		}
		return cursor;
	}
}
