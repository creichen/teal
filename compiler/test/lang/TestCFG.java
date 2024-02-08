package lang;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;

import lang.ast.ASTNode;
import lang.ast.CFGNode;
import lang.ast.CompilerError;
import lang.ast.Program;
import lang.ast.Module;

/**
 * Tests for AST printing (dumpTree).
 * This is a parameterized test: one test case is generated for each input
 * file found in TEST_DIRECTORY. Input files should have the ".in" extension.
 * @author Jesper Öqvist <jesper.oqvist@cs.lth.se>
 */
@RunWith(Parameterized.class)
public class TestCFG {
	/** Directory where the test input files are stored. */
	private static final File TEST_DIRECTORY = new File("testfiles/cfg");

	private final String filename;
	public TestCFG(String testFile) {
		filename = testFile;
	}

	@Test public void runTest() throws Exception {
		List<CompilerError> parseErrors = new ArrayList<>();
		Program program = Compiler.createProgramFromFiles(Collections.singletonList(new File(TEST_DIRECTORY, filename).getPath()),
								  Collections.emptyList(), parseErrors);
		assertTrue(parseErrors.isEmpty());
		assertTrue(program.semanticErrors().isEmpty());
		ArrayList<CFGNode> cfg_nodes = TestCFG.sorted(program.allCFGNodes());

		NodeNamer namer = new NodeNamer();
		for (CFGNode node : cfg_nodes) {
			// make sure nodes are numbered in consistent order
			namer.getName(node);
		}

		String actual = "";
		for (CFGNode node : cfg_nodes) {
			actual += namer.getName(node) + " @ " + ((ASTNode)node).sourceLocation().getStartLine() + " :\n";
			for (CFGNode successor : TestCFG.sorted(node.succ().stream().collect(java.util.stream.Collectors.toList()))) {
				actual += "  -> " + namer.getName(successor);
				if (!successor.pred().contains(node)) {
					actual += " (missing back-edge)";
				}
				actual += "\n";
			}
		}

		Util.compareOutput(actual, TEST_DIRECTORY, filename, null);
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> getTests() {
		return Util.getTestParameters(TEST_DIRECTORY, ".in");
	}

	private static ArrayList<CFGNode>
	sorted(Collection<CFGNode> coll) {
		ArrayList<CFGNode> nodes = new ArrayList<CFGNode>(coll);

		Collections.sort(nodes, new Comparator<CFGNode>() {
				public int compare(CFGNode left, CFGNode right) {
					int v = ((ASTNode)left).sourceLocation().compareTo(((ASTNode)right).sourceLocation());
					if (v != 0) {
						return v;
					}
					// otherwise assume that one is an ancestor of the other

					ASTNode ra = ((ASTNode)right).getParent();
					while (ra != null && ra != left) {
						ra = ra.getParent();
					}
					if (ra == null) {
						return 1; // left is ancestor of right
					}
					return -1;
				}
			});

		return nodes;
	}

	@Ignore
	private class NodeNamer {

		private HashMap<CFGNode, String> nodenames = new HashMap<>();
		private HashMap<String, Integer> nodenamesCount = new HashMap<>();

		public NodeNamer() {}

		public String getName(CFGNode node) {
			if (!this.nodenames.containsKey(node)) {
				final String[] segments = node.getClass().getName().split("\\.", 0);
				final String shortname = segments[segments.length-1];
				if (!nodenamesCount.containsKey(shortname)) {
					nodenamesCount.put(shortname, 0);
				}
				final int shortname_count = nodenamesCount.get(shortname);
				final String name = shortname + "#" + shortname_count;
				nodenamesCount.put(shortname, shortname_count + 1);
				this.nodenames.put(node, name);
				return name;
			}
			return this.nodenames.get(node);
		}
	}
}
