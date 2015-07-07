package com.github.dangxia.jedis;

import java.util.List;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import com.google.common.collect.Lists;

public class JedisPoolTest {

	public static void test() throws InterruptedException {
		JedisPoolConfig config = new JedisPoolConfig();

		// always create a new object when pool exhausted
		config.setBlockWhenExhausted(false);
		config.setMaxTotal(-1);

		config.setMaxIdle(20);
		config.setMinIdle(5);

		// 1min soft removal
		config.setSoftMinEvictableIdleTimeMillis(60000);
		// 3min hard removal
		config.setMinEvictableIdleTimeMillis(180000);
		// 30 seconds run eviction
		config.setTimeBetweenEvictionRunsMillis(30000);
		// test all objects
		config.setNumTestsPerEvictionRun(-1);

		config.setTestOnBorrow(true);
		config.setTestOnCreate(true);
		config.setTestWhileIdle(true);

		JedisPool pool = new JedisPool(config, RedisCreator.HOST,
				Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, null,
				Protocol.DEFAULT_DATABASE, "test-pool");

		List<Jedis> list = Lists.newArrayList();
		list.add(pool.getResource());
		list.add(pool.getResource());
		for (Jedis jedis : list) {
			jedis.close();
		}
		list.clear();
		new ClientList().start();
		System.out.println(pool.getNumIdle());

		TimeUnit.SECONDS.sleep(20);
		list.add(pool.getResource());
		list.add(pool.getResource());
		for (Jedis jedis : list) {
			jedis.close();
		}
		new ClientList().start();
		TimeUnit.SECONDS.sleep(3);
		pool.close();

	}

	public static class ClientList extends Thread {
		@Override
		public void run() {
			Jedis jedis = RedisCreator.createJedis();
			System.out.println(jedis.clientList());
			jedis.close();
		}

		private void killSomeone(Jedis jedis, String clinetName, String client) {

		}
	}

	public static void main(String[] args) throws InterruptedException {
		test();
	}
}
