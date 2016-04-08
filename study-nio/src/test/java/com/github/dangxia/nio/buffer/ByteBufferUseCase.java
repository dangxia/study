package com.github.dangxia.nio.buffer;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ByteBufferUseCase {
	@Test
	public void test() throws IOException {
		SeekableByteChannel c = mock(SeekableByteChannel.class);
		when(c.read((ByteBuffer) any())).thenAnswer(ReadAnswer.write(10)).thenAnswer(ReadAnswer.write(5));
		when(c.write((ByteBuffer) any())).thenAnswer(WriteAnswer.write(5)).thenAnswer(WriteAnswer.write(5));

		ByteBuffer bb = ByteBuffer.allocate(30);
		c.read(bb);

		assertEquals(bb.remaining(), 20);

		bb.flip();

		assertEquals(bb.remaining(), 10);

		c.write(bb);

		assertEquals(bb.remaining(), 5);

		bb.compact();

		assertEquals(bb.position(), 5);
		assertEquals(bb.remaining(), 25);

		bb.flip();
		c.write(bb);
		assertEquals(bb.remaining(), 0);

		bb.clear();

		c.read(bb);
		c.read(bb);

		assertEquals(bb.remaining(), 20);
		
		bb.flip();
		
		c.write(bb);
		
		assertEquals(bb.remaining(), 5);
		
		bb.clear();
		
		assertEquals(bb.remaining(), 30);
	}

	private static class ReadAnswer implements Answer<Integer> {
		private final int times;

		public ReadAnswer(final int times) {
			this.times = times;
		}

		@Override
		public Integer answer(InvocationOnMock invocation) throws Throwable {
			ByteBuffer bb = (ByteBuffer) (invocation.getArguments()[0]);
			for (int i = 0; i < times; i++) {
				bb.put((byte) i);
			}
			return times;
		}

		public static ReadAnswer write(int times) {
			return new ReadAnswer(times);
		}
	}

	private static class WriteAnswer implements Answer<Integer> {
		private final int times;

		public WriteAnswer(final int times) {
			this.times = times;
		}

		@Override
		public Integer answer(InvocationOnMock invocation) throws Throwable {
			ByteBuffer bb = (ByteBuffer) (invocation.getArguments()[0]);
			for (int i = 0; i < times; i++) {
				bb.get();
			}
			return times;
		}

		public static WriteAnswer write(int times) {
			return new WriteAnswer(times);
		}
	}
}
