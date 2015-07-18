package com.github.dangxia.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class TestSocketChannel {
	public static void main(String[] args) throws IOException,
			InterruptedException {
		final Selector selector = Selector.open();
		Process p = new Process(selector);
		p.start();
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.bind(new InetSocketAddress(9797));
		new Write().start();
		while (true) {
			SocketChannel socketChannel = serverSocketChannel.accept();
			if (socketChannel != null) {
				socketChannel.configureBlocking(false);
				socketChannel.register(selector, SelectionKey.OP_CONNECT
						| SelectionKey.OP_READ);
				System.out.println("sldjflsjldjf");
			}
		}
		// serverSocketChannel.close();
	}

	static class Write extends Thread {
		@Override
		public void run() {
			try {
				TimeUnit.SECONDS.sleep(1);
				SocketChannel socketChannel = SocketChannel.open();
				socketChannel.configureBlocking(true);
				socketChannel.connect(new InetSocketAddress(9797));
				if (socketChannel.finishConnect()) {
					ByteBuffer bb = ByteBuffer.allocate(5);
					bb.put("kk".getBytes());
					bb.flip();
					socketChannel.write(bb);

					System.out.println("finishConnect");
				}
				TimeUnit.SECONDS.sleep(10);
				socketChannel.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	static class Process extends Thread {
		private final Selector selector;

		public Process(Selector selector) {
			this.selector = selector;
		}

		@Override
		public void run() {
			try {
				while (true) {
					int readyChannels = selector.selectNow();
					if (readyChannels == 0)
						continue;
					Iterator<SelectionKey> iterator = selector.selectedKeys()
							.iterator();
					while (iterator.hasNext()) {
						SelectionKey selectionKey = iterator.next();
						if (selectionKey.isConnectable()) {
							System.out.println("isConnectable");
						} else if (selectionKey.isReadable()) {
							SocketChannel channel = (SocketChannel) selectionKey
									.channel();
							ByteBuffer bb = ByteBuffer.allocate(5);
							int read = channel.read(bb);
							while (read > 0) {
								read = channel.read(bb);
							}
							bb.flip();
							if (bb.limit() == 0) {
								channel.close();
								System.out.println("close");
							} else {
								byte[] bytes = new byte[bb.limit()];
								bb.get(bytes);
								System.out.println(new String(bytes));
							}

							// bb.clear();
							// bb.compact();
						} else {
							System.out.println("isWritable");
						}
						iterator.remove();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}
