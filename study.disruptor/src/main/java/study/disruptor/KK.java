package study.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class KK {

	public static void main(String[] args) throws InterruptedException {
		Disruptor<Event> disruptor = new Disruptor<Event>(new EventFactory(), 1024, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				return new Thread(r);
			}
		});

		disruptor.handleEventsWith(new EventHandler<Event>() {
			public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
				System.out.println(event.v + ",endOfBatch:" + endOfBatch);
				event.v += 10000;
			}
		}, new EventHandler<Event>() {
			public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
				System.out.println(event.v + ",endOfBatch:" + endOfBatch);
				event.v += 20000;
			}
		});
		RingBuffer<Event> ringBuffer = disruptor.start();

		for (int i = 0; i < 100; i++) {
			long seq = ringBuffer.next();
			Event event = ringBuffer.get(seq);
			event.v = i;
			ringBuffer.publish(seq);
			TimeUnit.MILLISECONDS.sleep(10);
		}

		TimeUnit.SECONDS.sleep(1);
		disruptor.shutdown();

//		TimeZone.getTimeZone("PST")

	}


	public static class Event {
		public int v = -1;
	}

	public static class EventFactory implements com.lmax.disruptor.EventFactory<Event> {

		public Event newInstance() {
			return new Event();
		}
	}
}


