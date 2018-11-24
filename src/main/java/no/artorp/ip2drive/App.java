package no.artorp.ip2drive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App {
	
	public static final String APPLICATION_NAME = "ip2drive";
	public static final java.io.File APPLICATION_DIR = new java.io.File(System.getProperty("user.home"), ".ip2drive");
	public static final String SHEET_ID_KEY = "spreadsheet.id";
	private static final String DEFAULT_SHEET_ID = "replace_this_with_spreadsheet_id";
	
	
	public static void main(String[] args) throws IOException {
		
		// Parse arguments
		CommandLineParser parser = new DefaultParser();
		
		Options options = new Options();
		
		options.addOption("h", "help", false, "print this message");
		options.addOption("v", "version", false, "print the version information and exit");
		options.addOption("s", "single", false, "fetch ip, update drive, then exit");
		options.addOption("c", "continual", false, "fetch ip and update drive every 5 minutes, while writing to sysout");
		
		if (args.length == 0) {
			printUsageThenExit(options);
		}
		
		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				printUsageThenExit(options);
			} else if (cmd.hasOption("v")) {
				System.out.println("ip2drive " + Version.getVersion());
				System.exit(0);
			} else if (cmd.hasOption("s")) {
				Callable<Void> updateSpreadsheet = getSpreadsheetUpdater();
				runSingle(updateSpreadsheet);
			} else if (cmd.hasOption("c")) {
				Callable<Void> updateSpreadsheet = getSpreadsheetUpdater();
				runPeriodically(updateSpreadsheet);
			}
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			printUsageThenExit(options);
		}
		
	}
	
	private static void printUsageThenExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		String footer = "Example:\n    ip2drive -h";
		formatter.printHelp("ip2drive [OPTIONS]", null, options, footer);
		System.exit(1);
		return;
	}
	
	private static Callable<Void> getSpreadsheetUpdater() throws IOException {
		
		if (!APPLICATION_DIR.exists() || APPLICATION_DIR.isFile()) {
			APPLICATION_DIR.mkdirs();
		}
		
		
		// search for client_secret.json
		boolean trouble = false;
		
		File clientSecret = APPLICATION_DIR.toPath().resolve("client_secret.json").toFile();
		
		if (!clientSecret.exists() || clientSecret.isDirectory()) {
			System.out.println("Place your client_secret.json into " + APPLICATION_DIR.getAbsolutePath());
			System.out.println("The name must match:");
			System.out.println(clientSecret.getAbsolutePath());
			System.out.println("\nFor information on how to create a Google Sheets client secret, see:");
			System.out.println("https://developers.google.com/sheets/api/quickstart/java#step_1_turn_on_the_api_name");
			trouble = true;
		}
		
		// search for properties file with spreadsheet id
		File spreadProp = APPLICATION_DIR.toPath().resolve("spreadsheet_id.txt").toFile();
		Properties sheetProp = new Properties();
		
		if (!spreadProp.exists() || spreadProp.isDirectory()) {
			System.out.println("\nCould not retrieve spreadsheet ID, put it in file\n\t" + spreadProp.getAbsolutePath());
			Properties p = new Properties();
			p.setProperty(SHEET_ID_KEY, DEFAULT_SHEET_ID);
			try (OutputStream out = new FileOutputStream(spreadProp)){
				p.store(out, " This file contains the Google Spreadsheet ID that will be edited by ip2drive\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
			trouble = true;
		} else {
			try (InputStream in = new FileInputStream(spreadProp)){
				sheetProp.load(in);
				if (sheetProp.getProperty(SHEET_ID_KEY).equals(DEFAULT_SHEET_ID)) {
					System.out.println("\nSpreadsheet ID not set, change it in file\n\t" + spreadProp.getAbsolutePath());
					trouble = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
				trouble = true;
			}
		}
		
		if (trouble) {
			System.exit(1);
		}
		
		// Set up callable spreadsheet updater
		final String spreadsheetId = sheetProp.getProperty(SHEET_ID_KEY);
		return new SpreadsheetUpdater(clientSecret, spreadsheetId);
	}
	
	private static void runSingle(Callable<Void> updateSpreadsheet) {
		try {
			updateSpreadsheet.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void runPeriodically(Callable<Void> updateSpreadsheet) {
		Runnable periodicUpdater = new UpdatePeriodically(5, updateSpreadsheet);
		new Thread(periodicUpdater).start();
	}
}
