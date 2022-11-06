package lang.common;

import java.util.ArrayList;

/**
 * Report class.  Use this to report information about the AST.
 */
public abstract class Report {
	private SourceLocation[] node_locations;
	private String kind;
	private String explanation;
	private CodeProber code_prober_format;

	public enum CodeProber {
		ERR,	// red squiggles
		WARN,	// yellow squiggles
		INFO,	// blue squiggles
		HINT	// grey dots
	}

	/**
	 * Creates a fresh report
	 *
	 * @param kind The kind of report to make; used as prefix to stdout output or as default message for codeprober hover
	 * @param codeProberFormat Marker codeProber.
	 * @param nodes ASTNodes of significance to the report, with the most prominent location (or otherwise the first location) first
	 */
	protected <ASTNode extends WithSourceLocation>
	Report(String kind, CodeProber codeProberFormat, ASTNode node0, ASTNode ... nodes) {
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

	protected Report(String kind, CodeProber codeProberFormat, SourceLocation ... locs) {
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

	/**
	 * code-prober magic extraction method
	 */
	public String getCodeProberReportString() {
	    return this.toCodeProberString();
	}

	/**
	 * hover text
	 */
	protected String getCodeProberExplanation() {
		if (this.explanation != null) {
			return this.explanation;
		}
		return this.kind;
	}

	protected String getCodeProberPrefix() {
		return this.code_prober_format.toString();
	}

	public String toCodeProberString() {
		String s = this.getCodeProberPrefix() + "@";
		SourceLocation loc = (this.node_locations.length == 0) ? SourceLocation.UNKNOWN : this.node_locations[0];

		s += (loc.forCodeProberAtStart()
		      + ";" +
		      loc.forCodeProberAtEnd()
		      + ";" +
		      this.getCodeProberExplanation());
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


	/**
	 * Report.Visual is a report that in an IDE does does not have hover text, only CSS styling
	 */
	public static class Visual extends Report {
		private String styling;

		/**
		 * Creates a fresh report
		 *
		 * @param kind The kind of report to make; used as prefix to stdout output or as default message for codeprober hover
		 * @param styling A CSS class, elaborated in <tt>Compiler.CodeProber_report_styles</tt> (or multiple comma-separated classes within the same string)
		 * @param nodes ASTNodes of significance to the report, with the most prominent location (or otherwise the first location) first
		 */
		protected <ASTNode extends WithSourceLocation>
		Visual(String kind, String styling, ASTNode node0, ASTNode ... nodes) {
			super(kind, null, node0, nodes);
			this.styling = styling;
		}

		@Override
		protected String getCodeProberExplanation() {
			// For STYLE messages, we don't have hover text, instead, the last entry is a CSS spec name
			return this.styling;
		}

		@Override
		protected String getCodeProberPrefix() {
			return "STYLE";
		}
	}
}