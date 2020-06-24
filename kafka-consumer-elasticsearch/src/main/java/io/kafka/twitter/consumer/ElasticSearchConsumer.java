package io.kafka.twitter.consumer;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParser;

public class ElasticSearchConsumer {

	static final Logger logger = LoggerFactory.getLogger(ElasticSearchConsumer.class);
	
	public static RestHighLevelClient createClient() {
		String hostname = "localhost";
			

        // credentials provider help supply username and password
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		/*
		 * credentialsProvider.setCredentials(AuthScope.ANY, new
		 * UsernamePasswordCredentials(username, password));
		 */

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname, 9200, "http"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
	
	public static KafkaConsumer<String,String> createConsumer(String topic){
		String bootstrapServerHost = "127.0.0.1:9092";
		String groupId = "kafka-elasticsearch-application";
		
		Properties properties = new Properties();
		
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerHost);
		
		//consumer convers bytes to string which is called deserialization
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");//disable auto commit for offsets
		properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

		//create consumer
		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);
		consumer.subscribe(Arrays.asList(topic));
		
		return consumer;
	}
	
	public static void main(String[] args) throws IOException {
		RestHighLevelClient client = createClient();
		
		KafkaConsumer<String, String> consumer = createConsumer("twitter_tweet");
		

		while(true) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
			
			logger.info("received records " +records.count());
			Integer recordCount = records.count();
			BulkRequest bulkRequest = new BulkRequest();
			for(ConsumerRecord<String,String> record: records) {
				
				
				//2 strategies
				//1. kafka generic id
				//the combination is unique
				//String id = record.topic() + "_" + record.partition() + "_" + record.offset();
				
				//2. twitter feed specific id
				try {
				String id = extractIdFromTweet(record.value());
				
				//insert data into elastic search
				
				IndexRequest indexRequest = new IndexRequest("twitter1","tweets",id)
						.source(record.value(),XContentType.JSON);
				
				bulkRequest.add(indexRequest);
				}catch(NullPointerException e) {
					logger.warn("skipping bad data "+record.value());
				}

			}
			if(recordCount > 0) {
				BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
				
				logger.info("committing offsets");
				consumer.commitSync();
				logger.info("offsets have been committed ");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		//client.close();
				
	}

	private static JsonParser jsonParser = new JsonParser();
	private static String extractIdFromTweet(String tweetJson) {
		
		return jsonParser.parse(tweetJson)
				  .getAsJsonObject()
				  .get("id_str")
				  .getAsString();
		 
	}
}
