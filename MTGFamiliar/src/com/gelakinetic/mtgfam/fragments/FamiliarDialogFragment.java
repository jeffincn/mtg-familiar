package com.gelakinetic.mtgfam.fragments;

import android.app.Dialog;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class FamiliarDialogFragment extends SherlockDialogFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setOnDismissListener(null);
		super.onDestroyView();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		//int id = savedInstanceState.getInt("id");
		return null;
	}

}
