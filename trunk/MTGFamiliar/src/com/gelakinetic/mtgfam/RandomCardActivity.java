package com.gelakinetic.mtgfam;

import java.net.URL;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.widget.TextView;

public class RandomCardActivity extends Activity implements OnGestureListener, Runnable {
	private LinearLayout		main;
	private TextView				viewA;

	private GestureDetector	gestureScanner;
	private CardDbAdapter		mDbAdapter;
	private Random					rand;
	private ImageView				imgView;
	private String					picurl;
	private BitmapDrawable	d;
	private Bitmap					bmp;
	private String					name;
	private Spinner					cmcChoice;
	private String[]				cmcChoices;
	private Cursor[]				momir_cursors;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.random_card_activity);

		gestureScanner = new GestureDetector(this);

		main = (LinearLayout) findViewById(R.id.randomCardLinearLayout);
		viewA = (TextView) findViewById(R.id.randomCardTextView);
		imgView = (ImageView) findViewById(R.id.randomCardImage);

		setContentView(main);

		cmcChoice = (Spinner) findViewById(R.id.momir_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.momir_spinner,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cmcChoice.setAdapter(adapter);

		mDbAdapter = new CardDbAdapter(this);
		mDbAdapter.open();

		String[] returnTypes = new String[] { /* CardDbAdapter.KEY_ID, */
		CardDbAdapter.KEY_NAME };

		cmcChoices = getResources().getStringArray(R.array.momir_spinner);
		momir_cursors = new Cursor[cmcChoices.length];
		for (int i = 0; i < cmcChoices.length; i++) {
			momir_cursors[i] = mDbAdapter.Search(null, null, "Creature", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
					CardDbAdapter.NOONECARES, null, i, "=", null, null, null, null, false, returnTypes);
		}
		rand = new Random(System.currentTimeMillis());
/*
		// implements http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
		a = new int[numChoices];
		int temp, i, j;
		for (i = 0; i < numChoices; i++) {
			a[i] = i;
		}
		for (i = numChoices - 1; i > 0; i--) {
			j = rand.nextInt(i + 1);// j = random integer with 0 <= j <= i
			temp = a[j];
			a[j] = a[i];
			a[i] = temp;
		}
		index = 0;
*/
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return gestureScanner.onTouchEvent(me);
	}

	public boolean onDown(MotionEvent arg0) {
		return true;
	}

	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
		if (arg2 < -1200) {
			int cmc;
			/*
			 * index++; if (index >= numChoices) { index -= numChoices; }
			 * c.moveToPosition(a[index]); name =
			 * c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
			 */
			try {
				cmc = Integer.parseInt(cmcChoices[cmcChoice.getSelectedItemPosition()]);
			}
			catch (NumberFormatException e) {
				cmc = -1;
			}

			int pos = rand.nextInt(momir_cursors[cmc].getCount());
			momir_cursors[cmc].moveToPosition(pos);
			name = momir_cursors[cmc].getString(momir_cursors[cmc].getColumnIndex(CardDbAdapter.KEY_NAME));

			viewA.setText(name);
			loadImage();
		}
		else if (arg2 > 1200) {
			/*
			index--;
			if (index < 0) {
				index += numChoices;
			}
			c.moveToPosition(a[index]);
			name = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
			viewA.setText(name);
			loadImage();
			*/
		}
		return true;
	}

	public void onLongPress(MotionEvent arg0) {
		viewA.setText("");
	}

	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
		return true;
	}

	public void onShowPress(MotionEvent arg0) {
	}

	public boolean onSingleTapUp(MotionEvent arg0) {
		return true;
	}

	void loadImage() {
		imgView.setImageResource(R.drawable.loading);
		Cursor card = mDbAdapter.fetchCardByName(name);

		String number = card.getString(card.getColumnIndex(CardDbAdapter.KEY_NUMBER));
		String mtgi_code = mDbAdapter.getCodeMtgi(card.getString(card.getColumnIndex(CardDbAdapter.KEY_SET)));

		picurl = "http://magiccards.info/scans/en/" + mtgi_code + "/" + number + ".jpg";
		picurl = picurl.toLowerCase();
		Thread thread = new Thread(this);
		thread.start();
	}

	public void run() {
		try {
			URL u = new URL(picurl);
			d = new BitmapDrawable(u.openStream());
			bmp = d.getBitmap();

			Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			int newHeight;
			int newWidth;
			float scale;
			if (display.getWidth() < display.getHeight()) {
				scale = (display.getWidth() - 20) / (float) d.getIntrinsicWidth();
			}
			else {
				DisplayMetrics metrics = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(metrics);
				int myHeight = 0;

				switch (metrics.densityDpi) {
					case DisplayMetrics.DENSITY_HIGH:
						myHeight = display.getHeight() - 48;
						break;
					case DisplayMetrics.DENSITY_MEDIUM:
						myHeight = display.getHeight() - 32;
						break;
					case DisplayMetrics.DENSITY_LOW:
						myHeight = display.getHeight() - 24;
						break;
					default:
						break;
				}

				scale = (myHeight - 10) / (float) d.getIntrinsicHeight();
			}
			newWidth = Math.round(bmp.getWidth() * scale);
			newHeight = Math.round(bmp.getHeight() * scale);

			bmp = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
			d = new BitmapDrawable(bmp);
			handler.sendEmptyMessage(0);
		}
		catch (Exception e) {

		}
	}

	private Handler	handler	= new Handler() {
														@Override
														public void handleMessage(Message msg) {
															imgView.setImageDrawable(d);
														}
													};
}
