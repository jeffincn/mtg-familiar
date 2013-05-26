package com.gelakinetic.mtgfam.test.clicklisteners;

import java.util.*;

import android.test.*;
import android.test.suitebuilder.annotation.*;
import android.view.*;
import android.widget.*;

import com.gelakinetic.mtgfam.clicklisteners.*;
import com.gelakinetic.mtgfam.fragments.*;
import com.gelakinetic.mtgfam.helpers.*;

public class AddCardToCollectionListenerTest extends AndroidTestCase {

	@SmallTest
	public void onClickAddsCardToList() {
		EditText numberOfCards = new EditText(getContext());
		AutoCompleteTextView cardText = new AutoCompleteTextView(getContext());
		List<MtgCard> cardList = new ArrayList<MtgCard>();
		AddCardToCollectionListener testObject = new AddCardToCollectionListener(numberOfCards, cardText, cardList);

		View view = new CollectionFragment().getView();

		testObject.onClick(view);

		assertEquals(1, cardList.size());
	}

	public void onClick() {

	}
}
