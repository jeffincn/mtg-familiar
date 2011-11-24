package com.gelakinetic.mtgfam;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class AutocompleteCursorAdapter extends CursorAdapter {

	private CardDbAdapter	mDbAdapter;

	public AutocompleteCursorAdapter(Context context, Cursor c) {
		super(context, c);
		mDbAdapter = new CardDbAdapter(context);
		mDbAdapter.open();
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
			cursor = mDbAdapter.Search(filter, null, null, "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
					CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, true, new String[] { CardDbAdapter.KEY_ID,
							CardDbAdapter.KEY_NAME }, true, CardDbAdapter.KEY_NAME);
		}
		catch (Exception e) {
			return null;
		}
		return cursor;
	}
}
