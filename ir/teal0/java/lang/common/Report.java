package lang.common;

import java.util.ArrayList;

/**
 * Report class.  Use this to report information about the AST.
 */
public abstract class Report {
	private SourceLocation[] node_locations;
	private String kind;
	private String explanation;
	private String code_prober_format;

	/**
	 * Creates a fresh report
	 *
	 * @param kind The kind of report to make; used as prefix to stdout output or as default message for codeprober hover
	 * @param codeProberFormat Prefix string for codeProber.  The following nodes will be appended.
	 * @param nodes ASTNodes of significance to the report, with the most prominent location (or otherwise the first location) first
	 */
	protected <ASTNode extends WithSourceLocation>
	Report(String kind, String codeProberFormat, ASTNode node0, ASTNode ... nodes) {
		ArrayList<SourceLocation> locs = new ArrayList<>();
		if (node0 != null) {
			locs.add(node0.sourceLocation());
		}
		for (ASTNode n : nodes) {
			if (n != null) {
				locs.add(n.sourceLocation());
			}
		}
		this.node_locations = locs.toArray(new SourceLocation[locs.size()]);
		this.kind = kind;
		this.code_prober_format = codeProberFormat;
	}

	protected Report(String kind, String codeProberFormat, SourceLocation ... locs) {
		this.node_locations = locs;
		this.kind = kind;
		this.code_prober_format = codeProberFormat;
	}

	/**
	 * Set the optional explanation string (for your own debugging purposes)
	 *
	 * @param explanation The explanation to attach to this report
	 * @return this
	 */
	public Report withExplanation(String explanation) {
		this.explanation = explanation;
		return this;
	}

	public String
	getExplanation() {
		return this.explanation;
	}

	public String toString() {
		String s = this.kind;
		if (this.node_locations.length == 0) {
			s += "\t" + SourceLocation.UNKNOWN;
		}
		for (SourceLocation loc : this.node_locations) {
			s += "\t" + loc;
		}
		if (this.explanation != null) {
			s += "\t" + this.explanation;
		}
		return s;
	}

	public String toCodeProberString() {
		String s = this.code_prober_format + "@";
		SourceLocation loc = (this.node_locations.length == 0) ? SourceLocation.UNKNOWN : this.node_locations[0];

		s += (loc.forCodeProberAtStart()
		      + ";" +
		      loc.forCodeProberAtEnd());
		if (this.explanation != null) {
			s += ";" + this.explanation;
		} else {
			s += ";" + this.kind;
		}
		return s;
	}

	/**
	 * Source location, for testing
	 */
	public SourceLocation
	getFirstSrcLoc() {
		if (this.node_locations.length == 0) {
			return SourceLocation.UNKNOWN;
		}
		return this.node_locations[0];
	}
}
