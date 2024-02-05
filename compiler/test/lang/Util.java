package lang;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;

import org.junit.Test;

import lang.ast.Program;
import lang.ast.TEALParser;
import lang.ast.LangScanner;

/** Utility methods for running tests. */
public final class Util {
  private static String SYS_LINE_SEP = System.getProperty("line.separator");

  // Workaround for bugs in Gradle/JUnit
  @Test public void thisIsNotATest() {}

  /**
   * Parses the given file
   * @return parser result, if everything went OK
   */
  public static Object parse(File file) throws Exception {
    LangScanner scanner = new LangScanner(new FileReader(file));
    TEALParser parser = new TEALParser();
    return parser.parse(scanner);
  }

  /**
   * Check that the string matches the contents of the given file.
   *
   * Also writes the actual output to file.
   * To support multiple Teal layers, this method automatically loads
   * alternative expected outputs.  For filename="foo" and Teal layer 1, it
   * first tries "foo.teal1.expected", then "foo.teal0.expected", and
   * finally "foo.expected".
   *
   * @param actual actual output
   * @param test_directory directory containing the expected output
   * @param filename filename prefix for expected and actual output.  The actual output
   *   is written to filename + ".out", and the expected input is read from
   *   filename + ".teal{X}.expected" or filename + ".expected" (see above).
   * @param normalizer An optional custom string normaliser
   */
    public static void compareOutput(String actual, File test_directory, String filename, StringNormalizer normalizer) {
      if (normalizer == null) {
	normalizer = new StringNormalizer.NoOp();
      }
    try {
      File out = new File(test_directory, Util.changeExtension(filename, ".out"));
      File expected = null;
      // first try ".teal{X}.exists" for current-to-lower layers
      for (int layer = Program.LAYER; layer >= 0; --layer) {
	expected = new File(test_directory, Util.changeExtension(filename, ".teal" + layer + ".expected"));
	if (expected.exists()) {
	  break;
	}
      }
      if (expected == null || !expected.exists()) {
	expected = new File(test_directory, Util.changeExtension(filename, ".expected"));
      }

      Files.write(out.toPath(), actual.getBytes());
      assertEquals("Output differs.",
		   normalizer.normalize(readFileToString(expected)),
		   normalizer.normalize(normalizeText(actual)));
    } catch (IOException e) {
      fail("IOException occurred while comparing output: " + e.getMessage());
    }
  }

  interface StringNormalizer {
    public String normalize(String s);

    public static class NoOp implements StringNormalizer {
      @Override
      public String normalize(String s) {
	return s;
      }
    }
    public static class TabToSpace implements StringNormalizer {
      @Override
      public String normalize(String s) {
	return s.replace('\t', ' ');
      }
    }
    public static class WhitespaceNormalize implements StringNormalizer {
      @Override
      public String normalize(String s) {
	return s.replaceAll("[\t ]+", " ");
      }
    }
  }

  /**
   * Reads an entire file to a string object.
   * <p>If the file does not exist an empty string is returned.
   * <p>The system dependent line separator char sequence is replaced by
   * the newline character.
   *
   * @return normalized text from file
   */
  private static String readFileToString(File file) throws FileNotFoundException {
    if (!file.isFile()) {
      return "";
    }

    Scanner scanner = new Scanner(file);
    scanner.useDelimiter("\\Z");
    String text = normalizeText(scanner.hasNext() ? scanner.next() : "");
    scanner.close();
    return text;
  }

  /** Trim whitespace and normalize newline characters. */
  private static String normalizeText(String text) {
    return text.replace(SYS_LINE_SEP, "\n").trim();
  }

  public static void testValidSyntax(File directory, String filename) {
    try {
      Util.parse(new File(directory, filename));
    } catch (Exception e) {
      fail("Unexpected error while parsing '" + filename + "': "
          + e.getMessage());
    }
  }

  public static void testSyntaxError(File directory, String filename) {
    PrintStream prevErr = System.err;

    // Beaver reports syntax error on the standard error.
    // We discard these messages since a syntax error is expected.
    System.setErr(new PrintStream(new ByteArrayOutputStream()));
    try {
      Util.parse(new File(directory, filename));

      fail("syntax is valid, expected syntax error");
    } catch (beaver.Parser.Exception | lang.ast.TEALParser.SyntaxError e) {
      // Ok (expected syntax error)!
    } catch (Exception e) {
      fail("IO error while trying to parse '" + filename + "': "
          + e.getMessage());
    } finally {
      // Restore the system error stream.
      System.setErr(prevErr);
    }
  }

  public static String changeExtension(String filename, String newExtension) {
    int index = filename.lastIndexOf('.');
    if (index != -1) {
      return filename.substring(0, index) + newExtension;
    } else {
      return filename + newExtension;
    }
  }

  private static void addTestsInDir(Collection<Object[]> tests, File testDirectory, String filePrefix, String extension) {
    if (!testDirectory.isDirectory()) {
      return;
    }
    for (File f: testDirectory.listFiles()) {
      if (f.getName().endsWith(extension)) {
        tests.add(new Object[] {filePrefix + f.getName()});
      }
    }
  }

  /**
   * Gets all files from <tt>testDirectory</tt> that have suffix <tt>extension</tt>
   *
   * Also checks the <tt>teal0</tt>...<tt>teal3</tt> subdirectories, if running on
   * that layer of Teal or newer
   */
  @SuppressWarnings("javadoc")
  public static Collection<Object[]> getTestParameters(File testDirectory, String extension) {
    Collection<Object[]> tests = new LinkedList<Object[]>();
    if (!testDirectory.isDirectory()) {
      throw new Error("Could not find '" + testDirectory + "' directory!");
    }
    addTestsInDir(tests, testDirectory, "", extension);
    for (int i = 0; i <= Program.LAYER; i++) {
      final String filePrefix = "teal" + i;
      addTestsInDir(tests, new File(testDirectory, filePrefix), filePrefix + File.separator, extension);
    }
    return tests;
  }
}
