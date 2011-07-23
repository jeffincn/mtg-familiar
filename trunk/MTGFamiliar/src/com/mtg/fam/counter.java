package com.mtg.fam;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class counter extends Activity  implements ViewSwitcher.ViewFactory{

	private TextSwitcher lifeP1;
	private TextSwitcher poisonP1;
	private TextSwitcher lifeP2;
	private TextSwitcher poisonP2;
	private Button life_p1_up;
	private Button life_p1_dn;
	private Button poison_p1_up;
	private Button poison_p1_dn;
	private Button life_p2_up;
	private Button life_p2_dn;
	private Button poison_p2_up;
	private Button poison_p2_dn;
	
	private static int life_p1;
	private static int poison_p1;
	private static int life_p2;
	private static int poison_p2;	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.counter);
		
		life_p1 = 20;
		poison_p1 = 0;
		life_p2 = 20;
		poison_p2 = 0;
		
		lifeP1 = (TextSwitcher)findViewById(R.id.life_p1);
		poisonP1 = (TextSwitcher)findViewById(R.id.poison_p1);
		lifeP2 = (TextSwitcher)findViewById(R.id.life_p2);
		poisonP2 = (TextSwitcher)findViewById(R.id.poison_p2);
		
		lifeP1.setFactory(this);
		poisonP1.setFactory(this);
		lifeP2.setFactory(this);
		poisonP2.setFactory(this);
		
		Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		lifeP1.setInAnimation(in);
		lifeP1.setOutAnimation(out);
		poisonP1.setInAnimation(in);
		poisonP1.setOutAnimation(out);
		lifeP2.setInAnimation(in);
		lifeP2.setOutAnimation(out);
		poisonP2.setInAnimation(in);
		poisonP2.setOutAnimation(out);
		
		lifeP1.setText(""+life_p1);
		poisonP1.setText(""+poison_p1);
		lifeP2.setText(""+life_p2);
		poisonP2.setText(""+poison_p2);
		
		life_p1_up = (Button)findViewById(R.id.life_p1_up);
		life_p1_dn = (Button)findViewById(R.id.life_p1_dn);
		poison_p1_up = (Button)findViewById(R.id.poison_p1_up);
		poison_p1_dn = (Button)findViewById(R.id.poison_p1_dn);
		life_p2_up = (Button)findViewById(R.id.life_p2_up);
		life_p2_dn = (Button)findViewById(R.id.life_p2_dn);
		poison_p2_up = (Button)findViewById(R.id.poison_p2_up);
		poison_p2_dn = (Button)findViewById(R.id.poison_p2_dn);
		
		life_p1_up.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				lifeP1.setText((++life_p1)+"");
			}
		});
		life_p1_dn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				lifeP1.setText((--life_p1)+"");
			}
		});
		poison_p1_up.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				poisonP1.setText((++poison_p1)+"");
			}
		});
		poison_p1_dn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				poisonP1.setText((--poison_p1)+"");
			}
		});
		life_p2_up.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				lifeP2.setText((++life_p2)+"");
			}
		});
		life_p2_dn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				lifeP2.setText((--life_p2)+"");
			}
		});
		poison_p2_up.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				poisonP2.setText((++poison_p2)+"");
			}
		});
		poison_p2_dn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				poisonP2.setText((--poison_p2)+"");
			}
		});

	}

	public View makeView() {
    TextView t = new TextView(this);
    t.setGravity(Gravity.CENTER);
    t.setTextSize(100);
//    t.setTextColor(R.color.textcolor);
      return t;
	}
}
