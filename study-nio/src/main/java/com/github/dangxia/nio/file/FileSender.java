package com.github.dangxia.nio.file;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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

public class FileSender {

	/**
	 * 当写缓存区满时:不触发OP_WRITE channel.write写直到写缓存区满,立即返回
	 * 如果channel被client关闭,channel.write 报java.io.IOException: Connection reset
	 * by peer
	 */
	private static final Logger LOG = LoggerFactory.getLogger(FileSender.class);

	private final Selector selector;
	private final RandomAccessFile r;

	private ServerSocketChannel serverSocketChannel;

	public FileSender(File file) throws IOException {
		this.selector = Selector.open();
		this.r = new RandomAccessFile(file, "r");
	}

	public void start() throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(new InetSocketAddress(9999));
		serverSocketChannel.configureBlocking(false);

		serverSocketChannel.register(selector, OP_ACCEPT);

		LOG.info("started");
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
				close(key);
			}
		}
		LOG.info("shutdown");
	}

	private void read(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer bb = ByteBuffer.allocate(64);
		int read = channel.read(bb);
		LOG.info("read: {}", read);

		if (read == -1) {
			close(key);
		}
	}

	private void close(SelectionKey key) throws IOException {
		key.cancel();
		key.channel().close();

		LOG.info("channel close");
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		long offset = getOffset(key);
		long remain = r.length() - offset;
		long write = r.getChannel().transferTo(offset, remain, channel);

		LOG.info("offset: {},write: {}", offset, write);

		setOffset(key, offset + write);
	}

	private long getOffset(SelectionKey key) throws IOException {
		Long offset = (Long) key.attachment();
		if (offset == null) {
			return 0l;
		}
		return offset.longValue();
	}

	private void setOffset(SelectionKey key, long offset) throws IOException {
		if (r.length() == offset) {
			// key.interestOps(key.interestOps() & ~OP_WRITE | OP_READ);
			close(key);
		} else {
			key.attach(offset);
		}
	}

	private void accept(SelectionKey key) throws IOException {
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setTcpNoDelay(true);
		socketChannel.socket().setKeepAlive(true);

		socketChannel.register(selector, OP_WRITE);
		LOG.info("new conn registered");
	}

	public static void main(String[] args) throws IOException {
		File file = new File("/home/hexh/download/eclipse-java-mars-1-linux-gtk-x86_64.tar.gz");
		FileSender sender = new FileSender(file);
		sender.start();
	}

}
