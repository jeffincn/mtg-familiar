package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.sqlite.SQLiteException;

import com.gelakinetic.mtgfam.R;
import com.gelakinetic.mtgfam.activities.FamiliarActivity.OTATask;

public class RulesParser {

	// Instance variables
	private Date										lastUpdated;
	private String									rulesUrl;
	private CardDbAdapter						mDbHelper;
	private Context context;
	private ProgressReporter									progReport;
	private ArrayList<RuleItem>			rules;
	private ArrayList<GlossaryItem>	glossary;

	// URLs and the regex
	private final String						source								= "http://www.wizards.com/Magic/TCG/Article.aspx?x=magic/rules";
	private final String						prefix								= "http://www.wizards.com";
	private final String						regex									= "(/magic/comprules/MagicCompRules_[0-9]{8}\\.txt)";

	// Delimiting tokens
	// NOTE: If WotC changes their rules file format drastically, these may need
	// to be changed as well
	private final String						preRules							= "Customer Service Information";
	private final String						postRulesPreGlossary	= "Glossary";
	private final String						postGlossary					= "Credits";

	// Result codes
	/**
	 * Returned from fetchAndLoad() if everything works correctly.
	 **/
	public static int								SUCCESS								= 0;

	/**
	 * Returned from fetchAndLoad() if some of the rules/terms failed, but some
	 * succeeded.
	 **/
	public static int								ERRORS								= 1;

	/**
	 * Returned from fetchAndLoad() if a catastrophic failure occurs.
	 **/
	public static int								FAILURE								= 2;

    public interface ProgressReporter {
        void reportRulesProgress(String... args);
    }

	public RulesParser(Date lastUpdated, CardDbAdapter mDbHelper, Context context, ProgressReporter progReport) {
		this.lastUpdated = lastUpdated;
		this.rulesUrl = null;
		this.mDbHelper = mDbHelper;
		this.context = context;
		this.progReport = progReport;

		this.rules = new ArrayList<RuleItem>();
		this.glossary = new ArrayList<GlossaryItem>();
	}

	/**
	 * Attempts to get the URL for the latest version of the rules and determine
	 * if an update is necessary. If it finds the file and its date is newer than
	 * this.lastUpdated, true will be returned. Otherwise, it will return false.
	 * If true is returned, this.rulesUrl will be populated.
	 * 
	 * @return Whether or this the rules need updating.
	 */
	public boolean needsToUpdate() {
		URL url;
		InputStream is = null;
		BufferedReader reader = null;

		try {
			url = new URL(source);
			is = url.openStream();
			reader = new BufferedReader(new InputStreamReader(is));
			Pattern p = Pattern.compile(regex);
			Matcher m;

			String line;
			while ((line = reader.readLine()) != null) {
				m = p.matcher(line);
				if (m.find()) {
					this.rulesUrl = prefix + line.substring(m.start(), m.end());
					break;
				}
			}
		}
		catch (MalformedURLException mue) {
			this.rulesUrl = null;
		}
		catch (IOException ioe) {
			this.rulesUrl = null;
		}
		finally {
			try {
				is.close();
				reader.close();
			}
			catch (IOException ioe) {
				// Eat it
			}
			catch (NullPointerException npe) {
				// Eat it
			}
		}

		if (this.rulesUrl == null) {
			return false;
		}
		else {
			int txtIndex = this.rulesUrl.indexOf(".txt");
			if (txtIndex < 0) {
				// Shouldn't happen; something obviously went wrong here
				return false;
			}
			String date = this.rulesUrl.substring(txtIndex - 8, txtIndex);
			Calendar c = Calendar.getInstance();
			c.set(Integer.parseInt(date.substring(0, 4)), // yyyy
					Integer.parseInt(date.substring(4, 6)) - 1, // mm (months are
																											// 0-indexed for some
																											// inane reason)
					Integer.parseInt(date.substring(6, 8)), // dd
					0, // hh
					0, // mm
					0); // ss
			Date rulesDate = c.getTime();
			return rulesDate.after(this.lastUpdated);
		}
	}

	/**
	 * Attempts to fetch the latest version of the rules and load it into the
	 * database. If the process is successful, true will be returned. Otherwise,
	 * false will be returned. This method should only be called if
	 * needsToUpdate() returns true.
	 * 
	 * @return Whether or not the parsing is successful
	 */
	public boolean parseRules() {
		if (this.rulesUrl == null) {
			return false;
		}

		URL url;
		InputStream is = null;
		BufferedReader reader = null;

		try {
			RuleItem currentRule = null;
			GlossaryItem currentTerm = null;
			int position = -1;
			this.rules.clear();
			this.glossary.clear();

			// Init the reader
			url = new URL(this.rulesUrl);
			is = url.openStream();
			reader = new BufferedReader(new InputStreamReader(is));

			// Then populate
			String line = reader.readLine().trim();
			while (!line.equals(preRules)) {
				// Burn through lines until we hit the pre-rules token
				line = reader.readLine().trim();
			}

			line = reader.readLine(); // Step past the token

			while (!line.equals(postRulesPreGlossary)) {
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
							currentRule.addExample(line.replace("{PW}", "{PWK}").replace("{P/W}", "{PW}").replace("{W/P}", "{WP}"));
						}
					}
				}

				// Then move to the next line
				line = reader.readLine().trim();
			}

			line = reader.readLine().trim(); // Step past the token

			while (!line.equals(postGlossary)) {
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
			try {
				is.close();
				reader.close();
			}
			catch (IOException ioe) {
				// Eat it
			}
			catch (NullPointerException npe) {
				// Eat it
			}
		}
	}

	/**
	 * Loads in the rules and glossary to the database, updating the count as it
	 * goes. The expected usage is that the main activity will have a progress
	 * dialog that gets updated as the count does.
	 * 
	 * @return SUCCESS if nothing goes wrong, ERRORS if some errors occur but some
	 *         data is loaded, and FAILURE if everything fails and no data is
	 *         loaded.
	 */
	public int loadRulesAndGlossary() {
		try {
			mDbHelper.dropRulesTables();
			mDbHelper.createRulesTables();

			int statusCode = SUCCESS;
			int numTotalElements = rules.size() + glossary.size();
			int elementsParsed = 0;
			String dialogText = context.getString(R.string.update_parse_rules);
			progReport.reportRulesProgress("determinate", "");
			progReport.reportRulesProgress(new String[] { dialogText, dialogText,
					"" + (int) Math.round(100 * elementsParsed / (double) numTotalElements) });
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
							"" + (int) Math.round(100 * elementsParsed / (double) numTotalElements) });
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
							"" + (int) Math.round(100 * elementsParsed / (double) numTotalElements) });
				}
			}

//			progReport.reportRulesProgress(new String[] { "Done Parsing Comprehensive Rules", "Done Parsing Comprehensive Rules",
//					"0" });
			return statusCode;
		}
		catch (SQLiteException sqe) {
//			progReport.reportRulesProgress(new String[] { "Done Parsing Comprehensive Rules", "Done Parsing Comprehensive Rules",
//					"0" });
			return FAILURE;
		}
	}

	private class RuleItem {
		public int		category;
		public int		subcategory;
		public String	entry;
		public String	text;
		public int		position;

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
		public String	term;
		public String	definition;

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
