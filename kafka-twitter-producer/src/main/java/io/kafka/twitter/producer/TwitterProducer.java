package io.kafka.twitter.producer;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class TwitterProducer {
	
	
	final Logger logger = LoggerFactory.getLogger(TwitterProducer.class);
	
	String consumerKey = "";
	String consumerSecret = "";
	String token = "";
	String secret = "";
	
	List<String> terms = Lists.newArrayList("bitcoin","india","cricket","covid-19");
	
	public static void main(String args[]) {
		new TwitterProducer().run();
	}

	public void run() {
		//step1. Create Twitter CLient
		
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);
	
	
		Client client = createTwitterClient(msgQueue);
		client.connect();
		
		//step2. Create a kafka producer
		
		KafkaProducer<String, String> producer = createKafkaProducer();
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("stopping application");
			logger.info("shutting down client from twitter...");
			client.stop();
			logger.info("closing producer");
			producer.close();
			logger.info("done!........");
			
		}));
		
		
		//step3. loop to send tweets to kafka
	
		while (!client.isDone()) {
			  String msg = null;
			try {
				msg = msgQueue.poll(5,TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				client.stop();
			}
			  
			  if(msg != null) {
				  logger.info("message is "+msg);
				  producer.send(new ProducerRecord<String, String>("twitter_tweet", null,msg), new Callback() {
					
					public void onCompletion(RecordMetadata metadata, Exception exception) {
						if(exception != null) {
							logger.error("error occured "+exception);
						}
						
					}
				});
			  }

			  logger.info("end of application");
			}
	}

	
	public Client createTwitterClient(BlockingQueue<String> msgQueue) {
		
	
		/** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		// Optional: set up some followings and track terms
		
		hosebirdEndpoint.trackTerms(terms);
		
		// These secrets should be read from a config file
		Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);
		
		ClientBuilder builder = new ClientBuilder()
				  .name("Hosebird-Client-01")                              // optional: mainly for the logs
				  .hosts(hosebirdHosts)
				  .authentication(hosebirdAuth)
				  .endpoint(hosebirdEndpoint)
				  .processor(new StringDelimitedProcessor(msgQueue));
	
		Client hosebirdClient = builder.build();
		return hosebirdClient;
			
	}
	
	public KafkaProducer<String, String> createKafkaProducer() {
		String bootstrapServerHost = "127.0.0.1:9092";
		Properties properties = new Properties();
		
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerHost);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		
		//create a safe producer
		properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
		properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
		properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
		properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
		
		//create high throughput producer at the expense of a bit of latency and cpu usage
		//messages are sent to kafka in batch and compressed
		
		properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
		properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20"); //wait for 20 ms for the messages
		properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));
		
		//create kafka producer with the config
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
		
		return producer;
				
	}
}
