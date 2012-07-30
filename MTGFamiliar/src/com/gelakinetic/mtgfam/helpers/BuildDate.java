/**
Copyright 2012 Michael Shick

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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.content.pm.ApplicationInfo;

public class BuildDate {
    public static Date get(Context context) {
        // courtesy StackOverflow user mice
        // http://stackoverflow.com/questions/7607165/how-to-write-build-time-stamp-into-apk
        try {
            ApplicationInfo ai = context.getPackageManager()
                .getApplicationInfo(context.getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            long time = ze.getTime();
            return new Date(time);
        }
        catch(Exception e) {
            return new GregorianCalendar(1990, 2, 13).getTime();
        }
    }
}
