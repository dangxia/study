package com.github.dangxia.nio.telnet;

import static com.github.dangxia.nio.telnet.Code.PROXY_PORT;
import static com.github.dangxia.nio.telnet.Code.SERVER_PORT;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dangxia.nio.telnet.attach.ChannelAttach;
import com.github.dangxia.nio.telnet.attach.ProxyAttach;
import com.google.common.base.Throwables;

public class NioEchoTelnetProxy {
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
					if (key.isValid()) {
						if (key.isAcceptable()) {
							accept(key);
						} else if (key.isConnectable()) {
							connect(key);
						} else {
							if (key.isValid() && key.isWritable()) {
								write(key);
							}
							if (key.isValid() && key.isReadable()) {
								read(key);
							}
						}
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
		((ChannelAttach) key.attachment()).finishConnect();
	}

	private void write(SelectionKey key) throws IOException {
		ChannelAttach attach = (ChannelAttach) key.attachment();
		attach.write();
	}

	private void read(SelectionKey key) throws IOException {
		ChannelAttach attach = (ChannelAttach) key.attachment();
		attach.read();
	}

	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setTcpNoDelay(true);
		socketChannel.socket().setKeepAlive(true);

		SelectionKey clientKey = socketChannel.register(selector, OP_WRITE);
		SelectionKey serverkey = connectServer();
		ProxyAttach attach = new ProxyAttach(clientKey, serverkey);
		clientKey.attach(attach.getClientAttach());
		serverkey.attach(attach.getServerAttach());
	}

	private SelectionKey connectServer() throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(SERVER_PORT));
		return socketChannel.register(selector, OP_CONNECT);
	}

}
