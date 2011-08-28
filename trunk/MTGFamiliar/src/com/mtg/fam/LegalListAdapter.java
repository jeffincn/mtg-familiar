package com.mtg.fam;

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

	public LegalListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, long cardID, CardDbAdapter cda) {
		super(context, layout, c, from, to);
		this.layout = layout;
		mCardID = cardID;
		mDbHelper = cda;
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

		int nameCol = c.getColumnIndex(CardDbAdapter.KEY_NAME);

		String name = c.getString(nameCol);
		
		int legality = mDbHelper.checkLegality(mCardID, name);

		/**
		 * Next set the name of the entry.
		 */
		TextView name_text = (TextView) v.findViewById(R.id.format);
		if (name_text != null) {
			name_text.setText(name + ":");
		}

		TextView status_text = (TextView) v.findViewById(R.id.status);
		if (status_text != null) {
			if(legality == CardDbAdapter.LEGAL){
				status_text.setText("Legal");
			}
			else if(legality == CardDbAdapter.BANNED){
				status_text.setText("Banned");
			}
			else if(legality == CardDbAdapter.RESTRICTED){
				status_text.setText("Restricted");
			}
		}
		mDbHelper.close();
	}
}
