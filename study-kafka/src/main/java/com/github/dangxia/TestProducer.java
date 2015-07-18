package com.github.dangxia;

import java.util.Date;
import java.util.Properties;
import java.util.Random;

import kafka.javaapi.producer.Producer;
import kafka.producer.DefaultPartitioner;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class TestProducer {
	public static void main(String[] args) {
		long events = 100;
		Random rnd = new Random();
		Properties props = new Properties();
		DefaultPartitioner p = new DefaultPartitioner(null);

		props.put("metadata.broker.list", Props.METADATA_BROKER_LIST);
		props.put("serializer.class", "kafka.serializer.StringEncoder");
		// props.put("partitioner.class", "example.producer.SimplePartitioner");
		props.put("request.required.acks", "1");

		ProducerConfig config = new ProducerConfig(props);

		Producer<String, String> producer = new Producer<String, String>(config);

		for (long nEvents = 0; nEvents < events; nEvents++) {
			long runtime = new Date().getTime();
			String ip = "192.168.2." + rnd.nextInt(255);
			String msg = runtime + ",www.example.com," + ip;

			KeyedMessage<String, String> data = new KeyedMessage<String, String>(
					"test-kafka-hexh", msg);
			producer.send(data);
		}
		producer.close();
	}
}
