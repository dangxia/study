package com.github.dangxia.nio.telnet;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ChannelAttach {
	protected final static Logger LOG = LoggerFactory.getLogger(ChannelAttach.class);

	protected final SelectionKey key;
	protected final SocketChannel socketChannel;

	public ChannelAttach(SelectionKey key) {
		this.key = key;
		this.socketChannel = (SocketChannel) key.channel();
		key.attach(this);
	}

	protected abstract int doRead() throws IOException;

	protected abstract int doWrite() throws IOException;

	public int write() {
		try {
			return doWrite();
		} catch (IOException e) {
			LOG.warn("chanell write failed", e);
			close();
		}
		return -2;
	}

	public int read() {
		try {
			return doRead();
		} catch (IOException e) {
			LOG.warn("chanell read failed", e);
			close();
		}
		return -2;
	}

	public ChannelAttach enableWrite() {
		key.interestOps(key.interestOps() | OP_WRITE);
		return this;
	}

	public ChannelAttach disableWrite() {
		key.interestOps(key.interestOps() & ~OP_WRITE);
		return this;
	}

	public ChannelAttach enableRead() {
		key.interestOps(key.interestOps() | OP_READ);
		return this;
	}

	public ChannelAttach disableRead() {
		key.interestOps(key.interestOps() & ~OP_READ);
		return this;
	}

	public void close() {
		key.cancel();
		try {
			socketChannel.close();
		} catch (IOException e) {
			LOG.warn("socket close failed", e);
		}
	}
}
