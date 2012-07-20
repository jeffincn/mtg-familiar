package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import com.gelakinetic.mtgfam.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Xml;
import android.view.View;
import android.widget.EditText;

public class GatheringsIO {
	final private static String	FOLDERPATH	= "Gatherings";
	final private static String	DEFAULTFILE	= "default";
	Context											ctx;

	public GatheringsIO(Context _ctx) {
		ctx = _ctx;
	}

	// returns the default Gathering file name.
	public ArrayList<GatheringsPlayerData> getDefaultGathering() {
		ArrayList<GatheringsPlayerData> players = new ArrayList<GatheringsPlayerData>();

		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		File defaultFile = new File(path, DEFAULTFILE + ".xml");

		try {
			if (!defaultFile.exists()) {
				throw new FileNotFoundException();
			}

			players = ReadGatheringXML(defaultFile);

		}
		catch (FileNotFoundException e) {
			players.add(new GatheringsPlayerData("Player 1", 20));
			players.add(new GatheringsPlayerData("Player 2", 20));
			return players;
		}

		return players;
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
	
	public void writeGatheringXML(ArrayList<GatheringsPlayerData> _players, String _gatheringName) { 
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddkkmmss");
	
		String gathering = sdf.format(date);
	
		writeGatheringXML(gathering, _players, _gatheringName);
	}
	
	public void writeDefaultGatheringXML(ArrayList<GatheringsPlayerData> _players){
		//Don't add .xml, it is appeneded just before the write.
		writeGatheringXML(DEFAULTFILE, _players, "default");
	}
	
	public void writeGatheringXML(String _fileName, ArrayList<GatheringsPlayerData> _players, String _gatheringName) {
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

			serializer.startTag("", "players");

			for(GatheringsPlayerData player : _players){
				
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
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public ArrayList<GatheringsPlayerData> ReadGatheringXML(String _gatheringFile) {
		File path = new File(ctx.getFilesDir(), FOLDERPATH);
		File gathering = new File(path, _gatheringFile);

		return ReadGatheringXML(gathering);
	}

	public ArrayList<GatheringsPlayerData> ReadGatheringXML(File _gatheringFile) {
		ArrayList<GatheringsPlayerData> returnList = new ArrayList<GatheringsPlayerData>();
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
				customName = (String) name.getChildNodes().item(0).getNodeValue();

				Element life = (Element) el.getElementsByTagName("startinglife").item(0);
				String sLife = (String) life.getChildNodes().item(0).getNodeValue();
				int startingLife = Integer.parseInt(sLife);

				GatheringsPlayerData player = new GatheringsPlayerData();
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
	}
	
	public void DeleteGatheringByName(String _name){
		for(String fileName : getGatheringFileList()){
			if (_name.equals(ReadGatheringNameFromXML(fileName))){
				DeleteGathering(fileName);
			}
		}
	}
}
