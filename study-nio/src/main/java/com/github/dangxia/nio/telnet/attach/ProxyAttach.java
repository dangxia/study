package com.github.dangxia.nio.telnet.attach;

import static com.github.dangxia.nio.telnet.Code.HEAD_SIZE;
import static com.github.dangxia.nio.telnet.Code.PROXY_BUFFER;
import static com.github.dangxia.nio.telnet.Code.SHUTDOWN_BYTES;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dangxia.nio.telnet.SplitAdaptors.Splitor;

public class ProxyAttach {
	protected final static Logger LOG = LoggerFactory.getLogger(ProxyAttach.class);
	private final ClientAttach clientAttach;
	private final ServerAttach serverAttach;
	private boolean isReady;
	private boolean isFinishedReadFromServer;

	private ByteBuffer bb;
	private final ByteBuffer header;
	private int length = 0;

	// 一直维持在写入状态
	private ByteBuffer telnetOut;

	public ProxyAttach(SelectionKey clientKey, SelectionKey serverkey) throws IOException {
		this.clientAttach = new ClientAttach(clientKey);
		this.serverAttach = new ServerAttach(serverkey);
		this.isReady = false;
		this.isFinishedReadFromServer = false;
		this.header = ByteBuffer.allocate(HEAD_SIZE);
		this.bb = ByteBuffer.allocate(PROXY_BUFFER);
		this.telnetOut = ByteBuffer.allocate(PROXY_BUFFER);
		this.queue = new LinkedBlockingQueue<>();
	}

	public ClientAttach getClientAttach() {
		return clientAttach;
	}

	public ServerAttach getServerAttach() {
		return serverAttach;
	}

	private void finishedReadFromClient() {
		bb.flip();
		writeMsgLength(bb.remaining());
		this.clientAttach.disableRead();
		this.serverAttach.enableWrite();
		setDataSpout();
	}

	private void writeMsgLength(int length) {
		header.clear();
		header.putInt(length);
		header.flip();

		this.length = length;
	}

	private void finishedWriteToClient() {
		if (isFinishedReadFromServer) {
			this.clientAttach.enableRead();
			isFinishedReadFromServer = false;
		}
	}

	private void finishedWriteToServer() {
		bb.rewind();
		byte[] bytes = new byte[bb.remaining()];
		bb.get(bytes);
		bb.clear();
		if (Arrays.equals(bytes, SHUTDOWN_BYTES)) {
			Thread.currentThread().interrupt();
		} else {
			this.serverAttach.disableWrite().enableRead();
		}
	}

	private void finishedReadFromServer() {
		bb.flip();
		while (telnetOut.remaining() < bb.remaining()) {
			telnetOut = extend(telnetOut);
		}
		telnetOut.put(bb);
		bb.clear();
		this.isFinishedReadFromServer = true;
		this.serverAttach.disableWrite().disableWrite();
	}

	protected void finishedConnectToServer() throws IOException {
		isReady = true;
		logToTelnet("has connect to server");
		this.clientAttach.enableRead();
	}

	private ByteBuffer extend(ByteBuffer bb) {
		ByteBuffer tmp = ByteBuffer.allocate(bb.capacity() * 2);
		bb.flip();
		tmp.put(bb);
		return tmp;
	}

	private void logToTelnet(String info) {
		info = info + "\r\n";
		byte[] bytes = info.getBytes();
		while (telnetOut.remaining() < bytes.length) {
			telnetOut = extend(telnetOut);
		}
		telnetOut.put(bytes);
	}

	private byte[][] data;
	private final LinkedBlockingQueue<byte[]> queue;
	private volatile int index = 0;
	private final Random r = new Random();

	public void setDataSpout() {
		byte[] playload = new byte[HEAD_SIZE + bb.remaining()];
		System.arraycopy(header.array(), 0, playload, 0, HEAD_SIZE);
		System.arraycopy(bb.array(), 0, playload, HEAD_SIZE, bb.remaining());
		int s = r.nextInt(4);
		Splitor splitor = null;
		if (s == 0) {
			splitor = Splitor.FULL;
		} else if (s == 1) {
			splitor = Splitor.HEAD;
		} else if (s == 2) {
			splitor = Splitor.BODY;
		} else if (s == 3) {
			splitor = Splitor.BOTH;
		}

		data = splitor.split(playload);
		index = 0;
		int interval = r.nextInt(4) + 1;

		logToTelnet("splitor:" + splitor.toString() + " split data " + data.length + " segments ,interval seconds: "
				+ interval);
		startPushThread(interval);
	}

	private void startPushThread(final int interval) {
		new Thread() {
			@Override
			public void run() {
				while (!isFinished()) {
					try {
						TimeUnit.SECONDS.sleep(interval);
						push();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	private void push() {
		if (!isFinished()) {
			queue.offer(data[index]);
			index++;
			logToTelnet("send segment " + index);
		}
	}

	private boolean isFinished() {
		return index >= data.length;
	}

	public void close() {
		LOG.info("proxy close conn");
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

	abstract class PartAttach extends ChannelAttach {
		public PartAttach(SelectionKey key) {
			super(key);
		}

		@Override
		public void close() {
			ProxyAttach.this.close();
		}
	}

	class ServerAttach extends PartAttach {
		public ServerAttach(SelectionKey key) {
			super(key);
		}

		@Override
		protected int doRead() throws IOException {
			int read = socketChannel.read(bb);
			if (read == -1) {
				close();
				return -1;
			}
			if (bb.position() == length) {
				finishedReadFromServer();
			}
			return read;
		}

		private ByteBuffer segment;

		@Override
		protected int doWrite() throws IOException {
			// int write = 0;
			// if (header.hasRemaining()) {
			// write += socketChannel.write(header);
			// if (!header.hasRemaining()) {
			// logToTelnet("write finished header");
			// }
			// }
			// if (!header.hasRemaining()) {
			// write += socketChannel.write(bb);
			// if (!bb.hasRemaining()) {
			// logToTelnet("write msg to server finished");
			// finishedWriteToServer();
			// }
			// }
			// return write;
			int write = 0;
			if (segment == null) {
				byte[] bytes = queue.poll();
				if (bytes != null) {
					segment = ByteBuffer.allocate(bytes.length);
					segment.put(bytes);
					segment.flip();
				}
			}

			if (segment != null) {
				write += socketChannel.write(segment);
				if (!segment.hasRemaining()) {
					segment = null;
					if (isFinished()) {
						logToTelnet("write msg to server finished");
						finishedWriteToServer();
					}
				}
			}

			return write;
		}

		protected void doFinishConnect() throws IOException {
			socketChannel.finishConnect();
			finishedConnectToServer();
		}

	}

	private class ClientAttach extends PartAttach {

		public ClientAttach(SelectionKey key) {
			super(key);
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
			} else if (!bb.hasRemaining()) {
				bb = extend(bb);
			}
			return read;
		}

		private boolean isReadEnd() {
			bb.flip();
			try {
				return bb.get(bb.limit() - 2) == '\r' && bb.get(bb.limit() - 1) == '\n';
			} finally {
				bb.compact();
			}
		}

		@Override
		protected int doWrite() throws IOException {
			telnetOut.flip();
			int write = 0;
			if (telnetOut.hasRemaining()) {
				write = socketChannel.write(telnetOut);
				if (!telnetOut.hasRemaining()) {
					finishedWriteToClient();
				}
			}
			telnetOut.compact();
			return write;
		}

	}

}