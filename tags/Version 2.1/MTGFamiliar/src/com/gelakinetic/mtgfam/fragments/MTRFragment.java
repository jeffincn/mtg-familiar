package com.gelakinetic.mtgfam.fragments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import com.gelakinetic.mtgfam.R;

public class MTRFragment extends FamiliarFragment {

	public MTRFragment() {
		/* http://developer.android.com/reference/android/app/Fragment.html
		 * All subclasses of Fragment must include a public empty constructor.
		 * The framework will often re-instantiate a fragment class when needed,
		 * in particular during state restore, and needs to be able to find this constructor
		 * to instantiate it. If the empty constructor is not available, a runtime exception
		 * will occur in some cases during state restore. 
		 */
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View v = inflater.inflate(R.layout.mtr_ipg_frag, container, false);

		final WebView wv = (WebView) v.findViewById(R.id.mtr_ipg_webview);
		final ProgressBar prog = (ProgressBar) v.findViewById(R.id.mtr_ipg_progress_bar);
		wv.setWebViewClient(new WebViewClient() {
			
			@Override
			public void onPageFinished(WebView view, String url) {
				if (getActivity() != null) {
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							prog.setVisibility(View.GONE);
						}
					});
				}
			}
		});
		wv.setBackgroundColor(0);
		File mtr = new File(getActivity().getFilesDir(), JudgesCornerFragment.MTR_LOCAL_FILE);
		StringBuilder html = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(mtr)));
			String line;
			while ((line = reader.readLine()) != null) {
				html.append(line);
			}
		} 
		catch (IOException e) {
			html.setLength(0);
			html.append("An error occurred.");
		}
		finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}
		wv.loadDataWithBaseURL(null, html.toString(), "text/html", "utf-8", null);

		Button b = (Button) v.findViewById(R.id.mtr_ipg_jump_to_top);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				wv.scrollTo(0, 0);
			}
		});

		return v;
	}
}
