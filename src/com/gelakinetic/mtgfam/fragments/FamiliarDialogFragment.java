package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.gelakinetic.mtgfam.activities.MainActivity;

public class FamiliarDialogFragment extends SherlockDialogFragment {

	public FamiliarDialogFragment() {
		/* All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onDestroyView() {
		if(getDialog() != null){
			getDialog().dismiss();
		}
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// int id = savedInstanceState.getInt("id");
		// fixes some bug. try rotating a dialog w/o this
		return null;
	}

	protected MainActivity getMainActivity() {
		return (MainActivity) this.getActivity();
	}
}
