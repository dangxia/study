package com.github.dangxia.nio.telnet;

public class SplitAdaptors {
	public interface SplitAdaptor {
		byte[][] split(byte[] playload);
	}

	public static class NoSplit implements SplitAdaptor {
		@Override
		public byte[][] split(byte[] playload) {
			return new byte[][] { playload };
		}
	}

	public static class MidSplit implements SplitAdaptor {
		@Override
		public byte[][] split(byte[] playload) {
			int len = playload.length;
			if (len < 2) {
				return new byte[][] { playload };
			}
			int mid = len / 2;

			byte[] head = new byte[mid];
			byte[] tail = new byte[len - mid];

			System.arraycopy(playload, 0, head, 0, head.length);
			System.arraycopy(playload, mid, tail, 0, tail.length);
			return new byte[][] { head, tail };
		}
	}

	public static void main(String[] args) {
		byte[] playload = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
		byte[][] bb = Splitor.BOTH.split(playload);
		for (byte[] bs : bb) {
			for (byte b : bs) {
				System.out.println(b);
			}
			System.out.println("------------");
		}
	}

	public static enum Splitor implements SplitAdaptor {
		FULL, HEAD, BODY, BOTH;

		private static final SplitAdaptor noSplit = new NoSplit();
		private static final SplitAdaptor midSplit = new MidSplit();

		@Override
		public byte[][] split(byte[] playload) {
			switch (this) {
			case FULL:
				return new byte[][] { playload };
			case HEAD:
				return split(playload, midSplit, noSplit);
			case BODY:
				return split(playload, noSplit, midSplit);
			case BOTH:
				return split(playload, midSplit, midSplit);
			}
			return null;
		}

		private static byte[][] split(byte[] playload, SplitAdaptor headSplit, SplitAdaptor bodySplit) {
			byte[] head = new byte[4];
			byte[] body = new byte[playload.length - 4];
			System.arraycopy(playload, 0, head, 0, head.length);
			System.arraycopy(playload, 4, body, 0, body.length);

			byte[][] headParts = headSplit.split(head);
			byte[][] bodyParts = bodySplit.split(body);

			byte[][] result = new byte[headParts.length + bodyParts.length][];
			System.arraycopy(headParts, 0, result, 0, headParts.length);
			System.arraycopy(bodyParts, 0, result, headParts.length, bodyParts.length);
			return result;
		}
	}

}
