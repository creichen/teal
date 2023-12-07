package lang.attrcmp;

import lang.ast.ASTNode;
import lang.ast.Program;
import lang.ast.Decl;

import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents AST nodes by string IDs that are unique (within one AST)
 */
public class NodeIDMap {
	private java.util.HashMap<String, ASTNode> into_map = new HashMap<>();
	private java.util.IdentityHashMap<ASTNode, String> outof_map = new IdentityHashMap<>();
	private List<String> nodeids = new ArrayList<>();
	private List<String> builtin_nodeids = new ArrayList<>();

	private NodeIDMap() {
	}

	/**
	 * Create a NodeIDMap
	 *
	 * @param program The program to represent in the map
	 * @return The constructed NodeIDMap
	 */
	public static NodeIDMap from(Program program) {
		NodeIDMap nidmap = new NodeIDMap();

		int max_path_size = 32;
		int[] base_path = new int[max_path_size];

		// First collect the built-in decls
		for (Decl decl : program.getBuiltinDeclsList()) {
			nidmap.addRecursively(nidmap.builtin_nodeids, base_path, 0, decl);
		}

		// Now collect the actual program:

		boolean success = false;
		while (!success) {
			base_path = new int[max_path_size];
			try {
				nidmap.addRecursively(nidmap.nodeids, base_path, 0, program);
				success = true;
			} catch (DeeperThanExpectedException __) {
				// Retry
				nidmap.nodeids.clear();
				max_path_size <<= 1;
			}
		}
		return nidmap;
	}

	/**
	 * Helper exception for iterative deepening during NodeIDMap construction (implementation detail)
	 */
	private static class DeeperThanExpectedException extends RuntimeException {
	}

	private void addRecursively(List<String> nodeids, int[] path, int depth, ASTNode node) {
		String nodeid = pathStringFromPath(path, depth);
		this.into_map.put(nodeid, node);
		this.outof_map.put(node, nodeid);
		nodeids.add(nodeid);
		if (node.getNumChild() != 0) {
			// Recurse
			if (depth + 1 == path.length) {
				// Abort and retry
				throw new DeeperThanExpectedException();
			}
			for (int child_nr = 0; child_nr < node.getNumChild(); ++child_nr) {
				path[depth] = child_nr;
				this.addRecursively(nodeids,
						    path, depth + 1,
						    node.getChild(child_nr));
			}
			if (node instanceof NTAOwner) {
				int count = node.getNumChild();
				++count; // Skip one index number to mark transition to NTAs
				NTAOwner ntaowner = (NTAOwner) node;
				for (ASTNode nta : ntaowner.getNTAs()) {
					path[depth] = count++;
					this.addRecursively(nodeids,
							    path, depth + 1,
							    nta);
				}
			}
		}
	}

	/**
	 * List of all (non-builtin) NodeIDs
	 *
	 * @return List of all NodeIDs in this NodeIDMap in traversal pre-order
	 */
	public List<? extends String> keys() {
		return this.nodeids;
	}

	/**
	 * List of all builtin NodeIDs
	 *
	 * @return List of all NodeIDs for built-in definitions
	 */
	public List<? extends String> builtinKeys() {
		return this.builtin_nodeids;
	}

	/**
	 * Map NodeID to corresponding AST node
	 *
	 * @param NodeID to map
	 * @return ASTNode for nodeid
	 */
	public ASTNode getNode(String nodeid) {
		return (this.into_map.get(nodeid));
	}

	/**
	 * Find NodeID for an AST node
	 *
	 * @param Node to identify
	 * @return String that contains the NodeID
	 */
	public String getNodeID(ASTNode node) {
		return (this.outof_map.get(node));
	}

	public static String indexFromInt(int index) {
		if (index < 10) {
			return Integer.toString(index);
		}
		if (index < 35) {
			return Character.toString('A' + (index - 10));
		}
		return "Z" + indexFromInt(index / 35) + indexFromInt(index % 35);
	}

	public static int indexToInt(char[] str, int[] start_offset) {
		int offset = start_offset[0]++;
		if (offset >= str.length) {
			throw new IllegalArgumentException();
		}

		char c = str[offset];

		if (c == 'Z') {
			int major = indexToInt(str, start_offset);
			int minor = indexToInt(str, start_offset);
			return major * 35 + minor;
		}
		if (c >= '0' && c <= '9') {
			return c - '0';
		}
		if (c >= 'A' && c <= 'Y') {
			return c - 'A' + 10;
		}
		throw new IllegalArgumentException();
	}

	public static String pathStringFromPath(int[] path, int length) {
		StringBuffer sb = new StringBuffer();
		sb.append("@");
		for (int i = 0; i < length; i++) {
			int index = path[i];
			sb.append(NodeIDMap.indexFromInt(index));
		}
		return sb.toString();
	}

	public static int[] pathFromPathString(String path) {
		char[] chars = path.toCharArray();
		if (chars.length < 1 || chars[0] != '@') {
			throw new IllegalArgumentException();
		}

		int[] offset = new int[] {1};

		int[] result = new int[chars.length];
		int result_index = 0;

		while (offset[0] < chars.length) {
			result[result_index++] = NodeIDMap.indexToInt(chars, offset);
		}
		if (result_index < result.length) {
			int[] old_result = result;
			result = new int[result_index];
			System.arraycopy(old_result, 0, result, 0, result_index);
		}
		return result;
	}
}
