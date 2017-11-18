package com.github.dangxia.ref;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RefTest {
	private static final int _3MB = 1024 * 1024;
	private static final int _64KB = 64 * 1024;
	private static final ReferenceQueue<RefClass> queue = new ReferenceQueue<RefClass>();

	public static class RefClass {
		private final ByteBuffer bb;
		public RefClass() {
			bb = ByteBuffer.allocate(_3MB);
		}
		public ByteBuffer getBb() {
			return bb;
		}

		@Override
		protected void finalize() throws Throwable {
			System.err.println("RefClass invoke finalize");
			super.finalize();
		}
	}

	/**
	 * VM Args : -Xmx5M
	 * 
	 */
	public static void main(String[] args) throws InterruptedException {
		Thread thread = new FinalizeCheck();
		thread.setDaemon(true);
		thread.start();

		test1();
		test2();
		test3();
	}

	private static void test3() throws InterruptedException {
		System.out.println("------------don't hold ref");
		finalzeTimes.set(0);
		new SoftReference<RefClass>(new RefClass(), queue);
		new WeakReference<RefClass>(new RefClass(), queue);
		new PhantomReference<RefClass>(new RefClass(), queue);

		TimeUnit.SECONDS.sleep(1);

		System.out.println("mem append");
		gc();
	}

	private static void test2() throws InterruptedException {
		System.out.println("------------ref hold single instance");
		finalzeTimes.set(0);
		RefClass single = new RefClass();
		Reference<RefClass> softRef = new SoftReference<RefClass>(single, queue);
		Reference<RefClass> weakRef = new WeakReference<RefClass>(single, queue);
		Reference<RefClass> phantomRef = new PhantomReference<RefClass>(single, queue);

		single = null;
		System.out.println("system gc");
		System.gc();
		TimeUnit.SECONDS.sleep(1);

		System.out.println("mem append");
		gc();
	}

	private static void test1() throws InterruptedException {
		System.out.println("-------------ref hold different instance");

		finalzeTimes.set(0);
		Reference<RefClass> softRef = new SoftReference<RefClass>(new RefClass(), queue);
		Reference<RefClass> weakRef = new WeakReference<RefClass>(new RefClass(), queue);
		Reference<RefClass> phantomRef = new PhantomReference<RefClass>(new RefClass(), queue);

		System.out.println("system gc");
		System.gc();
		TimeUnit.SECONDS.sleep(1);

		System.out.println("mem append");
		gc();
	}

	private static void gc() throws InterruptedException {
		List<Object> holder = new LinkedList<Object>();
		while (toToContinue()) {
			TimeUnit.MILLISECONDS.sleep(100);
			if (!toToContinue()) {
				break;
			}
			holder.add(ByteBuffer.allocate(_64KB));
		}
	}

	private static volatile AtomicInteger finalzeTimes = new AtomicInteger(0);

	private static class FinalizeCheck extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					@SuppressWarnings("rawtypes")
					Reference ref = queue.remove(TimeUnit.SECONDS.toMillis(10));
					if (ref != null) {
						finalzeTimes.incrementAndGet();
						System.out.println("queue removed:" + ref.toString());
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static boolean toToContinue() {
		return finalzeTimes.get() < 3;
	}

}
