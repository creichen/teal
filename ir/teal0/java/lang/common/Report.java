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
	 * Code-prober magic extraction method
	 */
	public String cpr_getDiagnostic() {
	    return this.toCodeProberString();
	}

	/**
	 * Hover text
	 */
	protected Object cpr_getOutput() {
		return this.getCodeProberExplanation();
	}

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
		return s + this.sourceLocationString();
	}

	protected String sourceLocationString() {
		SourceLocation loc = (this.node_locations.length == 0) ? SourceLocation.UNKNOWN : this.node_locations[0];

		return (loc.forCodeProberAtStart()
			+ ";" +
			loc.forCodeProberAtEnd()
			+ ";" +
			this.getCodeProberExplanation());
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
		protected String cpr_getOutput() {
			// For STYLE messages, we don't have hover text, instead, the last entry is a CSS spec name
			return this.styling;
		}

		@Override
		protected String getCodeProberPrefix() {
			return "STYLE";
		}
	}


        /**
         * Report.FlowEdge is shown as an arrow
         */
        public static class FlowEdge extends Report {
                private String color;
                private String arrowtype;
                private SourceLocation start_pos;
                private SourceLocation end_pos;
		private WithSourceLocation from_node;
		private WithSourceLocation to_node;

                public static <ASTNode extends WithSourceLocation>
                FlowEdge
                plain(ASTNode from, ASTNode to, String color) {
                        return new FlowEdge("LINE-PP", "―", color, from, to);
                }

                public static <ASTNode extends WithSourceLocation>
                FlowEdge
                arrow(ASTNode from, ASTNode to, String color) {
                        // This is probably a CodeProber labelling bug...?
                        return new FlowEdge("LINE-PA", "⟶", color, from, to);
                }

                public static <ASTNode extends WithSourceLocation>
                FlowEdge
                doubleArrow(ASTNode from, ASTNode to, String color) {
                        return new FlowEdge("LINE-AA", "⟷", color, from, to);
                }

		/**
		 * For edges, there is no hover text, so we instead report structural information.
		 */
		@Override
		protected Object cpr_getOutput() {
			return new Object[] {
				this.from_node,
				this.arrowtype,
				this.to_node
			};
		}

                /**
                 * Creates a fresh report
                 *
                 * @param kind The kind of report to make; used as prefix to stdout output or as default message for codeprober hover
                 * @param styling A CSS class, elaborated in <tt>Compiler.CodeProber_report_styles</tt> (or multiple comma-separated classes within the same string)
                 * @param nodes ASTNodes of significance to the report, with the most prominent location (or otherwise the first location) first
                 */
                private <ASTNode extends WithSourceLocation>
                FlowEdge(String arrowtype, String kind, String color, ASTNode from, ASTNode to) {
                        super(kind, null, from, to);
			this.from_node = from;
			this.to_node = to;
                        this.start_pos = from.sourceLocation();
                        this.end_pos = to.sourceLocation();
                        this.arrowtype = arrowtype;
                        this.color = color;
                }

		public WithSourceLocation
		getFrom() {
			return this.from_node;
		}

		public WithSourceLocation
		getTo() {
			return this.to_node;
		}

                @Override
                protected String getCodeProberExplanation() {
                        // For STYLE messages, we don't have hover text, instead, the last entry is a CSS spec name
                        return this.color;
                }

                @Override
                protected String getCodeProberPrefix() {
                        return this.arrowtype;
                }

                @Override
                protected String
                sourceLocationString() {
                        int from_pos;
                        int to_pos;
                        from_pos = this.start_pos.forCodeProberAtStart();
                        to_pos = this.end_pos.forCodeProberAtStart();
                        // // Heuristic edge placement
                        // if (this.start_pos.within(this.end_pos)) {
                        //      // From outer to inner
                        //      from_pos = this.start_pos.forCodeProberLeft();
                        //      to_pos = this.end_pos.forCodeProberLeft();
                        // } else if (this.end_pos.within(this.start_pos)) {
                        //      // from inner to outer
                        //      from_pos = this.start_pos.forCodeProberRight();
                        //      to_pos = this.end_pos.forCodeProberLeft();
                        // } else if (this.start_pos.compareTo(this.end_pos) < 0) {
                        //      // forward through the program
                        //      from_pos = this.start_pos.forCodeProberAtEnd();
                        //      to_pos = this.end_pos.forCodeProberAtStart();
                        // } else {
                        //      // assume start_pos > end_pos, i.e., back-edge
                        //      from_pos = this.start_pos.forCodeProberAtStart();
                        //      to_pos = this.end_pos.forCodeProberAtEnd();
                        // }

                        return (from_pos
                                + ";" +
                                to_pos
                                + ";" +
                                this.getCodeProberExplanation());
                }
        }
}
