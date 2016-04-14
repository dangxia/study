package com.github.dangxia.nio.telnet;

import static com.github.dangxia.nio.telnet.Code.CTRL_C_BYTES;
import static com.github.dangxia.nio.telnet.Code.EXIT_BYTES;
import static com.github.dangxia.nio.telnet.Code.HEAD_SIZE;
import static com.github.dangxia.nio.telnet.Code.SERVER_PORT;
import static com.github.dangxia.nio.telnet.Code.SHUTDOWN_BYTES;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;

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

import com.github.dangxia.nio.telnet.attach.ChannelAttach;
import com.google.common.base.Throwables;

public class NioEchoServer {

	private final Selector selector;
	private ServerSocketChannel serverSocketChannel;

	public NioEchoServer() throws IOException {
		selector = Selector.open();
	}

	public void start() throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(SERVER_PORT));
		serverSocketChannel.configureBlocking(false);

		serverSocketChannel.register(selector, OP_ACCEPT);

		System.out.println("started");
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
					} else if (key.isWritable()) {
						write(key);
					} else if (key.isReadable()) {
						read(key);
					}
				}
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
		NioEchoAttach reciever = (NioEchoAttach) key.attachment();
		if (reciever != null) {
			reciever.write();
		}
	}

	private void read(SelectionKey key) throws IOException {
		NioEchoAttach reciever = (NioEchoAttach) key.attachment();
		if (reciever == null) {
			reciever = new NioEchoAttach(key);
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

	private class NioEchoAttach extends ChannelAttach {
		private int readTimes = 0;
		private final ByteBuffer head = ByteBuffer.allocate(HEAD_SIZE);;
		private ByteBuffer body;

		public NioEchoAttach(SelectionKey key) {
			super(key);
		}

		@Override
		protected int doWrite() throws IOException {
			int write = socketChannel.write(body);
			if (!body.hasRemaining()) {
				disableWrite().enableRead();
			}
			head.clear();
			body = null;

			LOG.info("write back size: {}", write);

			return write;
		}

		@Override
		protected int doRead() throws IOException {
			readTimes++;
			if (readTimes > 0) {
				LOG.info("read multi times: {}", readTimes);
			}
			int headRead = 0;
			if (head.hasRemaining()) {
				headRead = socketChannel.read(head);
				if (headRead == -1) {
					close();
					return headRead;
				}
			}
			int bodyRead = 0;
			if (!head.hasRemaining()) {
				if (body == null) {
					head.flip();
					body = ByteBuffer.allocate(head.getInt());
				}
				if (body.hasRemaining()) {
					bodyRead = socketChannel.read(body);
					if (bodyRead == -1) {
						close();
						return bodyRead;
					}
				}

				if (!body.hasRemaining()) {
					readComplete();
				}
			}
			int size = bodyRead + headRead;
			LOG.info("receive size: {}", size);
			return size;
		}

		private void readComplete() throws IOException {
			readTimes = 0;

			body.flip();
			byte[] bytes = new byte[body.remaining()];
			body.get(bytes);
			body.rewind();
			if (Arrays.equals(bytes, CTRL_C_BYTES) || Arrays.equals(bytes, EXIT_BYTES)) {
				close();
			} else if (Arrays.equals(bytes, SHUTDOWN_BYTES)) {
				Thread.currentThread().interrupt();
			} else {
				LOG.info("receive {}", new String(bytes));
				disableRead().enableWrite();
			}
		}

	}

}
