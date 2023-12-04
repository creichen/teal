package lang.attrcmp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lang.attrcmp.AttributeSummary.Attribute;

public class Diff implements Iterable<Diff.Individual> {
	private LinkedHashMap<NodeInfo, NodeDiff> diffs = new LinkedHashMap<>();
	private LinkedHashMap<String, NodeInfo> nodeinfo = new LinkedHashMap<>();

	public Diff() {}

	public boolean isEmpty() {
		return this.diffs.isEmpty();
	}

	public int size() {
		return this.all().size();
	}

	public void add(String node_id,
			String node_type,
			NodeDiff diff) {
		if (diff.isEmpty()) {
			return;
		}
		NodeInfo ni = new NodeInfo(node_id, node_type);
		this.diffs.put(ni, diff);
		this.nodeinfo.put(node_id, ni);
	}

	public List<Individual> all() {
		List<Individual> results = new ArrayList<>();
		for (NodeInfo ni : this.diffs.keySet()) {
			NodeDiff ndiff = this.diffs.get(ni);
			for (Map.Entry<Attribute, SingleDiff> sdattr : ndiff) {
				Attribute attr = sdattr.getKey();
				SingleDiff sd = sdattr.getValue();
				results.add(new Individual(ni, attr, sd));
			}
		}
		return results;
	}

	public List<Individual> forNode(String node_id) {
		List<Individual> results = new ArrayList<>();
		NodeInfo ni = this.nodeinfo.get(node_id);
		if (ni != null) {
			NodeDiff ndiff = this.diffs.get(ni);
			for (Map.Entry<Attribute, SingleDiff> sdattr : ndiff) {
				Attribute attr = sdattr.getKey();
				SingleDiff sd = sdattr.getValue();
				results.add(new Individual(ni, attr, sd));
			}
		}
		return results;
	}

	public Iterator<Individual> iterator() {
		return this.all().iterator();
	}

	public Stream<Individual> stream() {
		return this.all().stream();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		for (Individual ir : this) {
			sb.append(ir);
			sb.append('\n');
		}

		return sb.toString();
	}

	static final class NodeInfo {
		public final String id;
		public final String type;
		public NodeInfo(String id, String type) {
			this.id = id;
			this.type = type;
		}

		@Override
		public String toString() {
			return this.id + " " + this.type;
		}

		@Override
		public int hashCode() {
			return this.id.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof NodeInfo) {
				NodeInfo o = (NodeInfo) other;
				return this.id.equals(o.id)
					&& this.type.equals(o.type);
			}
			return false;
		}
	}

	public static class Individual {
		private SingleDiff singlediff;
		private Attribute attr;
		private NodeInfo nodeinfo;
		public Individual(NodeInfo ni, Attribute attr, SingleDiff singlediff) {
			this.nodeinfo = ni;
			this.attr = attr;
			this.singlediff = singlediff;
		}

		public Attribute
		getAttribute() {
			return this.attr;
		}

		public String
		getNodeType() {
			return this.nodeinfo.type;
		}

		public String
		getNodeID() {
			return this.nodeinfo.id;
		}

		public boolean
		hasAttribute(String name) {
			return this.attr.getName().equals(name);
		}

		public boolean
		hasAttribute(Attribute attr) {
			return this.attr.equals(attr);
		}

		public boolean
		hasNodeType(String nodeType) {
			return this.nodeinfo.type.equals(nodeType);
		}

		public boolean
		hasNode(String node_id) {
			return this.nodeinfo.id.equals(node_id);
		}

		public String
		toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(this.singlediff.type.getPrefix());
			sb.append(' ');
			sb.append(this.nodeinfo.toString());
			sb.append(' ');
			sb.append(this.attr.signature());
			sb.append(' ');
			boolean first = true;
			for (Object obj : singlediff.explanation) {
				if (!first) {
					first = true;
				} else {
					sb.append(" ");
				}
				if (obj == null) {
					sb.append("null");
				} else if (obj instanceof String) {
					sb.append((String) obj);
				} else {
					String stringified = null;

					if (obj.getClass().isArray()) {
						if (obj instanceof char[]) {
							stringified = Arrays.toString((char[]) obj);
						} else if (obj instanceof int[]) {
							stringified = Arrays.toString((int[]) obj);
						} else if (obj instanceof short[]) {
							stringified = Arrays.toString((short[]) obj);
						} else if (obj instanceof long[]) {
							stringified = Arrays.toString((long[]) obj);
						} else if (obj instanceof byte[]) {
							stringified = Arrays.toString((byte[]) obj);
						} else if (obj instanceof float[]) {
							stringified = Arrays.toString((float[]) obj);
						} else if (obj instanceof double[]) {
							stringified = Arrays.toString((double[]) obj);
						} else if (obj instanceof boolean[]) {
							stringified = Arrays.toString((boolean[]) obj);
						} else if (obj instanceof Object[]) {
							stringified = Arrays.toString((Object[]) obj);
						}
					}
					if (stringified == null) {
						stringified = obj.toString();
					}
					sb.append(stringified.replace("\\", "\\\\").replace("\n", "\\n"));
				}
			}
			return sb.toString();
		}
	}
}
