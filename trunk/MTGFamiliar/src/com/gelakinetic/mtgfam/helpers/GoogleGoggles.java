package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;

public class GoogleGoggles {
    // Image should be less than 140Kb !!
	
	// Aethys 2012-10-25 : Using GoogleGoggles to identify a card is a way to fill the name in the search card activity with
	// the english name, even if you have a foreign card in front of you (a french one for example ;) ) with no idea of the english name !
	 
    // The POST body required to validate the CSSID.
    private static byte[] cssidPostBody = new byte[] {0x22,
        0x00, 0x62, 0x3C, 0x0A, 0x13, 0x22, 0x02, 0x65, 0x6E, (byte)0xBA,
        (byte)0xD3, (byte)0xF0, 0x3B,0x0A,0x08,0x01,0x10,0x01,0x28,0x01,
        0x30,0x00,0x38,0x01, 0x12,0x1D,0x0A,0x09,0x69,0x50,0x68,0x6F,0x6E,
        0x65,0x20,0x4F, 0x53,0x12,0x03,0x34,0x2E,0x31,0x1A,0x00,0x22,0x09,
        0x69,0x50,0x68,0x6F,0x6E,0x65,0x33,0x47,0x53,0x1A,0x02,0x08,0x02,
        0x22,0x02,0x08,0x01};    
 
    // Bytes trailing the image byte array.
    private static byte[] trailingBytes = new byte[] {
        0x18, 0x4B, 0x20, 0x01, 0x30, 0x00, (byte)0x92, (byte)0xEC, (byte)0xF4, 0x3B,
        0x09, 0x18, 0x00, 0x38, (byte)0xC6, (byte)0x97, (byte)0xDC, (byte)0xDF, (byte)0xF7, 0x25,
        0x22, 0x00 };
 
    // Put in static to reuse in the next connection
	private static String sCssid = null;

	private static String URL_WEBSERVICE_GOGGLES = "http://www.google.com/goggles/container_proto?cssid=";
	
    public static String StartCardSearch(Bitmap mImageBitmap, Context ctx, CardDbAdapter mDbHelper) throws IOException {
 
        int i = 0;
 
        boolean cssidIsValid = false;

        if (sCssid == null) {
        	sCssid = generateCSSID();
        }
 
        while (i < 3) {
 
            //System.out.println(sCssid);
             cssidIsValid = ValidateCSSID(sCssid);    
 
            if (cssidIsValid) {
                break;
            } else {
            	//ShowGogglesToast(ctx, ctx.getString(R.string.goggles_cssid_expired));
            	sCssid = generateCSSID();
            }
            i++;
        }
 
        if (cssidIsValid) {
            return getCardName(sCssid, mImageBitmap, mDbHelper);
        } else {
        	//ShowGogglesToast(ctx, ctx.getString(R.string.goggles_3_attempts));
            return "";
        }
    }
 
    // Generates a cssid.
    private static String generateCSSID(){
            BigInteger bi = new BigInteger(64, new Random());
            return bi.toString(16).toUpperCase();
    }
 
    // Validating cssid
    private static boolean ValidateCSSID(String cssid) throws IOException{
        URL url = new URL(URL_WEBSERVICE_GOGGLES + cssid);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-protobuffer");
        conn.setRequestProperty("Pragma", "no-cache");
        OutputStream out = conn.getOutputStream();
        out.write(cssidPostBody);
        out.close();
        
		return (conn.getResponseCode() == 200);

    }
     
    private static String getCardName(String cssid, Bitmap mBitmap, CardDbAdapter mDbHelper) throws IOException
    {
        URL url = new URL(URL_WEBSERVICE_GOGGLES + cssid);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-protobuffer");
        conn.setRequestProperty("Pragma", "no-cache");
 
        //Convert Bitmap to byte[]
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        
        int x = byteArray.length;
        byte[] xVarint = toVarint32(x);
        // a = x + 32
        byte[] aVarint = toVarint32(x+32);
        // b = x + 14
        byte[] bVarint = toVarint32(x+14);
        // c = x + 10
        byte[] cVarint = toVarint32(x+10);
        // Should send the next structure as body:
        // 0A [a] 0A [b] 0A [c] 0A [x] [image bytes]
        OutputStream out = conn.getOutputStream();
        // 0x0A
        out.write(new byte[] { 10 });
        // a
        out.write(aVarint);
        // 0x0A
        out.write(new byte[] { 10 });
        // b
        out.write(bVarint);
        // 0x0A
        out.write(new byte[] { 10 });
        // c
        out.write(cVarint);
        // 0x0A
        out.write(new byte[] { 10 });
        // x
        out.write(xVarint);
        // Write image
        out.write(byteArray);
        // Write trailing bytes
        out.write(trailingBytes);
        out.close();

		BufferedReader buffRead = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		ArrayList<String> names = new ArrayList<String>();
		while ((line = buffRead.readLine()) != null) {
			// Split on all characters not in a card name. This could use more punctuation (maybe) and accent marks.
			// We cant split on whitespace because the goggles return has random junk characters
			String words[] = line.split("[^A-Za-z\\-\\'\\,.]+");

			ArrayList<String> range = new ArrayList<String>();
			try {
				for (String word : words) {
					if (word.length() > 0 && mDbHelper.isPartOfACardName(word)) {
						range.add(word);
					} else {
						if (range.size() > 0) {
							String name = checkForCardName(range, mDbHelper);
							if (name != null) {
								names.add(name);
							}
						}
						range.clear();
					}
				}
				// If the last word gets added to the range, check outside the
				// loop
				if (range.size() > 0) {
					String name = checkForCardName(range, mDbHelper);
					if (name != null) {
						names.add(name);
					}
				}
			} catch (FamiliarDbException e) {
				return null;
			}
			if (names.size() > 0) {
				break;
			}
		}
		if (names.size() > 0) {
			String retval = null;
			int numSpaces = -1;
			for (String s : names) {
				if (countSpaces(s) > numSpaces) {
					retval = s;
					numSpaces = countSpaces(s);
				}
			}
			return retval;
		}
		return null;
	}
 
	private static String checkForCardName(ArrayList<String> range,
			CardDbAdapter mDbHelper) {
		ArrayList<String> names = new ArrayList<String>();
		for (int start = 0; start < range.size(); start++) {
			for (int finish = range.size(); finish > start; finish--) {
				String toSearch = "";
				for (int i = start; i < finish; i++) {
					toSearch += range.get(i) + " ";
				}
				toSearch = toSearch.trim();
				try {
					if (mDbHelper.isValidCardName(toSearch)) {
						names.add(toSearch);
					}
				} catch (FamiliarDbException e) {
					return null;
				}
			}
		}
		if (names.size() == 0) {
			return null;
		}
		String retval = "";
		int numSpaces = -1;
		for (String s : names) {
			if (countSpaces(s) > numSpaces) {
				retval = s;
				numSpaces = countSpaces(s);
			}
		}
		return retval;
	}
	
	static int countSpaces(String s) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == ' ') {
				count++;
			}
		}
		return count;
	}

	// Encodes an int32 into varint32.
    private static byte[] toVarint32(int value)
    {
        int index = 0;
        int tmp = value;
        while ((0x7F & tmp) != 0)
        {
            tmp = tmp >> 7;
            index++;
        }
        byte[] res = new byte[index];
        index = 0;
        while ((0x7F & value) != 0)
        {
            int i = (0x7F & value);
            if ((0x7F & (value >> 7)) != 0)
            {
                i += 128;
            }
            res[index] = ((byte)i);
            value = value >> 7;
            index++;
        }
        return res;
    }
	
}
