package bula;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**

	 * Fetches the news from news.google.com and returns the titles
	 * TODO add: 
	 * http://rss.cnn.com/rss/edition.rss
	 * http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml
	 * http://feeds.bbci.co.uk/news/rss.xml
	 * http://www.npr.org/rss/rss.php?id=1001
	
 */
public class DataExtractor {

	public List<String> extractData(List<String> rssUrls) {
		List<String> newsTitles = new ArrayList<String>();
		for (String rss: rssUrls) {
			try {
				URL url = new URL(rss);
				SyndFeedInput fi = new SyndFeedInput();
				SyndFeed feed = fi.build(new XmlReader(url));

				// HttpURLConnection httpcon =
				// (HttpURLConnection)url.openConnection();
				// SyndFeedInput input = new SyndFeedInput();
				// SyndFeed feed = input.build(new XmlReader(httpcon));

				for (@SuppressWarnings("unchecked")
				Iterator<SyndEntry> i = feed.getEntries().iterator(); i.hasNext();) {
					SyndEntry entry = (SyndEntry) i.next();
					String title = entry.getTitle();
					// title = title.substring(0, title.lastIndexOf("-"));
					title = entry.getTitle();

					String catPhrase = "  ";
					// Get the body of the news.
					// TODO sep method
					String content = getContent(entry.getLink(), catPhrase);
					newsTitles.add(title + "###CONTENT: " + content);
				}
			} catch (MalformedURLException e) {
				System.out.println("Malformed");

			} catch (IOException e) {
				System.out.println("IOException");
				e.printStackTrace();
				;
			} catch (FeedException e) {
				System.out.println("FeedException");
				;
			}
		}
		return newsTitles;
	}
	
	/**
	 * Retrieves the contents of the webpage by link.
	 */
	private  String getContent(String pageUrl, String catPhrase) {
		String content = new String();
		try {
			URL url = new URL(pageUrl);
			URLConnection conn = url.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				content += catPhrase + inputLine;
			}
			br.close();
		} catch (MalformedURLException e) {
			System.out.println("MalformedURLException in getContent");
			;
		} catch (IOException e) {
			System.out.println("IOException in getContent");
		}
		return content;
	}
}
