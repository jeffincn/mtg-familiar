package com.gelakinetic.mtgfam.fragments;

import java.util.ArrayList;
import java.util.Random;

import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.fragments.SearchViewFragment.SearchCriteria;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.InFragmentMenuLoader;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.ResultListAdapter;

public class ResultListFragment extends FamiliarFragment {

	private static int				cursorPosition = 0;
	private static int				cursorPositionOffset = 0;
	private ListView	lv;
	private static boolean		isSingle = false;
	private static boolean		isRandom = false;
	private static int				numChoices;
	private static int[]		randomSequence;
	private Cursor		c;
	private static int				randomIndex;
	private static boolean		randomFromMenu;
	private static long			id;

	public ResultListFragment() {
		/* http://developer.android.com/reference/android/app/Fragment.html
		 * All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}
	
	
	@Override
	public void receiveMessage(Bundle bundle) {
		try{
			setResultFromBundle(bundle);
			fillData(c);
		}
		catch (FamiliarDbException e) {
			getMainActivity().showDbErrorToast();
			getMainActivity().getSupportFragmentManager().popBackStack();
			return;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = this.getArguments();
		try{
			setResultFromBundle(args);
		}
		catch (FamiliarDbException e) {
			getMainActivity().showDbErrorToast();
			getMainActivity().getSupportFragmentManager().popBackStack();
			return;
		}
		
		/* Uncomment this to add the result of a search directly into the wishlist */
		/*
		ArrayList<CardData> cards = new ArrayList<CardData>();
		c.moveToFirst();
		while(!c.isAfterLast()) {
			CardData cd = (new TradeListHelpers()).new CardData(
					c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME)),
					c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)),
					1,
					c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER)),
					c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY)));
			cards.add(cd);
			c.moveToNext();
		}
		WishlistHelpers.WriteWishlist(this.getActivity(), cards);
		*/
		
		/*
		 * savedInstanceState is only null when the fragment is starting a new result
		 * reset the position then, but not on rotation or putting it on the backstack
		 * see onSaveInstanceState() in Familiar Fragment
		 */
		if(savedInstanceState == null) {
			cursorPosition = 0;
			cursorPositionOffset = 0;
		}
	}

	private void setResultFromBundle(Bundle args) throws FamiliarDbException{
		
		if(args == null) {
			return;
		}
		
		String[] returnTypes = new String[] { CardDbAdapter.KEY_ID, CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_SET,
				CardDbAdapter.KEY_RARITY, CardDbAdapter.KEY_MANACOST, CardDbAdapter.KEY_TYPE, CardDbAdapter.KEY_ABILITY,
				CardDbAdapter.KEY_POWER, CardDbAdapter.KEY_TOUGHNESS, CardDbAdapter.KEY_LOYALTY , CardDbAdapter.KEY_NUMBER };

			if ((id = args.getLong("id")) != 0L) {
				c = mDbHelper.fetchCard(id, null);
			}
			else if ((id = args.getLong("id0")) != 0L) {
				long id1 = args.getLong("id1");
				long id2 = args.getLong("id2");
				Cursor cs[] = new Cursor[3];
				cs[0] = mDbHelper.fetchCard(id, null);
				cs[1] = mDbHelper.fetchCard(id1, null);
				cs[2] = mDbHelper.fetchCard(id2, null);
				c = new MergeCursor(cs);
			}
			else {
				SearchCriteria criteria = (SearchCriteria) args.getSerializable(SearchViewFragment.CRITERIA);
				int setLogic = criteria.Set_Logic;
				boolean consolidate = (setLogic == CardDbAdapter.MOSTRECENTPRINTING || setLogic == CardDbAdapter.FIRSTPRINTING);
				if(mDbHelper == null) {
					// Just in case. it happened to a user once...
					mDbHelper = new CardDbAdapter(this.getActivity());
				}
				c = mDbHelper.Search(criteria.Name, criteria.Text, criteria.Type, criteria.Color, criteria.Color_Logic,
						criteria.Set, criteria.Pow_Choice, criteria.Pow_Logic, criteria.Tou_Choice, criteria.Tou_Logic, criteria.Cmc,
						criteria.Cmc_Logic, criteria.Format, criteria.Rarity, criteria.Flavor, criteria.Artist, criteria.Type_Logic,
						criteria.Text_Logic, criteria.Set_Logic, true, returnTypes, consolidate);
			}
			
			if (this.isAdded()) {
				if (c == null || c.getCount() == 0) {
					Toast.makeText(this.getActivity(), getString(R.string.search_toast_no_results), Toast.LENGTH_SHORT).show();
					getMainActivity().mFragmentManager.popBackStack();
					return;
				}
				else if (c.getCount() == 1) {
					isSingle = true;
					c.moveToFirst();
					id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
					startCardViewFrag(id, isRandom, isSingle);
				}
				else {
					isSingle = false;
				}
			}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		if (c != null) {
			c.close();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		cursorPosition = lv.getFirstVisiblePosition();
		View cursorPositionView = lv.getChildAt(0);
		cursorPositionOffset = (cursorPositionView == null) ? 0 : cursorPositionView.getTop();
	}

	@Override
	public void onResume() {
		super.onResume();
		fillData(c);
		lv.setSelectionFromTop(cursorPosition, cursorPositionOffset);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		if (container == null) {
			// Something is happening when the fragment is on the backstack
			return null;
		}

		View myFragmentView = inflater.inflate(R.layout.result_list_frag, container, false);
		Bundle args = this.getArguments();

		masterLayout = (LinearLayout)myFragmentView.findViewById(R.id.master_layout);
		addFragmentMenu();
		
		lv = (ListView) myFragmentView.findViewById(R.id.resultList);// getListView();
		registerForContextMenu(lv);
		Bundle res = getMainActivity().getFragmentResults();
		if (res != null) {
			onFragmentResult(res);
		}
		else if (args != null && args.getBoolean(SearchViewFragment.RANDOM)) {
			startRandom();
		}

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(getMainActivity().mThreePane) {
					Bundle args = new Bundle();
					args.putLong("id", id);
					args.putBoolean(SearchViewFragment.RANDOM, isRandom);
					args.putBoolean("isSingle", isSingle);
					getMainActivity().sendMessageToRightFragment(args);
				}
				else {
					startCardViewFrag(id, isRandom, isSingle);
				}
			}
		});
		
		return myFragmentView;
	}

	private void fillData(Cursor c) {
		if (c != null) {
			ArrayList<String> fromList = new ArrayList<String>();
			ArrayList<Integer> toList = new ArrayList<Integer>();
			fromList.add(CardDbAdapter.KEY_NAME);
			toList.add(R.id.cardname);
			if (getMainActivity().getPreferencesAdapter().getSetPref()) {
				fromList.add(CardDbAdapter.KEY_SET);
				toList.add(R.id.cardset);
			}
			if (getMainActivity().getPreferencesAdapter().getManaCostPref()) {
				fromList.add(CardDbAdapter.KEY_MANACOST);
				toList.add(R.id.cardcost);
			}
			if (getMainActivity().getPreferencesAdapter().getTypePref()) {
				fromList.add(CardDbAdapter.KEY_TYPE);
				toList.add(R.id.cardtype);
			}
			if (getMainActivity().getPreferencesAdapter().getAbilityPref()) {
				fromList.add(CardDbAdapter.KEY_ABILITY);
				toList.add(R.id.cardability);
			}
			if (getMainActivity().getPreferencesAdapter().getPTPref()) {
				fromList.add(CardDbAdapter.KEY_POWER);
				toList.add(R.id.cardp);
				fromList.add(CardDbAdapter.KEY_TOUGHNESS);
				toList.add(R.id.cardt);
				fromList.add(CardDbAdapter.KEY_LOYALTY);
				toList.add(R.id.cardt);
			}
			String[] from = new String[fromList.size()];
			fromList.toArray(from);

			int[] to = new int[toList.size()];
			for (int i = 0; i < to.length; i++) {
				to[i] = toList.get(i);
			}

			ResultListAdapter rla = new ResultListAdapter(getMainActivity(), R.layout.card_row, c, from, to,
					this.getResources());
			lv.setAdapter(rla);
		}
	}

	public void onFragmentResult(Bundle args) {
		long id;
		int resultCode = args.getInt("resultCode", -1);
		long lastID = args.getLong("lastID", -1L);
		boolean bundleIsSingle = args.getBoolean("isSingle", false);

		switch (resultCode) {
			case CardViewFragment.RANDOMLEFT:
				randomIndex--;
				if (randomIndex < 0) {
					randomIndex += numChoices;
				}
				c.moveToPosition(randomSequence[randomIndex]);
				id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
				startCardViewFrag(id, isRandom, isSingle);
				break;
			case CardViewFragment.RANDOMRIGHT:
				randomIndex++;
				if (randomIndex >= numChoices) {
					randomIndex -= numChoices;
				}
				c.moveToPosition(randomSequence[randomIndex]);
				id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
				startCardViewFrag(id, isRandom, isSingle);
				break;
			case CardViewFragment.SWIPELEFT:
				c.moveToFirst();
				while (!c.isAfterLast()) {
					if (lastID == c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID))) {
						c.moveToPrevious();

						// In case the id was matched against the first item.
						if (c.isBeforeFirst())
							c.moveToLast();

						break;
					}
					else
						c.moveToNext();
				}

				id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
				startCardViewFrag(id, isRandom, isSingle);
				break;
			case CardViewFragment.SWIPERIGHT:
				c.moveToFirst();
				while (!c.isAfterLast()) {
					if (lastID == c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID))) {
						c.moveToNext();

						// In case the id was matched against the last item.
						if (c.isAfterLast())
							c.moveToFirst();

						break;
					}
					else
						c.moveToNext();
				}

				id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
				startCardViewFrag(id, isRandom, isSingle);
				break;
			case CardViewFragment.ISCLOSING:
			default:
				if (bundleIsSingle || (isRandom && !randomFromMenu)) {
					// TODO check on rotation
					getMainActivity().mFragmentManager.popBackStack();
				}
				if (randomFromMenu) {
					randomFromMenu = false;
					isRandom = false;
				}
				break;
		}
	}

	private void startCardViewFrag(long id, boolean isRandom, boolean isSingle) {
		// add a fragment
		Bundle args = new Bundle();
		args.putLong("id", id);
		args.putBoolean(SearchViewFragment.RANDOM, isRandom);
		args.putBoolean("isSingle", isSingle);
		if(getMainActivity().mThreePane) {
			getMainActivity().sendMessageToRightFragment(args);
		}
		else {
			CardViewFragment cvFrag = new CardViewFragment();
			startNewFragment(cvFrag, args);
		}
	}

	private void startRandom() {
		if (c == null || c.getCount() == 0) {
			return;
		}
		
		if(c.getCount() == 1) {
			isRandom = false;
		}
		else {
			isRandom = true;
		}

		// implements http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
		numChoices = c.getCount();
		Random rand = new Random(System.currentTimeMillis());
		randomSequence = new int[numChoices];
		int temp, k, j;
		for (k = 0; k < numChoices; k++) {
			randomSequence[k] = k;
		}
		for (k = numChoices - 1; k > 0; k--) {
			j = rand.nextInt(k + 1);// j = random integer with 0 <= j <= i
			temp = randomSequence[j];
			randomSequence[j] = randomSequence[k];
			randomSequence[k] = temp;
		}

		int randomIndex = rand.nextInt(numChoices);
		c.moveToPosition(randomSequence[randomIndex]);
		long id = c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID));
		startCardViewFrag(id, isRandom, isSingle);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.search_menu_random_search:
				randomFromMenu = true;
				startRandom();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if(getMainActivity().mThreePane) {
			InFragmentMenuLoader cml = new InFragmentMenuLoader(this);
			cml.inflate(R.menu.result_list_menu, menu);
			mFragmentMenu = cml.getView();
			addFragmentMenu();
		}
		else {
			inflater.inflate(R.menu.result_list_menu, menu);			
		}
	}
}
