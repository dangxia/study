package com.github.dangxia.jedis;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.dangxia.jedis.util.JedisUtls;
import com.github.dangxia.jedis.util.JedisUtls.JedisRunable;

public class JedisEval {
	private static final Logger LOG = LoggerFactory.getLogger(JedisEval.class);

	public static void main(String[] args) {
		JedisUtls.execute(testLog());
	}

	public static JedisRunable testSimple() {
		return new JedisRunable() {

			@Override
			public void run(Jedis jedis) {
				Object result = jedis.eval(
						"return {KEYS[1],KEYS[2],ARGV[1],ARGV[2]}", 2, "key1",
						"key2", "first", "second");
				displayEvalResult(result);

			}
		};
	}

	public static JedisRunable testConversion1() {
		return new JedisRunable() {
			@Override
			public void run(Jedis jedis) {
				jedis.set("foo", "bar");
				displayEvalResult(jedis.eval("return 10", 0));
				displayEvalResult(jedis.eval("return {1,2,{3,'Hello World!'}}",
						0));
				displayEvalResult(jedis.eval("return redis.call('get','foo')",
						0));
				displayEvalResult(jedis.eval(
						"return {1,2,3.3333,'foo',nil,'bar'}", 0));
				displayEvalResult(jedis.eval("return true", 0));
				displayEvalResult(jedis.eval("return false", 0));
				displayEvalResult(jedis.eval(
						"return redis.call('set','foo','kk')", 0));
			}
		};
	}

	public static JedisRunable testHelper() {
		return new JedisRunable() {
			@Override
			public void run(Jedis jedis) {
				try {
					displayEvalResult(jedis.eval("return {err='My Error'}", 0));
				} catch (Exception e) {
					LOG.info("eval error", e);
				}
				try {
					displayEvalResult(jedis.eval(
							"return redis.error_reply('My Error')", 0));
				} catch (Exception e) {
					LOG.info("eval error", e);
				}

				displayEvalResult(jedis.eval(
						"return redis.status_reply('success')", 0));

			}
		};
	}

	public static JedisRunable testErrorHandling() {
		return new JedisRunable() {
			@Override
			public void run(Jedis jedis) {
				jedis.del("foo");
				jedis.lpush("foo", "a");
				try {
					displayEvalResult(jedis.eval(
							"return redis.call('get','foo')", 0));
				} catch (Exception e) {
					LOG.info("eval error", e);
				}
				try {
					displayEvalResult(jedis.eval(
							"return redis.pcall('get','foo')", 0));
				} catch (Exception e) {
					LOG.info("eval error", e);
				}

			}
		};
	}

	public static JedisRunable testEvalsha() {
		return new JedisRunable() {
			@Override
			public void run(Jedis jedis) {
				jedis.del("foo");
				jedis.set("foo", "bar");
				displayEvalResult(jedis.eval("return redis.sha1hex(ARGV[1])",
						0, "return redis.call('get','foo')"));
				displayEvalResult(jedis.eval("return redis.call('get','foo')",
						0));
				displayEvalResult(jedis
						.evalsha("6b1bf486c81ceb7edf3c093f4c48582e38c0e791"));

				try {
					displayEvalResult(jedis
							.evalsha("ffffffffffffffffffffffffffffffffffffffff"));
				} catch (Exception e) {
					LOG.info("eval error", e);
				}
			}
		};
	}

	public static JedisRunable testScriptCmds() {
		return new JedisRunable() {
			@Override
			public void run(Jedis jedis) {
				final String script_sha1 = "6b1bf486c81ceb7edf3c093f4c48582e38c0e791";
				LOG.info("script flush: " + jedis.scriptFlush());
				LOG.info(
						"script {} exists: " + jedis.scriptExists(script_sha1),
						script_sha1);
				LOG.info("script load: "
						+ jedis.scriptLoad("return redis.call('get','foo')"));
				LOG.info(
						"script {} exists: " + jedis.scriptExists(script_sha1),
						script_sha1);
				LOG.info("script flush: " + jedis.scriptFlush());
				LOG.info(
						"script {} exists: " + jedis.scriptExists(script_sha1),
						script_sha1);
			}
		};
	}

	public static JedisRunable testLog() {
		return new JedisRunable() {
			@Override
			public void run(Jedis jedis) {
				jedis.eval(
						"redis.log(redis.LOG_WARNING,\"Something is wrong with this script.\")",
						0);
			}
		};
	}

	public static void displayEvalResult(Object result) {
		displayEvalResult(result, 0);
	}

	public static void displayEvalResult(Object result, int indent) {
		StringBuffer sb = new StringBuffer("\nresult:\n");
		displayEvalResultStr(result, indent, sb);
		LOG.info(sb.toString());
	}

	public static StringBuffer addIndent(int indent, StringBuffer sb) {
		for (int i = 0; i < indent; i++) {
			sb.append("  ");
		}
		return sb;
	}

	public static void displayEvalResultStr(Object result, int indent,
			StringBuffer sb) {
		if (result == null) {
			addIndent(indent, sb).append("null");
		} else if (result instanceof List) {
			addIndent(indent, sb).append("[\n");
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) result;
			for (Object object : list) {
				displayEvalResultStr(object, indent + 1, sb);
			}
			addIndent(indent, sb).append("]");
		} else {
			addIndent(indent, sb).append(
					result.getClass().getSimpleName() + ":").append(
					result.toString() + "\n");
		}
	}
}
