# Kafka_Twitter_ElasticSearch
Producer streams tweets from twitter into Kafka and the consumers retrieves the data from kafka and loads in to the Elasticsearch Sink

This is a producer consumer application using Kafka

Prerequsities;

1. Download Zookeeper. Configure the config/zookeeper.properties file. zookeeper is required for kafka to run. Zookeeper will store the kafka configurations
2. Download Kafka. Configure the config/server.properties
3. Download ElasticSearch and run it on port 9200. Optionally you can use Bonsai Elastic Search cloud or Elastic Search cloud service provided with 3 node cluster in free tier.

#zookeeper start

zookeeper-server-start.bat config/zookeeper.properties

runs on port 2181

#kafka start

kafka-server-start.bat config/server.properties

#create multiple copies of server.properties and run them separately for multi broker kafka application

runs on port 9092

#Steps

1. Create a topic named twitter_tweet
2. Configure the producer with bootstrapServerHost = "127.0.0.1:9092";
3. Create an Application using Twitter Developer account and obtain the credentials
4. Configure the producer with the twitter credentials to retrieve tweets from Twitter
5. The producer will load the tweets from twitter into the kafka topic "twitter_tweet"
6. Configure the consumer with bootstrapServerHost = "127.0.0.1:9092";
7. Subscribe consumer to the topic "twitter_tweet"
8. Create a RestClientBuilder to connect to ElasticSearch running in port 9200 in local
9. Run the producer application to publish the tweets to kafka
10. Run the consumer application to consume the tweets from kafka and load into Elastic Search using IndexRequest object. BulkRequest is very efficient
11. View the indexed tweets from Kibana and create dashboards


url to find index in ElastiCSearch

http://localhost:9200/_cat/indices/?v

url to find data inside index

http://localhost:9200/elasticsearch/_search


#Kafka_Streams

 - Kafka Streams is used to process data in real time
 
 1. In this application, the Kafka Streams process the tweets from topic "twitter_tweet"
 2. It filters the tweet wtih more than 10,000 followers
 3. It then puts the filteres tweets in to "important_tweets" topic
 4. Start a console consumer to consume from this topic "important_tweets" and verify the data
 
