package com.gelakinetic.mtgfam.helpers;

import java.nio.ByteBuffer;

public class PriceInfo {
	public double low = 0;
	public double average = 0;
	public double high = 0;
	public double foil_average = 0;
	public String url;
	
	/**
	 * Pack all the fields into a byte buffer and return it.
	 * @return the byte buffer containing this object's information
	 */
	public byte[] toBytes() {

        // Longs to bytes
        byte[] bytes = new byte[8 * 4 + url.length()];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.putDouble(low);
        buf.putDouble(average);
        buf.putDouble(high);
        buf.putDouble(foil_average);
        buf.put(url.getBytes());
        
        return bytes;
	}

	/**
	 * Fill in the fields for this object from the byte buffer. The form
	 * should be 8 bytes for the double representation of the low, average,
	 * high, and foil prices, and then the bytes for the URL
	 * 
	 * @param bytes
	 */
	public void fromBytes(byte[] bytes) {

        // Bytes to longs
        ByteBuffer buf2 = ByteBuffer.wrap(bytes);
        low = buf2.getDouble();
        average = buf2.getDouble();
        high = buf2.getDouble();
        foil_average = buf2.getDouble();
        
        byte stringbuf[] = new byte[bytes.length - 8*4];
        buf2.get(stringbuf);
        url = new String(stringbuf);
	}
	
}
