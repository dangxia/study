package com.github.dangxia;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class ReentrantReadWriteLockTest {
	static final int SHARED_SHIFT = 16;
	static final int SHARED_UNIT = (1 << SHARED_SHIFT);
	static final int MAX_COUNT = (1 << SHARED_SHIFT) - 1;
	static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

	public static void main2(String[] args) {
		ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock(
				false);

		final ReadLock readLock = reentrantReadWriteLock.readLock();
		final WriteLock writeLock = reentrantReadWriteLock.writeLock();

		writeLock.lock();

		readLock.lock();

		readLock.unlock();

		new Thread() {
			public void run() {
				writeLock.lock();
			};
		}.start();
	}

	public static void main(String[] args) {
		System.out.println(SHARED_UNIT);
		System.out.println(MAX_COUNT);
		System.out.println(EXCLUSIVE_MASK);
	}
}
