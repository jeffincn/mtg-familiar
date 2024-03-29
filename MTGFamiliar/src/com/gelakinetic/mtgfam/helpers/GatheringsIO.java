package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Xml;

public class GatheringsIO {
	final private static String	FOLDERPATH	= "Gatherings";
	final private static String	DEFAULTFILE	= "KENNESSAS";
	// If someone happens to also name their file this without ever reading
	// this comment I'll donate my latest mythic rare to them. (Not really.)
	Context											ctx;

	public GatheringsIO(Context _ctx) {
		ctx = _ctx;
	}

	// returns the default Gathering file name.
	public Gathering getDefaultGathering() {
		ArrayList<GatheringsPlayerData> players = new ArrayList<GatheringsPlayerData>();
		Gathering gathering;

		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		File defaultFile = new File(path, DEFAULTFILE + ".xml");

		try {
			if (!defaultFile.exists()) {
				throw new FileNotFoundException();
			}

			gathering = ReadGatheringXML(defaultFile);

		}
		catch (FileNotFoundException e) {
			players.add(new GatheringsPlayerData("Player 1", 20));
			players.add(new GatheringsPlayerData("Player 2", 20));
			return new Gathering(players, 0);
		}

		return gathering;
	}

	public int getNumberOfGatherings() {
		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		if (!path.exists()) {
			return 0;
		}

		File[] gatheringList = path.listFiles();

		int count = 0;
		for (int idx = 0; idx < gatheringList.length; idx++) {
			if (gatheringList[idx].getName().equals(DEFAULTFILE + ".xml")) {
				continue;
			}
			count++;
		}
		return count;
	}

	public ArrayList<String> getGatheringFileList() {
		ArrayList<String> returnList = new ArrayList<String>();

		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		if (!path.exists()) {
			return returnList;
		}

		File[] gatheringList = path.listFiles();

		for (int idx = 0; idx < gatheringList.length; idx++) {
			if (gatheringList[idx].getName().equals(DEFAULTFILE + ".xml")) {
				continue;
			}
			returnList.add(gatheringList[idx].getName());
		}

		return returnList;
	}

	public void writeGatheringXML(ArrayList<GatheringsPlayerData> _players, String _gatheringName, int _displayMode) {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddkkmmss", Locale.ENGLISH);

		String gathering = sdf.format(date);

		writeGatheringXML(gathering, _players, _gatheringName, _displayMode);
	}

	public void writeGatheringXML(String _fileName, ArrayList<GatheringsPlayerData> _players, String _gatheringName,
			int _displayMode) {
		String dataXML = "";

		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);

			serializer.startTag("", "gathering");
			serializer.startTag("", "name");
			serializer.text(_gatheringName);
			serializer.endTag("", "name");

			serializer.startTag("", "displaymode");
			serializer.text(String.valueOf(_displayMode));
			serializer.endTag("", "displaymode");

			serializer.startTag("", "players");

			for (GatheringsPlayerData player : _players) {

				String name = player.getName();

				String life = String.valueOf(player.getStartingLife());
				if (life == null || life == "")
					life = "0";

				serializer.startTag("", "player");

				serializer.startTag("", "name");
				serializer.text(name);
				serializer.endTag("", "name");

				serializer.startTag("", "startinglife");
				serializer.text(String.valueOf(life));
				serializer.endTag("", "startinglife");

				serializer.endTag("", "player");
			}
			serializer.endTag("", "players");
			serializer.endTag("", "gathering");
			serializer.endDocument();

			dataXML = writer.toString();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			File path = new File(ctx.getFilesDir(), FOLDERPATH);
			if (!path.exists())
				if (path.mkdirs() == false)
					throw new FileNotFoundException("Folders not made");

			File file = new File(path, _fileName + ".xml");

			BufferedWriter out = new BufferedWriter(new FileWriter(file));

			out.write(dataXML);
			out.close();
		}
		catch (FileNotFoundException e) {
		}
		catch (IOException e) {
		}
	}

	public Gathering ReadGatheringXML(String _gatheringFile) {
		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		File gathering = new File(path, _gatheringFile);

		return ReadGatheringXML(gathering);
	}

	public Gathering ReadGatheringXML(File _gatheringFile) {
		ArrayList<GatheringsPlayerData> playerList = new ArrayList<GatheringsPlayerData>();
		Document dom = null;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(_gatheringFile);
		}
		catch (ParserConfigurationException pce) {
			return new Gathering(playerList, 0);
		}
		catch (SAXException se) {
			return new Gathering(playerList, 0);
		}
		catch (IOException ioe) {
			return new Gathering(playerList, 0);
		}

		if (dom == null)
			return new Gathering(playerList, 0);

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
				customName = (String) name.getChildNodes().item(0).getNodeValue();

				Element life = (Element) el.getElementsByTagName("startinglife").item(0);
				String sLife = (String) life.getChildNodes().item(0).getNodeValue();
				int startingLife = Integer.parseInt(sLife);

				GatheringsPlayerData player = new GatheringsPlayerData();
				player.setCustomName(customName);
				player.setStartingLife(startingLife);

				playerList.add(player);
			}
		}

		int displayMode;
		Element mode = (Element) docEle.getElementsByTagName("displaymode").item(0);
		if (mode != null) {
			String sMode = (String) mode.getChildNodes().item(0).getNodeValue();
			displayMode = Integer.parseInt(sMode);
		}
		else {
			displayMode = 0;
		}

		return new Gathering(playerList, displayMode);
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
			return returnString;
		}
		catch (SAXException se) {
			return returnString;
		}
		catch (IOException ioe) {
			return returnString;
		}

		if (dom == null)
			return returnString;

		Element docEle = dom.getDocumentElement();

		// Element playerList = (Element)
		// docEle.getElementsByTagName("players").item(0);
		// int numOfPlayers = Integer.parseInt(playerList.getAttribute("number"));

		Element name = (Element) docEle.getElementsByTagName("name").item(0);

		if (name.getChildNodes().item(0) == null) {
			return "";
		}
		String gatheringName = name.getChildNodes().item(0).getNodeValue();

		return gatheringName;
	}

	public void DeleteGathering(String fileName) {
		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		File gatheringFile = new File(path, fileName);
		gatheringFile.delete();
	}

	public void DeleteGatheringByName(String _name) {
		for (String fileName : getGatheringFileList()) {
			if (_name.equals(ReadGatheringNameFromXML(fileName))) {
				DeleteGathering(fileName);
			}
		}
	}
}
