package com.github.dangxia;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockSupportTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(LockSupportTest.class);

	public static void main(String[] args) throws InterruptedException {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(5);
					LOG.info("thread park once");
					LockSupport.park();
					LOG.info("thread park twice");
					LockSupport.park();
					LOG.info("thread park twice end");
				} catch (Exception e) {
				}
			}
		};
		t.start();
		LOG.info("unpark thread twice");
		LockSupport.unpark(t);
		LockSupport.unpark(t);

		TimeUnit.SECONDS.sleep(10);
		LockSupport.unpark(t);

	}
}
