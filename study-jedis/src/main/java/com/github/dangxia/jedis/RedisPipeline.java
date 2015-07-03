package com.github.dangxia.jedis;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class RedisPipeline {
	private static final Logger LOG = LoggerFactory
			.getLogger(RedisPipeline.class);

	public static void ping(Jedis jedis) {
		int i = 0;
		List<Long> rrts = Lists.newArrayList();
		while (i++ < 10) {
			long ts = System.currentTimeMillis();
			jedis.ping();
			rrts.add(System.currentTimeMillis() - ts);
		}
		LOG.info("ping " + (Joiner.on(',').join(rrts)));
	}

	public static void incrWithoutPipeline(Jedis jedis) {
		long ts = System.currentTimeMillis();
		int i = 0;
		LinkedList<Long> responses = Lists.newLinkedList();
		while (i++ < 1000) {
			responses.add(jedis.incr("test-incr-without-pipeline"));
		}
		LOG.info("test-incr-without-pipeline "
				+ (System.currentTimeMillis() - ts));

		long total = 0l;
		for (Long response : responses) {
			total += response;
		}
		LOG.info("size:" + responses.size() + ",total:" + total);
	}

	public static void incrWithPipeline(Jedis jedis) {
		Pipeline pipeline = jedis.pipelined();
		long ts = System.currentTimeMillis();
		LinkedList<Response<Long>> responses = Lists.newLinkedList();
		int i = 0;
		while (i++ < 1000) {
			responses.add(pipeline.incr("test-incr-with-pipeline"));
		}
		pipeline.sync();
		LOG.info("test-incr-with-pipeline " + (System.currentTimeMillis() - ts));

		long total = 0l;
		for (Response<Long> response : responses) {
			total += response.get();
		}
		LOG.info("size:" + responses.size() + ",total:" + total);
	}

	public static void main(String[] args) {
		Jedis jedis = RedisCreator.createJedis();
		ping(jedis);
		incrWithPipeline(jedis);
		incrWithoutPipeline(jedis);
		jedis.close();
	}
}
