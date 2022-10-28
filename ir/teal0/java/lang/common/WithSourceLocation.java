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
}
