package com.mtg.fam;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class rng extends Activity
{
    private Random r;
    private ImageView d2, d4, d6, d8, d10, d12, d20, d100;
    private TextView dieOutput;
    private boolean d2AsCoin;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Handler handler;

    public static final int updateDelay = 150;

    @Override
	public void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rng);

        SharedPreferences settings = getSharedPreferences("prefs", 0);
        d2AsCoin = settings.getBoolean("d2AsCoin", true);

        final rng anchor = this;
        r = new Random();
        handler = new Handler();

        dieOutput = (TextView) findViewById(R.id.die_output);

        d2 = (ImageView) findViewById(R.id.d2);
        d4 = (ImageView) findViewById(R.id.d4);
        d6 = (ImageView) findViewById(R.id.d6);
        d8 = (ImageView) findViewById(R.id.d8);
        d10 = (ImageView) findViewById(R.id.d10);
        d12 = (ImageView) findViewById(R.id.d12);
        d20 = (ImageView) findViewById(R.id.d20);
        d100 = (ImageView) findViewById(R.id.d100);

        if(d2 != null)
        {
            d2.setImageResource(R.drawable.dcoin);
            if(d2AsCoin)
            {
                d2.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view)
                    {
                        anchor.flipCoin();
                    }
                });
            }
            else
            {
                d2.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view)
                    {
                        anchor.rollDie(2);
                    }
                });
            }
        }
        if(d4 != null)
        {
            d4.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view)
                {
                    anchor.rollDie(4);
                }
            });
        }
        if(d6 != null)
        {
            d6.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view)
                {
                    anchor.rollDie(6);
                }
            });
        }
        if(d8 != null)
        {
            d8.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view)
                {
                    anchor.rollDie(8);
                }
            });
        }
        if(d10 != null)
        {
            d10.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view)
                {
                    anchor.rollDie(10);
                }
            });
        }
        if(d12 != null)
        {
            d12.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view)
                {
                    anchor.rollDie(12);
                }
            });
        }
        if(d20 != null)
        {
            d20.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view)
                {
                    anchor.rollDie(20);
                }
            });
        }
        if(d100 != null)
        {
            d100.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view)
                {
                    anchor.rollDie(100);
                }
            });
        }
    }

    public void rollDie(int d)
    {
        final int f_d = d;
        if(dieOutput != null)
        {
            dieOutput.setText("");
            scheduler.schedule(new Runnable() {
                    public void run()
                    {
                        handler.post(new Runnable() {
                            public void run() {
                                dieOutput.setText("" + (r.nextInt(f_d) + 1));
                            }
                        });
                    }
                },
                updateDelay,
                TimeUnit.MILLISECONDS);
        }
    }

    public void flipCoin()
    {
        if(dieOutput != null)
        {
            String output = "heads";
            dieOutput.setText("");
            if(r.nextInt(2) == 0)
            {
                output = "tails";
            }
            final String f_output = output;
            scheduler.schedule(new Runnable() {
                    public void run()
                    {
                        handler.post(new Runnable() {
                            public void run() {
                                dieOutput.setText(f_output);
                            }
                        });
                    }
                },
                updateDelay,
                TimeUnit.MILLISECONDS);
        }
    }
}
