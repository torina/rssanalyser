package bula;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.Sleeper;

import com.google.api.services.pubsub.Pubsub;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Streaming injector for News sources using Pubsub I/O.
 *
 * To run this example using the Dataflow service, you must provide an output
 * pubsub topic for news, using the {@literal --inputTopic} option. This
 * injector can be run locally using the direct runner.
 * </p>
 * E.g.: java -cp target/examples-1.jar \
 * com.google.cloud.dataflow.examples.StockInjector \
 * --runner=DirectPipelineRunner \ --project=google.com:clouddfe \
 * --stagingLocation=gs://clouddfe-test/staging-$USER \
 * --outputTopic=/topics/google.com:clouddfe/stocks1w1
 */

public class FeedPipeline {

	class RetryHttpInitializerWrapper implements HttpRequestInitializer {

		private Logger logger = Logger.getLogger(RetryHttpInitializerWrapper.class.getName());

		// Intercepts the request for filling in the "Authorization"
		// header field, as well as recovering from certain unsuccessful
		// error codes wherein the Credential must refresh its token for a
		// retry.
		private final GoogleCredential wrappedCredential;

		// A sleeper; you can replace it with a mock in your test.
		private final Sleeper sleeper;

		public RetryHttpInitializerWrapper(GoogleCredential wrappedCredential) {
			this(wrappedCredential, Sleeper.DEFAULT);
		}

		// Use only for testing.
		RetryHttpInitializerWrapper(GoogleCredential wrappedCredential, Sleeper sleeper) {
			this.wrappedCredential = Preconditions.checkNotNull(wrappedCredential);
			this.sleeper = sleeper;
		}

		@Override
		public void initialize(HttpRequest request) {
			final HttpUnsuccessfulResponseHandler backoffHandler = new HttpBackOffUnsuccessfulResponseHandler(
					new ExponentialBackOff()).setSleeper(sleeper);
			request.setInterceptor(wrappedCredential);
			request.setUnsuccessfulResponseHandler(new HttpUnsuccessfulResponseHandler() {
				@Override
				public boolean handleResponse(HttpRequest request, HttpResponse response, boolean supportsRetry)
						throws IOException {
					if (wrappedCredential.handleResponse(request, response, supportsRetry)) {
						// If credential decides it can handle it, the
						// return code or message indicated something
						// specific to authentication, and no backoff is
						// desired.
						return true;
					} else if (backoffHandler.handleResponse(request, response, supportsRetry)) {
						// Otherwise, we defer to the judgement of our
						// internal backoff handler.
						logger.info("Retrying " + request.getUrl());
						return true;
					} else {
						return false;
					}
				}
			});
			request.setIOExceptionHandler(
					new HttpBackOffIOExceptionHandler(new ExponentialBackOff()).setSleeper(sleeper));
		}
	}

	private static String newsTopic;
	private Pubsub pubsub;
	private Publisher publisher = new Publisher();

	/**
	 * A constructor of FeedPipeline.
	 */
	public FeedPipeline(Pubsub pubsub, String newsTopic) {
		this.pubsub = pubsub;
		this.newsTopic = newsTopic;
	}

	/**
	 * Fetches the news titles and publishes them.
	 */
	public void publishNews() {
		
		List<String> sources = new ArrayList<>();
//		sources.add("http://www.npr.org/rss/rss.php?id=1001");
//		sources.add("http://rss.cnn.com/rss/edition.rss");
//		sources.add("https://news.google.com/news?output=rss");
		sources.add("http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml");
	
	
		List<String> newsItems = new DataExtractor().extractData(sources);
		for (String news : newsItems) {
			publisher.publishData(pubsub, news, newsTopic);
		}
		
	}

	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/**
	 * Creates a Cloud Pub/Sub client.
	 */
	public Pubsub createPubsubClient() throws IOException, GeneralSecurityException {
		HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
		GoogleCredential credential = GoogleCredential.getApplicationDefault();
		HttpRequestInitializer initializer = new RetryHttpInitializerWrapper(credential);
		return new Pubsub.Builder(transport, JSON_FACTORY, initializer).build();
	}

	/**
	 * Fetches news and publishes them to the specified Cloud Pub/Sub topic.
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Starting Application");
		System.setProperty("java.net.useSystemProxies", "true");

		// Get options from command-line.
		// if (args.length < 1) {
		// System.out.println("Please specify the output Pubsub topic.");
		// return;
		// }
		// String newsTopic = new String(args[0]);

		FeedPipeline injector = new FeedPipeline(null, "");
		Pubsub client = injector.createPubsubClient();

		injector = new FeedPipeline(client, newsTopic);

		//prints the same data with interval  - 100 000. A few messages come (~4 from each rss). 
		//
		while (true) {
			injector.publishNews();
			try {
//				 thread to sleep for the specified number of milliseconds
				Thread.sleep(10000000);
			} catch (java.lang.InterruptedException ie) {
				System.out.println("java.lang.InterruptedException");
			}
		}
	}
}