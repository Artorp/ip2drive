package no.artorp.ip2drive;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
// import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.Sheets;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;



public class SheetService {
	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY =
			JacksonFactory.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;
	
	private static GoogleClientSecrets clientSecret = null;
	
	private static boolean savedCredentials = false;

	/** Global instance of the scopes required by this quickstart.
	*
	* If modifying these scopes, delete your previously saved credentials
	* at ~/.ip2drive
	*/
	private static final List<String> SCOPES =
			Arrays.asList(SheetsScopes.SPREADSHEETS);

	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(App.APPLICATION_DIR);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	public static GoogleClientSecrets clientSecretsWithCache(InputStream in) throws IOException {
		if (clientSecret == null) {
			clientSecret = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
		}
		return clientSecret;
	}
	
	/**
	* Creates an authorized Credential object.
	* @return an authorized Credential object.
	* @throws IOException
	*/
	public static Credential authorize(InputStream in) throws IOException {
	   // Load client secrets.
	   GoogleClientSecrets clientSecrets = clientSecretsWithCache(in);

	   // Build flow and trigger user authorization request.
	   GoogleAuthorizationCodeFlow flow =
			new GoogleAuthorizationCodeFlow.Builder(
					HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
			.setDataStoreFactory(DATA_STORE_FACTORY)
			.setAccessType("offline")
			.build();
	   Credential credential = new AuthorizationCodeInstalledApp(
			flow, new LocalServerReceiver()).authorize("user");
	   if (!savedCredentials) {
		   System.out.println("Credentials saved to " + App.APPLICATION_DIR.getAbsolutePath());
		   savedCredentials = true;
	   }
	   return credential;
	}

	/**
     * Build and return an authorized Sheets API client service.
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService(InputStream client_secret) throws IOException {
        Credential credential = authorize(client_secret);
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(App.APPLICATION_NAME)
                .build();
    }

}
