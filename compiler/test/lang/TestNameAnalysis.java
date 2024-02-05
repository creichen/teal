package lang;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import lang.ast.Program;
import lang.ast.IdUse;
import lang.ast.IdDecl;
import lang.ast.Module;

/**
 * Tests for AST name analysis.
 * This is a parameterized test: one test case is generated for each input
 * file found in TEST_DIRECTORY. Input files should have the ".in" extension.
 */
@RunWith(Parameterized.class)
public class TestNameAnalysis {
  /** Directory where the test input files are stored. */
  private static final File TEST_DIRECTORY = new File("testfiles/names");

  private final String filename;
  public TestNameAnalysis(String testFile) {
    filename = testFile;
  }

  @Test public void runTest() throws Exception {
    Module module = (Module) Util.parse(new File(TEST_DIRECTORY, filename));
    Program program = new Program().addModule(module);
    HashMap<IdUse, IdDecl> symTable = program.globalSymbolTable();
    ArrayList<String> lines = new ArrayList<>();
    for (Map.Entry<IdUse, IdDecl> entry : symTable.entrySet()) {
      String line = "";
      line += entry.getKey().getIdentifier() + "\t";
      line += entry.getKey().sourceLocation().toString() + "\t";
      line += entry.getValue().sourceLocation().toString() + "\n";
      lines.add(line);
    }

    Collections.sort(lines);
    String actual = "";
    for (String line : lines) {
      actual += line;
    }

    Util.compareOutput(actual, TEST_DIRECTORY, filename, null);
  }

  @Parameters(name = "{0}")
  public static Iterable<Object[]> getTests() {
    return Util.getTestParameters(TEST_DIRECTORY, ".in");
  }
}
