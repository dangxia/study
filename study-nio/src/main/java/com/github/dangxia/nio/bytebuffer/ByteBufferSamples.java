package com.github.dangxia.nio.bytebuffer;

import java.nio.ByteBuffer;

public class ByteBufferSamples {
	static void byteBuferfAction() {
		ByteBuffer bb = ByteBuffer.allocate(8);
		for (int i = 0; i < 5; i++) {
			bb.put((byte) i);
		}
		System.out.println(bb.remaining());
		bb.flip();

		for (int i = 0; i < 3; i++) {
			bb.get();
		}
		
		bb.compact();
		
		System.out.println(bb.remaining());
	}

	public static void main(String[] args) {
		byteBuferfAction();
	}
}
