package io.kafka.streams;

import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes.StringSerde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import com.google.gson.JsonParser;

public class StreamFilterTweets {

	public static void main(String[] args) {
		//create properties
		Properties properties = new Properties();
		String bootstrapServerHost = "127.0.0.1:9092";
		
		properties.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerHost);
		properties.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, "demo-kafka-streams");
		properties.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, StringSerde.class.getName());
		properties.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, StringSerde.class.getName());
		//create a topology
		
		StreamsBuilder streamsBuilder = new StreamsBuilder();
		
		KStream<String, String> inputTopic = streamsBuilder.stream("twitter_tweet");
		KStream<String, String> filteredStream = inputTopic.filter(
			(k,jsonTweet) -> extractUserFollowersInTweet(jsonTweet) > 10000
			//filter for tweets which has a user of over 10,000 followers
		); 
		filteredStream.to("important_tweets");
		
		//build the topology
		
		KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(), properties);
		
		//start streams application
		
		kafkaStreams.start();
	}
	
	private static JsonParser jsonParser = new JsonParser();
	private static int extractUserFollowersInTweet(String tweetJson) {
		
		try { 
			return jsonParser.parse(tweetJson)
				  .getAsJsonObject()
				  .get("user")
				  .getAsJsonObject()
				  .get("followers_count")
				  .getAsInt();
		}catch(NullPointerException e) {
			return 0;
		}
		 
	}
}
