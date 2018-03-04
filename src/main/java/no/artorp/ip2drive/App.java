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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
		
		// Time to edit the spreadsheet
		
		// Build a new authorized API client service.
		Sheets service = null;
		try (InputStream is = new FileInputStream(clientSecret)) {
			service = SheetService.getSheetsService(new FileInputStream(clientSecret));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		String spreadsheetId = sheetProp.getProperty(SHEET_ID_KEY);
		String range = "A1:A2";
		ValueRange response = service.spreadsheets().values()
				.get(spreadsheetId, range)
				.execute();
		List<List<Object>> values = response.getValues();
		if (values == null || values.size() == 0) {
			System.out.println("No data found!");
		} else {
			System.out.println("Found values!");
			for (List row : values) {
				// Print all values in column A, which corresponds to index 0
				System.out.println(row.get(0));
				Object cell = row.get(0);
				System.out.println(cell.getClass());
			}
		}
		
		
		// let's write some values
		
		List<List<Object>> myValues = Arrays.asList(
				Arrays.asList(
						// Cell values in first row
						"first", "second", "third", "fourth"
						),
				Arrays.asList(
						// Cell values in second row
						"first2", "second2", "third2", "fourth2"
						)
				);
		ValueRange body = new ValueRange().setValues(myValues);
		UpdateValuesResponse result =
				service.spreadsheets().values().update(spreadsheetId, "B1:E2", body)
				.setValueInputOption("RAW")
				.execute();
		System.out.printf("%d cells updated.\n", result.getUpdatedCells());
	}
}
