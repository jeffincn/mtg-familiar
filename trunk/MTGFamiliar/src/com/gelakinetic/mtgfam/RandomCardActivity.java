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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.widget.TextView;

public class RandomCardActivity extends Activity implements OnGestureListener,
		Runnable {
	private LinearLayout main;
	private TextView viewA;

	private GestureDetector gestureScanner;
	private CardDbAdapter mDbAdapter;
	private Cursor c;
	private int numChoices;
	private Random rand;
	private int[] a;
	private int index;
	private ImageView imgView;
	private String picurl;
	private BitmapDrawable d;
	private Bitmap bmp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.random_card_activity);

		gestureScanner = new GestureDetector(this);

		main = (LinearLayout) findViewById(R.id.randomCardLinearLayout);
		viewA = (TextView) findViewById(R.id.randomCardTextView);
		imgView = (ImageView) findViewById(R.id.randomCardImage);

		setContentView(main);

		mDbAdapter = new CardDbAdapter(this);
		mDbAdapter.open();

		String[] returnTypes = new String[] { CardDbAdapter.KEY_ID,
				CardDbAdapter.KEY_NAME };

		c = mDbAdapter.Search(this.getApplicationContext(), null, null,
				"Creature", "wubrgl", 0, null, CardDbAdapter.NOONECARES, null,
				CardDbAdapter.NOONECARES, null, -1, null, null, null, null,
				null, returnTypes);
		numChoices = c.getCount();

		// implements http://en.wikipedia.org/wiki/Fisher-Yates_shuffle
		rand = new Random(System.currentTimeMillis());
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

		c.moveToPosition(a[index]);
		String name = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
		viewA.setText(name);
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return gestureScanner.onTouchEvent(me);
	}

	public boolean onDown(MotionEvent arg0) {
		// viewA.setText("-" + "onDown" + "-");
		return true;
	}

	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		if (arg2 < -1200) {
			index++;
			if (index >= numChoices) {
				index -= numChoices;
			}
			c.moveToPosition(a[index]);
			String name = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
			viewA.setText(name);
		} else if (arg2 > 1200) {
			index--;
			if (index < 0) {
				index += numChoices;
			}
			c.moveToPosition(a[index]);
			String name = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));
			viewA.setText(name);
		}
		loadImage();
		return true;
	}

	public void onLongPress(MotionEvent arg0) {
		viewA.setText("");
	}

	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// viewA.setText("-" + "onScroll" + "-");
		return true;
	}

	public void onShowPress(MotionEvent arg0) {
		// viewA.setText("-" + "onShowPress" + "-");
	}

	public boolean onSingleTapUp(MotionEvent arg0) {
		// viewA.setText("-" + "onSingleTapUp" + "-");
		return true;
	}

	void loadImage() {
		imgView.setImageResource(R.drawable.loading);
		Cursor card = mDbAdapter.fetchCard(c.getLong(c
				.getColumnIndex(CardDbAdapter.KEY_ID)));

		String number = card.getString(card
				.getColumnIndex(CardDbAdapter.KEY_NUMBER));
		String mtgi_code = mDbAdapter.getCodeMtgi(card.getString(card
				.getColumnIndex(CardDbAdapter.KEY_SET)));

		picurl = "http://magiccards.info/scans/en/" + mtgi_code + "/" + number
				+ ".jpg";
		picurl = picurl.toLowerCase();
		Thread thread = new Thread(this);
		thread.start();
	}

	public void run() {
		try {
			URL u = new URL(picurl);
			d = new BitmapDrawable(u.openStream());
			bmp = d.getBitmap();

			Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
					.getDefaultDisplay();
			int newHeight;
			int newWidth;
			float scale;
			if (display.getWidth() < display.getHeight()) {
				scale = (display.getWidth() - 20)
						/ (float) d.getIntrinsicWidth();
			} else {
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
		} catch (Exception e) {

		}
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			imgView.setImageDrawable(d);
		}
	};
}
