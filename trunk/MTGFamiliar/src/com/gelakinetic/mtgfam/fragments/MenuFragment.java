package com.gelakinetic.mtgfam.fragments;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.activities.PreferencesActivity;
import com.gelakinetic.mtgfam.helpers.DbUpdaterService;

public class MenuFragment extends ListFragment {

	private MainActivity mActivity;
	private SlidingMenuAdapter mAdapter;

	public MenuFragment() {
		/* http://developer.android.com/reference/android/app/Fragment.html
		 * All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// TODO just to be safe?
		if (!(getActivity() instanceof MainActivity)) {			
			throw new IllegalStateException(
					"MenuFragment must be attached to an instance of MainActivity");
		}
		if (getActivity().findViewById(R.id.frag_view) == null) {
			throw new IllegalStateException(
					"MenuFragment must be attached to an Activity with R.id.frag_view");
		}
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
		mAdapter.addItem(R.string.main_judges_corner, R.drawable.rules_icon);
		mAdapter.addItem(R.string.main_mojhosto, R.drawable.mojhosto_icon);
		mAdapter.addHeader(R.string.main_extras);
		mAdapter.addItem(R.string.main_settings_title);
		mAdapter.addItem(R.string.main_force_update_title);
		mAdapter.addItem(R.string.main_export_data_title);
		mAdapter.addItem(R.string.main_import_data_title);
		mAdapter.addItem(R.string.main_whats_new_title);
		mAdapter.addItem(R.string.main_about_title);
		mAdapter.addItem(R.string.main_donate_title);
		setListAdapter(mAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
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
		case R.string.main_judges_corner:
			fragment = new JudgesCornerFragment();
			break;
		case R.string.main_mojhosto:
			fragment = new MoJhoStoFragment();
			break;
		case R.string.main_force_update_title:
			mActivity.getPreferencesAdapter().setLastLegalityUpdate(0);
			mActivity.startService(new Intent(mActivity, DbUpdaterService.class));
			mActivity.showContent();
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
		case R.string.main_import_data_title: {
			File sdCard = Environment.getExternalStorageDirectory();
			File zipIn = new File(sdCard, "MTGFamiliarBackup.zip");
			try {
				unZipIt(new ZipFile(zipIn));
				Toast.makeText(mActivity, getString(R.string.main_import_success), Toast.LENGTH_SHORT).show();
			} catch (ZipException e) {
				Toast.makeText(mActivity, getString(R.string.main_import_fail), Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				Toast.makeText(mActivity, getString(R.string.main_import_fail), Toast.LENGTH_SHORT).show();
			}
			mActivity.showContent();
			break;
		}
		case R.string.main_export_data_title: {
			ArrayList<File> files = findAllFiles(mActivity.getFilesDir());

			File sdCard = Environment.getExternalStorageDirectory();
			File zipOut = new File(sdCard, "MTGFamiliarBackup.zip");
			if(zipOut.exists()) {
				zipOut.delete();
			}
			try {
				zipIt(zipOut, files);
				Toast.makeText(mActivity, getString(R.string.main_export_success) + " " + zipOut.getAbsolutePath(), Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				Toast.makeText(mActivity, getString(R.string.main_export_fail), Toast.LENGTH_SHORT).show();				
			}
			mActivity.showContent();
			break;
		}
		}
		if (fragment != null) {
			switchContent(fragment);
		}
	}
	
	ArrayList<File> findAllFiles(File dir) {
		ArrayList<File> files = new  ArrayList<File>();
		for(File tmp : dir.listFiles()) {
			if(tmp.isDirectory()) {
				files.addAll(findAllFiles(tmp));
			}
			else {
				files.add(tmp);
			}
		}
		return files;
	}

	public void unZipIt(ZipFile zipFile) throws IOException {
		Enumeration<? extends ZipEntry> entries;

		entries = zipFile.entries();

		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();

			if (entry.isDirectory()) {
				// Assume directories are stored parents first then children.
				// This is not robust, just for demonstration purposes.
				(new File(entry.getName())).mkdir();
				continue;
			}
			String[] path = entry.getName().split("/");
			String pathCat = "";
			if(path.length > 1) {
				for(int i=0; i < path.length-1; i++) {
					pathCat += path[i] + "/";
					File tmp = new File(mActivity.getFilesDir(), pathCat);
					if(!tmp.exists()) {
						tmp.mkdir();
					}
				}
			}

			InputStream in = zipFile.getInputStream(entry);
			OutputStream out = new BufferedOutputStream(new FileOutputStream(
					new File(mActivity.getFilesDir(), entry.getName())));
			byte[] buffer = new byte[1024];
			int len;
			while ((len = in.read(buffer)) >= 0) {
				out.write(buffer, 0, len);
			}

			in.close();
			out.close();
		}

		zipFile.close();
	}

	public void zipIt(File zipFile, ArrayList<File> files) throws IOException {

		byte[] buffer = new byte[1024];

		FileOutputStream fos = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(fos);
		int fileDirLen = (int) mActivity.getFilesDir().getAbsolutePath().length()+1;
		for (File file : files) {
			ZipEntry ze = new ZipEntry(file.getAbsolutePath().substring(fileDirLen));
			zos.putNextEntry(ze);

			FileInputStream in = new FileInputStream(file);

			int len;
			while ((len = in.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}
			in.close();
		}

		zos.closeEntry();
		zos.close();
	}

	public void switchContent(final Fragment fragment) {
		
		// Clear the backstack, otherwise replacing a cardview after a search
		// messes up the hierarchy
		for(int i=0; i < mActivity.getSupportFragmentManager().getBackStackEntryCount(); i++) {
			mActivity.getSupportFragmentManager().popBackStack();
		}
		
		mActivity.getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.frag_view, fragment)
		.commit();
		Handler h = new Handler();
		h.postDelayed(new Runnable() {
			public void run() {
				mActivity.getSlidingMenu().showContent();
			}
		}, 50);
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
				int layout = item.isHeader ? R.layout.sliding_menu_header
						: R.layout.sliding_menu_item;
				convertView = LayoutInflater.from(getContext()).inflate(layout,
						null);
			}
			((TextView) convertView.findViewById(R.id.menu_title))
					.setText(item.title);
			ImageView icon = (ImageView) convertView
					.findViewById(R.id.menu_icon);
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
