package com.gelakinetic.mtgfam.helpers;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.SpiceRequest;

public class PriceFetchRequest extends SpiceRequest<String> {

	private String number;
	private CardDbAdapter mDbHelper;
	private String cardName;
	private String setCode;
	private int multiverseId;

	public PriceFetchRequest(String cardName, String setCode, String number, int multiverseID, CardDbAdapter helper) {
		super(String.class);
		this.cardName = cardName;
		this.setCode = setCode;
		this.number = number;
		this.multiverseId = multiverseID;
		this.mDbHelper = helper;
	}

	@Override
	public String loadDataFromNetwork() throws SpiceException {

		Log.e("robospice", "loadDataFromNetwork");
		try {
			if(number == null) {
				Cursor c = mDbHelper.fetchCardByNameAndSet(cardName, setCode);
				number = c.getString(c.getColumnIndex(CardDbAdapter.KEY_NUMBER));
				c.close();
			}

			String tcgname = mDbHelper.getTCGname(setCode);
			String tcgCardName;
			if (CardDbAdapter.isTransformable(number, setCode) && number.contains("b")) {
				tcgCardName = mDbHelper.getTransformName(setCode, number.replace("b", "a"));
			}
			else if (multiverseId!= -1 && mDbHelper.isSplitCard(multiverseId)) {
				tcgCardName = mDbHelper.getSplitName(multiverseId);
			}
			else {
				tcgCardName = cardName;
			}
			URL priceurl = new URL(CardDbAdapter.removeAccentMarks(new String("http://partner.tcgplayer.com/x2/phl.asmx/p?pk=MTGFAMILIA&s=" + tcgname + "&p="
					+ tcgCardName).replace(" ", "%20").replace("Æ", "Ae")));


			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
				System.setProperty("http.keepAlive", "false");
			}

			HttpURLConnection urlConnection = (HttpURLConnection) priceurl.openConnection();
			String result = IOUtils.toString(urlConnection.getInputStream());
			urlConnection.disconnect();

			String retval;
			Document d = loadXMLFromString(result);
			Element e = d.getDocumentElement();
			retval = getString("lowprice", e);
			if(retval == null) {
				throw new SpiceException("PriceDNE");
			}
			else {
			retval += "@@" + getString("avgprice", e) +
					"@@" + getString("hiprice", e) +
					"@@" + getString("link", e);
			}
			return retval;
		}
		catch(FamiliarDbException e) {
			throw new SpiceException("FamiliarDbException");
		} catch (MalformedURLException e) {
			throw new SpiceException("MalformedURLException");
		} catch (IOException e) {
			throw new SpiceException("IOException");
		} catch (ParserConfigurationException e) {
			throw new SpiceException("ParserConfigurationException");
		} catch (SAXException e) {
			throw new SpiceException("SAXException");
		}
	}

	public static Document loadXMLFromString(String xml) throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}

	protected String getString(String tagName, Element element) {
		NodeList list = element.getElementsByTagName(tagName);
		if (list != null && list.getLength() > 0) {
			NodeList subList = list.item(0).getChildNodes();

			if (subList != null) {
				String retval = "";
				for(int i = 0; i < subList.getLength(); i++) {
					retval += subList.item(i).getNodeValue();
				}
				return retval;
			}
		}

		return null;
	}
}
