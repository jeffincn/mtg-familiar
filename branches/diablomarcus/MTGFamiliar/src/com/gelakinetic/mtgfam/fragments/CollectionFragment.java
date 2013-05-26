package com.gelakinetic.mtgfam.fragments;

import android.os.*;
import android.view.*;
import android.widget.*;

import com.gelakinetic.mtgfam.*;
import com.gelakinetic.mtgfam.clicklisteners.*;
import com.gelakinetic.mtgfam.helpers.*;
import com.gelakinetic.mtgfam.listAdapters.*;

//Copyright 2013 Mark Katerberg This file is part of MTG Familiar. MTG 
//Familiar is free software: you can redistribute it and/or modify it under 
//the terms of the GNU General Public License as published by the Free Software 
//Foundation, either version 3 of the License, or (at your option) any later 
//version. MTG Familiar is distributed in the hope that it will be useful, 
//but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
//or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
//more details. You should have received a copy of the GNU General Public License 
//along with MTG Familiar. If not, see <http://www.gnu.org/licenses/>.

public class CollectionFragment extends FamiliarFragment {

	private EditText numberInput;
	private AutoCompleteTextView nameInput;
	private Button addCard;
	private ExpandableListView listOfCards;

	public CollectionFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View myFragmentView = inflater.inflate(R.layout.collection_frag, container, false);

		numberInput = (EditText) myFragmentView.findViewById(R.id.collectionNumberInput);
		nameInput = (AutoCompleteTextView) myFragmentView.findViewById(R.id.collectionNameSearch);
		addCard = (Button) myFragmentView.findViewById(R.id.collectionAddCard);
		listOfCards = (ExpandableListView) myFragmentView.findViewById(R.id.collectionList);

		addCard.setOnClickListener(new AddCardToCollectionListener(null, null, null));
		nameInput.setAdapter(new AutocompleteCursorAdapter(getActivity(), null));
		setKeyboardFocus(savedInstanceState, nameInput, false);
		numberInput.setText("1");

		listOfCards.setGroupIndicator(null);
		listOfCards.setChildIndicator(null);
		listOfCards.setDividerHeight(0);
		listOfCards.setAdapter(new CollectionListAdapter());

		return myFragmentView;
	}
}
