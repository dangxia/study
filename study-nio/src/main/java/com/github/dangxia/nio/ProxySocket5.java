package com.github.dangxia.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

public class ProxySocket5 {
	public static void main(String[] args) throws UnknownHostException {
		Proxy proxy = new Proxy(Type.SOCKS, new InetSocketAddress(
				InetAddress.getByName("115.182.210.110"), 10080));
		
		Socket socket = new Socket(proxy);

	}
}
