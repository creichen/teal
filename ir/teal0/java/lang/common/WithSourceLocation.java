package lang.common;

/**
 * Common interface for ASTNode objects for both the source AST and the IR AST
 */
public interface WithSourceLocation {
	/**
	 * Retrieve the source location associated with this entity
	 *
	 * @return The object's source location
	 */
	public SourceLocation sourceLocation();

	public static class Comparator implements java.util.Comparator<WithSourceLocation> {
		public int compare(WithSourceLocation l1, WithSourceLocation l2) {
			if (l1 == null) {
				if (l2 == null) {
					return 0;
				}
				return -1;
			}
			if (l2 == null) {
				return 1;
			}
			return l1.sourceLocation().compareTo(l2.sourceLocation());
		}
	}
}
