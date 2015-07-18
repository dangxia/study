package com.github.dangxia;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.MessageAndOffset;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stat.commons.kafka.KafkaUtils;
import stat.commons.kafka.model.Broker;
import stat.commons.kafka.model.PartitionState;

public class PartitionInfo {
	private static final Logger LOG = LoggerFactory
			.getLogger(PartitionInfo.class);

	public static void main(String[] args) {
		CuratorFramework client = CuratorFrameworkFactory.builder()
				.connectString(Props.ZOOKEEPER_KAFKA_LIST)
				.retryPolicy(new ExponentialBackoffRetry(500, 3)).build();
		client.start();
		Map<Broker, List<PartitionState>> data = KafkaUtils.getPartitionState(
				client, "test-kafka-hexh");
		client.close();

		for (Entry<Broker, List<PartitionState>> entry : data.entrySet()) {
			LOG.info("broker: {}", entry.getKey());
			for (PartitionState state : entry.getValue()) {
				LOG.info("\tstate: {}", state);
				showData(state);
			}
		}

	}

	public static void showData(PartitionState state) {
		String topic = state.getBrokerAndTopicPartition().getTopicPartition()
				.getTopic();
		int partition = state.getBrokerAndTopicPartition().getTopicPartition()
				.getPartition();

		SimpleConsumer consumer = createSimpleConsumer(state
				.getBrokerAndTopicPartition().getBroker());
		showDetails(consumer, topic, partition, state.getLatestOffset());
		showDetails(consumer, topic, partition, state.getEarliestOffset());
		consumer.close();
	}

	private static void showDetails(SimpleConsumer consumer, String topic,
			int partition, long offset) {
		ByteBufferMessageSet byteBufferMessageSet = fetch(consumer, topic,
				partition, offset);
		for (MessageAndOffset messageAndOffset : byteBufferMessageSet) {
			LOG.info("\toffset: {}", messageAndOffset.offset());
		}
	}

	private static ByteBufferMessageSet fetch(SimpleConsumer consumer,
			String topic, int partition, long offset) {
		FetchRequestBuilder builder = createFetchRequestBuilder(topic,
				partition, offset);

		FetchResponse fetchResponse = null;
		try {
			fetchResponse = consumer.fetch(builder.build());
			if (fetchResponse.hasError()) {
				short errorCode = fetchResponse.errorCode(topic, partition);
				throw new RuntimeException(
						"Error encountered during a fetch request from Kafka,Error Code generated : "
								+ errorCode);
			} else {
				return fetchResponse.messageSet(topic, partition);
			}
		} catch (Exception e) {
			throw new RuntimeException("Exception generated during fetch", e);
		}
	}

	private static FetchRequestBuilder createFetchRequestBuilder(String topic,
			int partition, long offset) {
		FetchRequestBuilder builder = new FetchRequestBuilder();
		builder.clientId("storm_kafka_consumer").maxWait(100).minBytes(1);
		// kafkaFetchMessageMaxBytes 1M
		builder.addFetch(topic, partition, offset, 8388608);
		return builder;
	}

	public static SimpleConsumer createSimpleConsumer(Broker broker) {
		// kafkaSocketTimeoutMs 3s
		// kafkaSocketReceiveBufferBytes 32K
		return new SimpleConsumer(broker.host(), broker.port(), 3000, 262144,
				"storm_kafka_consumer");
	}
}
