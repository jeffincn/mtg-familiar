package com.gelakinetic.mtgfam.helpers;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;

import com.actionbarsherlock.view.ActionProvider;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public class InFragmentMenuItem implements MenuItem {

	private Context mContext;
	private int mItemId;
	private ContextMenuInfo mMenuInfo;
	private char mNumericShortcut;
	private int mOrder;
	private CharSequence mTitle;
	private boolean mChecked;
	private boolean mVisible;
	private boolean mEnabled;
	private boolean mCheckable;
	private CharSequence mTitleCondensed;
	private char mAlphabeticShortcut;
	private ActionProvider mActionProvider;
	private Intent mIntent;
	private Drawable mIcon;
	private View mActionView;
	private int mGroupId;
	public SubMenu mSubMenu;
	public int mShowAsAction;
	public OnMenuItemClickListener mMenuItemClickListener;
	public OnActionExpandListener mOnActionExpandListener;
	public char mShortcutAlpahChar;
	public char mShortcutNumericChar;
	public int mShowAsActionFlags;

	public InFragmentMenuItem(Context ctx) {
		mContext = ctx;
	}

	@Override
	public boolean collapseActionView() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean expandActionView() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isActionViewExpanded() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public View getActionView() {
		return mActionView;
	}

	@Override
	public char getAlphabeticShortcut() {
		return mAlphabeticShortcut;
	}

	@Override
	public int getGroupId() {
		return mGroupId;
	}

	@Override
	public Drawable getIcon() {
		return mIcon;
	}

	@Override
	public Intent getIntent() {
		return mIntent;
	}

	public void setItemId(int id) {
		mItemId = id;
	}
	
	public void setGroupId(int id) {
		mGroupId = id;
	}
	
	@Override
	public int getItemId() {
		return mItemId;
	}

	@Override
	public ContextMenuInfo getMenuInfo() {
		return mMenuInfo;
	}

	@Override
	public char getNumericShortcut() {
		return mNumericShortcut;
	}

	@Override
	public int getOrder() {
		return mOrder;
	}

	@Override
	public CharSequence getTitle() {
		return mTitle;
	}

	@Override
	public CharSequence getTitleCondensed() {
		return mTitleCondensed;
	}

	@Override
	public boolean hasSubMenu() {
		return mSubMenu != null;
	}

	@Override
	public boolean isCheckable() {
		return mCheckable;
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public boolean isEnabled() {
		return mEnabled;
	}

	@Override
	public boolean isVisible() {
		return mVisible;
	}

	@Override
	public InFragmentMenuItem setActionView(View view) {
		mActionView = view;
		return this;
	}

	@Override
	public InFragmentMenuItem setActionView(int resId) {
		mActionView = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(resId, null);
		return this;
	}

	@Override
	public InFragmentMenuItem setAlphabeticShortcut(char alphaChar) {
		mAlphabeticShortcut = alphaChar;
		return this;
	}

	@Override
	public InFragmentMenuItem setCheckable(boolean checkable) {
		mCheckable = checkable;
		return this;
	}

	@Override
	public InFragmentMenuItem setChecked(boolean checked) {
		mChecked = checked;
		return this;
	}

	@Override
	public InFragmentMenuItem setEnabled(boolean enabled) {
		mEnabled = enabled;
		return this;
	}

	@Override
	public InFragmentMenuItem setIcon(Drawable icon) {
		mIcon = icon;
		return this;
	}

	@Override
	public InFragmentMenuItem setIcon(int iconRes) {
		if(iconRes != 0) {
			mIcon = mContext.getResources().getDrawable(iconRes);
		}
		else {
			Log.v("tag", "zerocion");
		}
		return this;
	}

	@Override
	public InFragmentMenuItem setIntent(Intent intent) {
		mIntent = intent;
		return this;
	}

	@Override
	public InFragmentMenuItem setNumericShortcut(char numericChar) {
		mNumericShortcut = numericChar;
		return this;
	}

	@Override
	public InFragmentMenuItem setOnActionExpandListener(OnActionExpandListener listener) {
		mOnActionExpandListener = listener;
		return this;
	}

	@Override
	public InFragmentMenuItem setOnMenuItemClickListener(
			OnMenuItemClickListener menuItemClickListener) {
		mMenuItemClickListener = menuItemClickListener;
		return this;
	}

	@Override
	public InFragmentMenuItem setShortcut(char numericChar, char alphaChar) {
		mShortcutNumericChar = numericChar;
		mShortcutAlpahChar = alphaChar;
		return this;
	}

	@Override
	public void setShowAsAction(int actionEnum) {
		mShowAsAction = actionEnum;
	}

	@Override
	public InFragmentMenuItem setShowAsActionFlags(int actionEnum) {
		mShowAsActionFlags = actionEnum;
		return this;
	}

	@Override
	public InFragmentMenuItem setTitle(CharSequence title) {
		mTitle = title;
		return this;
	}

	@Override
	public InFragmentMenuItem setTitle(int title) {
		mTitle = mContext.getString(title);
		return this;
	}

	@Override
	public InFragmentMenuItem setTitleCondensed(CharSequence title) {
		mTitleCondensed = title;
		return this;
	}

	@Override
	public InFragmentMenuItem setVisible(boolean visible) {
		mVisible = visible;
		return this;
	}

	@Override
	public InFragmentMenuItem setActionProvider(
			com.actionbarsherlock.view.ActionProvider arg0) {
		mActionProvider = arg0;
		return this;
	}

	@Override
	public com.actionbarsherlock.view.ActionProvider getActionProvider() {
		return mActionProvider;
	}

	@Override
	public com.actionbarsherlock.view.SubMenu getSubMenu() {
		return mSubMenu;
	}

	public Context getContext() {
		return mContext;
	}

}
