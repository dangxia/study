package com.github.dangxia;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import backtype.storm.generated.Bolt;
import backtype.storm.generated.GlobalStreamId;
import backtype.storm.generated.Grouping;
import backtype.storm.generated.SpoutSpec;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.StreamInfo;

public class StormTopologyPrinter {

	private static GsonBuilder gb = new GsonBuilder();
	private static Gson gson;

	static {
		gb.setPrettyPrinting();
		gson = gb.create();
	}

	public static class StreamInfoDetail implements Serializable {
		public String streamId;
		public List<String> outFields;
		public boolean isDirect;

		public StreamInfoDetail(Entry<String, StreamInfo> entry) {
			this.streamId = entry.getKey();
			this.outFields = entry.getValue().get_output_fields();
			this.isDirect = entry.getValue().is_direct();
		}
	}

	public static class InputDetail implements Serializable {
		public String streamId;
		public String componentId;
		public Object grouping;

		public InputDetail(Entry<GlobalStreamId, Grouping> entry) {
			this.streamId = entry.getKey().get_streamId();
			this.componentId = entry.getKey().get_componentId();
			this.grouping = entry.getValue().getSetField();

		}
	}

	private static void printSpouts(Map<String, SpoutSpec> spouts) {
		for (Entry<String, SpoutSpec> entry : spouts.entrySet()) {
			print(entry.getKey(), entry.getValue());
		}
	}

	private static void print(String spoutId, SpoutSpec spec) {
		Map<Object, Object> data = Maps.newLinkedHashMap();
		List<StreamInfoDetail> streams = Lists.newArrayList();
		data.put("spoutId", spoutId);
		data.put("streams", streams);

		for (Entry<String, StreamInfo> entry : spec.get_common().get_streams().entrySet()) {
			streams.add(new StreamInfoDetail(entry));
		}

		System.out.println(gson.toJson(data));
	}

	private static void printBolts(Map<String, Bolt> bolts) {
		for (Entry<String, Bolt> entry : bolts.entrySet()) {
			print(entry.getKey(), entry.getValue());
		}
	}

	private static void print(String key, Bolt bolt) {
		Map<Object, Object> data = Maps.newLinkedHashMap();
		List<StreamInfoDetail> streams = Lists.newArrayList();
		List<InputDetail> inputs = Lists.newArrayList();
		data.put("boltId", key);
		data.put("streams", streams);
		data.put("inputs", inputs);

		for (Entry<String, StreamInfo> entry : bolt.get_common().get_streams().entrySet()) {
			streams.add(new StreamInfoDetail(entry));
		}

		for (Entry<GlobalStreamId, Grouping> entry : bolt.get_common().get_inputs().entrySet()) {
			inputs.add(new InputDetail(entry));
		}

		System.out.println(gson.toJson(data));
	}

	public static void print(StormTopology stormTopology) {
		printSpouts(stormTopology.get_spouts());
		printBolts(stormTopology.get_bolts());
		// for (Entry<String, SpoutSpec> entry :
		// stormTopology.get_spouts().entrySet()) {
		// System.out.print(entry.getKey());
		// StringBuffer sb = new StringBuffer("\t{\n");
		// for (Entry<String, StreamInfo> entry2 :
		// entry.getValue().get_common().get_streams().entrySet()) {
		// sb.append("\tstreamId:");
		// sb.append(entry2.getKey());
		// sb.append("\tfields:");
		// sb.append(entry2.getValue().get_output_fields());
		// sb.append("\tdirected:");
		// sb.append(entry2.getValue().is_direct());
		// sb.append("\n");
		// }
		// sb.append("}\n");
		// System.out.println(sb.toString());
		// }

		// for (Entry<String, Bolt> entry :
		// stormTopology.get_bolts().entrySet()) {
		// System.out.print(entry.getKey());
		// StringBuffer sb = new StringBuffer("\noutput:\n{\n");
		// for (Entry<String, StreamInfo> entry2 :
		// entry.getValue().get_common().get_streams().entrySet()) {
		// sb.append("\tstreamId:");
		// sb.append(entry2.getKey());
		// sb.append("\tfields:");
		// sb.append(entry2.getValue().get_output_fields());
		// sb.append("\tdirected:");
		// sb.append(entry2.getValue().is_direct());
		// sb.append("\n");
		// }
		// sb.append("}\n");
		// System.out.print(sb.toString());
		//
		// sb = new StringBuffer("\ninput:\n{\n");
		// for (Entry<GlobalStreamId, Grouping> entry2 :
		// entry.getValue().get_common().get_inputs().entrySet()) {
		// sb.append("\tstreamId:");
		// sb.append(entry2.getKey().get_streamId());
		// sb.append("\tcomponentId:");
		// sb.append(entry2.getKey().get_componentId());
		// sb.append("\n");
		// }
		// sb.append("}\n");
		// System.out.print(sb.toString());
		// System.out.println("--------------------------------------");
		// }
	}

}
