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

import lang.ast.Module;
import lang.ast.Program;
import lang.ast.IdUse;
import lang.ast.CompilerError;
import lang.ast.IdDecl;
import lang.common.Report;

/**
 * Tests for custom (semantic) reports
 * This is a parameterized test: one test case is generated for each input
 * file found in TEST_DIRECTORY. Input files should have the ".in" extension.
 * @author Jesper Öqvist <jesper.oqvist@cs.lth.se>, Christoph Reichenbach
 */
@RunWith(Parameterized.class)
public class TestReports {
  /** Directory where the test input files are stored. */
  private static final File TEST_DIRECTORY = new File("testfiles/reports");

  private final String filename;
  public TestReports(String testFile) {
    filename = testFile;
  }

  @Test public void runTest() throws Exception {
    Module module = (Module) Util.parse(new File(TEST_DIRECTORY, filename));
    Program program = new Program().addModule(module);
    java.util.List<Report> reports = program.reports();
    Collections.sort(reports, new Comparator<Report>() {
	    public int compare(Report self, Report other) {
		return self.getFirstSrcLoc().compareTo(other.getFirstSrcLoc());
	    }
	});

    String actual = "";
    for (Report e : reports) {
	actual += e.toString() + "\n";
    }

    // Task 1: Enable this line!
    Util.compareOutput(actual, TEST_DIRECTORY, filename, new StringNormalizer.WhitespaceNormalize());
  }

  @Parameters(name = "{0}")
  public static Iterable<Object[]> getTests() {
    return Util.getTestParameters(TEST_DIRECTORY, ".in");
  }
}
