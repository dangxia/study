package com.github.dangxia.jedis;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import com.google.common.base.Joiner;

public class RedisTransactions {
	public static final String WATCHED_KEY = "watched-key";
	private final static Logger LOG = LoggerFactory
			.getLogger(RedisTransactions.class);

	public static void usage() {
		Jedis jedis = RedisCreator.createJedis();
		Transaction transaction = jedis.multi();
		Response<Long> foo = transaction.incr("foo");
		Response<Long> bar = transaction.incr("bar");
		List<Object> results = transaction.exec();
		jedis.close();

		LOG.info("foo: " + foo.get());
		LOG.info("bar: " + bar.get());
		LOG.info("results: " + (Joiner.on(',').join(results)));
	}

	public static void error() {
		Jedis jedis = RedisCreator.createJedis();
		Transaction transaction = jedis.multi();
		Response<String> r1 = transaction.set("a", "1");
		Response<String> r2 = transaction.lpop("a");
		Response<String> r3 = transaction.set("a", "2");
		List<Object> results = transaction.exec();
		jedis.close();

		LOG.info("r1: " + r1.get());
		try {
			LOG.info("r2: " + r2.get());
		} catch (Exception e) {
			LOG.warn("r2 get() throw a exception", e);
		}
		LOG.info("r3: " + r3.get());
		LOG.info("results: " + (Joiner.on(',').join(results)));
	}

	public static void main(String[] args) throws InterruptedException {
		testWatch();
	}

	public static void testWatchFailed() throws InterruptedException {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final CountDownLatch countDownLatch2 = new CountDownLatch(1);
		new Thread() {
			public void run() {
				try {
					countDownLatch.await();
					Jedis jedis = RedisCreator.createJedis();
					jedis.set(WATCHED_KEY, "10");
					jedis.close();
					countDownLatch2.countDown();
				} catch (Exception e) {
				}

			};
		}.start();
		Jedis jedis = RedisCreator.createJedis();
		jedis.set(WATCHED_KEY, "10");
		jedis.watch(WATCHED_KEY);
		countDownLatch.countDown();
		countDownLatch2.await();
		Transaction transaction = jedis.multi();
		Response<String> setResp = transaction.set(WATCHED_KEY, "1");
		List<Object> results = transaction.exec();
		jedis.close();
		LOG.info("results :" + String.valueOf(results));
		try {
			LOG.info("setResp :" + String.valueOf(setResp.get()));
		} catch (Exception e) {
			LOG.warn("watched keys changed", e);
		}
	}

	public static void testWatch() throws InterruptedException {
		Jedis jedis = RedisCreator.createJedis();
		jedis.del(WATCHED_KEY);

		int threadNum = 10;
		CountDownLatch countDownLatch = new CountDownLatch(threadNum);

		for (int i = 0; i < threadNum; i++) {
			new Incr(countDownLatch).start();
		}

		countDownLatch.await();

		LOG.info(jedis.get(WATCHED_KEY));
		jedis.close();
	}

	public static class Incr extends Thread {
		private final Jedis jedis;
		private final CountDownLatch countDownLatch;
		private long retryTimes = 0l;

		public Incr(CountDownLatch countDownLatch) {
			this.jedis = RedisCreator.createJedis();
			this.countDownLatch = countDownLatch;
		}

		protected void incr() {
			while (true) {
				if (doIncr()) {
					break;
				} else {
					retryTimes++;
				}
			}
		}

		protected boolean doIncr() {
			jedis.watch(WATCHED_KEY);
			String val = jedis.get(WATCHED_KEY);
			Transaction transaction = jedis.multi();
			if (val == null) {
				transaction.set(WATCHED_KEY, "1");
			} else {
				transaction.set(WATCHED_KEY,
						String.valueOf(Integer.parseInt(val) + 1));
			}
			List<Object> results = transaction.exec();
			if (results == null) {
				return false;
			}
			return true;
		}

		@Override
		public void run() {
			int i = 0;
			while (i++ < 100) {
				incr();
			}
			LOG.info(getName() + " retry " + retryTimes + " times");
			jedis.close();
			countDownLatch.countDown();
		}
	}
}
