package api;

import static api.Connection.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.net.ssl.*;
import org.scribe.services.*;
import org.json.simple.*;
import org.json.simple.parser.*;

/**
 * Class for aggregating Twitter data. 
 * OAuth and HTTPS is built-in.
 * Uses Twitter's Search API using App authentication.
 * @author Etienne
 */
public class TwitterFeed {// If you feel like checking: http://codebeautify.org/jsonvalidate
	
	private URL url;	
	private HttpsURLConnection connection;
	private JSONParser parser;
	
	// "Keep the "Consumer Secret" a secret. This key should never be human-readable in your application." - Twitter 
	// #YOLO - Leaving this in to minimize dependencies to other classes.
	private static final String CONSUMER_KEY = "";
	private static final String CONSUMER_SECRET = "";
	
	private static final String API_URL = "https://api.twitter.com/";
	
	/** The URL for requesting data about the amount of requests left for the current timeframe. */
	private static final String RATE_LIMIT_STATUS = "https://api.twitter.com/1.1/application/rate_limit_status.json";
	
	/** The base URL for requesting data. */
	private static final String SEARCH = "https://api.twitter.com/1.1/search/tweets.json";
	
	/**
	 * Creates a new TwitterFeed instance.
	 */
	public TwitterFeed() {
		parser = new JSONParser();
		try {
			this.url = new URL(API_URL);
		} catch (MalformedURLException ex) {
			Logger.getLogger(TwitterFeed.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	//<editor-fold defaultstate="collapsed" desc="Actual API shit.">
	
	/**
	 * Find a given number of most recent tweets containing the given keyword.
	 * @param keyword a word to search for. For hashtags, see getByHashtag().
	 * @param count the amount of tweets to return. Twitter counts the amount of requests, not the amount of data requested. Use sensibly.
	 * @return a list of tweets found by most recent date.
	 */
	public ArrayList<String> getTweets(String keyword, int count){
		ArrayList<String> tweets = new ArrayList<>();
		String query = "?q=" + keyword  
				     + "&result_type=recent"	
				     + "&count=" + count;		
		JSONObject json = (JSONObject) request(SEARCH + query);
		JSONArray list = (JSONArray) json.get("statuses"); //iz accualy lis, arry pls
		
		for (Object item : list) {
			json = (JSONObject) item;
			tweets.add((String)json.get("text"));
		}
		
		return tweets;
	}

	/**
	 * Returns tweets based on their GPS coordinates. 
	 * @param latitude
	 * @param longitude
	 * @param radius range from the coordinates in kilometres.
	 * @param keyword 
	 * @return
	 */	
	public ArrayList<String> getByLocation(double latitude, double longitude, int radius, String keyword, int count){
		String geolocating = "&geocode=" + latitude + "," + longitude + "," + radius + "km";
		ArrayList<String> tweets = new ArrayList<>();		
		String query = "?q=" + keyword
			+ "&result_type=recent"
			+ "&count=" + count 
			+ geolocating;		
		JSONObject json = (JSONObject) request(SEARCH + query);
		JSONArray list = (JSONArray) json.get("statuses"); //iz accualy lis, arry pls
		
		for (Object item : list) {
			json = (JSONObject) item;
			tweets.add((String)json.get("text"));
		}
		
		return tweets;
	}

	/**
	 * Search tweets by hashtag.
	 * @param tags an hashtag without the '#' sign.
	 */
	public ArrayList<String> getByTag(String tag){
		String query = /*SEARCH + */"%23" + tag /*+ "%20"*/;
		return getTweets(query, 25);
	}
	
	/**
	 * Search tweets by hashtags.
	 * @param tags an array of hashtags without the '#' sign.
	 */
	public ArrayList<String> getByTags(String[] tags){
		String query = ""; //= SEARCH;
		for (String tag : tags) {
			query += "%23" + tag + "%20";
		}
		//return request(query);
		return getTweets(query, 25);
	}
	
	/**
	 * Gets the amount of requests still available for the current timeframe.
	 * @return a formatted String with the rate limit status of the feed
	 * in the format of [Available / Remaining].
	 */
	public String getStatus(){
		String query = RATE_LIMIT_STATUS + "?resources=search";
		JSONObject out = (JSONObject) request(query);
			   out = (JSONObject) out.get("resources");
			   out = (JSONObject) out.get("search");
			   out = (JSONObject) out.get("/search/tweets");
		long limit = (long) out.get("limit");
		long remaining = (long) out.get("remaining");
		return "API usage status: " + remaining + " / " + limit + " remaining.";
	}
		
	/**
	 * Requests data for a given query from the API.
	 * @param query the full URL including the search parameters.
	 * @return the full JSON feed. This can either be a JSONObject or JSONArray.
	 */
	@SuppressWarnings("LocalVariableHidesMemberVariable")
	public Object request(String query){
		try {
			// Authenticate.
			URL oauth = new URL("https://api.twitter.com/oauth2/token");
			HttpsURLConnection conn = (HttpsURLConnection) oauth.openConnection();			
			String token = requestBearerToken("https://api.twitter.com/oauth2/token");
			writeRequest(conn, token);
			
			// Request data.	
			URL url = new URL(query);
			connection = (HttpsURLConnection) url.openConnection();          
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Host", "api.twitter.com");
			connection.setRequestProperty("User-Agent", "CIMS");
		        connection.setRequestProperty("Authorization", "Bearer " + token);
			return parser.parse(readResponse(connection));
		} catch (IOException | ParseException ex) {
			Logger.getLogger(TwitterFeed.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (connection != null)
	            connection.disconnect();
		}
		return null;
	}
	
	//</editor-fold>
	
	//<editor-fold defaultstate="collapsed" desc="Authorization.">
	
	/** 
	 * Encodes the consumer key and secret to create the basic authorization key.
	 * Used for authorizing for the Twitter API.
	 * @param consumerKey the Access Token fom the app.
	 * @param consumerSecret the Secret Access Token from the app.
	 * @return the encoded pair.
	 */
	private String encodeKeys(String consumerKey, String consumerSecret) {
	    try {
	        String encodedConsumerKey = URLEncoder.encode(consumerKey, "UTF-8");
	        String encodedConsumerSecret = URLEncoder.encode(consumerSecret, "UTF-8");
	        String pair = encodedConsumerKey + ":" + encodedConsumerSecret;
	        Base64Encoder b = Base64Encoder.getInstance();
			String encodedBytes = b.encode(pair.getBytes());
	        return encodedBytes; 
	    }
	    catch (UnsupportedEncodingException e) {
	        return new String();
	    }
	}
	
	/** 
	 * Constructs the request for requesting a bearer token and returns that token as a string.
	 * Used for authorizing for the Twitter API.
	 * @param endPointUrl the API URL.
	 * @throws IOException either if JSON is broken or the endpoint URL is incorrect.
	 * @return the Bearer token.
	 */
	@SuppressWarnings("LocalVariableHidesMemberVariable")
	private String requestBearerToken(String endPointUrl) throws IOException {
	    HttpsURLConnection connection = null;
	    String encodedCredentials = encodeKeys(CONSUMER_KEY, CONSUMER_SECRET);
	         
	    try {
	        URL url = new URL(endPointUrl);
	        connection = (HttpsURLConnection) url.openConnection();
	        connection.setDoOutput(true);
	        connection.setDoInput(true);
	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("Host", "api.twitter.com");
	        connection.setRequestProperty("User-Agent", "CIMS");
	        connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
	        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
	        connection.setRequestProperty("Content-Length", "29");
	        connection.setUseCaches(false);
	             
	        writeRequest(connection, "grant_type=client_credentials");
	             
	        JSONObject json = (JSONObject) parser.parse(readResponse(connection));
		
		if (json != null) {
		    String tokenType = (String) json.get("token_type");
		    String token = (String) json.get("access_token");
		    
		    return ((tokenType.equals("bearer")) && (token != null)) ? token : "";
		}
	        return "";
	    } catch (MalformedURLException e) {
	        throw new IOException("Invalid endpoint URL specified.", e);
	    }	catch (ParseException e) {
		throw new IOException("Invalid JSON response.", e);
	    } finally {
	        if (connection != null)
	            connection.disconnect();
	    }
	}	

	//</editor-fold>
	
	/**
	 * Prints some output for testing/demonstration and reference.
	 * 400 ~ 403 = Unauthorized.
	 */
	public static void main(String[] args) throws Exception {
		TwitterFeed t = new TwitterFeed();
		
		// TODO: make this a unit test.		
		JSONArray out = (JSONArray) t.request("https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=twitterapi&count=2");
		for (int i = 0; i < out.size(); i++) {
			if (!(out.get(i) instanceof JSONArray)) {
				JSONObject obj = (JSONObject) out.get(i);
				System.out.println(obj.get("text"));
			}
			System.out.println("");
		}
		
		
		// TwitterFeed.getStatus():
		System.out.println(t.getStatus());

		// TwitterFeed.getByTag():
		System.out.println(t.getByTag("CIMS"));
		
		// TwitterFeed.getByTags():
		System.out.println(t.getByTags(new String[]{"fontysict", "fontys"}));
		
		// TwitterFeed.getTweets():
		ArrayList<String> tweets = t.getTweets("incident", 50);
		for (String tweet : tweets) {System.out.println(tweet);}
		
		// DIY: http://www.gps-coordinates.net/
		// TwitterFeed.getByLocation():
		//ArrayList<String> geotweets = t.getByLocation(51.450922, 5.479748, 15); // Rachelsmolen, Eindhoven.
		//for (String tweet : geotweets) {System.out.println(tweet);}
		//System.out.println(t.getByLocation(51.450922, 5.479748, 15)); // Rachelsmolen, Eindhoven.
		//System.out.println(out);
    }
}
