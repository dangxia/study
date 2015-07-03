package com.github.dangxia.jedis.util;

import redis.clients.jedis.Jedis;

import com.github.dangxia.jedis.RedisCreator;

public class JedisUtls {
	public interface JedisRunable {
		public void run(Jedis jedis);
	}

	public static void execute(JedisRunable jedisRunable) {
		if (jedisRunable == null) {
			return;
		}
		Jedis jedis = null;
		try {
			jedis = RedisCreator.createJedis();
			jedisRunable.run(jedis);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}
}
