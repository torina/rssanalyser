package bula;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.PublishRequest;
import com.google.api.services.pubsub.model.PubsubMessage;

import bula.FeedPipeline.RetryHttpInitializerWrapper;

public class Publisher {
	private PrintWriter writer;

	private static Logger logger = Logger.getLogger(Publisher.class.getName());
	
	public Publisher () {
		try {
			this.writer = new PrintWriter("C:/Users/Administrator/Downloads/ex.txt", "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public  void publishData(Pubsub pubsub, String message, String outputTopic) {
		/**
		 * Publishes the given message to the given topic.
		 */
		int maxLogMessageLength = 400;
		String textOnly = filterMessage(message);
		if (textOnly.length() < maxLogMessageLength) {
			return;
		}
		// String nohtmlMessage = message.replaceAll("\\<.*?>","");
		logger.info("Received::" + "length:" + message.length() + "; " + message.substring(0, maxLogMessageLength));
		writer.println(
				"*****+\n" + Calendar.getInstance().getTime() + ": " + textOnly + System.getProperty("line.separator"));
		// writer.close();

		// Publish message to Pubsub.
		PubsubMessage pubsubMessage = new PubsubMessage();
		pubsubMessage.encodeData(message.getBytes());

		PublishRequest publishRequest = new PublishRequest();
		publishRequest.setTopic(outputTopic).setMessage(pubsubMessage);
		try {
			pubsub.topics().publish(publishRequest).execute();
		} catch (java.io.IOException e) {
			logger.warning(e.getStackTrace().toString());
		}
	}
	
	private static String filterMessage(String message) {
//		Document doc = Jsoup.parse(message);
//		Element link = doc.select("a").first();
//		String text = doc.body().text(); // "An example link"
//		System.out.println("text:" + text);
//		return text;
		return Jsoup.parse(message).text();
	}
}
