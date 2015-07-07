package com.github.dangxia.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

public class JedisTransactionsWitPipeline {
	public static void main(String[] args) {
		Jedis jedis = RedisCreator.createJedis();
		// jedis.flushAll();
		Pipeline pipeline = jedis.pipelined();
		Response<Long> incr = pipeline.incr("test-incr");
		pipeline.multi();
		pipeline.set("kk", "pp");
		pipeline.set("pp", "kk");
		pipeline.exec();

		pipeline.multi();
		pipeline.set("kk", "pp2");
		pipeline.set("pp", "kk2");
		pipeline.discard();

		pipeline.sync();

		jedis.close();

		System.out.println(incr.get());
	}
}
