package lang.common;

import java.io.PrintStream;

public class SourceLocation implements Comparable<SourceLocation> {
	public static final String BUILTIN_FILENAME_PLACEHOLDER = "<BUILTIN>";
	private String file = "UNKNOWN";
	private int startLine, endLine, startColumn, endColumn;

	public static SourceLocation UNKNOWN = new SourceLocation() {
			@Override
			public String toString() { return ("UNKNOWN"); };
		};

	public static SourceLocation BUILTIN = new SourceLocation() {
			@Override
			public String toString() { return ("BUILTIN"); };
		};

	private SourceLocation() {}

	public SourceLocation(String fileInit,
			      int startLineInit, int startColumnInit,
			      int endLineInit, int endColumnInit) {
		this.file = fileInit;
		this.startLine = startLineInit;
		this.startColumn = startColumnInit;
		this.endLine = endLineInit;
		this.endColumn = endColumnInit;
	}

	public void print(PrintStream out) {
		out.print("!");
		out.print(file);
		out.print(",");
		out.print(startLine + ":" + startColumn + "," +  endLine + ":" + endColumn);
	}

	public int
	forCodeProberAtStart() {
		return (this.getStartLine() << 12) | (this.getStartColumn());
	}

	public int
	forCodeProberAtEnd() {
		return (this.getEndLine() << 12) | (this.getEndColumn());
	}

	public int
	forCodeProberinMiddle() {
		int line = (this.getStartLine() + this.getEndLine() + 1) >> 1;
		int column = (this.getStartColumn() + this.getEndColumn() + 1) >> 1;
		return (line << 12) | (column);
	}

	public boolean
	isReal() {
		return !(this == UNKNOWN || this == BUILTIN);
	}

	public int
	getStartLine() {
		return this.startLine;
	}

	public int
	getStartColumn() {
		return this.startColumn;
	}

	public int
	getEndLine() {
		return this.endLine;
	}

	public int
	getEndColumn() {
		return this.endColumn;
	}

	@Override
	public String toString() {
		return this.file + "["
			+ this.startLine + ":" + this.startColumn
			+ "-"
			+ this.endLine + ":" + this.endColumn
			+ "]";
	}

	@Override
	public int compareTo(SourceLocation r) {
		if (!this.file.equals(r.file))
			return this.file.compareTo(r.file);

		if (this.startLine != r.startLine)
			return Integer.compare(this.startLine, r.startLine);

		if (this.startColumn != r.startColumn)
			return Integer.compare(this.startColumn, r.startColumn);

		if (this.endLine != r.endLine)
			return Integer.compare(this.endLine, r.endLine);

		if (this.endColumn != r.endColumn)
			return Integer.compare(this.endColumn, r.endColumn);

		return 0;
	}
}
