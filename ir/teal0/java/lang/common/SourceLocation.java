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
	    String maskedFile;
	    String tmpDir = System.getProperty("java.io.tmpdir");
	    // FIXME: should probably only use this if we're using CodeProber
	    if (tmpDir != null && this.file.startsWith(tmpDir)) {
		maskedFile = "CodeProber";
	    } else {
		maskedFile = file;
	    }
	    return maskedFile + "["
		+ this.startLine + ":" + this.startColumn
		+ "-"
		+ this.endLine + ":" + this.endColumn
		+ "]";
	}

	private static int[]
	splitCoord(String coordStr) {
		String[] splits = coordStr.split(":", -1);
		if (splits.length != 2) {
			return null;
		}
		try {
			return new int[] {
				Integer.parseInt(splits[0]),
				Integer.parseInt(splits[1])
			};
		} catch (NumberFormatException exn) {
			return null;
		}
	}

	public static SourceLocation
	fromString(String str) {
		if (!str.endsWith("]")) {
			return null;
		}
		String[] splits = str.substring(0, str.length() - 1).split("\\[", 2);
		if (splits.length != 2) {
			return null;
		}
		final String filename = splits[0];
		final String[] locs = splits[1].split("-", 2);
		if (locs.length != 2) {
			return null;
		}
		int[] lcoord = splitCoord(locs[0]);
		int[] rcoord = splitCoord(locs[1]);
		if (lcoord == null || rcoord == null) {
			return null;
		}
		return new SourceLocation(filename, lcoord[0], lcoord[1], rcoord[0], rcoord[1]);
	}

	public SourceLocation
	withoutFile() {
		return new SourceLocation("UNKNOWN",
					  this.startLine, this.startColumn,
					  this.endLine, this.endColumn);
	}

	/**
	 * Is this location within another location?
	 */
	public boolean
	within(SourceLocation outer) {
		if (!outer.file.equals(this.file)) {
			return false;
		}

		if (outer.endLine < this.endLine
		    || outer.startLine > this.startLine) {
			return false;
		}
		if (outer.startLine == this.startLine
		    && outer.startColumn > this.startColumn) {
			return false;
		}
		if (outer.endLine == this.endLine
		    && outer.endColumn < this.endColumn) {
			return false;
		}
		return true;
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

	@Override
	public boolean
	equals(Object other) {
		if (!(other instanceof SourceLocation)) {
			return false;
		}
		SourceLocation otherLoc = (SourceLocation) other;
		return 0 == this.compareTo(otherLoc);
	}

	public static class Comparator implements java.util.Comparator<SourceLocation> {
		public int compare(SourceLocation l1, SourceLocation l2) {
			if (l1 == null) {
				if (l2 == null) {
					return 0;
				}
				return -1;
			}
			if (l2 == null) {
				return 1;
			}
			return l1.compareTo(l2);
		}
	}
}
