package api;

import java.io.*;
import java.net.*;
import java.util.logging.*;
import javax.net.ssl.*;

/**
 * Modularization of TwitterFeed. TODO: Change this header and package name.
 * @author Etienne
 */
public class Connection {
	
	/**
	 * Connects to the stream.
	 * @param endpoint the URL of the API.
	 * @return the connection if succeeded, null otherwise.
	 */
	private HttpsURLConnection connect(URL endpoint){
		try {
			return (HttpsURLConnection)endpoint.openConnection();
		} catch (IOException ex) {
			Logger.getLogger(TwitterFeed.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}
	
	/** 
	 * Writes a request to a connection
	 * @param connection the connection to write the request over.
	 * @param request the httprequest for a response.
	 * @return true if succeeded, false otherwise.
	 */
	public static boolean writeRequest(HttpsURLConnection connection, String request) {
		connection.setDoOutput(true);
	    try {
		    try (BufferedWriter wr = new BufferedWriter(
			new OutputStreamWriter(
			    connection.getOutputStream()))) {
			    wr.write(request);
		    }
	             
	        return true;
	    }
	    catch (IOException e) {
		Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, e);
		return false; 
	    }
	}
	     	  
	/** 
	 * Reads a response for a given connection and returns it as a string.
	 * @param connection the connection to read from.
	 * @return 
	 */
	@SuppressWarnings("UnusedAssignment")
	public static String readResponse(HttpsURLConnection connection) {
	    try {
	        StringBuilder str = new StringBuilder();	             
	        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        String line = "";
	        while((line = br.readLine()) != null) {
	            str.append(line).append(System.getProperty("line.separator"));
	        }
	        return str.toString();
	    }
	    catch (IOException e) { return new String(); }
	}
}
