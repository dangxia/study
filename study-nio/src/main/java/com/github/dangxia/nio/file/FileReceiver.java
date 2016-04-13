package com.github.dangxia.nio.file;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class FileReceiver {
	private static final Logger LOG = LoggerFactory.getLogger(FileReceiver.class);
	private final SocketChannel socketChannel;
	private final Selector selector;
	private final File file;

	public FileReceiver(File file) throws IOException {
		this.file = file;
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(9999));
		selector = Selector.open();

		socketChannel.register(selector, OP_CONNECT | OP_READ);
	}

	public void start() {
		while (selector.isOpen() && !Thread.interrupted()) {
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
							LOG.info("finishConnect");
						} else if (key.isReadable()) {
							read(key);
						}
					}
				}
			} catch (Exception e) {
				Throwables.propagate(e);
			}
		}
	}

	private void read(SelectionKey key) throws IOException {
		FileChannelHolder holder = (FileChannelHolder) key.attachment();
		if (holder == null) {
			holder = new FileChannelHolder();
			key.attach(holder);
		}
		long position = holder.getPosition();
		// socketChannel -1 still return 0
		long read = holder.fileChannel.transferFrom(socketChannel, position, 1024);
		LOG.info("read:{}", read);

		if (read == 0) {
			holder.r.close();
			holder.fileChannel.close();
			key.cancel();
			key.channel().close();

			selector.close();
		} else {
			holder.addRead(read);
		}
	}

	private class FileChannelHolder {
		private final FileChannel fileChannel;
		private final RandomAccessFile r;
		private long position;

		public FileChannelHolder() throws IOException {
			r = new RandomAccessFile(file, "rw");
			fileChannel = r.getChannel();
			this.position = 0l;
		}

		public long getPosition() {
			return position;
		}

		public void addRead(long read) {
			this.position += read;
		}
	}

	public static void main(String[] args) throws IOException {
		FileReceiver receiver = new FileReceiver(
				new File("/tmp/hexh/test/eclipse-java-mars-1-linux-gtk-x86_64.tar.gz"));
		receiver.start();
	}
}
