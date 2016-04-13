package com.github.dangxia.nio.telnet;

public class Code {
	public static final byte[] CTRL_C_BYTES = new byte[] { -1, -12, -1, -3, 6 };
	public static final byte[] EXIT_BYTES = "exit\r\n".getBytes();
	public static final byte[] SHUTDOWN_BYTES = "shutdown\r\n".getBytes();
	public static final int HEAD_SIZE = 4;
	public static final int SERVER_PORT = 9999;
	public static final int PROXY_PORT = 8888;
	public static final int PROXY_BUFFER = 64;
}
