package no.artorp.ip2drive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

public class App {
	
	public static final String APPLICATION_NAME = "ip2drive";
	public static final java.io.File APPLICATION_DIR = new java.io.File(System.getProperty("user.home"), ".ip2drive");
	public static final String SHEET_ID_KEY = "spreadsheet.id";
	private static final String DEFAULT_SHEET_ID = "replace_this_with_spreadsheet_id";
	
	
	public static void main(String[] args) throws IOException {
		
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
		
		// Set up executor
		
		final String spreadsheetId = sheetProp.getProperty(SHEET_ID_KEY);
		
		Runnable updateSpreadsheet = () -> {
			// Build a new authorized API client service.
			Sheets service = null;
			try (InputStream is = new FileInputStream(clientSecret)) {
				service = SheetService.getSheetsService(new FileInputStream(clientSecret));
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("\nAborting current iteration of task\n");
				return;
			}
			
			try {
				// get previous values row 2..15, skipping last row (row 16)
				ValueRange response = service.spreadsheets().values()
						.get(spreadsheetId, "A2:B15")
						.setValueRenderOption("FORMATTED_VALUE")
						.execute();
				List<List<Object>> vals = response.getValues();
				if (vals == null || vals.size() == 0) {
					System.out.println("No data found, aborting current iteration of task.");
					return;
				} else {
					System.out.println("Successfully got previous values, last row was:");
					List<Object> upperRow = vals.get(0);
					System.out.printf("\t%s | %s\n", upperRow.get(0), upperRow.get(1));
				}
				
				// Get current IP and date
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				String date = sdf.format(new Date());
				String extIp = IpRetriever.externalIp();
				System.out.println("New time and IP:");
				System.out.printf("\t%s | %s\n", date, extIp);
				
				List<List<Object>> updatedData = new ArrayList<List<Object>>();
				updatedData.add(Arrays.asList(date, extIp));
				
				// add all old values into array
				updatedData.addAll(vals);
				
				// write to spreadsheet
				ValueRange body = new ValueRange().setValues(updatedData);
				UpdateValuesResponse result = service
						.spreadsheets().values()
						.update(spreadsheetId, "A2:B16", body)
						.setValueInputOption("USER_ENTERED")
						.execute();
				System.out.printf("%d cells updated.\n", result.getUpdatedCells());
			} catch (IOException e) {
				e.printStackTrace();
			}
		};
		
		updateSpreadsheet.run();
		
		
		/*
		ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		
		Runnable task = () -> {
			System.out.println("Executing Task At " + System.nanoTime() + " inside " + Thread.currentThread().getName());
		};
		
		System.out.println("Starting service, while inside thread " + Thread.currentThread().getName());
		scheduledExecutorService.scheduleAtFixedRate(task, 1, 1, TimeUnit.SECONDS);
		*/
		/*
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shutdownhook running");
			scheduledExecutorService.shutdown();
			try {
				scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS);
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}));
		*/
		/*
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//scheduledExecutorService.shutdown();
		*/
		
	}
}
