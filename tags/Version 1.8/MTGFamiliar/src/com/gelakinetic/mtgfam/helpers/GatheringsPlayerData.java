package com.gelakinetic.mtgfam.helpers;

public class GatheringsPlayerData {
		private String		customName;
		private int			startingLife;

		public GatheringsPlayerData() {
			customName = "";
			startingLife = 20;
		}
		
		public GatheringsPlayerData(String _name, int _life){
			customName = _name;
			startingLife = _life;
		}

		public void setCustomName(String _customName) {
			customName = _customName;
		}

		public String getName() {
			return customName;
		}

		public void setStartingLife(int _startingLife) {
			startingLife = _startingLife;
		}

		public int getStartingLife() {
			return startingLife;
		}
}

