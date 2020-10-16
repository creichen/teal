package lang.ir;

import java.io.PrintStream;

public class SourceLocation {
	private String file = "UNKNOWN";
	private int startLine, endLine, startColumn, endColumn;

	public static SourceLocation UNKNOWN = new SourceLocation();

	public static SourceLocation fromASTNode(lang.ast.ASTNode n) {
		SourceLocation s = new SourceLocation();
		s.file = n.sourceFile();
		s.startLine = n.startLine();
		s.endLine = n.endLine();
		s.startColumn = n.startColumn();
		s.endColumn = n.endColumn();
		return s;
    }

	public void print(PrintStream out) {
		out.print("!");
		out.print(file);
		out.print(",");
		out.print(startLine + ":" + startColumn + "," +  endLine + ":" + endColumn);
    }
}
