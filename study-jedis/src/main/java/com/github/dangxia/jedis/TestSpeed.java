package com.github.dangxia.jedis;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import com.google.common.collect.Maps;

public class TestSpeed {
	private static final Logger LOG = LoggerFactory.getLogger(TestSpeed.class);

	public static Map<String, String> data = Maps.newHashMap();
	public static long total = 100000;
	public static int keyLen = 10;
	public static int valueLen = 500;

	public static void createData() {
		for (int i = 0; i < total; i++) {
			data.put(RandomStringUtils.random(keyLen, true, true),
					RandomStringUtils.random(valueLen, true, true));
		}
		LOG.info("data size:{}", data.size());
		// for (Entry<String, String> entry : data.entrySet()) {
		// LOG.info("key:{}\tvalue:{}", entry.getKey(), entry.getValue());
		// }
	}

	public static void testWriteDataInMap() {
		Jedis jedis = RedisCreator.createJedis();
		Pipeline pipeline = jedis.pipelined();
		long ts = System.currentTimeMillis();
		for (Entry<String, String> entry : data.entrySet()) {
			pipeline.hset("test-map", entry.getKey(), entry.getValue());
		}
		pipeline.sync();
		LOG.info("testWriteDataInMap use time: {}", System.currentTimeMillis()
				- ts);
		ts = System.currentTimeMillis();
		jedis.hmget("test-map", data.keySet().toArray(new String[] {}));
		LOG.info("testReadDataInMap use time: {}", System.currentTimeMillis()
				- ts);
		jedis.close();
	}

	public static void testWriteDataWithKeys() {
		Jedis jedis = RedisCreator.createJedis();
		Pipeline pipeline = jedis.pipelined();
		long ts = System.currentTimeMillis();
		for (Entry<String, String> entry : data.entrySet()) {
			pipeline.set(entry.getKey(), entry.getValue());
		}
		pipeline.sync();
		LOG.info("testWriteDataWithKeys use time: {}",
				System.currentTimeMillis() - ts);

		ts = System.currentTimeMillis();
		jedis.mget(data.keySet().toArray(new String[] {}));
		LOG.info("testReadDataWithKeys use time: {}",
				System.currentTimeMillis() - ts);
		jedis.close();
	}

	public static void flushall() {
		Jedis jedis = RedisCreator.createJedis();
		jedis.flushAll();
		jedis.close();
	}

	public static void main(String[] args) {
		createData();
		int i = 0;
		while (i++ < 10) {
			flushall();
			testWriteDataInMap();
			flushall();
			testWriteDataWithKeys();
		}

	}
}
