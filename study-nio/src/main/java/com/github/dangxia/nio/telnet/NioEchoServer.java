package com.github.dangxia.nio.telnet;

import static com.github.dangxia.nio.telnet.Code.CTRL_C_BYTES;
import static com.github.dangxia.nio.telnet.Code.EXIT_BYTES;
import static com.github.dangxia.nio.telnet.Code.HEAD_SIZE;
import static com.github.dangxia.nio.telnet.Code.SHUTDOWN_BYTES;
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

import com.google.common.base.Throwables;

public class NioEchoServer {

	private final Selector selector;
	private ServerSocketChannel serverSocketChannel;

	public NioEchoServer() throws IOException {
		selector = Selector.open();
	}

	public void start() throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(9999));
		serverSocketChannel.configureBlocking(false);

		serverSocketChannel.register(selector, OP_ACCEPT);

		System.out.println("started");
		while (!Thread.interrupted()) {
			try {
				int ready = selector.select(500);
				if (ready > 0) {
					Set<SelectionKey> keys = selector.selectedKeys();
					Iterator<SelectionKey> iter = keys.iterator();
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						iter.remove();
						if (key.isAcceptable()) {
							accept(key);
						} else if (key.isWritable()) {
							write(key);
						} else if (key.isReadable()) {
							read(key);
						}
					}
				}
			} catch (Exception e) {
				Throwables.propagate(e);
			}
		}
		Set<SelectionKey> keys = selector.keys();
		Iterator<SelectionKey> iter = keys.iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			if (key.isValid()) {
				try {
					key.channel().close();
				} catch (IOException e) {
					Throwables.propagate(e);
				}
				key.cancel();
			}
		}
		System.out.println("shutdown");

	}

	private void write(SelectionKey key) throws IOException {
		Reciever reciever = (Reciever) key.attachment();
		if (reciever != null) {
			reciever.write();
		}
	}

	private void read(SelectionKey key) throws IOException {
		Reciever reciever = (Reciever) key.attachment();
		if (reciever == null) {
			reciever = new Reciever(key);
			key.attach(reciever);
		}

		reciever.read();
	}

	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setTcpNoDelay(true);
		socketChannel.socket().setKeepAlive(true);

		socketChannel.register(selector, OP_READ);

		System.out.println("new conn registered");
	}

	private class Reciever {
		private final SelectionKey key;
		private final SocketChannel socketChannel;
		private boolean alive = true;

		private final ByteBuffer head = ByteBuffer.allocate(HEAD_SIZE);;
		private ByteBuffer body;

		private int readTimes = 0;

		public Reciever(SelectionKey key) {
			this.key = key;
			this.socketChannel = (SocketChannel) key.channel();
		}

		public void close() throws IOException {
			alive = false;
			key.cancel();
			socketChannel.close();
		}

		private void read(ByteBuffer bb) throws IOException {
			if (!alive) {
				return;
			}
			int head = socketChannel.read(bb);
			if (head == -1) {
				close();
			}
		}

		public void read() throws IOException {
			if (readTimes > 0) {
				System.out.println("read multi times: " + readTimes);
			}
			readTimes++;
			if (head.hasRemaining()) {
				read(head);
			}

			if (!head.hasRemaining()) {
				if (body == null) {
					head.flip();
					body = ByteBuffer.allocate(head.getInt());
				}

				if (body.hasRemaining()) {
					read(body);
				}

				if (!body.hasRemaining()) {
					readComplete();
				}
			}

		}

		public void write() throws IOException {
			if (body != null) {
				socketChannel.write(body);
				key.interestOps(key.interestOps() & ~OP_WRITE | OP_READ);

				head.clear();
				body = null;
			}
		}

		private void readComplete() throws IOException {
			readTimes = 0;

			body.flip();
			byte[] bytes = new byte[body.remaining()];
			body.get(bytes);
			body.rewind();
			if (Arrays.equals(bytes, CTRL_C_BYTES) || Arrays.equals(bytes, EXIT_BYTES)) {
				key.cancel();
				key.channel().close();
			} else if (Arrays.equals(bytes, SHUTDOWN_BYTES)) {
				Thread.currentThread().interrupt();
			} else {
				System.out.print(new String(bytes));
				key.interestOps(key.interestOps() & ~OP_READ | OP_WRITE);
			}
		}
	}

}
