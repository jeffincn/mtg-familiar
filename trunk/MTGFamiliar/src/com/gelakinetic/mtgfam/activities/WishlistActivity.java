/**
Copyright 2012 

This file is part of MTG Familiar.

MTG Familiar is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MTG Familiar is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MTG Familiar. If not, see <http://www.gnu.org/licenses/>.
 */
package com.gelakinetic.mtgfam.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.WishlistFragment;

public class WishlistActivity extends FamiliarActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_activity);

		if (savedInstanceState == null) {
			mFragmentManager = getSupportFragmentManager();
			FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

			WishlistFragment frag = new WishlistFragment();
			fragmentTransaction.add(R.id.frag_view, frag);
			fragmentTransaction.commit();
		}
	}
}
