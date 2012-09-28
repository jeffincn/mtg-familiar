package com.gelakinetic.mtgfam.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.activities.PreferencesActivity;
import com.gelakinetic.mtgfam.helpers.DbUpdaterService;

public class MenuFragment extends ListFragment {

	private MainActivity mActivity;
	private SlidingMenuAdapter mAdapter;

	@Override
	public void onResume() {
		super.onResume();
		// TODO just to be safe?
		if (!(getActivity() instanceof MainActivity))
			throw new IllegalStateException("MenuFragment must be attached to an instance of MainActivity");
		if (getActivity().findViewById(R.id.frag_view) == null)
			throw new IllegalStateException("MenuFragment must be attached to an Activity with R.id.frag_view");
		mActivity = (MainActivity) getActivity();
		// add everything
		mAdapter = new SlidingMenuAdapter(mActivity);
		mAdapter.addHeader(R.string.main_pages);
		mAdapter.addItem(R.string.main_card_search, R.drawable.card_search_icon);
		mAdapter.addItem(R.string.main_life_counter, R.drawable.life_counter_icon);
		mAdapter.addItem(R.string.main_mana_pool, R.drawable.mana_pool_icon);
		mAdapter.addItem(R.string.main_dice, R.drawable.dice_icon);
		mAdapter.addItem(R.string.main_trade, R.drawable.trade_icon);
		mAdapter.addItem(R.string.main_wishlist, R.drawable.wishlist_icon);
		mAdapter.addItem(R.string.main_timer, R.drawable.round_timer_icon);
		mAdapter.addItem(R.string.main_rules, R.drawable.rules_icon);
		mAdapter.addItem(R.string.main_mojhosto, R.drawable.mojhosto_icon);
		mAdapter.addHeader(R.string.main_extras);
		mAdapter.addItem(R.string.main_force_update_title);
		mAdapter.addItem(R.string.main_settings_title);
		mAdapter.addItem(R.string.main_donate_title);
		mAdapter.addItem(R.string.main_whats_new_title);
		mAdapter.addItem(R.string.main_about_title);
		setListAdapter(mAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.sliding_menu_list, null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		SlidingMenuItem item = mAdapter.getItem(position);
		Fragment fragment = null;
		switch (item.title) {
		case R.string.main_card_search: 
			fragment = new SearchViewFragment();
			break;
		case R.string.main_life_counter: 
			fragment = new LifeFragment();
			break;
		case R.string.main_mana_pool:
			fragment = new ManaPoolFragment();
			break;
		case R.string.main_dice: 
			fragment = new DiceFragment();
			break;
		case R.string.main_trade: 
			fragment = new TradeFragment();
			break;
		case R.string.main_wishlist: 
			fragment = new WishlistFragment();
			break;
		case R.string.main_timer: 
			fragment = new RoundTimerFragment();
			break;
		case R.string.main_rules: 
			fragment = new RulesFragment();
			break;
		case R.string.main_mojhosto: 
			fragment = new MoJhoStoFragment();
			break;
		case R.string.main_force_update_title:
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt("lastLegalityUpdate", 0);
			editor.commit();
			mActivity.startService(new Intent(mActivity, DbUpdaterService.class));
			mActivity.showAbove();
			break;
		case R.string.main_settings_title: 
			Intent i = new Intent(mActivity, PreferencesActivity.class);
			startActivity(i);
			break;
		case R.string.main_donate_title: 
			mActivity.showDialogFragment(MainActivity.DONATEDIALOG);
			break;
		case R.string.main_whats_new_title:
			mActivity.showDialogFragment(MainActivity.CHANGELOGDIALOG);
			break;
		case R.string.main_about_title: 
			mActivity.showDialogFragment(MainActivity.ABOUTDIALOG);
			break;
		}
		if (fragment != null)
			replaceFragment(fragment);
	}

	protected void replaceFragment(Fragment frag) {
		FragmentTransaction fragmentTransaction = mActivity.getSupportFragmentManager().beginTransaction();
		fragmentTransaction.replace(R.id.frag_view, frag);
		fragmentTransaction.commit();
		mActivity.hideKeyboard();
		mActivity.showAbove();
	}

	private class SlidingMenuItem {
		public int title;
		public int icon;
		public boolean isHeader;
		public SlidingMenuItem(int title, int icon, boolean isHeader) {
			this.title = title;
			this.icon = icon;
			this.isHeader = isHeader;
		}
	}

	private class SlidingMenuAdapter extends ArrayAdapter<SlidingMenuItem> {

		public SlidingMenuAdapter(Context context) {
			super(context, 0);
		}

		public void addItem(int title) {
			addItem(title, -1);
		}

		public void addItem(int title, int icon) {
			add(new SlidingMenuItem(title, icon, false));
		}

		public void addHeader(int title) {
			add(new SlidingMenuItem(title, -1, true));
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return getItem(position).isHeader ? 0 : 1;
		}

		@Override
		public boolean isEnabled(int position) {
			return !getItem(position).isHeader;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			SlidingMenuItem item = getItem(position);
			if (convertView == null) {
				int layout = item.isHeader ? R.layout.sliding_menu_header : R.layout.sliding_menu_item;
				convertView = LayoutInflater.from(getContext()).inflate(layout, null);
			}
			((TextView) convertView.findViewById(R.id.menu_title)).setText(item.title);
			ImageView icon = (ImageView) convertView.findViewById(R.id.menu_icon);
			if (icon != null) {
				if (item.icon > 0) {
					icon.setVisibility(View.VISIBLE);
					icon.setImageResource(item.icon);
				} else {
					icon.setVisibility(View.GONE);
				}
			}
			return convertView;
		}		

	}

}
