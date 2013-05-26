package com.gelakinetic.mtgfam.clicklisteners;

import java.util.*;

import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

import com.gelakinetic.mtgfam.helpers.*;

public class AddCardToCollectionListener implements OnClickListener {

	private List<MtgCard> cardList;

	public AddCardToCollectionListener(EditText editText, AutoCompleteTextView cardText, List<MtgCard> cardList) {
		this.cardList = cardList;
	}

	@Override
	public void onClick(View v) {
		cardList.add(new MtgCard());
	}
}
