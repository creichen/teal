package lang;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;

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
public class TestSemanticCheck {
  /** Directory where the test input files are stored. */
  private static final File TEST_DIRECTORY = new File("testfiles/semacheck");

  private final String filename;
  public TestSemanticCheck(String testFile) {
    filename = testFile;
  }

  @Test public void runTest() throws Exception {
	List<CompilerError> parseErrors = new ArrayList<>();
	Program program = Compiler.createProgramFromFiles(Collections.singletonList(new File(TEST_DIRECTORY, filename).getPath()),
													  Collections.emptyList(), parseErrors);
	assertTrue(parseErrors.isEmpty());
    java.util.List<CompilerError> nameErrors = program.semanticErrors();
    Collections.sort(nameErrors, new Comparator<CompilerError>() {
			public int compare(CompilerError left, CompilerError right) {
				return left.getSrcLoc().compareTo(right.getSrcLoc());
			}
	});

    String actual = "";
    for (CompilerError e : nameErrors) {
	actual += e.report() + "\n";
    }
    // Normalise path separators
    actual = actual.replace(File.separatorChar, '/');

    Util.compareOutput(actual, TEST_DIRECTORY, filename);
  }

  @Parameters(name = "{0}")
  public static Iterable<Object[]> getTests() {
    return Util.getTestParameters(TEST_DIRECTORY, ".in");
  }
}
