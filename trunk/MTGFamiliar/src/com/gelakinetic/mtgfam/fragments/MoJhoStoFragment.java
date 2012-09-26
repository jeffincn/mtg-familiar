package com.gelakinetic.mtgfam.fragments;

import java.util.Random;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.helpers.CardDbAdapter;
import com.gelakinetic.mtgfam.helpers.ImageGetterHelper;

public class MoJhoStoFragment extends FamiliarFragment {

	private Random							rand;
	private String							name;
	private Spinner							momirCmcChoice;
	private String[]						cmcChoices;
	private Button							momirButton;
	private Button							stonehewerButton;
	private Spinner							stonehewerCmcChoice;
	private Button							jhoiraInstantButton;
	private Button							jhoiraSorceryButton;
	private ImageView						stonehewerImage;
	private ImageView						momirImage;
	private ImageView						jhoiraImage;
	private OnClickListener			jhoiraSorceryListener;
	private OnClickListener			jhoiraInstantListener;
	private OnClickListener			stonehewerListener;
	private OnClickListener			momirListener;

	private static final int		RULESDIALOG				= 1;
	protected static final int	MOMIR_IMAGE				= 2;
	protected static final int	STONEHEWER_IMAGE	= 3;
	protected static final int	JHOIRA_IMAGE			= 4;
	protected static final int	CORRUPTION				= 5;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gelakinetic.fragments.FamiliarFragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		momirListener = new View.OnClickListener() {

			public void onClick(View v) {
				int cmc;
				try {
					cmc = Integer.parseInt(cmcChoices[momirCmcChoice.getSelectedItemPosition()]);
				}
				catch (NumberFormatException e) {
					cmc = -1;
				}

				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };

				try {
					Cursor doods = mDbHelper.Search(null, null, "Creature", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
							CardDbAdapter.NOONECARES, null, cmc, "=", null, null, null, null, 0, 0, CardDbAdapter.MOSTRECENTPRINTING,
							false, returnTypes, true);

					int pos = rand.nextInt(doods.getCount());
					doods.moveToPosition(pos);
					name = doods.getString(doods.getColumnIndex(CardDbAdapter.KEY_NAME));
					doods.close();

					// add a fragment
					Bundle args = new Bundle();
					args.putLong("id", mDbHelper.fetchIdByName(name));
					ResultListFragment rlFrag = new ResultListFragment();
					anchor.startNewFragment(rlFrag, args);
				}
				catch (SQLiteDatabaseCorruptException e) {
					showDialog(CORRUPTION);
					return;
				}
			}
		};

		stonehewerListener = new View.OnClickListener() {
			public void onClick(View v) {
				int cmc;
				try {
					cmc = Integer.parseInt(cmcChoices[stonehewerCmcChoice.getSelectedItemPosition()]);
				}
				catch (NumberFormatException e) {
					cmc = -1;
				}

				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };

				try {
					Cursor equipment = mDbHelper.Search(null, null, "Equipment", "wubrgl", 0, null, CardDbAdapter.NOONECARES,
							null, CardDbAdapter.NOONECARES, null, cmc + 1, "<", null, null, null, null, 0, 0,
							CardDbAdapter.MOSTRECENTPRINTING, false, returnTypes, true);

					int pos = rand.nextInt(equipment.getCount());
					equipment.moveToPosition(pos);
					name = equipment.getString(equipment.getColumnIndex(CardDbAdapter.KEY_NAME));
					equipment.close();

					// add a fragment
					Bundle args = new Bundle();
					args.putLong("id", mDbHelper.fetchIdByName(name));
					ResultListFragment rlFrag = new ResultListFragment();
					anchor.startNewFragment(rlFrag, args);
				}
				catch (SQLiteDatabaseCorruptException e) {
					showDialog(CORRUPTION);
					return;
				}
			}
		};
		jhoiraInstantListener = new View.OnClickListener() {
			public void onClick(View v) {

				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };

				try {
					Cursor instants = mDbHelper.Search(null, null, "instant", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
							CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, 0, 0, CardDbAdapter.MOSTRECENTPRINTING,
							false, returnTypes, true);

					// Get 3 random, distinct numbers
					int pos[] = new int[3];
					pos[0] = rand.nextInt(instants.getCount());
					pos[1] = rand.nextInt(instants.getCount());
					while (pos[0] == pos[1]) {
						pos[1] = rand.nextInt(instants.getCount());
					}
					pos[2] = rand.nextInt(instants.getCount());
					while (pos[0] == pos[2] || pos[1] == pos[2]) {
						pos[2] = rand.nextInt(instants.getCount());
					}

					Bundle args = new Bundle();
					for (int i = 0; i < 3; i++) {
						instants.moveToPosition(pos[i]);
						args.putLong("id" + i,
								mDbHelper.fetchIdByName(instants.getString(instants.getColumnIndex(CardDbAdapter.KEY_NAME))));
					}
					instants.close();

					// add a fragment
					ResultListFragment rlFrag = new ResultListFragment();
					anchor.startNewFragment(rlFrag, args);
				}
				catch (SQLiteDatabaseCorruptException e) {
					showDialog(CORRUPTION);
					return;
				}
			}
		};
		jhoiraSorceryListener = new View.OnClickListener() {
			public void onClick(View v) {

				String[] returnTypes = new String[] { CardDbAdapter.KEY_NAME };

				try {
					Cursor sorceries = mDbHelper.Search(null, null, "sorcery", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
							CardDbAdapter.NOONECARES, null, -1, null, null, null, null, null, 0, 0, CardDbAdapter.MOSTRECENTPRINTING,
							false, returnTypes, true);

					// Get 3 random, distinct numbers
					int pos[] = new int[3];
					pos[0] = rand.nextInt(sorceries.getCount());
					pos[1] = rand.nextInt(sorceries.getCount());
					while (pos[0] == pos[1]) {
						pos[1] = rand.nextInt(sorceries.getCount());
					}
					pos[2] = rand.nextInt(sorceries.getCount());
					while (pos[0] == pos[2] || pos[1] == pos[2]) {
						pos[2] = rand.nextInt(sorceries.getCount());
					}

					String names[] = new String[3];
					Bundle args = new Bundle();
					for (int i = 0; i < 3; i++) {
						sorceries.moveToPosition(pos[i]);
						names[i] = sorceries.getString(sorceries.getColumnIndex(CardDbAdapter.KEY_NAME));
						args.putLong("id" + i, mDbHelper.fetchIdByName(names[i]));
					}
					sorceries.close();

					// add a fragment
					ResultListFragment rlFrag = new ResultListFragment();
					rlFrag.setArguments(args);
					anchor.startNewFragment(rlFrag, args);
				}
				catch (SQLiteDatabaseCorruptException e) {
					showDialog(CORRUPTION);
					return;
				}
			}
		};

		rand = new Random(System.currentTimeMillis());

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		boolean b = preferences.getBoolean("mojhostoFirstTime", true);
		if (b) {
			showDialog(RULESDIALOG);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("mojhostoFirstTime", false);
			editor.commit();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
	 * android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View myFragmentView = inflater.inflate(R.layout.mojhosto_frag, container, false);

		momirImage = (ImageView) myFragmentView.findViewById(R.id.imageViewMo);
		stonehewerImage = (ImageView) myFragmentView.findViewById(R.id.imageViewSto);
		jhoiraImage = (ImageView) myFragmentView.findViewById(R.id.imageViewJho);

		momirImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(MOMIR_IMAGE);
			}
		});
		stonehewerImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(STONEHEWER_IMAGE);
			}
		});
		jhoiraImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(JHOIRA_IMAGE);
			}
		});

		momirButton = (Button) myFragmentView.findViewById(R.id.momir_button);
		stonehewerButton = (Button) myFragmentView.findViewById(R.id.stonehewer_button);
		jhoiraInstantButton = (Button) myFragmentView.findViewById(R.id.jhorira_instant_button);
		jhoiraSorceryButton = (Button) myFragmentView.findViewById(R.id.jhorira_sorcery_button);

		momirButton.setOnClickListener(momirListener);
		stonehewerButton.setOnClickListener(stonehewerListener);
		jhoiraInstantButton.setOnClickListener(jhoiraInstantListener);
		jhoiraSorceryButton.setOnClickListener(jhoiraSorceryListener);

		momirCmcChoice = (Spinner) myFragmentView.findViewById(R.id.momir_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.momir_spinner,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		momirCmcChoice.setAdapter(adapter);

		stonehewerCmcChoice = (Spinner) myFragmentView.findViewById(R.id.stonehewer_spinner);
		ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(getActivity(), R.array.momir_spinner,
				android.R.layout.simple_spinner_item);
		adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		stonehewerCmcChoice.setAdapter(adapter1);

		cmcChoices = getResources().getStringArray(R.array.momir_spinner);

		return myFragmentView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.random_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.random_rules:
				showDialog(RULESDIALOG);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected void showDialog(final int id) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_TAG);
		if (prev != null) {
			ft.remove(prev);
		}

		// Create and show the dialog.
		FamiliarDialogFragment newFragment = new FamiliarDialogFragment() {

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				switch (id) {
					case RULESDIALOG: {
						AlertDialog.Builder builder = new AlertDialog.Builder(this.getFamiliarActivity());
						builder.setNeutralButton(R.string.dialog_play, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
						builder.setMessage(ImageGetterHelper.jellyBeanHack(getString(R.string.mojhosto_rules_text)));
						builder.setTitle(R.string.mojhosto_rules_title);
						return builder.create();
					}
					case MOMIR_IMAGE: {
						Dialog d = new Dialog(this.getFamiliarActivity());
						d.requestWindowFeature(Window.FEATURE_NO_TITLE);

						d.setContentView(R.layout.image_dialog);

						ImageView image = (ImageView) d.findViewById(R.id.cardimage);
						image.setImageResource(R.drawable.momir_full);
						return d;
					}
					case STONEHEWER_IMAGE: {
						Dialog d = new Dialog(this.getFamiliarActivity());
						d.requestWindowFeature(Window.FEATURE_NO_TITLE);

						d.setContentView(R.layout.image_dialog);

						ImageView image = (ImageView) d.findViewById(R.id.cardimage);
						image.setImageResource(R.drawable.stonehewer_full);
						return d;
					}
					case JHOIRA_IMAGE: {
						Dialog d = new Dialog(this.getFamiliarActivity());
						d.requestWindowFeature(Window.FEATURE_NO_TITLE);

						d.setContentView(R.layout.image_dialog);

						ImageView image = (ImageView) d.findViewById(R.id.cardimage);
						image.setImageResource(R.drawable.jhoira_full);
						return d;
					}
					case CORRUPTION: {
						View dialogLayout = this.getFamiliarActivity().getLayoutInflater()
								.inflate(R.layout.simple_message_layout, null);
						TextView text = (TextView) dialogLayout.findViewById(R.id.message);
						text.setText(ImageGetterHelper.jellyBeanHack(getString(R.string.error_corruption)));
						text.setMovementMethod(LinkMovementMethod.getInstance());

						return new AlertDialog.Builder(this.getFamiliarActivity()).setTitle(R.string.error).setView(dialogLayout)
								.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										anchor.getFamiliarActivity().finish();
									}
								}).setCancelable(false).create();
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

}
