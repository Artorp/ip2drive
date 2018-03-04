package no.artorp.ip2drive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class IpRetriever {
	
	private static final String[] IP_HOSTS = {
			"http://checkip.amazonaws.com/",
			"http://icanhazip.com/",
			"http://www.trackip.net/ip",
			"http://myexternalip.com/raw",
			"http://ipecho.net/plain",
			"http://bot.whatismyipaddress.com/"
			};
	
	/**
	 * Queries remote hosts until one responds with an IP
	 * 
	 * @return the external IP of the running process
	 */
	public static String externalIp() {
		// Loop through each remote host until one successfully returns
		String ip = null;
		
		for (int i = 0; i < IP_HOSTS.length; i++) {
			try {
				URL remoteHost = new URL(IP_HOSTS[i]);
				try (BufferedReader br = new BufferedReader(new InputStreamReader(remoteHost.openStream()))) {
					ip = br.readLine();
					if (ip != null) {
						// successfully got a response, break
						break;
					}
				}
			} catch (IOException e) {
				System.err.println(IP_HOSTS[i] + " failed with error message " + e.getMessage());
			}
		}
		
		if (ip == null) {
			return "NULL";
		}
		
		return ip;
	}
}
