package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class GatheringsIO {
	final private static String	FOLDERPATH	= "Gatherings";
	final private static String	DEFAULTFILE	= "default.info";
	Context											ctx;

	public GatheringsIO(Context _ctx) {
		ctx = _ctx;
	}

	public class PlayerData {
		private boolean	isDefaultName;
		private String	customName;
		private int			startingLife;

		public PlayerData() {
			isDefaultName = true;
			customName = "";
			startingLife = 20;
		}

		private void setCustomName(String _customName) {
			customName = _customName;
		}

		public String getName() {
			return customName;
		}

		private void setStartingLife(int _startingLife) {
			startingLife = _startingLife;
		}

		public int getStartingLife() {
			return startingLife;
		}

		private void setDefaultName(boolean _isDefault) {
			isDefaultName = _isDefault;
		}

		public boolean getIsDefaultName() {
			return isDefaultName;
		}
	}

	// returns the default Gathering file name.
	public String getDefaultGathering() {
		String defaultGathering = "";
		BufferedReader reader;

		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		File defaultFile = new File(path, DEFAULTFILE);

		try {
			if (!defaultFile.exists()) {
				throw new FileNotFoundException();
			}

			reader = new BufferedReader(new FileReader(defaultFile));
			defaultGathering = reader.readLine();
			reader.close();

		}
		catch (FileNotFoundException e) {
			return "";
		}
		catch (IOException e) {
			return "";
		}

		return defaultGathering;
	}

	public ArrayList<String> getGatheringFileList() {
		ArrayList<String> returnList = new ArrayList<String>();

		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		if (!path.exists()) {
			return returnList;
		}

		File[] gatheringList = path.listFiles();

		for (int idx = 0; idx < gatheringList.length; idx++) {
			if (gatheringList[idx].getName().equals(DEFAULTFILE)) {
				continue;
			}
			returnList.add(gatheringList[idx].getName());
		}

		return returnList;
	}

	public ArrayList<PlayerData> ReadGatheringXML(String _gatheringFile) {
		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		File gathering = new File(path, _gatheringFile);

		return ReadGatheringXML(gathering);
	}

	public ArrayList<PlayerData> ReadGatheringXML(File _gatheringFile) {
		ArrayList<PlayerData> returnList = new ArrayList<PlayerData>();
		Document dom = null;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(_gatheringFile);
		}
		catch (ParserConfigurationException pce) {
			// pce.printStackTrace();
			return returnList;
		}
		catch (SAXException se) {
			// se.printStackTrace();
			return returnList;
		}
		catch (IOException ioe) {
			// .printStackTrace();
			return returnList;
		}

		if (dom == null)
			return returnList;

		Element docEle = dom.getDocumentElement();

		// Element playerList = (Element)
		// docEle.getElementsByTagName("players").item(0);
		// int numOfPlayers = Integer.parseInt(playerList.getAttribute("number"));

		NodeList nl = docEle.getElementsByTagName("player");
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0; i < nl.getLength(); i++) {

				Element el = (Element) nl.item(i);

				Element name = (Element) el.getElementsByTagName("name").item(0);
				String customName = "";
				boolean isDefault = Boolean.parseBoolean(name.getAttribute("default"));
				customName = (String) name.getChildNodes().item(0).getNodeValue();

				Element life = (Element) el.getElementsByTagName("startinglife").item(0);
				String sLife = (String) life.getChildNodes().item(0).getNodeValue();
				int startingLife = Integer.parseInt(sLife);

				PlayerData player = new PlayerData();
				player.setDefaultName(isDefault);
				player.setCustomName(customName);
				player.setStartingLife(startingLife);

				returnList.add(player);
			}
		}

		return returnList;
	}

	public String ReadGatheringNameFromXML(String _gatheringFile) {
		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		File gathering = new File(path, _gatheringFile);

		return ReadGatheringNameFromXML(gathering);
	}

	public String ReadGatheringNameFromXML(File _gatheringFile) {
		String returnString = "";
		Document dom = null;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(_gatheringFile);
		}
		catch (ParserConfigurationException pce) {
			// pce.printStackTrace();
			return returnString;
		}
		catch (SAXException se) {
			// se.printStackTrace();
			return returnString;
		}
		catch (IOException ioe) {
			// .printStackTrace();
			return returnString;
		}

		if (dom == null)
			return returnString;

		Element docEle = dom.getDocumentElement();

		// Element playerList = (Element)
		// docEle.getElementsByTagName("players").item(0);
		// int numOfPlayers = Integer.parseInt(playerList.getAttribute("number"));

		Element name = (Element) docEle.getElementsByTagName("name").item(0);
		String gatheringName = name.getChildNodes().item(0).getNodeValue();

		return gatheringName;
	}

	public void DeleteGathering(String fileName) {
		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		File gatheringFile = new File(path, fileName);
		gatheringFile.delete();

		String defaultGathering = getDefaultGathering();
		if (defaultGathering.equals(fileName)) {
			File defaultFile = new File(path, DEFAULTFILE);
			defaultFile.delete();

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
			Editor editor = preferences.edit();
			editor.putString("player_data", null);
			editor.commit();
		}
	}
}
