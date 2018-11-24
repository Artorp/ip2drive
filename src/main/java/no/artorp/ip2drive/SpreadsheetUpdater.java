package no.artorp.ip2drive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

public class SpreadsheetUpdater implements Callable<Void> {
	
	final private File clientSecret;
	final private String spreadsheetId;
	
	public SpreadsheetUpdater(File clientSecret, String spreadsheetId) {
		this.clientSecret = clientSecret;
		this.spreadsheetId = spreadsheetId;
	}
	

	@Override
	public Void call() throws Exception {
		// Build a new authorized API client service.
		Sheets service = null;
		try (InputStream is = new FileInputStream(clientSecret)) {
			service = SheetService.getSheetsService(new FileInputStream(clientSecret));
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("\nAborting current iteration of task\n");
			return null;
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
				return null;
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
		return null;
	}

}
