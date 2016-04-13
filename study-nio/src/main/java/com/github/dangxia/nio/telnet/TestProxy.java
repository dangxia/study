package com.github.dangxia.nio.telnet;

import java.io.IOException;

public class TestProxy {
	public static void main(String[] args) {
		new Thread() {
			@Override
			public void run() {
				try {
					new NioEchoServer().start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();

		new Thread() {
			@Override
			public void run() {
				try {
					new NioEchoTelnetProxy().start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
