/**
Copyright 2011 Michael Shick

This file is part of MTG Familiar.

MTG Familiar is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MTG Familiar is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.gelakinetic.mtgfam;

import java.util.Vector;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class counter extends Activity
{
    public static final int INITIAL_LIFE = 20, INITIAL_POISON = 0, TERMINAL_LIFE = 0, TERMINAL_POISON = 10;
    public static final int CONSTRAINT_POISON = 0, CONSTRAINT_LIFE = Integer.MAX_VALUE - 1;
    public static final int ONE = 0, TWO = 1;
    public static final int NUM_PLAYERS = 2;
    public static final int DIALOG_RESET_CONFIRM = 0;
    public static enum Type {
        LIFE, POISON
    }

    private int timerTick, timerValue, timerStart;
    private Object timerLock;
    private Player player[];
    private ImageView poisonButton, lifeButton, dieButton, poolButton, resetButton;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Handler handler;
    private Type activeType;

    @Override
    public void onCreate(Bundle savedInstance)
    {
        super.onCreate(savedInstance);
        setContentView(R.layout.counter);

        handler = new Handler();

        timerStart = 1000;
        timerTick = 100;
        timerValue = 0;
        timerLock = new Object();

        final Activity anchor = this;

        player = new Player[2];

        player[ONE] = new Player(this, (Button) findViewById(R.id.p1_plus1), (Button) findViewById(R.id.p1_plus5),
                (Button) findViewById(R.id.p1_minus1), (Button) findViewById(R.id.p1_minus5),
                (TextView) findViewById(R.id.p1_readout), (ListView) findViewById(R.id.p1_history));
        player[TWO] = new Player(this, (Button) findViewById(R.id.p2_plus1), (Button) findViewById(R.id.p2_plus5),
                (Button) findViewById(R.id.p2_minus1), (Button) findViewById(R.id.p2_minus5),
                (TextView) findViewById(R.id.p2_readout), (ListView) findViewById(R.id.p2_history));

        poisonButton = (ImageView) findViewById(R.id.poison_button);
        lifeButton = (ImageView) findViewById(R.id.life_button);
        dieButton = (ImageView) findViewById(R.id.die_button);
        poolButton = (ImageView) findViewById(R.id.pool_button);
        resetButton = (ImageView) findViewById(R.id.reset_button);

        Object thingsThatReallyShouldntBeNull[] = {
            player[ONE].plus1, player[ONE].plus5, player[ONE].minus1,
            player[ONE].minus5, player[ONE].readout, player[ONE].history,
            player[TWO].plus1, player[TWO].plus5, player[TWO].minus1,
            player[TWO].minus5, player[TWO].readout, player[TWO].history,
            poisonButton, lifeButton, dieButton, poolButton, resetButton
        };

        for(Object e : thingsThatReallyShouldntBeNull)
        {
            if(e == null)
            {
                Log.e("Life Counter", "Failed to locate all views from inflated XML");
                Toast.makeText(this, "Life counter failed to load!", Toast.LENGTH_LONG).show();
                this.finish();
            }
        }

        poisonButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                setType(Type.POISON);
                update();
            }
        });
        lifeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                setType(Type.LIFE);
                update();
            }
        });
        dieButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                Intent nextActivity = new Intent(anchor, rng.class);
                startActivity(nextActivity);
            }
        });
        poolButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                Toast.makeText(anchor, "Mana pool isn't implemented yet", Toast.LENGTH_LONG).show();
            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                showDialog(DIALOG_RESET_CONFIRM);
            }
        });

        player[ONE].plus1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view)
            {
                synchronized (timerLock)
                {
                    timerValue = timerStart;
                }
                incrementValue(ONE, activeType, 1);
                update();
            }
        });
        player[ONE].plus5.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view)
            {
                synchronized (timerLock)
                {
                    timerValue = timerStart;
                }
                incrementValue(ONE, activeType, 5);
                update();
            }
        });
        player[ONE].minus1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view)
            {
                synchronized (timerLock)
                {
                    timerValue = timerStart;
                }
                incrementValue(ONE, activeType, -1);
                update();
            }
        });
        player[ONE].minus5.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view)
            {
                synchronized (timerLock)
                {
                    timerValue = timerStart;
                }
                incrementValue(ONE, activeType, -5);
                update();
            }
        });

        player[TWO].plus1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view)
            {
                synchronized (timerLock)
                {
                    timerValue = timerStart;
                }
                incrementValue(TWO, activeType, 1);
                update();
            }
        });
        player[TWO].plus5.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view)
            {
                synchronized (timerLock)
                {
                    timerValue = timerStart;
                }
                incrementValue(TWO, activeType, 5);
                update();
            }
        });
        player[TWO].minus1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view)
            {
                synchronized (timerLock)
                {
                    timerValue = timerStart;
                }
                incrementValue(TWO, activeType, -1);
                update();
            }
        });
        player[TWO].minus5.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view)
            {
                synchronized (timerLock)
                {
                    timerValue = timerStart;
                }
                incrementValue(TWO, activeType, -5);
                update();
            }
        });

        reset();
        update();
        
        scheduler.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                boolean doCommit = false;
                synchronized (timerLock)
                {
                    if(timerValue > 0)
                    {
                        timerValue -= timerTick;
                        if(timerValue <= 0)
                        {
                            /*
                             * This is used instead of having the commit loop
                             * here so I don't have to think about deadlock
                             */
                            doCommit = true;
                        }
                    }
                }

                if(doCommit)
                {
                    handler.post(new Runnable() {
                        public void run()
                        {
                            for(int ii = 0; ii < NUM_PLAYERS; ii++)
                            {
                                synchronized (player[ii].lifeAdapter)
                                {   
                                    player[ii].lifeAdapter.commit();
                                }
                                synchronized (player[ii].poisonAdapter)
                                {   
                                    player[ii].poisonAdapter.commit();
                                }
                            }
                        }
                    });
                }
            }
        }, timerTick, timerTick, TimeUnit.MILLISECONDS);
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog;
        switch(id)
        {
            case DIALOG_RESET_CONFIRM:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Reset counters and pool?")
                       .setCancelable(true)
                       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               reset();
                               update();
                           }
                       })
                       .setNegativeButton("No", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                           }
                       });

                dialog = builder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    private void reset()
    {
        setType(Type.LIFE);

        for(int ii = 0; ii < NUM_PLAYERS; ii++)
        {
            setValue(ii, Type.LIFE, INITIAL_LIFE);
            setValue(ii, Type.POISON, INITIAL_POISON);
            player[ii].lifeAdapter = new HistoryAdapter(this, INITIAL_LIFE);
            player[ii].poisonAdapter = new HistoryAdapter(this, INITIAL_POISON);
            player[ii].history.setAdapter(player[ii].lifeAdapter);
        }
    }

    private void setType(Type type)
    {
        activeType = type;

        switch(activeType)
        {
            case LIFE:
                lifeButton.setImageResource(R.drawable.life_button_highlighted);
                poisonButton.setImageResource(R.drawable.poison_button);
                for(int ii = 0; ii < NUM_PLAYERS; ii++)
                {
                    player[ii].history.setAdapter(player[ii].lifeAdapter);
                }
                break;
            case POISON:
                lifeButton.setImageResource(R.drawable.life_button);
                poisonButton.setImageResource(R.drawable.poison_button_highlighted);
                for(int ii = 0; ii < NUM_PLAYERS; ii++)
                {
                    player[ii].history.setAdapter(player[ii].poisonAdapter);
                }
                break;
        }
    }

    private void setValue(int playerNum, Type type, int value)
    {
        if(playerNum != ONE && playerNum != TWO)
        {
            Log.w("Life Counter", "Attempted to set values for nonexistent player " + player);
            return;
        }
        switch(type)
        {
            case LIFE:
                if(value > CONSTRAINT_LIFE)
                {
                    value = CONSTRAINT_LIFE;
                }
                player[playerNum].life = value;
                break;
            case POISON:
                if(value < CONSTRAINT_POISON)
                {
                    value = CONSTRAINT_POISON;
                }
                player[playerNum].poison = value;
        }
    }
    
    private void incrementValue(int playerNum, Type type, int delta)
    {
        int value = 0;
        if(playerNum != ONE && playerNum != TWO)
        {
            Log.w("Life Counter", "Attempted to increment values for nonexistent player " + player);
            return;
        }
        switch(type)
        {
            case LIFE:
                value = player[playerNum].life;
                if(value + delta > CONSTRAINT_LIFE)
                {
                    return;
                }
                player[playerNum].lifeAdapter.update(delta);
                break;
            case POISON:
                value = player[playerNum].poison;
                if(value + delta < CONSTRAINT_POISON)
                {
                    return;
                }
                player[playerNum].poisonAdapter.update(delta);
                break;
        }
        setValue(playerNum, type, value + delta);
    }

    private void update()
    {
        switch(activeType)
        {
            case LIFE:
                player[ONE].readout.setTextColor(0xFFFFFFFF);
                player[ONE].readout.setText("" + player[ONE].life);
                player[TWO].readout.setTextColor(0xFFFFFFFF);
                player[TWO].readout.setText("" + player[TWO].life);
                break;
            case POISON:
                player[ONE].readout.setTextColor(0xFF009000);
                player[ONE].readout.setText("" + player[ONE].poison);
                player[TWO].readout.setTextColor(0xFF009000);
                player[TWO].readout.setText("" + player[TWO].poison);
                break;
        }
    }

    private class Player
    {
        public Button plus1, plus5, minus1, minus5;
        public TextView readout;
        public ListView history;
        public int life, poison;
        public HistoryAdapter lifeAdapter, poisonAdapter;

        public Player(Context context, Button plus1, Button plus5, Button minus1, Button minus5,
                TextView readout, ListView history)
        {
            this.plus1 = plus1;
            this.plus5 = plus5;
            this.minus1 = minus1;
            this.minus5 = minus5;
            this.readout = readout;
            this.history = history;

            this.lifeAdapter = new HistoryAdapter(context, INITIAL_LIFE);
            this.poisonAdapter = new HistoryAdapter(context, INITIAL_POISON);
        }
    }

    private class HistoryAdapter extends BaseAdapter
    {
        private int count, initialValue, delta;
        private ArrayList<Vector<Integer>> list;
        private Context context;

        public static final int ABSOLUTE = 0, RELATIVE = 1;

        public HistoryAdapter(Context context, int initialValue)
        {
            this.context = context;
            list = new ArrayList<Vector<Integer>>();
            count = 0;
            delta = 0;
            this.initialValue = initialValue;
        }

        public void update(int magnitude)
        {
            delta += magnitude;
        }

        public void commit()
        {
            int lastValue = initialValue;
            if(delta == 0)
            {
                return;
            }
            if(count > 0)
            {
                lastValue = list.get(0).get(ABSOLUTE).intValue();
            }
            Vector<Integer> v = new Vector<Integer>();
            v.add(new Integer(lastValue + delta));
            v.add(new Integer(delta));
            list.add(0,v);
            count++;
            delta = 0;
            notifyDataSetChanged();
        }

        public int getCount()
        {
            return count;
        }

        public Object getItem(int position)
        {
            if(position < 0 || position >= count)
            {
                return null;
            }
            return list.get(position);
        }

        public long getItemId(int position)
        {
            if(position < 0 || position >= count)
            {
                return -1l;
            }
            return (long) position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            TextView relative, absolute;
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = vi.inflate(R.layout.history_adapter_row, null);
            Vector<Integer> row = list.get(position);
            absolute = (TextView) v.findViewById(R.id.absolute);
            relative = (TextView) v.findViewById(R.id.relative);
            if(relative == null || absolute == null)
            {
                Log.e("Life Counter", "failed to inflate history adapter row view correctly");
                TextView error = new TextView(context);
                error.setText("ERROR!");
                return error;
            }
            absolute.setText("" + row.get(ABSOLUTE).intValue());
            String relativeString = "";
            int relativeValue = row.get(RELATIVE).intValue();
            if(relativeValue > 0)
            {
                relativeString += "+";
            }
            relativeString += relativeValue;
            relative.setText(relativeString);
            return v;
        }
    }
}
