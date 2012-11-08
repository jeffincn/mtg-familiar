package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.database.sqlite.SQLiteException;

import com.gelakinetic.mtgfam.R;

public class RulesParser {

	// Instance variables
	private Date lastUpdated;
	private CardDbAdapter mDbHelper;
	private Context context;
	private InputStream is;
	private BufferedReader reader;
	private ProgressReporter progReport;
	private ArrayList<RuleItem> rules;
	private ArrayList<GlossaryItem> glossary;

	// URL and delimiting tokens
	private static final String SOURCE = "https://sites.google.com/site/mtgfamiliar/rules/MagicCompRules.txt";
	private static final String RULES_TOKEN = "RULES_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES";
	private static final String GLOSSARY_TOKEN = "GLOSSARY_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES";
	private static final String EOF_TOKEN = "EOF_VERYLONGSTRINGOFLETTERSUNLIKELYTOBEFOUNDINTHEACTUALRULES";

	// Result codes
	/**
	 * Returned from fetchAndLoad() if everything works correctly.
	 **/
	public static int SUCCESS = 0;

	/**
	 * Returned from fetchAndLoad() if some of the rules/terms failed, but some succeeded.
	 **/
	public static int ERRORS = 1;

	/**
	 * Returned from fetchAndLoad() if a catastrophic failure occurs.
	 **/
	public static int FAILURE = 2;

	public interface ProgressReporter {
		void reportRulesProgress(String... args);
	}

	public RulesParser(Date lastUpdated, CardDbAdapter mDbHelper, Context context, ProgressReporter progReport) {
		this.lastUpdated = lastUpdated;
		this.mDbHelper = mDbHelper;
		this.context = context;
		this.is = null;
		this.reader = null;
		this.progReport = progReport;

		this.rules = new ArrayList<RuleItem>();
		this.glossary = new ArrayList<GlossaryItem>();
	}

	/**
	 * Attempts to get the URL for the latest version of the rules and determine if an update is necessary. If it finds
	 * the file and its date is newer than this.lastUpdated, true will be returned. Otherwise, it will return false. If
	 * true is returned, this.rulesUrl will be populated.
	 * 
	 * @return Whether or this the rules need updating.
	 */
	public boolean needsToUpdate() {
		URL url;

		try {
			url = new URL(SOURCE);
			this.is = url.openStream();
			this.reader = new BufferedReader(new InputStreamReader(is));

			//First line will be the date formatted as YYYY-MM-DD
			String line = this.reader.readLine();
			String[] parts = line.split("-");
			Calendar c = Calendar.getInstance();
			c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			
			if (c.getTime().after(this.lastUpdated)) {
				return true;	
			} 
			else {
				closeReader();
				return false;
			}
		}
		catch (Exception e) {
			closeReader();
			return false;
		}
	}

	/**
	 * Attempts to fetch the latest version of the rules and load it into the database. If the process is successful,
	 * true will be returned. Otherwise, false will be returned. This method should only be called if needsToUpdate()
	 * returns true.
	 * 
	 * @return Whether or not the parsing is successful
	 */
	public boolean parseRules() {
		if (this.reader == null) {
			//This should only be the case if we called parseRules() before needsToUpdate()
			//or if needsToUpdate() returned false
			return false;
		}

		try {
			RuleItem currentRule = null;
			GlossaryItem currentTerm = null;
			int position = -1;
			this.rules.clear();
			this.glossary.clear();

			String line = this.reader.readLine().trim();
			while (!line.equals(RULES_TOKEN)) {
				// Burn through lines until we hit the rules token
				line = reader.readLine().trim();
			}

			line = reader.readLine(); // Step past the token

			while (!line.equals(GLOSSARY_TOKEN)) {
				// Parse the line
				if (line.length() == 0) {
					if (currentRule != null) {
						// Rule is over and we have one; add it to the list and null it
						this.rules.add(currentRule);
						currentRule = null;
					}
				}
				else {
					if (Character.isDigit(line.charAt(0))) {
						// If the line starts with a number, it's the start of a rule
						int category, subcategory;
						String entry, text;

						String numberToken = line.split(" ")[0];
						String[] subTokens = numberToken.split("\\.");

						int rawCategory = Integer.parseInt(subTokens[0]);
						if (rawCategory >= 100) {
							category = rawCategory / 100;
							subcategory = rawCategory % 100;
						}
						else {
							category = rawCategory;
							subcategory = -1;
						}

						if (subTokens.length > 1) {
							entry = subTokens[1];
						}
						else {
							entry = null;
						}

						text = line.substring(numberToken.length()).trim();
						text = text.replace("{PW}", "{PWK}").replace("{P/W}", "{PW}").replace("{W/P}", "{WP}");

						if (entry == null) {
							position = -1;
						}
						else {
							position++;
						}

						currentRule = new RuleItem(category, subcategory, entry, text, position);
					}
					else {
						if (currentRule != null) {
							currentRule.addExample(line.replace("{PW}", "{PWK}").replace("{P/W}", "{PW}")
									.replace("{W/P}", "{WP}"));
						}
					}
				}

				// Then move to the next line
				line = reader.readLine().trim();
			}

			line = reader.readLine().trim(); // Step past the token

			while (!line.equals(EOF_TOKEN)) {
				// Parse the line
				if (line.length() == 0) {
					if (currentTerm != null) {
						// Term is over and we have one; add it to the list and null it
						glossary.add(currentTerm);
						currentTerm = null;
					}
				}
				else {
					if (currentTerm == null) {
						currentTerm = new GlossaryItem(line);
					}
					else {
						currentTerm.addDefinitionLine(line.replace("{PW}", "{PWK}").replace("{P/W}", "{PW}")
								.replace("{W/P}", "{WP}"));
					}
				}

				// Then move to the next line
				line = reader.readLine().trim();
			}

			return true;
		}
		catch (Exception e) {
			// POKEMON THAT SHIT
			return false;
		}
		finally {
			closeReader();
		}
	}

	/**
	 * Loads in the rules and glossary to the database, updating the count as it goes. The expected usage is that the
	 * main activity will have a progress dialog that gets updated as the count does.
	 * 
	 * @return SUCCESS if nothing goes wrong, ERRORS if some errors occur but some data is loaded, and FAILURE if
	 *         everything fails and no data is loaded.
	 * @throws FamiliarDbException
	 */
	public int loadRulesAndGlossary() throws FamiliarDbException {
		try {
			mDbHelper.dropRulesTables();
			mDbHelper.createRulesTables();

			int statusCode = SUCCESS;
			int numTotalElements = rules.size() + glossary.size();
			int elementsParsed = 0;
			String dialogText = context.getString(R.string.update_parse_rules);
			progReport.reportRulesProgress("determinate", "");
			progReport.reportRulesProgress(new String[] { dialogText, dialogText,
					"" + (int)Math.round(100 * elementsParsed / (double)numTotalElements) });
			// main.setNumCards(rules.size() + glossary.size());

			for (RuleItem rule : rules) {
				try {
					mDbHelper.insertRule(rule.category, rule.subcategory, rule.entry, rule.text, rule.position);
				}
				catch (SQLiteException sqe) {
					statusCode = ERRORS;
				}
				finally {
					elementsParsed++;
					progReport.reportRulesProgress(new String[] { dialogText, dialogText,
							"" + (int)Math.round(100 * elementsParsed / (double)numTotalElements) });
				}
			}

			for (GlossaryItem term : glossary) {
				try {
					mDbHelper.insertGlossaryTerm(term.term, term.definition);

				}
				catch (SQLiteException sqe) {
					statusCode = ERRORS;
				}
				finally {
					elementsParsed++;
					progReport.reportRulesProgress(new String[] { dialogText, dialogText,
							"" + (int)Math.round(100 * elementsParsed / (double)numTotalElements) });
				}
			}

			// progReport.reportRulesProgress(new String[] { "Done Parsing Comprehensive Rules",
			// "Done Parsing Comprehensive Rules",
			// "0" });
			return statusCode;
		}
		catch (SQLiteException sqe) {
			// progReport.reportRulesProgress(new String[] { "Done Parsing Comprehensive Rules",
			// "Done Parsing Comprehensive Rules",
			// "0" });
			return FAILURE;
		}
	}
	
	private void closeReader() {
		try {
			this.is.close();
			this.reader.close();
		}
		catch (Exception e) {
		}
		
		this.is = null;
		this.reader = null;
	}

	private class RuleItem {
		public int category;
		public int subcategory;
		public String entry;
		public String text;
		public int position;

		public RuleItem(int category, int subcategory, String entry, String text, int position) {
			this.category = category;
			this.subcategory = subcategory;
			this.entry = entry;
			this.text = text;
			this.position = position;
		}

		public void addExample(String example) {
			this.text += "<br><br>" + example.trim();
		}
	}

	private class GlossaryItem {
		public String term;
		public String definition;

		public GlossaryItem(String term) {
			this.term = term;
			this.definition = "";
		}

		public void addDefinitionLine(String line) {
			if (this.definition.length() > 0) {
				this.definition += "<br>";
			}
			this.definition += line.trim();
		}
	}
}
