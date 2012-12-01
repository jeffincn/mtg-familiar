/**
Copyright 2011 Adam Feinstein

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

package com.gelakinetic.mtgfam.helpers;

import android.app.Application;

import com.gelakinetic.mtgfam.activities.FamiliarActivity;

public class MyApp extends Application {

	private int	myState;
	private boolean updating;
	private FamiliarActivity updatingActivity;

	public int getState() {
		return myState;
	}

	public void setState(int s) {
		myState = s;
	}
	
	public boolean isUpdating() {
		return this.updating;
	}
	
	public void setUpdating(boolean updating) {
		this.updating = updating;
	}
	
	public FamiliarActivity getUpdatingActivity() {
		return this.updatingActivity;
	}
	
	public void setUpdatingActivity(FamiliarActivity updatingActivity) {
		this.updatingActivity = updatingActivity;
	}
}
