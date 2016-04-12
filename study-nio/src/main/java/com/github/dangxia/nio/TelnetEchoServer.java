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

import com.google.common.base.Throwables;

public class TelnetEchoServer {
	private static final byte[] CTRL_C_BYTES = new byte[] { -1, -12, -1, -3, 6 };
	private static final byte[] EXIT_BYTES = "exit\r\n".getBytes();
	private static final byte[] SHUTDOWN_BYTES = "shutdown\r\n".getBytes();

	public static void main(String[] args) throws IOException {
		new TelnetEchoServer().start();
	}

	private final Selector socketChannelSelector;
	private final Selector serverSocketChannelSelector;

	private ServerSocketChannel serverSocketChannel;
	private Thread socketChannelThread;
	private Thread serverSocketChannelThread;

	public TelnetEchoServer() throws IOException {
		socketChannelSelector = Selector.open();
		serverSocketChannelSelector = Selector.open();
	}

	public void start() throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(9999));
		serverSocketChannel.configureBlocking(false);

		serverSocketChannel.register(serverSocketChannelSelector, OP_ACCEPT);

		socketChannelThread = new SocketChannelThread();
		socketChannelThread.start();

		serverSocketChannelThread = new ServerSocketChannelThread();
		serverSocketChannelThread.start();
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

		socketChannel.register(socketChannelSelector, OP_READ);

		System.out.println("new conn registered");
	}

	private void shutdown() {
		if (socketChannelThread != null) {
			socketChannelThread.interrupt();
		}
		if (serverSocketChannelThread != null) {
			serverSocketChannelThread.interrupt();
		}
	}

	private class ServerSocketChannelThread extends Thread {
		@Override
		public void run() {
			System.out.println("ServerSocketChannelThread start");
			while (!Thread.interrupted()) {
				try {
					int ready = serverSocketChannelSelector.select(500);
					if (ready > 0) {
						Set<SelectionKey> keys = serverSocketChannelSelector.selectedKeys();
						Iterator<SelectionKey> iter = keys.iterator();
						while (iter.hasNext()) {
							SelectionKey key = iter.next();
							iter.remove();
							if (key.isAcceptable()) {
								accept(key);
							}
						}
					}
				} catch (Exception e) {
					Throwables.propagate(e);
				}
			}
			try {
				serverSocketChannel.close();
			} catch (IOException e) {
				Throwables.propagate(e);
			}
			System.out.println("ServerSocketChannelThread shutdown");
		}
	}

	private class SocketChannelThread extends Thread {
		@Override
		public void run() {
			System.out.println("SocketChannelThread start");
			while (!Thread.interrupted()) {
				try {
					int ready = socketChannelSelector.select(500);
					if (ready > 0) {
						Set<SelectionKey> keys = socketChannelSelector.selectedKeys();
						Iterator<SelectionKey> iter = keys.iterator();
						while (iter.hasNext()) {
							SelectionKey key = iter.next();
							iter.remove();
							if (key.isWritable()) {
								write(key);
							} else if (key.isReadable()) {
								read(key);
							}
						}
					}
				} catch (IOException e) {
					Throwables.propagate(e);
				}
			}

			Set<SelectionKey> keys = socketChannelSelector.keys();
			Iterator<SelectionKey> iter = keys.iterator();
			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				try {
					key.channel().close();
				} catch (IOException e) {
					Throwables.propagate(e);
				}
				key.cancel();
			}
			System.out.println("SocketChannelThread shutdown");
		}
	}

	private class Reciever {
		private final SelectionKey key;
		private final SocketChannel socketChannel;

		private ByteBuffer playload;
		private int readTimes = 0;

		public Reciever(SelectionKey key) {
			this.key = key;
			this.socketChannel = (SocketChannel) key.channel();
		}

		public void read() throws IOException {
			if (readTimes > 0) {
				System.out.println("read multi times: " + readTimes);
			}
			readTimes++;

			ByteBuffer bb = ByteBuffer.allocate(4);
			int read = socketChannel.read(bb);
			if (read == -1) {
				key.cancel();
				key.channel().close();
			} else if (read == 0) {
				append(bb);
				readComplete();
			} else if (!bb.hasRemaining()) {
				append(bb);
				readComplete();
			} else if (bb.hasRemaining() && socketChannel.read(bb) == 0) {
				// 读满就不会触发OP_READ
				append(bb);
				readComplete();
			} else {
				append(bb);
			}
		}

		public void write() throws IOException {
			if (playload != null) {
				socketChannel.write(playload);
				key.interestOps(key.interestOps() & ~OP_WRITE | OP_READ);

				playload = null;
			}
		}

		private void append(ByteBuffer bb) {
			bb.flip();
			if (!bb.hasRemaining()) {
				return;
			}
			if (playload == null) {
				bb.compact();
				playload = bb;
			} else if (playload.remaining() >= bb.remaining()) {
				playload.put(bb);
			} else {
				ByteBuffer playload = ByteBuffer.allocate(this.playload.capacity() + bb.capacity());
				this.playload.flip();
				playload.put(this.playload);
				playload.put(bb);
				this.playload = playload;
			}
		}

		private void readComplete() throws IOException {
			readTimes = 0;

			playload.flip();
			byte[] bytes = new byte[playload.remaining()];
			playload.get(bytes);
			playload.rewind();
			if (Arrays.equals(bytes, CTRL_C_BYTES) || Arrays.equals(bytes, EXIT_BYTES)) {
				key.cancel();
				key.channel().close();
			} else if (Arrays.equals(bytes, SHUTDOWN_BYTES)) {
				shutdown();
			} else {
				System.out.print(new String(bytes));
				key.interestOps(key.interestOps() & ~OP_READ | OP_WRITE);
			}
		}
	}
}
