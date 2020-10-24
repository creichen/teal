package lang.common;

import java.io.PrintStream;

public class SourceLocation {
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

	@Override
	public String toString() {
		return this.file + "["
			+ this.startLine + ":" + this.startColumn
			+ "-"
			+ this.endLine + ":" + this.endColumn
			+ "]";
	}
}
