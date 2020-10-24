package lang.common;

import java.io.PrintStream;

public class SourceLocation {
	private String file = "UNKNOWN";
	private int startLine, endLine, startColumn, endColumn;

	public static SourceLocation UNKNOWN = new SourceLocation();

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
}
