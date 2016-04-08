package com.github.dangxia;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.task.IMetricsContext;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import storm.trident.TridentTopology;
import storm.trident.operation.TridentCollector;
import storm.trident.operation.TridentOperationContext;
import storm.trident.spout.IOpaquePartitionedTridentSpout;
import storm.trident.spout.ISpoutPartition;
import storm.trident.state.State;
import storm.trident.state.StateFactory;
import storm.trident.state.StateUpdater;
import storm.trident.topology.TransactionAttempt;

public class TestTridentTopology {

	private static final Logger LOG = LoggerFactory.getLogger(TestTridentTopology.class);

	public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException {
		TridentTopology topology = new TridentTopology();
		topology.newStream("spout1", new TestBaseRichSpout()).shuffle()
				.partitionPersist(new StateFactory1(), new StateUpdater1()).newValuesStream().shuffle()
				.partitionPersist(new StateFactory2(), new StateUpdater2());

		LocalCluster cluster = new LocalCluster();
		Config config = new Config();
		config.setMaxSpoutPending(1);
		config.setMaxTaskParallelism(1);
		config.setMessageTimeoutSecs(60);
		config.setNumWorkers(1);
		config.setDebug(true);
		StormTopology stormTopology = topology.build();
		// StormTopologyPrinter.print(stormTopology);
		cluster.submitTopology("sjldfjlsjd", config, stormTopology);
	}

	public static class State1 implements State {

		private final String name;

		public State1(String name) {
			this.name = name;
		}

		@Override
		public void beginCommit(Long txid) {
			LOG.info("state {} beginCommit with txid {}", name, txid);
		}

		@Override
		public void commit(Long txid) {
			LOG.info("state {} commit with txid {}", name, txid);
		}

	}

	public static class StateFactory1 implements StateFactory {

		@Override
		public State makeState(Map conf, IMetricsContext metrics, int partitionIndex, int numPartitions) {
			return new State1("11111");
		}

	}

	public static class StateUpdater1 implements StateUpdater {

		@Override
		public void prepare(Map conf, TridentOperationContext context) {
		}

		@Override
		public void cleanup() {
		}

		@Override
		public void updateState(State state, List tuples, TridentCollector collector) {
			sleep(1);
			collector.emit(new Values("value1111111"));
		}

	}

	public static class StateFactory2 implements StateFactory {

		@Override
		public State makeState(Map conf, IMetricsContext metrics, int partitionIndex, int numPartitions) {
			return new State1("22222222222");
		}

	}

	public static class StateUpdater2 implements StateUpdater {

		@Override
		public void prepare(Map conf, TridentOperationContext context) {
		}

		@Override
		public void cleanup() {
		}

		@Override
		public void updateState(State state, List tuples, TridentCollector collector) {
			sleep(1);
			collector.emit(new Values("value1111111"));
		}

	}

	public static class LongSpoutPartition implements ISpoutPartition, Comparable<LongSpoutPartition> {
		private final long v;

		public LongSpoutPartition(long v) {
			this.v = v;
		}

		@Override
		public String getId() {
			return String.valueOf(v);
		}

		@Override
		public int compareTo(LongSpoutPartition o) {
			return new Long(this.v).compareTo(o.v);
		}

	}

	public static class TestBaseRichSpout implements IOpaquePartitionedTridentSpout {

		@Override
		public Emitter getEmitter(Map conf, TopologyContext context) {
			return new Emitter<List<LongSpoutPartition>, LongSpoutPartition, Long>() {

				@Override
				public Long emitPartitionBatch(TransactionAttempt tx, TridentCollector collector,
						LongSpoutPartition partition, Long lastPartitionMeta) {
					for (int i = 0; i < 4; i++) {
						collector.emit(new Values(i));
					}
					return 0l;
				}

				@Override
				public void refreshPartitions(List<LongSpoutPartition> partitionResponsibilities) {

				}

				@Override
				public List<LongSpoutPartition> getOrderedPartitions(List<LongSpoutPartition> allPartitionInfo) {
					List<LongSpoutPartition> _new = Lists.newArrayList(allPartitionInfo);
					Collections.sort(_new);
					return _new;
				}

				@Override
				public void close() {

				}
			};
		}

		@Override
		public Coordinator getCoordinator(Map conf, TopologyContext context) {
			return new Coordinator<List<LongSpoutPartition>>() {

				@Override
				public boolean isReady(long txid) {
					return true;
				}

				@Override
				public List<LongSpoutPartition> getPartitionsForBatch() {
					return Lists.newArrayList(new LongSpoutPartition(1l));
				}

				@Override
				public void close() {

				}
			};
		}

		@Override
		public Map getComponentConfiguration() {
			return null;
		}

		@Override
		public Fields getOutputFields() {
			return new Fields("word");
		}

	}

	public static void sleep(int second) {
		try {
			TimeUnit.SECONDS.sleep(second);
		} catch (InterruptedException e) {
			Throwables.propagate(e);
		}
	}
}
