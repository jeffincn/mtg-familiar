package com.gelakinetic.mtgfam.fragments;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.actionbarsherlock.widget.SearchView;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.MainActivity;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;

public abstract class FamiliarFragment extends SherlockFragment {

	public CardDbAdapter								mDbHelper;
	protected ProgressDialog progDialog;
	private ImageSearchTask	mImageSearchTask;
	public static final String	DIALOG_TAG	= "dialog";

	public static final int 	ACTIVITY_CAMERA_GOGGLES		= 1;
	protected static final int GOGGLES_ANALYSIS = 1;
	private static final String TMP_IMG_FILENAME = "mtgfam-tmp.jpg";
	File tmp_img_file;

	protected LinearLayout mFragmentMenu;
	protected ViewGroup masterLayout;
	
	public FamiliarFragment() {
		/* http://developer.android.com/reference/android/app/Fragment.html
		 * All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			mDbHelper = new CardDbAdapter(this.getMainActivity());
		} catch (FamiliarDbException e) {
			getMainActivity().showDbErrorToast();
			getMainActivity().getSupportFragmentManager().popBackStack();
		}
		this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		progDialog = new ProgressDialog(this.getMainActivity());
		progDialog.setTitle("");
		progDialog.setMessage(getString(R.string.goggles_photo_analysis));
		progDialog.setIndeterminate(true);
		progDialog.setCancelable(true);

		if(mImageSearchTask != null) {
			progDialog.show();
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onDestroyView() {
		if(progDialog.isShowing()) {
			progDialog.cancel();
		}
		super.onDestroyView();
	}

	@Override
	public void onPause() {
		super.onPause();
		removeDialog();
		if(masterLayout != null && mFragmentMenu != null){
			masterLayout.removeView(mFragmentMenu);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Clear any results. We don't want them persisting past this fragment, and
		// they should have been looked at by now anyway
		getMainActivity().getFragmentResults();
		if(getMainActivity().mThreePane) {
			addFragmentMenu();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		//first saving my state, so the bundle wont be empty.
		//http://code.google.com/p/android/issues/detail?id=19917
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO && !canInterceptSearchKey()) {

			SearchView sv = new SearchView(getActivity());
			SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
			SearchableInfo si = searchManager.getSearchableInfo(getActivity().getComponentName());
			sv.setSearchableInfo(si);
			
			menu.add(R.string.name_search_hint)
				.setIcon(R.drawable.menu_search)
				.setActionView(sv)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		else {
			menu.add(R.string.name_search_hint)
				.setIcon(R.drawable.menu_search)
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

	/*
	 * Just return true if the search key is interceptable. This is checked when
	 * building the me
	 */
	public boolean canInterceptSearchKey() {
		return false;
	}

	protected void startNewFragment(FamiliarFragment frag, Bundle args){
		startNewFragment(frag, args, true);
	}

	protected void startNewFragment(FamiliarFragment frag, Bundle args, boolean allowBackStack) {
		frag.setArguments(args);
		this.getMainActivity().attachSingleFragment(frag, "left_frag", allowBackStack, true);
		this.getMainActivity().hideKeyboard();
	}

	void removeDialog() {
		try {
			FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
			Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
			if (prev != null) {
				ft.remove(prev);
			}
			ft.commit();
		}
		catch(NullPointerException e) {
			// eat it
		}
	}

	public FragmentManager getSupportFragmentManager() {
		return this.getFragmentManager();
	}

	protected void takePictureAndSearchImageIntent() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		tmp_img_file = new File (Environment.getExternalStorageDirectory(), TMP_IMG_FILENAME);
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmp_img_file));

		startActivityForResult(takePictureIntent, ACTIVITY_CAMERA_GOGGLES);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTIVITY_CAMERA_GOGGLES:
			switch (resultCode) {
			case android.app.Activity.RESULT_OK:
				mImageSearchTask = new ImageSearchTask();
				mImageSearchTask.execute(data);
				return;
			}
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public class ImageSearchTask extends AsyncTask<Intent, Void, Void> {

		private long	multiverseId;
		private String	name;

		@Override
		protected void onPreExecute() {
			try {
				progDialog.show();
			} catch (RuntimeException re) {
			}
		}

		@Override
		protected Void doInBackground(Intent... params) {
			try {
				if(tmp_img_file == null) {
					return null;
				}

				// First decode with inJustDecodeBounds=true to check dimensions
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				options.inPurgeable = true;
				BitmapFactory.decodeFile(tmp_img_file.getAbsolutePath(), options);

				// Calculate inSampleSize
				options.inSampleSize = options.outWidth / 280;

				// Decode bitmap with inSampleSize set
				options.inJustDecodeBounds = false;
				Bitmap mImageBitmap = BitmapFactory.decodeFile(tmp_img_file.getAbsolutePath(), options);

				// Compress to JPG
				ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
				mImageBitmap.compress(CompressFormat.JPEG, 75, baos);
				
				// Encode with base64
				byte[] base64image = (com.gelakinetic.mtgfam.helpers.Base64.encodeBytes(baos.toByteArray())).getBytes();

				// POST to the server
				URL url = new URL("http://93.103.149.115/search/image");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setUseCaches(false);
				conn.setRequestMethod("POST");
				OutputStream out = conn.getOutputStream();
				out.write(base64image);
				out.close();

				// Read result
				InputStream in = conn.getInputStream();
				StringBuffer sb = new StringBuffer();
				int chr;
				while ((chr = in.read()) != -1) {
					sb.append((char) chr);
				}
				in.close();

				// Parse the json
				final JSONObject obj = new JSONObject(sb.toString());
				multiverseId = obj.getInt("match");

				// Find the name, query the server again if necessary
				name = mDbHelper.getImageSearchNameFromMultiverseID(multiverseId);
				
				// Cleanup
				mImageBitmap.recycle();
				tmp_img_file.delete();
			} catch (Exception e) {
				multiverseId = -1;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void param) {
			System.gc(); // take out the trash
			try {
				progDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
			}

			if(multiverseId != -1) {
				onImageSearchSuccess(multiverseId, name);
			}
			else {
				try {
					Toast.makeText(getActivity(), R.string.goggles_no_card_on_photo,
							Toast.LENGTH_LONG).show();
				} catch (RuntimeException re) {
				}
			}
		}
	}

	protected void onImageSearchSuccess(long multiverseId, String name) {
		// this method must be overridden by each class calling takePictureAndSearchImageIntent
		mImageSearchTask = null;
	}
	
	public static void setKeyboardFocus(Bundle savedInstanceState, final EditText primaryTextField, final boolean selectAll, long postTime) {
		if (!(savedInstanceState != null && !savedInstanceState.isEmpty())){
			(new Handler()).postDelayed(new Runnable() {
				public void run() {
					primaryTextField.clearFocus();
					primaryTextField.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
					primaryTextField.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
					if(selectAll) {
						primaryTextField.selectAll();
					}
				}
			}, postTime);
		}
	}
	
	protected void addFragmentMenu() {
		if(getMainActivity().mThreePane && masterLayout != null && mFragmentMenu != null &&
				masterLayout.findViewWithTag(mFragmentMenu.getTag()) == null) {
			masterLayout.addView(mFragmentMenu);
		}
	}

	/*
	 * Override this method to receive messages from other fragments in three-pane mide
	 */
	public void receiveMessage(Bundle bundle) {
		;
	}
}
