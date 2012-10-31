package com.gelakinetic.mtgfam.fragments;

import java.io.IOException;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.GoogleGoggles;
import com.gelakinetic.mtgfam.helpers.MyApp;

public class FamiliarFragment extends SherlockFragment {

	public CardDbAdapter								mDbHelper;
	protected FamiliarFragment	anchor;
	public static final String	DIALOG_TAG	= "dialog";

	public static final int 	ACTIVITY_CAMERA_GOGGLES		= 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		anchor = this;
		mDbHelper = new CardDbAdapter(this.getMainActivity());
		try {
			mDbHelper.openReadable();
		} catch (FamiliarDbException e) {
			mDbHelper.showDbErrorToast(this.getActivity());
			this.getMainActivity().getFragmentManager().popBackStack();
		}
		this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		anchor = this;
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onPause() {
		super.onPause();
		removeDialog();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		MyApp appState = ((MyApp) getActivity().getApplicationContext());
		String classname = this.getClass().getCanonicalName();
		if (classname.equalsIgnoreCase("com.gelakinetic.mtgfam.fragments.CardViewFragment")) {
			if (appState.getState() == CardViewFragment.QUITTOSEARCH) {
				if (this.getMainActivity().mFragmentManager.getBackStackEntryCount() == 0) {
					getActivity().finish();
				}
				else {
					getMainActivity().mFragmentManager.popBackStack();
				}
				return;
			}
		}
		else {
			appState.setState(0);
		}

		// Clear any results. We don't want them persisting past this fragment, and
		// they should have been looked at by now anyway
		getMainActivity().getFragmentResults();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		menu.clear();
		menu.add(R.string.name_search_hint).setIcon(R.drawable.menu_search)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								try{
									new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_SEARCH);
								}
								catch(java.lang.SecurityException e){
									//apparently this can inject an event into another app if the user switches fast enough
								}
							}
						}).start();
						return true;
					}
				}).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	public MainActivity getMainActivity() {
		return (MainActivity) this.getActivity();
	}

	/*
	 * When the search key is pressed, it will tell the fragment If the fragment
	 * doesn't care what happens, return false Otherwise override this, do
	 * whatever, and return true
	 */
	public boolean onInterceptSearchKey() {
		return false;
	}
	
	protected void startNewFragment(FamiliarFragment frag, Bundle args){
		startNewFragment(frag, args, true);
	}

	protected void startNewFragment(FamiliarFragment frag, Bundle args, boolean allowBackStack) {
		frag.setArguments(args);

		FragmentTransaction fragmentTransaction = this.getMainActivity().mFragmentManager.beginTransaction();
		if (allowBackStack) {
			fragmentTransaction.addToBackStack(null);
		}
		
		fragmentTransaction.replace(R.id.frag_view, frag);
		fragmentTransaction.commit();
		this.getMainActivity().hideKeyboard();
	}

	void removeDialog() {
		try {
			FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
			Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
			if (prev != null) {
				ft.remove(prev);
			}
			ft.commit();
		}
		catch(NullPointerException e) {
			// eat it
		}
	}
	
    protected void takePictureAndSearchGoogleGogglesIntent() {
    	Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, ACTIVITY_CAMERA_GOGGLES);

    }

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case ACTIVITY_CAMERA_GOGGLES:
				switch (resultCode) {
				case android.app.Activity.RESULT_OK:
					new GoogleGogglesTask().execute(data);
					return;
				}
				return;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private class GoogleGogglesTask extends AsyncTask<Intent, Void, Void> {

		private String	cardName;

		@Override
		protected void onPreExecute() {
			try {
				Toast.makeText(anchor.getActivity(),R.string.goggles_photo_analysis,
						Toast.LENGTH_LONG).show();
			} catch (RuntimeException re) {
			}
		}

		@Override
		protected Void doInBackground(Intent... params) {
			try {
			    Bitmap mImageBitmap = (Bitmap) params[0].getExtras().get("data");
					
			    cardName = "";
			    try {
			    	cardName = GoogleGoggles.StartCardSearch(mImageBitmap, anchor.getActivity());

				} catch (IOException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
			} catch (Exception e) {
				cardName = null;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void param) {
			if(cardName != null && cardName.length() > 0) {
				onGoogleGogglesSuccess(cardName);
			}
			else {
				try {
					Toast.makeText(anchor.getActivity(), R.string.goggles_no_card_on_photo,
							Toast.LENGTH_LONG).show();
				} catch (RuntimeException re) {
				}
			}
		}
	}

    protected void onGoogleGogglesSuccess(String cardName) {
    	// this method must be overridden by each class calling takePictureAndSearchGoogleGogglesIntent
	}
}
