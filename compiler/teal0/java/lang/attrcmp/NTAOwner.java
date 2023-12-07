package lang.attrcmp;

import lang.ast.ASTNode;
import java.util.List;

/**
 * Interface for nodes that "host" NTAs that can participate in AST diffing.
 */
public interface NTAOwner {
	public List<ASTNode> getNTAs();
}
