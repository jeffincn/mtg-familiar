package com.gelakinetic.translator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVReader;

public class TranslationSpreadsheetMaker {

	public static void main(String args[]) {
		ArrayList<LinkedHashMap<String, String>> translationMaps = new ArrayList<LinkedHashMap<String, String>>();
		ArrayList<String> langTags = new ArrayList<String>();

		if (args.length != 1) {
			System.out.println("Please supply a path to the res/ directory, or a csv");
			return;
		}

		File tld = new File(args[0]);
		if (!tld.isDirectory()) {
			ArrayList<BufferedWriter> outputFiles = new ArrayList<BufferedWriter>();

			// Turn a CSV back into some XML
			try {
				CSVReader csvr = new CSVReader(new InputStreamReader(new FileInputStream(tld), "UTF8"));
				String[] headers = csvr.readNext();

				// Make the res folder, and all necessary subfolders
				// Open a strings.xml file in each subfolder and save the writer for
				// later
				File resFolder = new File("res");
				if (!resFolder.exists()) {
					resFolder.mkdir();
				}

				for (int i = 1; i < headers.length; i++) {
					if (headers[i].length() > 0) {
						File folder = new File(resFolder, "values-" + headers[i]);
						if (!folder.exists()) {
							folder.mkdir();
						}
						BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(folder,
								"strings.xml")), "UTF8"));
						bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n");
						outputFiles.add(bw);
					}
				}

				boolean pluralsHaveStarted = false;
				int pluralsWritten = 0;
				// Read in the CSV row by row, and write the XML files as it goes along
				String[] nextLine;
				while ((nextLine = csvr.readNext()) != null) {
					for (int i = 0; i < outputFiles.size(); i++) {
						// Make sure ampersands, <>, and ' are all happy
						if(nextLine[0].contains("__plurals__")) {
							String key = nextLine[0].split("&&")[1];
							String quantity = nextLine[0].split("&&")[2];

							if(!pluralsHaveStarted) {
								outputFiles.get(i).write("<plurals name=\"" + key + "\">\n");
								if(i == outputFiles.size() - 1) {
									pluralsHaveStarted = true;
								}
							}
							
							String line = formatLine(nextLine[i + 1]);
							
							outputFiles.get(i).write("<item quantity=\"" + quantity + "\">" + line +"</item>\n");
							
							if(i == 0) {
								pluralsWritten++;
							}
							
							if(pluralsWritten == 4){
								outputFiles.get(i).write("</plurals>\n");
								if(i == outputFiles.size()-1) {
									pluralsWritten = 0;
									pluralsHaveStarted = false;
								}
							}
						}
						else {
							String line = formatLine(nextLine[i + 1]);
							outputFiles.get(i).write("<string name=\"" + nextLine[0] + "\">" + line + "</string>\n");
						}
					}
				}

				// Close everything up
				for (BufferedWriter bw : outputFiles) {
					bw.write("</resources>\n");
					bw.close();
				}
				csvr.close();
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			return;
		}
		else {
			// Turn some XML into a single CSV

			// Look for all the values-x folders and save the language tags.
			for (File f : tld.listFiles()) {
				if (f.isDirectory() && f.getName().toLowerCase().contains("values")) {
					// Get language tag from folder
					String langTag;
					String[] parts = f.getName().split("-");
					if (parts.length == 2) {
						langTag = parts[1];
					}
					else {
						langTag = "en";
					}

					// look in each values folder for a strings.xml file.
					// Parse it into a linkedHashMap if it exists
					try {
						File xml = new File(f, "strings.xml");
						if (xml.exists()) {
							LinkedHashMap<String, String> translationMap = new LinkedHashMap<String, String>();

							// Parse XML into the new hash map
							InputSource is;
							is = new InputSource(new FileInputStream(xml));
							is.setEncoding("UTF-8");

							Document dom = parseXmlFile(is);
							parseDocument(dom, translationMap);

							// Save the map and the tag for writing
							langTags.add(langTag);
							translationMaps.add(translationMap);
						}
					}
					catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
			}

			// Find all unique keys
			LinkedHashSet<String> uniqueKeys = new LinkedHashSet<String>();
			for (LinkedHashMap<String, String> lhm : translationMaps) {
				for (String key : lhm.keySet()) {
					uniqueKeys.add(key);
				}
			}

			// Print the CSV
			try {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("fout.csv"), "UTF8"));

				bw.write("key,");
				for (int i = 0; i < langTags.size(); i++) {
					bw.write(langTags.get(i) + ",");
				}
				bw.write("\n");

				// Write the key, and then all translations in order
				for (String key : uniqueKeys) {
					bw.write("\"" + key + "\",");
					for (int j = 0; j < translationMaps.size(); j++) {
						String val = translationMaps.get(j).get(key);
						if (val != null) {
							bw.write("\"" + val + "\",");
						}
						else {
							bw.write("\"\",");
						}
					}
					bw.write("\n");
				}
				bw.close();
				System.out.println("Spreadsheet written to " + (new File("fout.csv")).getAbsolutePath());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String formatLine(String string) {
		string = string.replace("&", "&amp;");
		
		string = string.replace("\\'", "'"); // clear forward slashes
		string = string.replace("'", "\\'"); // add forward slashes

		string = string.replace("\"\"\"", "\\\"");
		string = string.replace("\"\"", "\"");

		string = string.replace("\\\"", "\"");
		string = string.replace("\"", "\\\"");

		string = string.replace("<", "&lt;").replace(">", "&gt;"); // format html tags
		string = string.replaceAll("\u00a0", "&#160;");
		if (string.length() == 0) {
			string = "NO TRANSLATION";
		}
		
		return string;
	}

	private static Document parseXmlFile(InputSource is) {
		// get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// parse using builder to get DOM representation of the XML file
			return db.parse(is);
		}
		catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		}
		catch (SAXException se) {
			se.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}

	private static void parseDocument(Document dom, LinkedHashMap<String, String> translationMap) {
		// get the root element
		Element docEle = dom.getDocumentElement();

		// get a nodelist of elements
		NodeList nl = docEle.getElementsByTagName("string");
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0; i < nl.getLength(); i++) {

				// get the employee element
				Element el = (Element) nl.item(i);
				try {
					translationMap.put(el.getAttribute("name"), el.getFirstChild().getNodeValue());
				}
				catch (NullPointerException e) {
					System.out.println(e + " " + el.getAttribute("name"));
				}
			}
		}
		
		nl = docEle.getElementsByTagName("plurals");
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0; i < nl.getLength(); i++) {
				String pluralName = ((Element)nl.item(i)).getAttribute("name");
				NodeList nls = ((Element)nl.item(i)).getElementsByTagName("item");
				for(int j = 0; j < nls.getLength(); j++) {
					translationMap.put("__plurals__&&" + pluralName + "&&" + ((Element)nls.item(j)).getAttribute("quantity"), ((Element)nls.item(j)).getFirstChild().getNodeValue());
				}
			}
		}
	}
}
