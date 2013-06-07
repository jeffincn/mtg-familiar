package com.gelakinetic.mtgfam.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import android.os.Environment;

import com.gelakinetic.mtgfam.fragments.JudgesCornerFragment;

public class MTRIPGParser {

	private Date lastUpdated;
//	private Context context;
//	private ProgressReporter progReport;

	private static final String MTR_SOURCE = "https://sites.google.com/site/mtgfamiliar/rules/MagicTournamentRules.html";
	private static final String IPG_SOURCE = "https://sites.google.com/site/mtgfamiliar/rules/InfractionProcedureGuide.html";

	public interface ProgressReporter {
		void reportMtrIpgProgress(String... args);
	}
	
//	public MTRIPGParser(Date lastUpdated, Context context, ProgressReporter progReport) {
//		this.lastUpdated = lastUpdated;
//		this.context = context;
//		this.progReport = progReport;
//	}
	
	public MTRIPGParser(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	public boolean performMtrUpdateIfNeeded() {
		boolean updated = false;
		InputStream is = null;
		BufferedReader reader = null;
		FileOutputStream fos = null;
		
		try {
			URL url = new URL(MTR_SOURCE);
			is = url.openStream();
			reader = new BufferedReader(new InputStreamReader(is));
			
			String line = reader.readLine();
			String[] parts = line.split("-");
			Calendar c = Calendar.getInstance();
			c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			
			if (c.getTime().after(this.lastUpdated)) {
				StringBuilder sb = new StringBuilder();
				line = reader.readLine();
				while (line != null) {
					sb.append(line.trim());
					line = reader.readLine();
				}
				
				// Write to MTR_LOCAL_FILE
				File storage = Environment.getExternalStorageDirectory();
				File output = new File(storage, JudgesCornerFragment.MTR_LOCAL_FILE);
				fos = new FileOutputStream(output);
				fos.write(sb.toString().getBytes());
				fos.flush(); // Unnecessary?
				
				updated = true;
			}
		}
		catch (Exception e) {
		}
		finally {
			try {
				if (is != null) {
					is.close();
				}
				if (reader != null) {
					reader.close();	
				}
				if (fos != null) {
					fos.close();
				}
			}
			catch (Exception e) {
			}
		}
		
		return updated;
	}
	
	public boolean performIpgUpdateIfNeeded() {
		boolean updated = false;
		InputStream is = null;
		BufferedReader reader = null;
		FileOutputStream fos = null;
		
		try {
			URL url = new URL(IPG_SOURCE);
			is = url.openStream();
			reader = new BufferedReader(new InputStreamReader(is));
			
			String line = reader.readLine();
			String[] parts = line.split("-");
			Calendar c = Calendar.getInstance();
			c.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			
			if (c.getTime().after(this.lastUpdated)) {
				StringBuilder sb = new StringBuilder();
				line = reader.readLine();
				while (line != null) {
					sb.append(line.trim());
					line = reader.readLine();
				}
				
				// Write to IPG_LOCAL_FILE
				File storage = Environment.getExternalStorageDirectory();
				File output = new File(storage, JudgesCornerFragment.IPG_LOCAL_FILE);
				fos = new FileOutputStream(output);
				fos.write(sb.toString().getBytes());
				fos.flush(); // Unnecessary?
				
				updated = true;
			}
		}
		catch (Exception e) {
		}
		finally {
			try {
				if (is != null) {
					is.close();
				}
				if (reader != null) {
					reader.close();	
				}
				if (fos != null) {
					fos.close();
				}
			}
			catch (Exception e) {
			}
		}
		
		return updated;
	}
}
