package com.github.dangxia.nio.telnet;

import static com.github.dangxia.nio.telnet.Code.HEAD_SIZE;
import static com.github.dangxia.nio.telnet.Code.PROXY_BUFFER;
import static com.github.dangxia.nio.telnet.Code.PROXY_PORT;
import static com.github.dangxia.nio.telnet.Code.SERVER_PORT;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class NioEchoTelnetProxy {
	private class ProxyAttach {

		private final ClientAttach clientAttach;
		private final ServerAttach serverAttach;
		private boolean isReady;

		private ByteBuffer bb;

		public ProxyAttach(SelectionKey clientKey, SelectionKey serverkey) throws IOException {
			this.clientAttach = new ClientAttach(clientKey, this);
			this.serverAttach = new ServerAttach(serverkey, this);
			this.isReady = false;
			this.bb = ByteBuffer.allocate(PROXY_BUFFER);
		}

		public ClientAttach getClientAttach() {
			return clientAttach;
		}

		public ServerAttach getServerAttach() {
			return serverAttach;
		}

		private void finishedReadFromClient() {
			this.clientAttach.disableRead().disableWrite();
			this.serverAttach.enableWrite();
		}

		private void finishedWriteToClient() {
			bb.clear();
			this.clientAttach.enableRead().disableWrite();
		}

		private void finishedWriteToServer() {
			bb = ByteBuffer.allocate(bb.capacity() - HEAD_SIZE);
			this.serverAttach.disableWrite().enableRead();
		}

		private void finishedReadFromServer() {
			bb.flip();
			this.serverAttach.disableWrite().disableWrite();
			this.clientAttach.enableWrite();
		}

		public void close() {
			this.clientAttach.key.cancel();
			try {
				this.clientAttach.key.channel().close();
			} catch (IOException e) {
				LOG.warn("close client channel failed", e);
			}
			this.serverAttach.key.cancel();
			try {
				this.serverAttach.key.channel().close();
			} catch (IOException e) {
				LOG.warn("close server channel failed", e);
			}
		}

		private abstract class PartAttach extends ChannelAttach {
			private final ProxyAttach parent;

			public PartAttach(SelectionKey key, ProxyAttach parent) {
				super(key);
				this.parent = parent;
			}

			public ProxyAttach getParent() {
				return parent;
			}

			public abstract boolean isServerAttach();

			@Override
			public void close() {
				ProxyAttach.this.close();
			}
		}

		private class ServerAttach extends PartAttach {

			public ServerAttach(SelectionKey key, ProxyAttach parent) {
				super(key, parent);
			}

			@Override
			protected int doRead() throws IOException {
				int read = socketChannel.read(bb);
				LOG.info("proxy read from server size: {}", read);
				if (!bb.hasRemaining()) {
					finishedReadFromServer();
				}
				return read;
			}

			@Override
			protected int doWrite() throws IOException {
				int write = socketChannel.write(bb);
				if (!bb.hasRemaining()) {
					finishedWriteToServer();
				}
				LOG.info("proxy write to server size: {}", write);
				return write;
			}

			public void finishConnect() throws IOException {
				socketChannel.finishConnect();
				isReady = true;
				LOG.info("has connect to server");
			}

			@Override
			public boolean isServerAttach() {
				return true;
			}

		}

		private class ClientAttach extends PartAttach {

			public ClientAttach(SelectionKey key, ProxyAttach parent) {
				super(key, parent);
			}

			@Override
			protected int doRead() throws IOException {
				if (!isReady) {
					return 0;
				}
				int read = socketChannel.read(bb);
				if (read == -1) {
					close();
					return -1;
				}
				if (isReadEnd()) {
					finishedReadFromClient();
				}
				return read;
			}

			private boolean isReadEnd() {
				bb.flip();
				if (bb.get(bb.limit() - 2) == '\r' && bb.get(bb.limit() - 1) == '\n') {
					int length = bb.remaining();
					ByteBuffer tmp = ByteBuffer.allocate(length + HEAD_SIZE);
					tmp.putInt(length);
					tmp.put(bb);
					bb = tmp;
					bb.flip();
					return true;
				}
				bb.compact();
				if (!bb.hasRemaining()) {
					ByteBuffer tmp = ByteBuffer.allocate(bb.capacity() * 2);
					bb.flip();
					tmp.put(bb);
					bb = tmp;
				}
				return false;
			}

			@Override
			protected int doWrite() throws IOException {
				int write = socketChannel.write(bb);
				if (!bb.hasRemaining()) {
					finishedWriteToClient();
				}
				LOG.info("write to client size: {}", write);
				return 0;
			}

			@Override
			public boolean isServerAttach() {
				return false;
			}

		}

	}

	protected final static Logger LOG = LoggerFactory.getLogger(NioEchoTelnetProxy.class);

	private final Selector selector;
	private ServerSocketChannel serverSocketChannel;

	public NioEchoTelnetProxy() throws IOException {
		selector = Selector.open();
	}

	public void start() throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(PROXY_PORT));
		serverSocketChannel.configureBlocking(false);

		serverSocketChannel.register(selector, OP_ACCEPT);

		LOG.info("proxy started");
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
					} else if (key.isConnectable()) {
						connect(key);
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
		System.out.println("proxy shutdown");

	}

	private void connect(SelectionKey key) throws IOException {
		((ProxyAttach.ServerAttach) key.attachment()).finishConnect();
	}

	private void write(SelectionKey key) throws IOException {
		ProxyAttach.PartAttach attach = (ProxyAttach.PartAttach) key.attachment();
		attach.write();
	}

	private void read(SelectionKey key) throws IOException {
		ProxyAttach.PartAttach attach = (ProxyAttach.PartAttach) key.attachment();
		attach.read();
	}

	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setTcpNoDelay(true);
		socketChannel.socket().setKeepAlive(true);

		SelectionKey clientKey = socketChannel.register(selector, OP_READ);
		SelectionKey serverkey = connectServer();
		ProxyAttach attach = new ProxyAttach(clientKey, serverkey);
		clientKey.attach(attach.getClientAttach());
		serverkey.attach(attach.getServerAttach());

		LOG.info("new conn from telnet");
	}

	private SelectionKey connectServer() throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(SERVER_PORT));
		return socketChannel.register(selector, OP_CONNECT);
	}

}
