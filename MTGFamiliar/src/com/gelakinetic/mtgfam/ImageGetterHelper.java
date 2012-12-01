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

package com.gelakinetic.mtgfam;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;

public class ImageGetterHelper {

	public static ImageGetter GlyphGetter(final Resources r) {
		return new ImageGetter() {
			public Drawable getDrawable(String source) {
				Drawable d = null;
				source = source.replace("/", "");

				int[] drawableNums = { R.drawable.zero, R.drawable.one, R.drawable.two, R.drawable.three, R.drawable.four,
						R.drawable.five, R.drawable.six, R.drawable.seven, R.drawable.eight, R.drawable.nine, R.drawable.ten,
						R.drawable.eleven, R.drawable.twelve, R.drawable.thirteen, R.drawable.fourteen, R.drawable.fifteen,
						R.drawable.sixteen, R.drawable.seventeen, R.drawable.eighteen, R.drawable.ninteen, R.drawable.twenty };

				if (source.equalsIgnoreCase("w")) {
					d = r.getDrawable(R.drawable.w);
				}
				else if (source.equalsIgnoreCase("u")) {
					d = r.getDrawable(R.drawable.u);
				}
				else if (source.equalsIgnoreCase("b")) {
					d = r.getDrawable(R.drawable.b);
				}
				else if (source.equalsIgnoreCase("r")) {
					d = r.getDrawable(R.drawable.r);
				}
				else if (source.equalsIgnoreCase("g")) {
					d = r.getDrawable(R.drawable.g);
				}
				else if (source.equalsIgnoreCase("t")) {
					d = r.getDrawable(R.drawable.tap);
				}
				else if (source.equalsIgnoreCase("q")) {
					d = r.getDrawable(R.drawable.untap);
				}
				else if (source.equalsIgnoreCase("wu") || source.equalsIgnoreCase("uw")) {
					d = r.getDrawable(R.drawable.wu);
				}
				else if (source.equalsIgnoreCase("ub") || source.equalsIgnoreCase("bu")) {
					d = r.getDrawable(R.drawable.ub);
				}
				else if (source.equalsIgnoreCase("br") || source.equalsIgnoreCase("rb")) {
					d = r.getDrawable(R.drawable.br);
				}
				else if (source.equalsIgnoreCase("rg") || source.equalsIgnoreCase("gr")) {
					d = r.getDrawable(R.drawable.rg);
				}
				else if (source.equalsIgnoreCase("gw") || source.equalsIgnoreCase("wg")) {
					d = r.getDrawable(R.drawable.gw);
				}
				else if (source.equalsIgnoreCase("wb") || source.equalsIgnoreCase("bw")) {
					d = r.getDrawable(R.drawable.wb);
				}
				else if (source.equalsIgnoreCase("bg") || source.equalsIgnoreCase("gb")) {
					d = r.getDrawable(R.drawable.bg);
				}
				else if (source.equalsIgnoreCase("gu") || source.equalsIgnoreCase("ug")) {
					d = r.getDrawable(R.drawable.gu);
				}
				else if (source.equalsIgnoreCase("ur") || source.equalsIgnoreCase("ru")) {
					d = r.getDrawable(R.drawable.ur);
				}
				else if (source.equalsIgnoreCase("rw") || source.equalsIgnoreCase("wr")) {
					d = r.getDrawable(R.drawable.rw);
				}
				else if (source.equalsIgnoreCase("2w") || source.equalsIgnoreCase("w2")) {
					d = r.getDrawable(R.drawable.w2);
				}
				else if (source.equalsIgnoreCase("2u") || source.equalsIgnoreCase("u2")) {
					d = r.getDrawable(R.drawable.u2);
				}
				else if (source.equalsIgnoreCase("2b") || source.equalsIgnoreCase("b2")) {
					d = r.getDrawable(R.drawable.b2);
				}
				else if (source.equalsIgnoreCase("2r") || source.equalsIgnoreCase("r2")) {
					d = r.getDrawable(R.drawable.r2);
				}
				else if (source.equalsIgnoreCase("2g") || source.equalsIgnoreCase("g2")) {
					d = r.getDrawable(R.drawable.g2);
				}
				else if (source.equalsIgnoreCase("s")) {
					d = r.getDrawable(R.drawable.s);
				}
				else if (source.equalsIgnoreCase("pw") || source.equalsIgnoreCase("wp")) {
					d = r.getDrawable(R.drawable.pw);
				}
				else if (source.equalsIgnoreCase("pu") || source.equalsIgnoreCase("up")) {
					d = r.getDrawable(R.drawable.pu);
				}
				else if (source.equalsIgnoreCase("pb") || source.equalsIgnoreCase("bp")) {
					d = r.getDrawable(R.drawable.pb);
				}
				else if (source.equalsIgnoreCase("pr") || source.equalsIgnoreCase("rp")) {
					d = r.getDrawable(R.drawable.pr);
				}
				else if (source.equalsIgnoreCase("pg") || source.equalsIgnoreCase("gp")) {
					d = r.getDrawable(R.drawable.pg);
				}
				else if (source.equalsIgnoreCase("p")) {
					d = r.getDrawable(R.drawable.p);
				}
				else if (source.equalsIgnoreCase("+oo")) {
					d = r.getDrawable(R.drawable.inf);
				}
				else if (source.equalsIgnoreCase("100")) {
					d = r.getDrawable(R.drawable.hundred);
				}
				else if (source.equalsIgnoreCase("1000000")) {
					d = r.getDrawable(R.drawable.million);
				}
				else if (source.equalsIgnoreCase("hr") || source.equalsIgnoreCase("rh")) {
					d = r.getDrawable(R.drawable.hr);
				}
				else if (source.equalsIgnoreCase("hw") || source.equalsIgnoreCase("wh")) {
					d = r.getDrawable(R.drawable.hw);
				}
				else if (source.equalsIgnoreCase("c")) {
					d = r.getDrawable(R.drawable.c);
				}
				else if (source.equalsIgnoreCase("z")) {
					d = r.getDrawable(R.drawable.z);
				}
				else if (source.equalsIgnoreCase("y")) {
					d = r.getDrawable(R.drawable.y);
				}
				else if (source.equalsIgnoreCase("x")) {
					d = r.getDrawable(R.drawable.x);
				}
				else if (source.equalsIgnoreCase("pwk")) {
					d = r.getDrawable(R.drawable.pwk);
				}

				for (int i = 0; i < drawableNums.length; i++) {
					if (source.equals(Integer.valueOf(i).toString())) {
						d = r.getDrawable(drawableNums[i]);
					}
				}

				d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
				return d;
			}
		};
	}
}
