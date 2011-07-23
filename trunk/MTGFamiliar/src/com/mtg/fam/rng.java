package com.mtg.fam;

import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class rng extends Activity implements ViewSwitcher.ViewFactory{

	private TextSwitcher rollResult;
	private EditText rollMax;
	private Button rollButton;
	Random r;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rng);

		rollResult = (TextSwitcher) findViewById(R.id.rollresult);
		rollResult.setFactory(this);
		rollMax = (EditText) findViewById(R.id.rollnum);
		rollButton = (Button) findViewById(R.id.rollButton);
		
		Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		rollResult.setInAnimation(in);
		rollResult.setOutAnimation(out);
		
		rollResult.setText("0");

		r = new Random();

		rollButton.setOnClickListener(new View.OnClickListener() {
			private int rand;

			public void onClick(View v) {
				try {
					rand = r.nextInt(Integer.parseInt(rollMax.getText().toString()))+1;
					rollResult.setText(rand+"");
				}
				catch (NumberFormatException e) {
					rollResult.setText("0");
				}
			}
		});

	}

	public View makeView() {
    TextView t = new TextView(this);
    t.setGravity(Gravity.CENTER);
    t.setTextSize(150);
//    t.setTextColor(R.color.textcolor);
      return t;
	}

}
