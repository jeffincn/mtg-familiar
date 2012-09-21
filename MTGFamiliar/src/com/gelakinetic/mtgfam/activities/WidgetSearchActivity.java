package com.gelakinetic.mtgfam.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.SearchWidgetFragment;

public class WidgetSearchActivity extends FamiliarActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_activity);

		if (savedInstanceState == null) {
			mFragmentManager = getSupportFragmentManager();
			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

			SearchWidgetFragment swFrag = new SearchWidgetFragment();
			fragmentTransaction.add(R.id.frag_view, swFrag);
			fragmentTransaction.commit();
		}
	}
	
}