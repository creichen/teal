package lang;

import java.io.File;

import org.junit.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParseTests {
	/** Directory where the test input files are stored. */
	private static final File TEST_DIRECTORY = new File("testfiles/parser");

	private String filename;

	public ParseTests(String filename) {
		this.filename = filename;
	}

	@Test public void runTest() throws Exception {
		if (this.filename.startsWith("error")) {
			Util.testSyntaxError(TEST_DIRECTORY, this.filename);
		} else {
			Util.testValidSyntax(TEST_DIRECTORY, this.filename);
		}
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> getTests() {
		return Util.getTestParameters(TEST_DIRECTORY, ".in");
	}
}
