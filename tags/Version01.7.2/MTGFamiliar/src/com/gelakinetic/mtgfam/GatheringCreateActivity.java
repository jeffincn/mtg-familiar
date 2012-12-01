/**
Copyright 2012 Jonathan Bettger

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import android.R.bool;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.XmlResourceParser;

import android.os.Bundle;

import android.support.v4.app.FragmentActivity;

import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.widget.RadioGroup;
import android.widget.TextView;

public class GatheringCreateActivity extends FragmentActivity {
	final private String						FOLDERPATH = "Gatherings";
	final private String						DEFAULTFILE = "default.info";
	final private String						name_gathering = "Please entering a name for the Gathering.";
	
	private Context								mCtx;
	private LinearLayout						mainLayout;	
	
	private TextView							gatheringName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gathering_create_activity);

		MenuFragmentCompat.init(this, R.menu.gathering_menu, "trading_menu_fragment");

		gatheringName = (TextView) findViewById(R.id.gathering_name);
		
		mainLayout = (LinearLayout) findViewById(R.id.gathering_player_list);
		
		mCtx = this;
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		File defaultGathering = new File(FOLDERPATH, "default.info");
		if (defaultGathering.exists())
		{
			
		}
		
		MyApp appState = ((MyApp) getApplicationContext());
		appState.setState(0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Read values from the "savedInstanceState"-object and put them in your
		// textview
		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {

		// Save the values you need from your textview into "outState"-object
		super.onSaveInstanceState(outState);
	}
	
	private class PlayerData{
		private boolean isDefaultName;
		private String customName;
		private int startingLife;
		
		PlayerData(){
			isDefaultName = true;
			customName = "";
			startingLife = 20;
		}
		
		private void setCustomName(String _customName){
			customName = _customName;
		}
		
		private String getName(){
			return customName;
		}
		
		private void setStartingLife(int _startingLife){
			startingLife = _startingLife;
		}
		
		private int getStartingLife(){
			return startingLife;
		}
		
		private void setDefaultName(boolean _isDefault){
			isDefaultName = _isDefault;
		}
		
		private boolean getIsDefaultName() {
			return isDefaultName;
		}
	}
	
	
	private ArrayList<PlayerData> ReadGatheringXML(String _gatheringFile){
		File path = new File(getFilesDir(), FOLDERPATH);
		File gathering = new File(path, _gatheringFile);
		
		return ReadGatheringXML(gathering);
	}
	
	private ArrayList<PlayerData> ReadGatheringXML(File _gatheringFile){
		ArrayList<PlayerData> returnList = new ArrayList<PlayerData>();
		Document dom = null;
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom = db.parse(_gatheringFile);
		}catch(ParserConfigurationException pce) {
			//pce.printStackTrace();
			return returnList;
		}catch(SAXException se) {
			//se.printStackTrace();
			return returnList;
		}catch(IOException ioe) {
			//.printStackTrace();
			return returnList;
		}
		
		if (dom == null)
			return returnList;
		
		Element docEle = dom.getDocumentElement();
		
		//Element playerList = (Element) docEle.getElementsByTagName("players").item(0);
		//int numOfPlayers = Integer.parseInt(playerList.getAttribute("number"));
		
		NodeList nl = docEle.getElementsByTagName("player");
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength();i++) {
				
				Element el = (Element)nl.item(i);
				
				Element name = (Element) el.getElementsByTagName("name").item(0);
				String customName = "";
				boolean isDefault = Boolean.parseBoolean(name.getAttribute("default"));
				if (isDefault == false) {
					customName = (String)name.getChildNodes().item(0).getNodeValue();
				}
				
				Element life = (Element) el.getElementsByTagName("startinglife").item(0);
				String sLife = (String)life.getChildNodes().item(0).getNodeValue();
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
	
	private String writeGatheringXML(){
		int players = mainLayout.getChildCount();
		
	    XmlSerializer serializer = Xml.newSerializer();
	    StringWriter writer = new StringWriter();
	    try {
	        serializer.setOutput(writer);
	        serializer.startDocument("UTF-8", true);
	        
	        serializer.startTag("", "gathering");
	        serializer.startTag("", "name");
	        serializer.text(gatheringName.getText().toString());
	        serializer.endTag("", "name");
	        
	        serializer.startTag("", "players");
	        //serializer.attribute("", "number", String.valueOf(players));
	        
	        for (int idx = 0; idx < players; idx++) {
	        	View player = mainLayout.getChildAt(idx);	
	        	
	        	RadioGroup nameSelection = (RadioGroup) player.findViewById(R.id.radioGroupName);
	        	String defaultname = ((nameSelection.getCheckedRadioButtonId() == R.id.defaultNameRadio) ? "true" : "false");
	        	
	        	EditText customName = (EditText) player.findViewById(R.id.custom_name);
	        	String name = customName.getText().toString();
	        	if (name == null)
	        		name = "";
	        	
	        	EditText startingLife = (EditText) player.findViewById(R.id.starting_life);
	        	String life = startingLife.getText().toString();
	        	if (life == null || life == "")
	        		life = "0";
	        	
	        	
	            serializer.startTag("", "player");
		            serializer.startTag("", "name");
		            serializer.attribute("", "default", defaultname);
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
	        return writer.toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    } 
	}
	
	private String getDefaultGathering(){
		String defaultGathering = "";
		BufferedReader reader;
		
		File path = new File(getFilesDir(), FOLDERPATH);
		File defaultFile = new File(path, DEFAULTFILE);
		
		if (!defaultFile.exists()){
			return "";
		}
		
		try {
			reader = new BufferedReader(new FileReader(defaultFile));
			defaultGathering = reader.readLine();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		
		return defaultGathering;
	}
	
	private ArrayList<String> getGatheringList(){
		ArrayList<String> returnList = new ArrayList<String>();
		
		File path = new File(getFilesDir(), FOLDERPATH);
		if (!path.exists()){
			return returnList;
		}
		
		File[] gatheringList = path.listFiles();
		
		for (int idx = 0; idx < gatheringList.length; idx++){
			if (gatheringList[idx].getName().equals(DEFAULTFILE)){
				continue;
			}
			returnList.add(gatheringList[idx].getName());
		}
		
		return returnList;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.add_player:
				LayoutInflater inf = getLayoutInflater();
				View v = inf.inflate(R.layout.gathering_create_player_row, null);
				mainLayout.addView(v);
				
				return true;
			case R.id.remove_player:
				mainLayout.removeViewAt(mainLayout.getChildCount() - 1);
				return true;
			case R.id.save_gathering:
				if (gatheringName.getText().toString().trim().equals("")) {
					Toast.makeText(getApplicationContext(), name_gathering, Toast.LENGTH_LONG).show();
				}
				
				try {
					String gathering = gatheringName.getText().toString().replaceAll("[^A-Za-z0-9]", "");
					
					File path = new File(getFilesDir(), FOLDERPATH);
					if (!path.exists())
						if (path.mkdirs() == false)
							throw new FileNotFoundException("Folders not made");
					
					File file = new File(path,  gathering + ".xml");
					
					String string = writeGatheringXML();
					
					BufferedWriter out = new BufferedWriter(new FileWriter(file));
				
					out.write(string);
					out.close();
					
					//Write out the new default
					File namedDefault = new File(path, DEFAULTFILE);
					out = new BufferedWriter(new FileWriter(namedDefault));
					out.write(gathering);
					out.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				return true;
			case R.id.load_gathering:
				ArrayList<String> gatherings = getGatheringList();
				final String[] fGatherings = gatherings.toArray(new String[gatherings.size()]);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
				builder.setTitle("Load a Gathering");
				builder.setItems(fGatherings, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int item) {
						ReadGatheringXML(fGatherings[item]);
						return;
					}
				});
				builder.create().show();
				
				
				
				
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
