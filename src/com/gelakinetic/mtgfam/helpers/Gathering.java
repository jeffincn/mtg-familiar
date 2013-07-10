package com.gelakinetic.mtgfam.helpers;

import java.util.ArrayList;

public class Gathering {
	private ArrayList<GatheringsPlayerData> playerList;
    private int displayMode;

	public Gathering() {
		playerList = new ArrayList<GatheringsPlayerData>();
		displayMode = 0;
	}

    public Gathering(ArrayList<GatheringsPlayerData> _playerList, int _displayMode) {
        playerList = _playerList;
        displayMode = _displayMode;
    }

    public void setPlayerList(ArrayList<GatheringsPlayerData> _playerList) {
        playerList = _playerList;
    }

    public ArrayList<GatheringsPlayerData> getPlayerList() {
        return playerList;
    }

    public void setDisplayMode(int _displayMode) {
        displayMode = _displayMode;
    }

    public int getDisplayMode() {
        return displayMode;
    }
}
