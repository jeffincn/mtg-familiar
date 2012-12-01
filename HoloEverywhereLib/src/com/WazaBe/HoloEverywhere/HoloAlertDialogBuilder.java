package com.WazaBe.HoloEverywhere;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HoloAlertDialogBuilder extends AlertDialog.Builder {

	private final Context	mContext;
	private TextView			mTitle;
	private ImageView			mIcon;
	private TextView			mMessage;
	private View	customMessage;
	private LinearLayout	bodyLayout;
	
	public HoloAlertDialogBuilder(Context context) {
		super(context);
		mContext = context;
				
		// Using the full layout give me a strange top divider in Donut and
		// Eclair in when using setView. Using second idea breaks custom View.
		Boolean useFullLayout = true;

		if (useFullLayout) {
			View customTitle = View.inflate(mContext, R.layout.alert_dialog_title, null);
			mTitle = (TextView) customTitle.findViewById(R.id.alertTitle);
			mIcon = (ImageView) customTitle.findViewById(R.id.icon);
			
			mTitle.setText("Custom title");
			setCustomTitle(customTitle);

			customMessage = View.inflate(mContext, R.layout.alert_dialog_message, null);
			bodyLayout = (LinearLayout)customMessage.findViewById(R.id.bodyPanel);
			
//			mMessage = (TextView) customMessage.findViewById(R.id.message);

//			setView(customMessage);
		}
		else {
//			View customView = View.inflate(mContext, R.layout.alert_dialog_holo, null);
//			mTitle = (TextView) customView.findViewById(R.id.alertTitle);
//			mIcon = (ImageView) customView.findViewById(R.id.icon);
//			
//			mMessage = (TextView) customView.findViewById(R.id.message);
//			setView(customView);
		}

	}

	@Override
	public HoloAlertDialogBuilder setTitle(int textResId) {
		mTitle.setText(textResId);
		return this;
	}

	@Override
	public HoloAlertDialogBuilder setTitle(CharSequence text) {
		mTitle.setText(text);
		return this;
	}

	@Override
	public HoloAlertDialogBuilder setMessage(int textResId) {
		
		mMessage = new TextView(mContext);
		mMessage.setText(textResId);
		this.setView(mMessage);
		
		return this;
	}

	@Override
	public HoloAlertDialogBuilder setMessage(CharSequence text) {
		mMessage = new TextView(mContext);
		mMessage.setText(text);
		this.setView(mMessage);

		return this;
	}

	@Override
	public HoloAlertDialogBuilder setIcon(int drawableResId) {
		mIcon.setImageResource(drawableResId);
		return this;
	}

	@Override
	public HoloAlertDialogBuilder setIcon(Drawable icon) {
		mIcon.setImageDrawable(icon);
		return this;
	}
	
//	@Override
//	public HoloAlertDialogBuilder setNeutralButton(CharSequence text, DialogInterface.OnClickListener da){
//		return this;
//	}
	
	@Override
	public HoloAlertDialogBuilder setView(View v){
		bodyLayout.removeAllViews();
		bodyLayout.addView(v);
		return (HoloAlertDialogBuilder) super.setView(customMessage);
	}

}