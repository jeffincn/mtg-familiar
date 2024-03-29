import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

public class Main implements ActionListener {

	private static String[][] subs = {
		{"\"name\"",								"\"a\""},
		{"\"set\"",									"\"b\""},
		{"\"type\"",								"\"c\""},
		{"\"rarity\"",							"\"d\""},
		{"\"manacost\"",						"\"e\""},
		{"\"converted_manacost\"",	"\"f\""},
		{"\"power\"",								"\"g\""},
		{"\"toughness\"",						"\"h\""},
		{"\"loyalty\"",							"\"i\""},
		{"\"ability\"",							"\"j\""},
		{"\"flavor\"",							"\"k\""},
		{"\"artist\"",							"\"l\""},
		{"\"number\"",							"\"m\""},
		{"\"color\"",								"\"n\""},
		{"\"id\"",									"\"x\""},
//	{"\"RULINGS\"",							"\"z\""},

		{"\"card\"",								"\"o\""},
		{"\"cards\"",								"\"p\""},
		{"\"code\"",								"\"q\""},
		{"\"code_magiccards\"",			"\"r\""},
		{"\"date\"",								"\"y\""},
		{"\"sets\"",								"\"s\""},
		{"\"mtg_carddatabase\"",		"\"t\""},
		{"\"bdd_version\"",					"\"u\""},
		{"\"bdd_date\"",						"\"v\""},
		{"\"num_cards\"",						"\"w\""},

    {"�",												"<br>"},
    {"#_",											"<i>"},
    {"_#",											"</i>"},

    {"�",												"'"},
    {"'",												"'"}};
	
 



	private static JFrame			UIFrame;
	private static JPanel			UIPanel;
	private JFileChooser			fileChooser;
	private JButton						openButton;
	private JButton						convertButton;
	private File							XMLfile	= null;
	private JLabel						chooserLabel;
	private JLabel						statusLabel;

	public static void main(String args[]) {
		// Create the frame and container.
		UIFrame = new JFrame("Convert XML to JSON for mtg-familiar");
		UIPanel = new JPanel();
		UIPanel.setLayout(new GridLayout(2, 2));

		// Add the widgets.
		@SuppressWarnings("unused")
		Main m = new Main();

		// Add the panel to the frame.
		UIFrame.getContentPane().add(UIPanel, BorderLayout.CENTER);

		// Exit when the window is closed.
		UIFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Show the converter.
		UIFrame.pack();
		UIFrame.setVisible(true);
	}

	private static String readFileAsString(String filePath) throws java.io.IOException {
		StringBuilder fileData = new StringBuilder(6000000);
		InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath), "UTF-8");
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

	public Main() {
		addWidgets();
	}

	// Create and add the widgets for converter.
	private void addWidgets() {
		// Create widgets.

		openButton = new JButton("Select the XML File");
		convertButton = new JButton("Barf the JSON");
		fileChooser = new JFileChooser(new File("./"));
		fileChooser.setFileFilter(new XmlFilter());
		chooserLabel = new JLabel("");
		statusLabel = new JLabel("");

		// Listen to events from Convert button.
		openButton.addActionListener(this);
		convertButton.addActionListener(this);

		// Add widgets to container.
		UIPanel.add(openButton);
		UIPanel.add(chooserLabel);
		UIPanel.add(convertButton);
		UIPanel.add(statusLabel);
	}

	// Implementation of ActionListener interface.
	public void actionPerformed(ActionEvent event) {
		// Handle open button action.
		if (event.getSource() == openButton) {
			int returnVal = fileChooser.showOpenDialog(openButton);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				XMLfile = fileChooser.getSelectedFile();
				chooserLabel.setText(XMLfile.getName());
				// This is where a real application would open the file.
			}
		}
		else if (event.getSource() == convertButton) {
			if (XMLfile != null) {
				if (XMLfile.getName().endsWith("xml")) {
					convertXMLtoJSON(XMLfile);
				}
				else {
					statusLabel.setText("Non-XML file selected");
				}
			}
			else {
				statusLabel.setText("No File Selected");
			}
		}
	}

	static CharsetEncoder asciiEncoder = 
		Charset.forName("US-ASCII").newEncoder(); // or "ISO-8859-1" for ISO Latin 1

	public static boolean isPureAscii(String v) {
		for (int i = 0; i < subs.length; i++) {
			v = v.replace(subs[i][0], subs[i][1]);
		}
		boolean retval = asciiEncoder.canEncode(v);

		if(retval == false){
			//System.out.println(v);
		}

		return retval;
	}

	private void convertXMLtoJSON(File f) {
		try {
			JSONObject jo = XML.toJSONObject(readFileAsString(f.getAbsolutePath()));

			JSONObject cdb = jo.getJSONObject("mtg_carddatabase");
			
			if(cdb.remove("bdd_editor") == null) {
				System.out.println("fail");
			}
			if(cdb.remove("bdd_name") == null) {
				System.out.println("fail");
			}
			
			JSONObject sets = cdb.getJSONObject("sets");
			
			JSONArray set = null;
			int setLen;
			try{
				set= sets.getJSONArray("set");
				setLen = set.length();
			}
			catch(Exception e){
				setLen = 1;
			}
			
			for(int i=0; i < setLen; i++){
				JSONObject s;
				if(set != null){
					s = (JSONObject)set.remove(0);
				}
				else{
					s = (JSONObject) sets.remove("set");
				}
				System.out.print(s.getString("name")+"\t"+s.getString("date")+"\t");
				
				String date = s.getString("date");
				s.remove("date");
				String parts[] = date.split("/");
				
				Calendar cal = Calendar.getInstance();
				cal.set(Integer.parseInt(parts[1]), Integer.parseInt(parts[0])-1, 1);
				long epochTime = cal.getTimeInMillis();
				s.put("date", epochTime);
				System.out.println(epochTime);
				if(set != null){
					set.put(s);
				}
				else{
					sets.put("set", s);
				}
			}
			
			JSONObject cards = cdb.getJSONObject("cards");
			JSONArray card = cards.getJSONArray("card");
			//System.out.println("cards: " + card.length());

			int card_cnt=card.length();
			for (int i = 0; i < card.length(); i++) {
				JSONObject c = card.getJSONObject(i);

				if (c.getString("name").contains("//")) {
					card_cnt++;
					
					String names[] = c.getString("name").split(" // ");
					String types[] = c.getString("type").split(" // ");
					String rarities[] = c.getString("rarity").split(" // ");
					String manacosts[] = c.getString("manacost").split(" // ");
					String converted_manacosts[] = c.getString("converted_manacost").split(" // ");
					String abilities[] = c.getString("ability").split(" // ");
					String artists[] = c.getString("artist").split(" // ");
					String colors[] = c.getString("color").split(" // ");
					String mID = c.getString("id");

					JSONObject card1 = new JSONObject();
					JSONObject card2 = new JSONObject();

					card1.put("name", names[0]);
					card2.put("name", names[1]);
					card1.put("set", c.getString("set"));
					card2.put("set", c.getString("set"));
					card1.put("type", types[0]);
					card2.put("type", types[1]);
					card1.put("rarity", rarities[0]);
					card2.put("rarity", rarities[1]);
					card1.put("manacost", manacosts[0]);
					card2.put("manacost", manacosts[1]);
					card1.put("converted_manacost", converted_manacosts[0]);
					card2.put("converted_manacost", converted_manacosts[1]);
					card1.put("power", "");
					card2.put("power", "");
					card1.put("toughness", "");
					card2.put("toughness", "");
					card1.put("loyalty", "");
					card2.put("loyalty", "");
					card1.put("ability", abilities[0]);
					card2.put("ability", abilities[1]);
					card1.put("flavor", "");
					card2.put("flavor", "");
					card1.put("artist", artists[0]);
					card2.put("artist", artists[1]);
					card1.put("number", c.getString("number") + "a");
					card2.put("number", c.getString("number") + "b");
					card1.put("color", colors[0]);
					card2.put("color", colors[1]);
					card1.put("id", mID);
					card2.put("id", mID);

					card.remove(i--);
					card.put(card1);
					card.put(card2);
				}
			}

			String s = jo.toString();

			for (int i = 0; i < subs.length; i++) {
				s = s.replace(subs[i][0], subs[i][1]);
			}

			s = new StringBuffer(s).insert(6, "\"w\":" + card_cnt + ",").toString();
			
			//REGEX ALL THE THINGS
			//...By which I mean validate the JSON to make sure all the identifiers are one character long
			Pattern p = Pattern.compile("(\"[a-zA-Z0-9_]{2,}\":)");
			Matcher m = p.matcher(s);
			if(m.find()) {
				//If we find a match, that's a bad thing
				statusLabel.setText("Validation error; " + m.group());
				
				return;
			}

			String name = f.getName().substring(0, f.getName().length() - 4);
			
			// Write the ISO-whatever
			File jsonout = new File(f.getParent(), name + ".json");
			FileOutputStream fos = new FileOutputStream(jsonout);
			fos.write(s.getBytes());
			fos.flush();
			fos.close();
			
			// Compress it
			File gzipout = new File(f.getParent(), name + ".json.gzip");
			GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(gzipout));
			FileInputStream fis = new FileInputStream(jsonout);
						
			byte[] buffer = new byte[1024];
			int length;
			while ((length = fis.read(buffer)) > 0) {
				gos.write(buffer, 0, length);
			}

			//Close the streams
			gos.flush();
			gos.close();
			fis.close();
			
			statusLabel.setText("All done");
		}
		catch (JSONException e) {
			statusLabel.setText("JSON Exception");
		}
		catch (IOException e) {
			statusLabel.setText("IO Exception");
		}
		catch (Exception e) {
			statusLabel.setText("Some Exception");
		}
	}
}
