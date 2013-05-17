/**
Copyright 2011 Adam Feinstein

This file is part of MTG Familiar.

MTG Familiar is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MTG Familiar is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.mtgfam.fragments;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.InFragmentMenuLoader;
import com.gelakinetic.mtgfam.helpers.FamiliarDbException;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;
import com.gelakinetic.mtgfam.helpers.PriceFetchRequest;
import com.gelakinetic.mtgfam.helpers.PriceInfo;
import com.gelakinetic.mtgfam.helpers.WishlistHelpers;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

public class CardViewFragment extends FamiliarFragment {

	// Dont use 0, thats the default when the back key is pressed
	public static final int						RANDOMLEFT			= 2;
	public static final int						RANDOMRIGHT			= 3;
	public static final int						QUITTOSEARCH		= 4;
	public static final int						SWIPELEFT				= 5;
	public static final int						SWIPERIGHT			= 6;

	// Dialogs
	private static final int					GETPRICE				= 1;
	private static final int					GETIMAGE				= 2;
	private static final int					CHANGESET				= 3;
	private static final int					CARDRULINGS			= 4;
	private static final int					WISHLIST_COUNTS	= 6;
	private static final int					GETLEGALITY			= 7;

	// Where the card image is loaded to
	private static final int					MAINPAGE				= 1;
	private static final int					DIALOG					= 2;

	// Random useful things
	private ImageGetter								imgGetter;
	private TextView									copyView;

	// UI elements
	private TextView									name;
	private TextView									cost;
	private TextView									type;
	private TextView									set;
	private TextView									ability;
	private TextView									pt;
	private TextView									flavor;
	private TextView									artist;
	private Button										transform;
	private Button										leftRandom;
	private Button										rightRandom;
	private ImageView									cardpic;
	private ImageView									DialogImageView;

	// Stuff for AsyncTasks
	private BitmapDrawable						cardPicture;
	private String[]									legalities;
	private String[]									formats;
	public ArrayList<Ruling>					rulingsArrayList;
	AsyncTask<Void, Void, Void>	asyncTask;

	// Card info
	private long											cardID;
	private String										number;
	private String										setCode;
	private String										cardName;
	private String										mtgi_code;
	private int												multiverseId;

	// Preferences
	private int												loadTo = 0;
	private static boolean										isRandom;
	private boolean										isSingle;
	private boolean										scroll_results;
	private View											myFragmentView;
	private String cardLanguage;

    private PriceInfo mPriceInfo;

	public CardViewFragment() {
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
		setInfoFromBundle(bundle);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		myFragmentView = inflater.inflate(R.layout.card_view_frag, container, false);

		masterLayout = (LinearLayout)myFragmentView.findViewById(R.id.master_layout);
		addFragmentMenu();
		
		name = (TextView) myFragmentView.findViewById(R.id.name);
		cost = (TextView) myFragmentView.findViewById(R.id.cost);
		type = (TextView) myFragmentView.findViewById(R.id.type);
		set = (TextView) myFragmentView.findViewById(R.id.set);
		ability = (TextView) myFragmentView.findViewById(R.id.ability);
		flavor = (TextView) myFragmentView.findViewById(R.id.flavor);
		artist = (TextView) myFragmentView.findViewById(R.id.artist);
		pt = (TextView) myFragmentView.findViewById(R.id.pt);
		transform = (Button) myFragmentView.findViewById(R.id.transformbutton);
		leftRandom = (Button) myFragmentView.findViewById(R.id.randomLeft);
		rightRandom = (Button) myFragmentView.findViewById(R.id.randomRight);

		imgGetter = ImageGetterHelper.GlyphGetter(getResources());

		registerForContextMenu(name);
		registerForContextMenu(cost);
		registerForContextMenu(type);
		registerForContextMenu(set);
		registerForContextMenu(ability);
		registerForContextMenu(pt);
		registerForContextMenu(flavor);
		registerForContextMenu(artist);

		progDialog = new ProgressDialog(this.getMainActivity());
		progDialog.setTitle("");
		progDialog.setMessage(getString(R.string.card_view_loading_dialog));
		progDialog.setIndeterminate(true);
		progDialog.setCancelable(true);
		progDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface pd) {
				// when the dialog is dismissed
				if(asyncTask != null) {
					asyncTask.cancel(true);
				}
			}
		});
		
		Bundle extras = this.getArguments();
		
		setInfoFromBundle(extras);

		return myFragmentView;
	}

	private void setInfoFromBundle(Bundle extras) {
		if(extras == null) {
			name.setText("");
			cost.setText("");
			type.setText("");
			set.setText("");
			ability.setText("");
			flavor.setText("");
			artist.setText("");
			pt.setText("");
			transform.setVisibility(View.GONE);
			leftRandom.setVisibility(View.GONE);
			rightRandom.setVisibility(View.GONE);
			return;
		}
		cardID = extras.getLong("id");
		isRandom = extras.getBoolean(SearchViewFragment.RANDOM);
		isSingle = extras.getBoolean("isSingle", false);
		if (getMainActivity().getPreferencesAdapter().getPicFirst()) {
			loadTo = MAINPAGE;
		}
		else {
			loadTo = DIALOG;
		}
		scroll_results = getMainActivity().getPreferencesAdapter().getScrollResults();
		cardLanguage = getMainActivity().getPreferencesAdapter().getCardLanguage();

		try {
			setInfoFromID(cardID);
		} catch (FamiliarDbException e) {
			getMainActivity().showDbErrorToast();
			getMainActivity().getSupportFragmentManager().popBackStack();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (isSingle) {
			Bundle res = new Bundle();
			res.putBoolean("isSingle", isSingle);
			this.getMainActivity().setFragmentResult(res);
		}

		if (progDialog != null && progDialog.isShowing()) {
			progDialog.cancel();
		}
		if (asyncTask != null) {
			asyncTask.cancel(true);
		}
	}

	private void setInfoFromID(long id) throws FamiliarDbException {

		cardPicture = null;

		Cursor c = mDbHelper.fetchCard(id, null);
		c.moveToFirst();

		// http://magiccards.info/scans/en/mt/55.jpg
		cardName = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
		setCode = c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET));

		mtgi_code = mDbHelper.getCodeMtgi(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)));
		number = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));

		switch ((char) c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY))) {
			case 'C':
			case 'c':
				set.setTextColor(this.getResources().getColor(R.color.common));
				break;
			case 'U':
			case 'u':
				set.setTextColor(this.getResources().getColor(R.color.uncommon));
				break;
			case 'R':
			case 'r':
				set.setTextColor(this.getResources().getColor(R.color.rare));
				break;
			case 'M':
			case 'm':
				set.setTextColor(this.getResources().getColor(R.color.mythic));
				break;
			case 'T':
			case 't':
				set.setTextColor(this.getResources().getColor(R.color.timeshifted));
				break;
		}

		String sCost = c.getString(c.getColumnIndex(CardDbAdapter.KEY_MANACOST));
		sCost = sCost.replace("{", "<img src=\"").replace("}", "\"/>");

		CharSequence csCost = ImageGetterHelper.jellyBeanHack(sCost, imgGetter, null);

		c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));

		name.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME)));
		cost.setText(csCost);
		type.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_TYPE)));
		set.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)));

		String sAbility = c.getString(c.getColumnIndex(CardDbAdapter.KEY_ABILITY)).replace("{", "<img src=\"")
				.replace("}", "\"/>");
		CharSequence csAbility = ImageGetterHelper.jellyBeanHack(sAbility, imgGetter, null);
		ability.setText(csAbility);

		String sFlavor = c.getString(c.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
		CharSequence csFlavor = ImageGetterHelper.jellyBeanHack(sFlavor, imgGetter, null);
		flavor.setText(csFlavor);

		artist.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_ARTIST)));

		int loyalty = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
		float p = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_POWER));
		float t = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
		if (loyalty != CardDbAdapter.NOONECARES) {
			pt.setText(Integer.valueOf(loyalty).toString());
		}
		else if (p != CardDbAdapter.NOONECARES && t != CardDbAdapter.NOONECARES) {

			String spt = "";

			if (p == CardDbAdapter.STAR)
				spt += "*";
			else if (p == CardDbAdapter.ONEPLUSSTAR)
				spt += "1+*";
			else if (p == CardDbAdapter.TWOPLUSSTAR)
				spt += "2+*";
			else if (p == CardDbAdapter.SEVENMINUSSTAR)
				spt += "7-*";
			else if (p == CardDbAdapter.STARSQUARED)
				spt += "*^2";
			else {
				if (p == (int) p) {
					spt += (int) p;
				}
				else {
					spt += p;
				}
			}

			spt += "/";

			if (t == CardDbAdapter.STAR)
				spt += "*";
			else if (t == CardDbAdapter.ONEPLUSSTAR)
				spt += "1+*";
			else if (t == CardDbAdapter.TWOPLUSSTAR)
				spt += "2+*";
			else if (t == CardDbAdapter.SEVENMINUSSTAR)
				spt += "7-*";
			else if (t == CardDbAdapter.STARSQUARED)
				spt += "*^2";
			else {
				if (t == (int) t) {
					spt += (int) t;
				}
				else {
					spt += t;
				}
			}

			pt.setText(spt);
		}
		else {
			pt.setText("");
		}

		boolean isMulticard = false;
		switch (CardDbAdapter.isMulticard(number, c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)))) {
			case CardDbAdapter.NOPE:
				isMulticard = false;
				transform.setVisibility(View.GONE);
				break;
			case CardDbAdapter.TRANSFORM:
				isMulticard = true;
				transform.setVisibility(View.VISIBLE);
				transform.setText(R.string.card_view_transform);
				break;
			case CardDbAdapter.FUSE:
				isMulticard = true;
				transform.setVisibility(View.VISIBLE);
				transform.setText(R.string.card_view_fuse);
				break;
			case CardDbAdapter.SPLIT:
				isMulticard = true;
				transform.setVisibility(View.VISIBLE);
				transform.setText(R.string.card_view_other_half);
				break;
		}


		if(isMulticard) {
			transform.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					cardPicture = null;
					if (number.contains("a")) {
						number = number.replace("a", "b");
					}
					else if (number.contains("b")) {
						number = number.replace("b", "a");
					}
					try {
						long id = mDbHelper.getTransform(setCode, number);
						setInfoFromID(id);
					} catch (FamiliarDbException e) {
						getMainActivity().showDbErrorToast();
						getMainActivity().getSupportFragmentManager().popBackStack();
					}
					
				}
			});
		}
		
		if (!isSingle && isRandom) {
			leftRandom.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Bundle res = new Bundle();
					res.putInt("resultCode", RANDOMLEFT);
					if(getMainActivity().mThreePane) {
						getMainActivity().sendMessageToMiddleFragment(res);
					}
					else {
						getMainActivity().setFragmentResult(res);
						getMainActivity().mFragmentManager.popBackStack();
					}
				}
			});
			rightRandom.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Bundle res = new Bundle();
					res.putInt("resultCode", RANDOMRIGHT);
					if(getMainActivity().mThreePane) {
						getMainActivity().sendMessageToMiddleFragment(res);
					}
					else {
						getMainActivity().setFragmentResult(res);
						getMainActivity().mFragmentManager.popBackStack();
					}
				}
			});
			leftRandom.setVisibility(View.VISIBLE);
			rightRandom.setVisibility(View.VISIBLE);
		}
		else if (!isSingle && scroll_results) {
			leftRandom.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Bundle res = new Bundle();
					res.putInt("resultCode", SWIPELEFT);
					res.putLong("lastID", cardID);
					if(getMainActivity().mThreePane) {
						getMainActivity().sendMessageToMiddleFragment(res);
					}
					else {
						getMainActivity().setFragmentResult(res);
						getMainActivity().mFragmentManager.popBackStack();
					}
				}
			});
			rightRandom.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Bundle res = new Bundle();
					res.putInt("resultCode", SWIPERIGHT);
					res.putLong("lastID", cardID);
					if(getMainActivity().mThreePane) {
						getMainActivity().sendMessageToMiddleFragment(res);
					}
					else {
						getMainActivity().setFragmentResult(res);
						getMainActivity().mFragmentManager.popBackStack();
					}
				}
			});
			leftRandom.setVisibility(View.VISIBLE);
			rightRandom.setVisibility(View.VISIBLE);
		}
		else {
			leftRandom.setVisibility(View.GONE);
			rightRandom.setVisibility(View.GONE);
		}

		if (loadTo == MAINPAGE) {
			cardpic = (ImageView) myFragmentView.findViewById(R.id.cardpic);
			cardpic.setVisibility(View.VISIBLE);
			
			name.setVisibility(View.GONE);
			cost.setVisibility(View.GONE);
			type.setVisibility(View.GONE);
			set.setVisibility(View.GONE);
			ability.setVisibility(View.GONE);
			pt.setVisibility(View.GONE);
			flavor.setVisibility(View.GONE);
			artist.setVisibility(View.GONE);
			((FrameLayout) myFragmentView.findViewById(R.id.frameLayout1)).setVisibility(View.GONE);

			progDialog.show();
			asyncTask = new FetchPictureTask();
			asyncTask.execute((Void[]) null);
		}
		else {
			((ImageView) myFragmentView.findViewById(R.id.cardpic)).setVisibility(View.GONE);
			
			name.setVisibility(View.VISIBLE);
			cost.setVisibility(View.VISIBLE);
			type.setVisibility(View.VISIBLE);
			set.setVisibility(View.VISIBLE);
			ability.setVisibility(View.VISIBLE);
			pt.setVisibility(View.VISIBLE);
			flavor.setVisibility(View.VISIBLE);
			artist.setVisibility(View.VISIBLE);
			((FrameLayout) myFragmentView.findViewById(R.id.frameLayout1)).setVisibility(View.VISIBLE);			
		}

		multiverseId = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_MULTIVERSEID));

		c.close();
	}

	private class FetchLegalityTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			try {
				Cursor cFormats = mDbHelper.fetchAllFormats();
				formats = new String[cFormats.getCount()];
				legalities = new String[cFormats.getCount()];
	
				cFormats.moveToFirst();
				for (int i = 0; i < cFormats.getCount(); i++) {
					formats[i] = cFormats.getString(cFormats.getColumnIndex(CardDbAdapter.KEY_NAME));
					switch (mDbHelper.checkLegality(cardName, formats[i])) {
						case CardDbAdapter.LEGAL:
							legalities[i] = getString(R.string.card_view_legal);
							break;
						case CardDbAdapter.RESTRICTED:
							legalities[i] = getString(R.string.card_view_restricted);
							break;
						case CardDbAdapter.BANNED:
							legalities[i] = getString(R.string.card_view_banned);
							break;
						default:
							legalities[i] = getString(R.string.card_view_error);
							break;
					}
					cFormats.moveToNext();
				}
	
				cFormats.close();
			}
			catch(FamiliarDbException e) {
				legalities = null;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				progDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
			}

			showDialog(GETLEGALITY);
		}

		@Override
		protected void onCancelled() {
			// TODO something when canceled?
		}

	}

	private class FetchPictureTask extends AsyncTask<Void, Void, Void> {

		private String	error;

		@SuppressWarnings("deprecation") // getHeight() / getWidth() deprecated as of API13, in favor of getSize()

		@Override
		protected Void doInBackground(Void... params) {
			error = null;
			String lang = cardLanguage;

			boolean bRetry = true;
			
			while (bRetry){
				
				bRetry = false;
				
				try {
	
					String picurl;
					if (setCode.equals("PP2")) {
						picurl = "http://magiccards.info/extras/plane/planechase-2012-edition/" + cardName + ".jpg";
						picurl = picurl.replace(" ", "-").replace(Character.toChars(0xC6)[0]+"", "Ae").replace("?", "").replace(",", "").replace("'", "")
								.replace("!", "");
					}
					else if (setCode.equals("PCP")) {
						if (cardName.equalsIgnoreCase("tazeem")) {
							cardName = "tazeem-release-promo";
							picurl = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
						}
						if (cardName.equalsIgnoreCase("celestine reef")) {
							cardName = "celestine-reef-pre-release-promo";
							picurl = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
						}
						if (cardName.equalsIgnoreCase("horizon boughs")) {
							cardName = "horizon-boughs-gateway-promo";
							picurl = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
						}
						else {
							picurl = "http://magiccards.info/extras/plane/planechase/" + cardName + ".jpg";
						}
						picurl = picurl.replace(" ", "-").replace(Character.toChars(0xC6)[0]+"", "Ae").replace("?", "").replace(",", "").replace("'", "")
								.replace("!", "");
					}
					else if (setCode.equals("ARS")) {
						picurl = "http://magiccards.info/extras/scheme/archenemy/" + cardName + ".jpg";
						picurl = picurl.replace(" ", "-").replace(Character.toChars(0xC6)[0]+"", "Ae").replace("?", "").replace(",", "").replace("'", "")
								.replace("!", "");
					}
					else {
						picurl = "http://magiccards.info/scans/" + lang + "/" + mtgi_code + "/" + number + ".jpg";
					}
					picurl = picurl.toLowerCase(Locale.ENGLISH);
	
					URL u = new URL(picurl);
					cardPicture = new BitmapDrawable(getMainActivity().getResources(), u.openStream());
	
					int height = 0, width = 0;
					float scale = 0;
					int border = 16;
					Display display = ((WindowManager) getMainActivity().getSystemService(Context.WINDOW_SERVICE))
							.getDefaultDisplay();
					if (loadTo == MAINPAGE) {
						Rect rectgle = new Rect();
						Window window = getMainActivity().getWindow();
						window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
	
						LinearLayout scrollButtons = (LinearLayout) myFragmentView.findViewById(R.id.scrollButtons);
	
						height = (display.getHeight() - rectgle.top - getMainActivity().getSupportActionBar().getHeight() - scrollButtons
								.getHeight()) - border;
						width = display.getWidth() - border;
					}
					else if (loadTo == DIALOG) {
						height = display.getHeight() - border;
						width = display.getWidth() - border;
					}
	
					float screenAspectRatio = (float) height / (float) (width);
					float cardAspectRatio = (float) cardPicture.getIntrinsicHeight() / (float) cardPicture.getIntrinsicWidth();
	
					if (screenAspectRatio > cardAspectRatio) {
						scale = (width) / (float) cardPicture.getIntrinsicWidth();
					}
					else {
						scale = (height) / (float) cardPicture.getIntrinsicHeight();
					}
	
					int newWidth = Math.round(cardPicture.getIntrinsicWidth() * scale);
					int newHeight = Math.round(cardPicture.getIntrinsicHeight() * scale);
	
					cardPicture = resize(cardPicture, newWidth, newHeight);
				}
				catch (FileNotFoundException e) {
					// internet works, image not found
					if (lang == "en") {
						error = "Image Not Found";
					} else {
						// If image doesn't exist in the preferred language, let's retry with "en"
						lang = "en";
						bRetry = true;
					}
				}
				catch (ConnectException e) {
					// no internet
					error = "No Internet Connection";
				}
				catch (UnknownHostException e) {
					// no internet
					error = "No Internet Connection";
				}
				catch (MalformedURLException e) {
					error = "MalformedURLException";
				}
				catch (IOException e) {
					error = "No Internet Connection";
				}
				catch (NullPointerException e) {
					error = "Image load failed. Please try again later.";
				}
			}
			return null;
		}

		private BitmapDrawable resize(BitmapDrawable image, int newWidth, int newHeight) {
			Bitmap d = ((BitmapDrawable) image).getBitmap();
			Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, newWidth, newHeight, true);
			return new BitmapDrawable(getMainActivity().getResources(), bitmapOrig);
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				progDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
			}
			if (error == null) {
				if (loadTo == DIALOG) {
					removeDialog();
					showDialog(GETIMAGE);
				}
				else if (loadTo == MAINPAGE) {
					cardpic.setImageDrawable(cardPicture);
				}
			}
			else {
				if (loadTo == MAINPAGE) {
					cardpic.setVisibility(View.GONE);
					name.setVisibility(View.VISIBLE);
					cost.setVisibility(View.VISIBLE);
					type.setVisibility(View.VISIBLE);
					set.setVisibility(View.VISIBLE);
					ability.setVisibility(View.VISIBLE);
					pt.setVisibility(View.VISIBLE);
					flavor.setVisibility(View.VISIBLE);
					artist.setVisibility(View.VISIBLE);
					((FrameLayout) myFragmentView.findViewById(R.id.frameLayout1)).setVisibility(View.VISIBLE);
				}
			}
		}

		@Override
		protected void onCancelled() {
			// TODO something when canceled?
			if (loadTo == MAINPAGE) {
				cardpic.setVisibility(View.GONE);
				name.setVisibility(View.VISIBLE);
				cost.setVisibility(View.VISIBLE);
				type.setVisibility(View.VISIBLE);
				set.setVisibility(View.VISIBLE);
				ability.setVisibility(View.VISIBLE);
				pt.setVisibility(View.VISIBLE);
				flavor.setVisibility(View.VISIBLE);
				artist.setVisibility(View.VISIBLE);
				((FrameLayout) myFragmentView.findViewById(R.id.frameLayout1)).setVisibility(View.VISIBLE);
			}
		}
	}

	private class FetchRulingsTask extends AsyncTask<Void, Void, Void> {

		private boolean	error	= false;

		@Override
		protected Void doInBackground(Void... params) {

			URL url;
			InputStream is = null;
			BufferedReader br;
			String line;

			rulingsArrayList = new ArrayList<Ruling>();

			try {
				url = new URL("http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + multiverseId);
				is = url.openStream(); // throws an IOException
				br = new BufferedReader(new InputStreamReader(new BufferedInputStream(is)));

				String date = null, ruling;
				while ((line = br.readLine()) != null) {
					if (line.contains("rulingDate") && line.contains("<td")) {
						date = (line.replace("<autocard>", "").replace("</autocard>", "")).split(">")[1].split("<")[0];
					}
					if (line.contains("rulingText") && line.contains("<td")) {
						ruling = (line.replace("<autocard>", "").replace("</autocard>", "")).split(">")[1].split("<")[0];
						Ruling r = new Ruling(date, ruling);
						rulingsArrayList.add(r);
					}
				}
			}
			catch (MalformedURLException mue) {
				error = true;
			}
			catch (IOException ioe) {
				error = true;
			}
			finally {
				try {
					if (is != null) {
						is.close();
					}
				}
				catch (IOException ioe) {
					error = true;
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				progDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
			}

			if (!error) {
				showDialog(CARDRULINGS);
			}
			else {
				Toast.makeText(getMainActivity(), "No Internet Connection", Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected void onCancelled() {
			// TODO something when canceled?
		}
	}

	private static class Ruling {
		public String	date, ruling;

		public Ruling(String d, String r) {
			date = d;
			ruling = r;
		}

		public String toString() {
			return date + ": " + ruling;
		}
	}

	protected void showDialog(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}

		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			private WishlistHelpers	wh = new WishlistHelpers();

			@Override
			public void onDismiss(DialogInterface dialog) {
				super.onDismiss(dialog);
				switch(id) {
					case WISHLIST_COUNTS:
						wh.onDialogDismissed();
						break;
				}
			}
			
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				
				setShowsDialog(true);
				switch (id) {
					case GETIMAGE: {

						if (cardPicture == null) {
							return new Dialog(this.getMainActivity());
						}

						Dialog dialog = new Dialog(this.getMainActivity());
						dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

						dialog.setContentView(R.layout.image_dialog);

						DialogImageView = (ImageView) dialog.findViewById(R.id.cardimage);
						DialogImageView.setImageDrawable(cardPicture);

						return dialog;
					}
					case GETLEGALITY: {
						if (formats == null) {
							setShowsDialog(false);
							return null;
						}
						if(legalities == null) {
							getMainActivity().showDbErrorToast();
							getMainActivity().getSupportFragmentManager().popBackStack();
							setShowsDialog(false);
							return null;
						}

						Dialog dialog = new Dialog(this.getMainActivity());
						dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

						dialog.setContentView(R.layout.legality_dialog);

						// create the grid item mapping
						String[] from = new String[] { "format", "status" };
						int[] to = new int[] { R.id.format, R.id.status };

						// prepare the list of all records
						List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
						for (int i = 0; i < formats.length; i++) {
							HashMap<String, String> map = new HashMap<String, String>();
							map.put(from[0], formats[i]);
							map.put(from[1], legalities[i]);
							fillMaps.add(map);
						}

						SimpleAdapter adapter = new SimpleAdapter(this.getMainActivity(), fillMaps, R.layout.legal_row, from,
								to);
						ListView lv = (ListView) dialog.findViewById(R.id.legallist);
						lv.setAdapter(adapter);
						return dialog;
					}
					case GETPRICE: { // price

						Dialog dialog = new Dialog(this.getMainActivity());
						dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

						dialog.setContentView(R.layout.price_dialog);

						TextView l = (TextView) dialog.findViewById(R.id.low);
						TextView m = (TextView) dialog.findViewById(R.id.med);
						TextView h = (TextView) dialog.findViewById(R.id.high);
						TextView f = (TextView) dialog.findViewById(R.id.foil);
						TextView pricelink = (TextView) dialog.findViewById(R.id.pricelink);

						l.setText(String.format("$%1$,.2f", mPriceInfo.low));
						m.setText(String.format("$%1$,.2f", mPriceInfo.average));
						h.setText(String.format("$%1$,.2f", mPriceInfo.high));
						
						if(mPriceInfo.foil_average != 0) {
							f.setText(String.format("$%1$,.2f", mPriceInfo.foil_average));
						}
						else {
							f.setVisibility(View.GONE);
							dialog.findViewById(R.id.foil_label).setVisibility(View.GONE);
						}
						pricelink.setMovementMethod(LinkMovementMethod.getInstance());
						pricelink.setText(ImageGetterHelper.jellyBeanHack("<a href=\"" + mPriceInfo.url + "\">"
								+ getString(R.string.card_view_price_dialog_link) + "</a>"));
						return dialog;
					}
					case CHANGESET: {
						try {
							Cursor c = mDbHelper.fetchCardByName(cardName,
									new String[] { CardDbAdapter.KEY_SET, CardDbAdapter.KEY_ID });
							Set<String> sets = new LinkedHashSet<String>();
							Set<Long> cardIds = new LinkedHashSet<Long>();
							while (!c.isAfterLast()) {
								if (sets.add(mDbHelper.getTCGname(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET))))) {
									cardIds.add(c.getLong(c.getColumnIndex(CardDbAdapter.KEY_ID)));
								}
								c.moveToNext();
							}
							c.close();

							final String[] aSets = sets.toArray(new String[sets.size()]);
							final Long[] aIds = cardIds.toArray(new Long[cardIds.size()]);
							AlertDialog.Builder builder = new AlertDialog.Builder(this.getMainActivity());
							builder.setTitle(R.string.card_view_set_dialog_title);
							builder.setItems(aSets, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialogInterface, int item) {
									try {
										setInfoFromID(aIds[item]);
									} catch (FamiliarDbException e) {
										getMainActivity().showDbErrorToast();
										getMainActivity().getSupportFragmentManager().popBackStack();
									}
								}
							});
							return builder.create();
						}
						catch (SQLException e) {
							getMainActivity().showDbErrorToast();
							getMainActivity().getSupportFragmentManager().popBackStack();
						} catch (FamiliarDbException e) {
							getMainActivity().showDbErrorToast();
							getMainActivity().getSupportFragmentManager().popBackStack();
						}
					}
					case CARDRULINGS: {

						if (rulingsArrayList == null) {
							setShowsDialog(false);
							return null;
						}

						Dialog dialog = new Dialog(this.getMainActivity());
						dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

						dialog.setContentView(R.layout.rulings_dialog);

						TextView textViewRules = (TextView) dialog.findViewById(R.id.rules);
						TextView textViewUrl = (TextView) dialog.findViewById(R.id.url);

						String message = "";
						if (rulingsArrayList.size() == 0) {
							message = getString(R.string.card_view_no_rulings);
						}
						else {
							for (Ruling r : rulingsArrayList) {
								message += (r.toString() + "<br><br>");
							}

							message = message.replace("{Tap}", "{T}").replace("{", "<img src=\"").replace("}", "\"/>");
						}
						CharSequence messageGlyph = ImageGetterHelper.jellyBeanHack(message, imgGetter, null);

						textViewRules.setText(messageGlyph);

						textViewUrl.setMovementMethod(LinkMovementMethod.getInstance());
						textViewUrl.setText(Html
								.fromHtml("<a href=http://gatherer.wizards.com/Pages/Card/Details.aspx?multiverseid=" + multiverseId
										+ ">" + getString(R.string.card_view_gatherer_page) + "</a>"));

						return dialog;
					}
					case WISHLIST_COUNTS: {
						try {
							return wh.getDialog(cardName, CardViewFragment.this, this.getMainActivity());
						} catch (FamiliarDbException e) {
							getMainActivity().showDbErrorToast();
							getMainActivity().getSupportFragmentManager().popBackStack();
							setShowsDialog(false);
							return null;
						}
					}
					default: {
						savedInstanceState.putInt("id", id);
						return super.onCreateDialog(savedInstanceState);
					}
				}
			}
		};
		newFragment.show(ft, DIALOG_TAG);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		copyView = (TextView) v;

		android.view.MenuInflater inflater = this.getMainActivity().getMenuInflater();
		inflater.inflate(R.menu.copy_menu, menu);
	}

	private static final boolean	useOldClipboard	= (android.os.Build.VERSION.SDK_INT < 11);

	@SuppressLint({ "NewApi" })
	@SuppressWarnings("deprecation") // android.text.ClipboardManager is deprecated as of API11

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		// use a final static boolean for JIT compile-time culling of deprecated
		// calls for future-proofing
		// this is probably overkill because the old name space will likely be
		// retained for backwards compatibility
		// Scoped name space usage is poor practice, but direct references allow
		// us
		// to target the correct SDK.
		String copyText = "";
		switch (item.getItemId()) {
			case R.id.copy:
				copyText = copyView.getText().toString();
				break;
			case R.id.copyall:
				copyText = name.getText().toString() + '\n' + cost.getText().toString() + '\n' + type.getText().toString()
						+ '\n' + set.getText().toString() + '\n' + ability.getText().toString() + '\n'
						+ flavor.getText().toString() + '\n' + pt.getText().toString() + '\n' + artist.getText().toString();
				break;
			default:
				return super.onContextItemSelected(item);
		}

		if (useOldClipboard) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) this.getMainActivity()
					.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
			clipboard.setText(copyText);
			return true;
		}
		else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) this.getMainActivity()
					.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
			clipboard.setText(copyText);
			return true;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(cardName == null) {
			//disable menu buttons if the card isn't initialized
			return false;
		}
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.image:
				progDialog.show();
				asyncTask = new FetchPictureTask();
				asyncTask.execute((Void[]) null);
				return true;
			case R.id.price:
				progDialog.show();
				
				PriceFetchRequest priceRequest = new PriceFetchRequest(cardName, setCode, number, multiverseId,mDbHelper);
				getMainActivity().getSpiceManager().execute( priceRequest, cardName + "-" + setCode, DurationInMillis.ONE_DAY, new RequestListener< PriceInfo >(){

					@Override
			        public void onRequestFailure( SpiceException spiceException ) {
			        	progDialog.dismiss();
			        	Toast.makeText( getMainActivity(), spiceException.getMessage(), Toast.LENGTH_SHORT ).show();
			        }

			        @Override
			        public void onRequestSuccess( final PriceInfo result ) {
			        	progDialog.dismiss();
			        	if (result != null) {
			        		mPriceInfo = result;
			        		showDialog(GETPRICE);
			        	}
			        	else {
			        		Toast.makeText( getMainActivity(), R.string.card_view_price_not_found, Toast.LENGTH_SHORT ).show();
			        	}
			        }
				} );

				return true;
			case R.id.changeset:
				showDialog(CHANGESET);
				return true;
			case R.id.legality:
				progDialog.show();
				asyncTask = new FetchLegalityTask();
				asyncTask.execute((Void[]) null);
				return true;
			case R.id.cardrulings:
				progDialog.show();
				asyncTask = new FetchRulingsTask();
				asyncTask.execute((Void[]) null);
				return true;
			case R.id.addtowishlist:
				showDialog(WISHLIST_COUNTS);
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
			cml.inflate(R.menu.card_menu, menu);
			mFragmentMenu = cml.getView();
			addFragmentMenu();
		}
		else {
			inflater.inflate(R.menu.card_menu, menu);
		}
	}	
}