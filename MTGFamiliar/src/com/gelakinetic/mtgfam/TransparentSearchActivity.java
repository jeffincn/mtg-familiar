package com.gelakinetic.mtgfam;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

public class TransparentSearchActivity extends Activity {

	public AutoCompleteTextView namefield;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.transparent_search_activity);
//		onSearchRequested();
		
		namefield = (AutoCompleteTextView)findViewById(R.id.transparent_namefield);
		
		namefield.setAdapter(new AutocompleteCursorAdapter(this, null));
		// So pressing enter does the search
		namefield.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		namefield.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				if (arg1 == EditorInfo.IME_ACTION_SEARCH) {
					//searchbutton.performClick();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
	        // do something on back.
	    	this.finish();
	    	
	        return true;
	    }

	    return super.onKeyDown(keyCode, event);
	}

}
