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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TCGPlayerXMLHandler extends DefaultHandler {

	// ===========================================================
	// Fields
	// ===========================================================
	/*
	 * private boolean in_products = false; private boolean in_product = false;
	 * private boolean in_id = false;
	 */
	private boolean	in_hiprice	= false;
	private boolean	in_lowprice	= false;
	private boolean	in_avgprice	= false;
	private boolean	in_link			= false;

	String					hiprice;
	String					avgprice;
	String					lowprice;
	String					link;

	// ===========================================================
	// Methods
	// ===========================================================
	@Override
	public void startDocument() throws SAXException {
		/*
		 * in_products = false; in_product = false; in_id = false;
		 */
		in_hiprice = false;
		in_lowprice = false;
		in_avgprice = false;
		in_link = false;
	}

	@Override
	public void endDocument() throws SAXException {
	}

	/**
	 * Gets be called on opening tags like: <tag> Can provide attribute(s), when
	 * xml was like: <tag attribute="attributeValue">
	 */
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		/*
		 * if (localName.equals("products")) { in_products = true; } else if
		 * (localName.equals("product")) { in_product = true; } else if
		 * (localName.equals("id")) { in_id = true; }
		 * 
		 * else
		 */if (localName.equals("hiprice")) {
			in_hiprice = true;
		}
		else if (localName.equals("lowprice")) {
			in_lowprice = true;
		}
		else if (localName.equals("avgprice")) {
			in_avgprice = true;
		}
		else if (localName.equals("link")) {
			in_link = true;
		}
	}

	/**
	 * Gets be called on closing tags like: </tag>
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		/*
		 * if (localName.equals("products")) { in_products = false; } else if
		 * (localName.equals("product")) { in_product = false; } else if
		 * (localName.equals("id")) { in_id = false; } else
		 */if (localName.equals("hiprice")) {
			in_hiprice = false;
		}
		else if (localName.equals("lowprice")) {
			in_lowprice = false;
		}
		else if (localName.equals("avgprice")) {
			in_avgprice = false;
		}
		else if (localName.equals("link")) {
			in_link = false;
		}
	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		String s = new String(ch, start, length);
		if (in_hiprice) {
			hiprice = s;
		}
		if (in_lowprice) {
			lowprice = s;
		}
		if (in_avgprice) {
			avgprice = s;
		}
		if (in_link) {
			if (link == null) {
				link = s;
			}
			else {
				link += s;
			}
		}
	}
}