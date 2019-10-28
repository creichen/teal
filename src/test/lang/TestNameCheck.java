package lang;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import lang.ast.Program;
import lang.ast.IdUse;
import lang.ast.CompilerError;
import lang.ast.IdDecl;

/**
 * Tests for AST printing (dumpTree).
 * This is a parameterized test: one test case is generated for each input
 * file found in TEST_DIRECTORY. Input files should have the ".in" extension.
 * @author Jesper Öqvist <jesper.oqvist@cs.lth.se>
 */
@RunWith(Parameterized.class)
public class TestNameCheck {
  /** Directory where the test input files are stored. */
  private static final File TEST_DIRECTORY = new File("testfiles/namecheck");

  private final String filename;
  public TestNameCheck(String testFile) {
    filename = testFile;
  }

  @Test public void runTest() throws Exception {
    Program program = (Program) Util.parse(new File(TEST_DIRECTORY, filename));
    java.util.List<CompilerError> nameErrors = program.nameErrors();
	Collections.sort(nameErrors, new Comparator<CompilerError>() {
			public int compare(CompilerError left, CompilerError right) {
				return Integer.compare(left.getStartLoc(), right.getStartLoc());
			}
		});

	String actual = "";
	for (CompilerError e : nameErrors)
		actual += e.report() + "\n";

    Util.compareOutput(actual,
        new File(TEST_DIRECTORY, Util.changeExtension(filename, ".out")),
        new File(TEST_DIRECTORY, Util.changeExtension(filename, ".expected")));
  }

  @Parameters(name = "{0}")
  public static Iterable<Object[]> getTests() {
    return Util.getTestParameters(TEST_DIRECTORY, ".in");
  }
}
