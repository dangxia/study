package com.github.dangxia.nio;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Server {

	private static final byte[] CTRL_C_BYTES = new byte[] { -1, -12, -1, -3, 6, 0, 0, 0 };

	private static Selector socketChannelSelector;
	private static Executor executor;

	public static void main(String[] args) throws IOException {
		socketChannelSelector = Selector.open();
		executor = Executors.newSingleThreadExecutor();

		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(9999));
		serverSocketChannel.configureBlocking(false);

		Selector selector = Selector.open();
		serverSocketChannel.register(selector, OP_ACCEPT);

		executor.execute(new Runnable() {

			@Override
			public void run() {
				while (!Thread.interrupted()) {
					try {
						int ready = socketChannelSelector.select(500);
						if (ready > 0) {
							Set<SelectionKey> keys = socketChannelSelector.selectedKeys();
							Iterator<SelectionKey> iter = keys.iterator();
							while (iter.hasNext()) {
								SelectionKey key = iter.next();
								iter.remove();

								if (key.isConnectable()) {
									System.out.println("Connectable");
								}

								if (key.isWritable()) {
									SocketChannel socketChannel = (SocketChannel) key.channel();
									ByteBuffer bb = (ByteBuffer) key.attachment();
									bb.flip();
									socketChannel.write(bb);

									key.interestOps(key.interestOps() & ~OP_WRITE | OP_READ);
								}

								if (key.isReadable()) {
									System.out.println("readable");

									ByteBuffer bb = ByteBuffer.allocate(4);
									SocketChannel socketChannel = (SocketChannel) key.channel();
									int read = 0;
									while ((read = socketChannel.read(bb)) > 0 && !bb.hasRemaining()) {
										ByteBuffer _bb = ByteBuffer.allocate(bb.capacity() * 2);
										bb.flip();
										_bb.put(bb);
										bb = _bb;
									}
									byte[] bytes = bb.array();
									if (read == -1 || Arrays.equals(bytes, CTRL_C_BYTES)) {
										key.cancel();
										socketChannel.close();
									} else {
										key.attach(bb);
										System.out.println(new String(bytes));
										key.interestOps(key.interestOps() & ~OP_READ | OP_WRITE);
									}
								}

							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		while (!Thread.interrupted()) {

			int ready = selector.select(500);
			if (ready > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> iter = keys.iterator();
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					iter.remove();

					if (key.isAcceptable()) {
						accept(key);
					}

				}
			}

		}

	}

	public static void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setTcpNoDelay(true);
		socketChannel.socket().setKeepAlive(true);

		socketChannel.register(socketChannelSelector, OP_READ);
		System.out.println("new conn");
	}
}
