package lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;

import lang.ast.TEALParser;
import lang.ast.CompilerError;
import lang.ast.LangScanner;
import lang.ast.Program;
import lang.ast.Module;
import lang.ir.IRProgram;
import lang.ir.IRModule;
import lang.ir.IRValue;
import lang.ir.IRIntegerValue;
import lang.ir.IRStringValue;
import lang.ir.InterpreterException;
import lang.ir.IRTypeRef;

/**
 * Test class for the IR Interpreter
 */
@RunWith(Parameterized.class)
public class TestInterpreter {
	private static final String TEST_DIRECTORY_NAME = "testfiles/interpreter";
	private static final File TEST_DIRECTORY = new File(TEST_DIRECTORY_NAME);
	private final String filename;

	public TestInterpreter(String testFile) {
		filename = testFile;
	}


	@Test public void runTest() throws Exception {
		runTestWithSpec(filename);
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> getTests() {
		return Util.getTestParameters(TEST_DIRECTORY, ".in");
	}


	private static IRProgram loadAndCompileProgram(String... name) {
		Path file = Paths.get(TEST_DIRECTORY_NAME, name);

		List<CompilerError> compilerErrors = new LinkedList<>();
                Program program = Compiler.createProgramFromFiles(Collections.singletonList(file.toString()),
                                                                  Collections.emptyList(), compilerErrors);


                List<CompilerError> semaErrors = program.semanticErrors();
		List<CompilerError> nameErrors = program.nameErrors();

		for (CompilerError e : compilerErrors) {
			System.err.println("ERROR " + e.report());
		}

		for (CompilerError e : nameErrors) {
			System.err.println("ERROR " + e.report());
		}

		for (CompilerError e : semaErrors) {
			System.err.println("ERROR " + e.report());
		}

		if (compilerErrors.size() != 0)
			return null;

		if (semaErrors.size() != 0)
			return null;

		if (nameErrors.size() != 0)
			return null;

		IRProgram p = program.genIR();
		return p;
	}

	private static boolean checkResultNoCatch(IRProgram p, Object expectedReturn, Object ... testInput) throws InterpreterException {
		ArrayList<IRValue> args = new ArrayList<>();
		for (Object input : testInput) {
			if (input instanceof String) {
				args.add(new IRStringValue((String) input));
			} else if (input instanceof Integer) {
				args.add(new IRIntegerValue((long)(int) input));
			} else {
				System.err.println("Unsupported type for argument.");
				return false;
			}
		}

		IRValue ret = p.eval(args);
		if (ret instanceof IRIntegerValue) {
			if (((IRIntegerValue)ret).asLong() != (long)(int) expectedReturn) {
				System.err.println("Expected: " + expectedReturn + " but got " + ret + ".");
				return false;
			}
			return true;
		} else if (ret instanceof IRStringValue) {
			if (!((IRStringValue)ret).asString().equals(expectedReturn)) {
				System.err.println("Expected: " + expectedReturn + " but got " + ret + ".");
				return false;
			}
			return true;
		}

		return false;
	}

	private static boolean checkResult(IRProgram p, Object expectedReturn, Object ... testInput) {
		try {
			return checkResultNoCatch(p, expectedReturn, testInput);
		} catch (InterpreterException e) {
			System.err.println("Error while intepreting program: " + e.toString());
		}
		return false;
	}

        public static class TestSpec {
                public Optional<Object[]> inputs;
                public Optional<Object> output;
                public Optional<Class> exception;

                public TestSpec() {
                        this.inputs = Optional.empty();
                        this.output = Optional.empty();
                        this.exception = Optional.empty();
                }

                public TestSpec(Optional<Object[]> inputs,
                                Optional<Object> output,
                                Optional<Class> exception) {
                        this.inputs = inputs;
                        this.output = output;
                        this.exception = exception;
                }

                public static Pattern INPUT_PATTERN = Pattern.compile("// IN: (.+)");
                public static Pattern OUTPUT_PATTERN = Pattern.compile("// OUT: (([-0-9]+)|(\".*\"))$");
                public static Pattern EXCEPTION_PATTERN = Pattern.compile("// EXCEPTION: (.+)");
                public static Pattern NO_INPUT_PATTERN = Pattern.compile("^// IN:NONE$");

                public static TestSpec parseInputs(String line) {
                        Matcher m = INPUT_PATTERN.matcher(line);
			Matcher m_none = NO_INPUT_PATTERN.matcher(line);

			boolean found_empty = m_none.find();

                        if (m.find() || found_empty) {
				String[] values;

				if (found_empty) {
					values = new String[0];
				} else {
					values = m.group(1).split(" ");
				}
                                Object[] results = new Object[values.length];
                                int i = 1;
                                for (String val : values) {
                                        results[i-1] = parseValue(val);
                                        i++;
                                }

                                return new TestSpec(Optional.of(results),
                                                    Optional.empty(),
                                                    Optional.empty());
                        } else {
                                return new TestSpec();
                        }
                }

                /**
                 * Parses a value for outputs or inputs
                 * Supports Integers and String
                 */
                public static Object parseValue(String valueString) {
                        try {
                                return Integer.parseInt(valueString);
                        } catch (java.lang.NumberFormatException e) {
                                Boolean isQuoted = valueString.startsWith("\"") & valueString.endsWith("\"");
                                if (!isQuoted) {
                                        throw new RuntimeException("Invalid value in test spec: " + valueString);
                                } else {
                                        return valueString.subSequence(1, valueString.length() - 1);
                                }
                        }
                }

                public static TestSpec parseOutput(String line) {
                        Pattern p = OUTPUT_PATTERN;
                        Matcher m = p.matcher(line);

                        if (m.find()) {
                                return new TestSpec(Optional.empty(),
                                                    Optional.of(parseValue(m.group(1))),
                                                    Optional.empty());
                        } else {
                                return new TestSpec();
                        }
                }


                public static Class parseExceptionValue(String valueString) {
                        try {
                                return Class.forName(valueString);
                        } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                                throw new RuntimeException("Parsing exception name failed for: " + valueString);
                        }
                }

                public static TestSpec parseException(String line) {
                        Pattern p = EXCEPTION_PATTERN;
                        Matcher m = p.matcher(line);

                        if (m.find()) {
                                return new TestSpec(Optional.empty(),
                                                    Optional.empty(),
                                                    Optional.of(parseExceptionValue(m.group(1))));
                        } else {
                                return new TestSpec();
                        }
                }

                private <T> Optional<T> combineOptionals(Optional<T> o1, Optional<T> o2) {
                        if (o1.isPresent() & !o2.isPresent()) {
                                return o1;
                        }
                        if (!o1.isPresent() & o2.isPresent()) {
                                return o2;
                        } if (!o1.isPresent() & !o2.isPresent()) {
                                return Optional.empty();
                        } else {
                                throw new RuntimeException(String.format("Cannot combine values: %s and %s",
                                                                         o1.toString(),
                                                                         o2.toString()));
                        }
                }

                public void combineWith(TestSpec other) {
                        if (other.isBlank()) { return; }
                        this.inputs = combineOptionals(this.inputs, other.inputs);
                        this.output = combineOptionals(this.output, other.output);
                        this.exception = combineOptionals(this.exception, other.exception);
                }


                public Boolean isComplete() {
                        return inputs.isPresent() & (output.isPresent() | exception.isPresent());
                }

                public Boolean isBlank() {
                        return !inputs.isPresent() & !output.isPresent() & !exception.isPresent();
                }
        }

        @Test
        public void testReadInputSpec() {
                TestSpec i = TestSpec.parseInputs("// IN: 0");
                assertNotEquals(i.inputs, Optional.empty());
                assertTrue(i.inputs.get().length == 1);
                assertEquals(0, i.inputs.get()[0]);
        }

        @Test
        public void testReadInputSpecString() {
                TestSpec i = TestSpec.parseInputs("// IN: 0 \"Cat\"");
                assertNotEquals(i.inputs, Optional.empty());
                assertTrue(i.inputs.get().length == 2);
                assertEquals(0, i.inputs.get()[0]);
                assertEquals("Cat", i.inputs.get()[1]);
        }

        @Test
        public void testOutputSpec() {
                TestSpec i = TestSpec.parseOutput("// OUT: 0");
                assertNotEquals(i.output, Optional.empty());
                assertEquals(0, i.output.get());
        }

        @Test
        public void testOutputSpecString() {
                TestSpec i = TestSpec.parseOutput("// OUT: \"Dog\"");
                assertNotEquals(i.output, Optional.empty());
                assertEquals("Dog", i.output.get());
        }

        @Test
        public void testExceptionSpec() {
                TestSpec e = TestSpec.parseException("// EXCEPTION: lang.ir.InterpreterException");
                assertNotEquals(e.exception, Optional.empty());
                assertEquals(lang.ir.InterpreterException.class, e.exception.get());
        }

        @Test
        public void testMultipleSpecs() {
                String text = "// IN: 10 10\n// OUT: 5\n// IN: 2 3\n// OUT: 0\n";
                Scanner scanner = new Scanner(text);
                List<String> lines = new ArrayList();
                while (scanner.hasNextLine()) {
                        lines.add(scanner.nextLine());
                }
                List<TestSpec> testSpecs = readTestSpecLines(lines);
                assertTrue(testSpecs.size() == 2);
                assertEquals(10,
                             testSpecs.get(0).inputs.get()[0]);
                assertEquals(10,
                             testSpecs.get(0).inputs.get()[1]);

                assertEquals(Optional.of(5),
                             testSpecs.get(0).output);

                assertEquals(2,
                             testSpecs.get(1).inputs.get()[0]);
                assertEquals(3,
                             testSpecs.get(1).inputs.get()[1]);
                assertEquals(Optional.of(0),
                             testSpecs.get(1).output);
        }

        @Test(expected=RuntimeException.class)
        public void testAbsentSpec() {
                List<String> lines = new ArrayList();
                lines.add("Some text with IN: and OUT: and stuff.");
                lines.add("// even has comments and EXCEPTION: and stuff.");
                lines.add("But yet no spec to be seen");
                List<TestSpec> testSpecs = readTestSpecLines(lines);
        }

        public List<TestSpec> readTestSpecLines(List<String> lines) {
                TestSpec currentSpec = new TestSpec();
                List<TestSpec> results = new ArrayList();
                for (String l : lines) {
                        currentSpec.combineWith(TestSpec.parseInputs(l));
                        currentSpec.combineWith(TestSpec.parseOutput(l));
                        currentSpec.combineWith(TestSpec.parseException(l));
                        if(currentSpec.isComplete()) {
                                results.add(currentSpec);
                                currentSpec = new TestSpec();
                        }
                }
                if (!currentSpec.isBlank()) {
                        throw new RuntimeException("Incomplete spec: " + currentSpec.toString());
                }

                if (results.isEmpty()) {
                        throw new RuntimeException("No spec found in lines");
                }
                return results;
        }


        /**
         * Reads comments from a program to find inputs to the test
         * and expected outputs or exceptions
         * @param programName the file where the program
         */
        public List<TestSpec> readTestSpec(String name) {
                Path file = Paths.get(TEST_DIRECTORY_NAME, name);
                try {
                        List<String> contents = Files.lines(file, StandardCharsets.UTF_8).collect(Collectors.toList());
                        return readTestSpecLines(contents);
                } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Failed to read file: " + name);
                }

        }

        public void checkTestSpec(IRProgram p, List<TestSpec> testCases) {
                for (TestSpec t : testCases) {
                        assertTrue(t.isComplete());
			if (t.exception.isPresent()) {
				// exceptional execution
				try {
					checkResultNoCatch(p, null, t.inputs.get());
					assertFalse("Missed exception: " + t.exception.get(),
						    true);
				} catch (Throwable exn) {
					assertEquals("Expected to abort on " + t.exception.get() + " but got " + exn.getClass(),
						     t.exception.get(), exn.getClass());
				}
			} else {
				// normal execution
				assertTrue(checkResult(p, t.output.get(), t.inputs.get()));
			}
                }
        }

        /**
         * Function that runs a test which contains an inline test
         * spec (In the comments).
         * filename: String: name of the file to load and test
         */
        public void runTestWithSpec(String filename) {
                IRProgram m = loadAndCompileProgram(filename);
                assertNotNull(m);
		List<TestSpec> spec = readTestSpec(filename);
		assertTrue(0 < spec.size());
                checkTestSpec(m, spec);
        }
}
