package com.gelakinetic.mtgfam;

import android.app.Application;

public class MyApp extends Application {

  private int myState;

  public int getState(){
    return myState;
  }
  public void setState(int s){
    myState = s;
  }
}
