package com.github.dangxia.nio.telnet;

import static com.github.dangxia.nio.telnet.Code.HEAD_SIZE;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.github.dangxia.nio.telnet.SplitAdaptors.Splitor;
import com.google.common.base.Throwables;

public class NioEchoClient {
	private static final String msg = "qqwweerrttyyuu";
	private static final byte[] playload = new byte[HEAD_SIZE + msg.getBytes().length];
	static {
		byte[] body = msg.getBytes();
		ByteBuffer b = ByteBuffer.allocate(4);
		b.putInt(body.length);

		byte[] result = b.array();
		System.arraycopy(result, 0, playload, 0, 4);
		System.arraycopy(body, 0, playload, 4, body.length);
	}

	private final SocketChannel socketChannel;
	private final Selector selector;

	public NioEchoClient() throws IOException {
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(9999));
		selector = Selector.open();

		socketChannel.register(selector, OP_WRITE | OP_CONNECT | OP_READ);
	}

	public void start() {
		while (!Thread.interrupted()) {
			try {
				int ready = selector.select(500);
				if (ready > 0) {
					Set<SelectionKey> keys = selector.selectedKeys();
					Iterator<SelectionKey> iter = keys.iterator();
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						iter.remove();
						if (key.isConnectable()) {
							socketChannel.finishConnect();
						} else if (key.isWritable()) {
							write(key);
						} else if (key.isReadable()) {
						}
					}
				}
			} catch (Exception e) {
				Throwables.propagate(e);
			}
		}
	}

	private void write(SelectionKey key) throws IOException {
		DataHolder holder = (DataHolder) key.attachment();
		if (holder == null) {
			holder = new DataHolder(Splitor.BOTH, key);
			key.attach(holder);
		}
		holder.write();
	}

	private class DataHolder {
		private final byte[][] data;
		private final LinkedBlockingQueue<byte[]> queue;
		private final SelectionKey key;
		private volatile int index = 0;

		public DataHolder(Splitor splitor, SelectionKey key) {
			data = splitor.split(playload);
			queue = new LinkedBlockingQueue<>();
			this.key = key;
			startPushThread();

		}

		private void push() {
			if (!isFinished()) {
				queue.offer(data[index]);
				index++;
			}

		}

		private boolean isFinished() {
			return index >= data.length;
		}

		private void startPushThread() {
			push();
			new Thread() {
				@Override
				public void run() {
					while (!isFinished()) {
						try {
							TimeUnit.SECONDS.sleep(5);
							System.out.println("send segment");
							push();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					}
				}
			}.start();
		}

		public void write() throws IOException {
			byte[] bytes = queue.poll();
			if (bytes != null) {
				ByteBuffer bb = ByteBuffer.allocate(bytes.length);
				bb.put(bytes);
				bb.flip();
				socketChannel.write(bb);
			}
			if (isFinished()) {
				key.interestOps(key.interestOps() & ~OP_WRITE);
			}

		}
	}

	public static void main(String[] args) {
		new Thread() {
			@Override
			public void run() {
				try {
					new NioEchoServer().start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();

		new Thread() {
			@Override
			public void run() {
				try {
					new NioEchoClient().start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

	}

}
