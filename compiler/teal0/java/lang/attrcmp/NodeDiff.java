package lang.attrcmp;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import lang.attrcmp.AttributeSummary.Attribute;
import lang.attrcmp.AttributeSummary.Value;


public class NodeDiff implements Iterable<Map.Entry<Attribute, SingleDiff>> {
	private LinkedHashMap<Attribute, SingleDiff> diffs;

	public static final NodeDiff EMPTY = new NodeDiff() {
		@Override public void add(NodeDiff other) { throw new UnsupportedOperationException(); }
	};

	public NodeDiff() {
	}

	@Override
	public Iterator<Map.Entry<Attribute, SingleDiff>> iterator() {
		return this.diffs().entrySet().iterator();
	}

	private LinkedHashMap<Attribute, SingleDiff> diffs() {
		if (this.diffs == null) {
			this.diffs = new LinkedHashMap<>();
		}
		return this.diffs;
	}

	protected NodeDiff(Attribute attr, SingleDiff d) {
		this.diffs().put(attr, d);
	}

	public void add(NodeDiff other) {
		if (other.isEmpty()) {
			return;
		}
		for (Attribute attr : other.diffs().keySet()) {
			this.diffs().put(attr, other.diffs.get(attr));
		}
	}

	public static NodeDiff unexpected(Attribute attr, Value<?> val) {
		if (!attr.trackUnexpected()) {
			return EMPTY;
		}
		return new NodeDiff(attr, new SingleDiff(DiffType.UNEXPECTED,
							 "unexpected:",
							 val.isNull() ? null : val.getValue())
			);
	}

	public static NodeDiff missing(Attribute attr, Value<?> val) {
		if (!attr.trackMissing()) {
			return EMPTY;
		}
		return new NodeDiff(attr, new SingleDiff(DiffType.MISSING,
							 "missing:",
							 val.isNull() ? null : val.getValue())
			);
	}

	public static NodeDiff make(Attribute attr, Object[] delta) {
		return new NodeDiff(attr, new SingleDiff(DiffType.DELTA, delta));
	}

	public boolean isEmpty() {
		return (this.diffs == null
			|| this.diffs.isEmpty());
	}
}
