package com.gelakinetic.mtgfam;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;



import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class cardview extends Activity implements Runnable {

	private static final int			PICLOAD		= 0;
	private static final int			PRICELOAD	= 1;
	protected static final int		TRANSFORM	= 7;
	protected static final String	NUMBER		= "number";
	protected static final String	SET				= "set";
	private CardDbAdapter					mDbHelper;
	private TextView							name;
	private TextView							cost;
	private TextView							type;
	private TextView							set;
	private TextView							ability;
	private TextView							pt;
	private TextView							flavor;
	private TextView							artist;
	private Cursor								c;
	private ImageView							image;
	private BitmapDrawable				d;
	private Bitmap								bmp;
	private long									cardID;
	private Cursor								formats		= null;
	private String								mtgi_code;
	private static String					priceurl;
	private String								picurl;

	private ImageGetter						imgGetter	= new ImageGetter() {
																						public Drawable getDrawable(String source) {
																							Drawable d = null;

																							int[] drawableNums = { R.drawable.zero, R.drawable.one, R.drawable.two,
			R.drawable.three, R.drawable.four, R.drawable.five, R.drawable.six, R.drawable.seven, R.drawable.eight,
			R.drawable.nine, R.drawable.ten, R.drawable.eleven, R.drawable.twelve, R.drawable.thirteen, R.drawable.fourteen,
			R.drawable.fifteen, R.drawable.sixteen, R.drawable.seventeen, R.drawable.eighteen, R.drawable.ninteen,
			R.drawable.twenty											};

																							if (source.equalsIgnoreCase("w")) {
																								d = getResources().getDrawable(R.drawable.w);
																							}
																							else if (source.equalsIgnoreCase("u")) {
																								d = getResources().getDrawable(R.drawable.u);
																							}
																							else if (source.equalsIgnoreCase("b")) {
																								d = getResources().getDrawable(R.drawable.b);
																							}
																							else if (source.equalsIgnoreCase("r")) {
																								d = getResources().getDrawable(R.drawable.r);
																							}
																							else if (source.equalsIgnoreCase("g")) {
																								d = getResources().getDrawable(R.drawable.g);
																							}
																							else if (source.equalsIgnoreCase("t")) {
																								d = getResources().getDrawable(R.drawable.tap);
																							}
																							else if (source.equalsIgnoreCase("q")) {
																								d = getResources().getDrawable(R.drawable.untap);
																							}
																							else if (source.equalsIgnoreCase("wu")) {
																								d = getResources().getDrawable(R.drawable.wu);
																							}
																							else if (source.equalsIgnoreCase("ub")) {
																								d = getResources().getDrawable(R.drawable.ub);
																							}
																							else if (source.equalsIgnoreCase("br")) {
																								d = getResources().getDrawable(R.drawable.br);
																							}
																							else if (source.equalsIgnoreCase("rg")) {
																								d = getResources().getDrawable(R.drawable.rg);
																							}
																							else if (source.equalsIgnoreCase("gw")) {
																								d = getResources().getDrawable(R.drawable.gw);
																							}
																							else if (source.equalsIgnoreCase("wb")) {
																								d = getResources().getDrawable(R.drawable.wb);
																							}
																							else if (source.equalsIgnoreCase("bg")) {
																								d = getResources().getDrawable(R.drawable.bg);
																							}
																							else if (source.equalsIgnoreCase("gu")) {
																								d = getResources().getDrawable(R.drawable.gu);
																							}
																							else if (source.equalsIgnoreCase("ur")) {
																								d = getResources().getDrawable(R.drawable.ur);
																							}
																							else if (source.equalsIgnoreCase("rw")) {
																								d = getResources().getDrawable(R.drawable.rw);
																							}
																							else if (source.equalsIgnoreCase("2w")) {
																								d = getResources().getDrawable(R.drawable.w2);
																							}
																							else if (source.equalsIgnoreCase("2u")) {
																								d = getResources().getDrawable(R.drawable.u2);
																							}
																							else if (source.equalsIgnoreCase("2b")) {
																								d = getResources().getDrawable(R.drawable.b2);
																							}
																							else if (source.equalsIgnoreCase("2r")) {
																								d = getResources().getDrawable(R.drawable.r2);
																							}
																							else if (source.equalsIgnoreCase("2g")) {
																								d = getResources().getDrawable(R.drawable.g2);
																							}
																							else if (source.equalsIgnoreCase("s")) {
																								d = getResources().getDrawable(R.drawable.s);
																							}
																							else if (source.equalsIgnoreCase("pw")) {
																								d = getResources().getDrawable(R.drawable.pw);
																							}
																							else if (source.equalsIgnoreCase("pu")) {
																								d = getResources().getDrawable(R.drawable.pu);
																							}
																							else if (source.equalsIgnoreCase("pb")) {
																								d = getResources().getDrawable(R.drawable.pb);
																							}
																							else if (source.equalsIgnoreCase("pr")) {
																								d = getResources().getDrawable(R.drawable.pr);
																							}
																							else if (source.equalsIgnoreCase("pg")) {
																								d = getResources().getDrawable(R.drawable.pg);
																							}
																							else if (source.equalsIgnoreCase("p")) {
																								d = getResources().getDrawable(R.drawable.p);
																							}
																							else if (source.equalsIgnoreCase("+oo")) {
																								d = getResources().getDrawable(R.drawable.inf);
																							}
																							else if (source.equalsIgnoreCase("100")) {
																								d = getResources().getDrawable(R.drawable.hundred);
																							}
																							else if (source.equalsIgnoreCase("1000000")) {
																								d = getResources().getDrawable(R.drawable.million);
																							}
																							else if (source.equalsIgnoreCase("hr")) {
																								d = getResources().getDrawable(R.drawable.hr);
																							}
																							else if (source.equalsIgnoreCase("hw")) {
																								d = getResources().getDrawable(R.drawable.hw);
																							}
																							else if (source.equalsIgnoreCase("c")) {
																								d = getResources().getDrawable(R.drawable.c);
																							}
																							else if (source.equalsIgnoreCase("z")) {
																								d = getResources().getDrawable(R.drawable.z);
																							}
																							else if (source.equalsIgnoreCase("y")) {
																								d = getResources().getDrawable(R.drawable.y);
																							}
																							else if (source.equalsIgnoreCase("x")) {
																								d = getResources().getDrawable(R.drawable.x);
																							}

																							for (int i = 0; i < drawableNums.length; i++) {
																								if (source.equals(new Integer(i).toString())) {
																									d = getResources().getDrawable(drawableNums[i]);
																								}
																							}

																							d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
																							return d;
																						}
																					};
	private int										threadtype;
	private float[]								prices;
	private TextView							l;
	private TextView							m;
	private TextView							h;
	private Button								transform;
	private String								number;
	private String								setName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cardview);

		Bundle extras = getIntent().getExtras();
		cardID = extras.getLong("id");

		mDbHelper = new CardDbAdapter(this);
		mDbHelper.open();

		c = mDbHelper.fetchCard(cardID);
		c.moveToFirst();

		// http://magiccards.info/scans/en/mt/55.jpg

		setName = c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET));
		mtgi_code = mDbHelper.getCodeMtgi(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)));
		number = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));
		picurl = "http://magiccards.info/scans/en/" + mtgi_code + "/" + number + ".jpg";
		picurl = picurl.toLowerCase();
		priceurl = "http://partner.tcgplayer.com/syn/synhighlow.ashx?pk=MAGCINFO&pi=" + mtgi_code + "-" + number;

		name = (TextView) findViewById(R.id.name);
		cost = (TextView) findViewById(R.id.cost);
		type = (TextView) findViewById(R.id.type);
		set = (TextView) findViewById(R.id.set);
		ability = (TextView) findViewById(R.id.ability);
		flavor = (TextView) findViewById(R.id.flavor);
		artist = (TextView) findViewById(R.id.artist);
		pt = (TextView) findViewById(R.id.pt);
		transform = (Button) findViewById(R.id.transformbutton);

		switch ((char) c.getInt(c.getColumnIndex(CardDbAdapter.KEY_RARITY))) {
			case 'C':
				set.setTextColor(this.getResources().getColor(R.color.common));
				break;
			case 'U':
				set.setTextColor(this.getResources().getColor(R.color.uncommon));
				break;
			case 'R':
				set.setTextColor(this.getResources().getColor(R.color.rare));
				break;
			case 'M':
				set.setTextColor(this.getResources().getColor(R.color.mythic));
				break;
		}

		String sCost = c.getString(c.getColumnIndex(CardDbAdapter.KEY_MANACOST));
		sCost = sCost.replace("{", "<img src=\"").replace("}", "\"/>");

		CharSequence csCost = Html.fromHtml(sCost, imgGetter, null);

		c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME));

		name.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_NAME)));
		cost.setText(csCost);
		type.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_TYPE)));
		set.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_SET)));

		String sAbility = c.getString(c.getColumnIndex(CardDbAdapter.KEY_ABILITY)).replace("{", "<img src=\"")
				.replace("}", "\"/>");
		CharSequence csAbility = Html.fromHtml(sAbility, imgGetter, null);
		ability.setText(csAbility);

		String sFlavor = c.getString(c.getColumnIndex(CardDbAdapter.KEY_FLAVOR));
		CharSequence csFlavor = Html.fromHtml(sFlavor, imgGetter, null);
		flavor.setText(csFlavor);

		artist.setText(c.getString(c.getColumnIndex(CardDbAdapter.KEY_ARTIST)));

		int loyalty = c.getInt(c.getColumnIndex(CardDbAdapter.KEY_LOYALTY));
		float p = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_POWER));
		float t = c.getFloat(c.getColumnIndex(CardDbAdapter.KEY_TOUGHNESS));
		if (loyalty != CardDbAdapter.NOONECARES) {
			pt.setText(new Integer(loyalty).toString());
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

		if (number.contains("a") || number.contains("b")) {
			transform.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent i = new Intent();
					i.putExtra(SET, setName);
					i.putExtra(NUMBER, number);
					setResult(TRANSFORM, i);
					finish();// transform!
					// Froyo+ only, disabled animations
					// overridePendingTransition(0, 0);
				}
			});
		}
		else {
			transform.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (formats != null) {
			formats.deactivate();
			formats.close();
		}
		if (c != null) {
			c.deactivate();
			c.close();
		}
		if (mDbHelper != null) {
			mDbHelper.close();
		}
		if(bmp != null){
			bmp.recycle();
		}
	}

	Drawable drawable_from_url(String url, String src_name) throws java.net.MalformedURLException, java.io.IOException {
		return Drawable.createFromStream(((java.io.InputStream) new java.net.URL(url).getContent()), src_name);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.card_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.image:
				showDialog(0);
				return true;
			case R.id.price:
				showDialog(2);
				return true;
			case R.id.legality:
				showDialog(1);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == 0) {
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.image_dialog);

			image = (ImageView) dialog.findViewById(R.id.cardimage);

			threadtype = PICLOAD;
			Thread thread = new Thread(this);
			thread.start();

			return dialog;
		}
		else if (id == 1) {
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.legalitydialog);

			formats = mDbHelper.fetchAllFormats();
			String[] formatNames = new String[formats.getCount()];
			formats.moveToFirst();
			for (int i = 0; i < formats.getCount(); i++) {
				formatNames[i] = formats.getString(formats.getColumnIndex(CardDbAdapter.KEY_NAME));
				formats.moveToNext();
			}

			LegalListAdapter lla = new LegalListAdapter(this, R.layout.legal_row, formats, new String[] {
					CardDbAdapter.KEY_NAME, CardDbAdapter.KEY_NAME }, new int[] { R.id.format, R.id.status }, cardID, mDbHelper);
			ListView lv = (ListView) dialog.findViewById(R.id.legallist);
			lv.setAdapter(lla);

			return dialog;
		}
		else if (id == 2) { // price
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.pricedialog);

			l = (TextView) dialog.findViewById(R.id.low);
			m = (TextView) dialog.findViewById(R.id.med);
			h = (TextView) dialog.findViewById(R.id.high);

			l.setText("Loading");
			m.setText("Loading");
			h.setText("Loading");

			threadtype = PRICELOAD;
			Thread thread = new Thread(this);
			thread.start();

			return dialog;
		}
		return null;
	}

	public void run() {
		switch (threadtype) {
			case (PICLOAD):
				try {
					URL u = new URL(picurl);
					d = new BitmapDrawable(u.openStream());
					bmp = d.getBitmap();

					Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
					float scale = (display.getWidth() - 20) / (float) d.getIntrinsicWidth();
					int newWidth = Math.round(bmp.getWidth() * scale);
					int newHeight = Math.round(bmp.getHeight() * scale);

					bmp = Bitmap.createScaledBitmap(d.getBitmap(), newWidth, newHeight, true);
					d = new BitmapDrawable(bmp);
				}
				catch (IOException e) {
					d = (BitmapDrawable) getResources().getDrawable(R.drawable.nonet);
				}
				catch (Exception e) {
					d = (BitmapDrawable) getResources().getDrawable(R.drawable.nonet);
				}
				handler.sendEmptyMessage(PICLOAD);
				break;
			case (PRICELOAD):
				prices = scrapePrices(mtgi_code, 0);
				handler.sendEmptyMessage(PRICELOAD);
				break;
		}
	}

	private Handler	handler	= new Handler() {
														@Override
														public void handleMessage(Message msg) {
															switch (msg.what) {
																case PICLOAD:
																	image.setImageDrawable(d);
																	break;
																case PRICELOAD:
																	if (prices[0] == 0 && prices[1] == 0 && prices[2] == 0) {
																		l.setText("No");
																		m.setText("Internet");
																		h.setText("Connection");
																	}
																	else {
																		l.setText(String.format("$%.2f", prices[0]));
																		m.setText(String.format("$%.2f", prices[1]));
																		h.setText(String.format("$%.2f", prices[2]));
																	}
																	break;
															}
														}
													};

	static float[] scrapePrices(String set, int num) {
		URL u;
		try {
			u = new URL(priceurl);
			InputStream is = u.openStream();

			int BUFSIZE = 128;
			byte[] buf = new byte[BUFSIZE];

			StringBuilder sb = new StringBuilder();
			int read;
			while ((read = is.read(buf)) == BUFSIZE) {
				sb.append(new String(buf));
			}
			for (int i = 0; i < read; i++) {
				sb.append((char) buf[i]);
			}

			String[] arr = sb.toString().split("\\$");
			float[] prices = new float[3];

			for (int i = 1; i < arr.length; i++) {
				prices[i - 1] = Float.parseFloat(arr[i].substring(0, arr[i].indexOf('<')));
			}
			return prices;
		}
		catch (MalformedURLException e) {
			float[] f = { 0, 0, 0 };
			return f;
		}
		catch (IOException e) {
			float[] f = { 0, 0, 0 };
			return f;
		}
	}
}
