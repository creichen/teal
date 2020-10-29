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
public class TestInterpreter {
	private static final String TEST_DIRECTORY = "testfiles/interpreter";

	private static IRProgram loadAndCompileProgram(String... name) {
		Path file = Paths.get(TEST_DIRECTORY, name);

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
                public Optional<Object> exception;

                public TestSpec() {
                        this.inputs = Optional.empty();
                        this.output = Optional.empty();
                        this.exception = Optional.empty();
                }

                public TestSpec(Optional<Object[]> inputs,
                                Optional<Object> output,
                                Optional<Object> exception) {
                        this.inputs = inputs;
                        this.output = output;
                        this.exception = exception;
                }

                public static String INPUT_PATTERN = "IN: ([-0-9] ?)+";
                public static String OUTPUT_PATTERN = "OUT: ([-0-9]+)";
                public static String EXCEPTION_PATTERN = "EXCEPTION: [-0-9]+";

                public static TestSpec parseInputs(String line) {
                        Pattern p = Pattern.compile(INPUT_PATTERN);
                        Matcher m = p.matcher(line);

                        if (m.find()) {
                                Object[] results = new Object[m.groupCount()];
                                for (int i = 0; i < m.groupCount(); ++i) {
                                        results[i] = m.group(i);
                                }
                                return new TestSpec(Optional.of(results),
                                                    Optional.empty(),
                                                    Optional.empty());
                        } else {
                                return new TestSpec();
                        }
                }

                public static TestSpec parseOutput(String line) {
                        Pattern p = Pattern.compile(OUTPUT_PATTERN);
                        Matcher m = p.matcher(line);

                        if (m.find()) {
                                return new TestSpec(Optional.empty(),
                                                    Optional.of(m.group()),
                                                    Optional.empty());
                        } else {
                                return new TestSpec();
                        }
                }

                public static TestSpec parseException(String line) {
                        Pattern p = Pattern.compile(EXCEPTION_PATTERN);
                        Matcher m = p.matcher(line);

                        if (m.find()) {
                                return new TestSpec(Optional.empty(),
                                                    Optional.of(m.group()),
                                                    Optional.empty());
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

        /**
         * Reads comments from a program to find inputs to the test
         * and expected outputs or exceptions
         * @param programName the file where the program
         */
        public List<TestSpec> readTestSpec(String name) throws IOException {
                Path file = Paths.get(TEST_DIRECTORY, name);
                List<String> contents = Files.lines(file, StandardCharsets.UTF_8).collect(Collectors.toList());
                TestSpec currentSpec = new TestSpec();
                List<TestSpec> results = new ArrayList();
                for (String l : contents) {
                        if(currentSpec.isComplete()) {
                                results.add(currentSpec);
                                currentSpec = new TestSpec();
                                break;
                        }

                        if(currentSpec.isBlank()) {
                                currentSpec.combineWith(TestSpec.parseInputs(l));
                                break;
                        }

                        // Else
                        currentSpec.combineWith(TestSpec.parseOutput(l));
                        currentSpec.combineWith(TestSpec.parseException(l));
                }
                return results;
        }

        public List<Boolean> checkTestSpec(IRProgram p, List<TestSpec> testCases) {
                List<Boolean> result = new ArrayList();
                for (TestSpec t : testCases) {
                        result.add(checkResult(p, t.output, t.inputs));
                }
                return result;
        }

	@Test
        public void testAdd() throws IOException {
		IRProgram m = loadAndCompileProgram("add.in");
		assertNotNull(m);
                List<Boolean> results = checkTestSpec(m, readTestSpec("add.in"));
                assertFalse(results.isEmpty());
                for (Boolean b : results) {
                        assertTrue(b);
                }
	}

	@Test
	public void testEq() {
		IRProgram m = loadAndCompileProgram("eq.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 0, 10, 13));
		assertTrue(checkResult(m, 1, 1001, 1001));
	}

	@Test
	public void testArith() {
		IRProgram m = loadAndCompileProgram("arith.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 0, 11, 8));
	}

	@Test
	public void testWhile() {
		IRProgram m = loadAndCompileProgram("sum.in");
		assertNotNull(m);

		int n = 10000;
		int r = (n - 1) * n / 2;
		assertTrue(checkResult(m, r, n));
	}

	@Test
	public void testRecursiveCall() {
		IRProgram m = loadAndCompileProgram("sum_rec.in");
		assertNotNull(m);

		int n = 20;
		int r = (n + 1) * n / 2;
		assertTrue(checkResult(m, r, n));
	}

	@Test
	public void testSingleClass() {
		if (Program.LAYER == 0) { return; } // FIXME: make this more elegant
		IRProgram m = loadAndCompileProgram("teal3", "single-class.in"); // FIXME: in teal3/ subdir now
		assertNotNull(m);

		int x = 5;
		int y = 10;
		int r = (x + y) * 10 * 100;

		assertTrue(checkResult(m, r, x, y));
	}

	@Test
	public void testClassWithMemberInit() {
		if (Program.LAYER == 0) { return; } // FIXME: make this more elegant
		IRProgram m = loadAndCompileProgram("teal3", "class-with-init.in"); // FIXME: in teal3/ subdir now
		assertNotNull(m);

		assertTrue(checkResult(m, 1115));
	}

	@Test
	public void testMethodCall() {
		if (Program.LAYER == 0) { return; } // FIXME: make this more elegant
		IRProgram m = loadAndCompileProgram("teal3", "method-call.in"); // FIXME: in teal3/ subdir now
		assertNotNull(m);
		assertTrue(checkResult(m, 1010, 10, 1000));
	}

	@Test
	public void testGenericClass() {
		if (Program.LAYER == 0) { return; } // FIXME: make this more elegant
		IRProgram m = loadAndCompileProgram("teal3", "generic-class.in"); // FIXME: in teal3/ subdir now
		assertNotNull(m);
		assertTrue(checkResult(m, 50, 10, 15));
	}

	@Test
	public void testConstructor() {
		if (Program.LAYER == 0) { return; } // FIXME: make this more elegant
		IRProgram m = loadAndCompileProgram("teal3", "constructor.in"); // FIXME: in teal3/ subdir now
		assertNotNull(m);
		assertTrue(checkResult(m, 11, 10, 5));
		assertTrue(checkResult(m, 5, 5, 10));
	}

	@Test
	public void testFactorial() {
		// the test contains a circular function reference
		IRProgram m = loadAndCompileProgram("fact.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 1, 0));
		assertTrue(checkResult(m, 720, 6));
	}

	@Test
	public void testPair() {
		if (Program.LAYER == 0) { return; } // FIXME: make this more elegant
		// the test contains a circular type reference
		IRProgram m = loadAndCompileProgram("teal3", "pair.in"); // FIXME: in teal3/ subdir now
		assertNotNull(m);
		assertTrue(checkResult(m, 100, 5, 10));
		assertTrue(checkResult(m, 100, 10, 5));
	}

	@Test
	public void testSubclass() {
		if (Program.LAYER == 0) { return; } // FIXME: make this more elegant
		IRProgram m = loadAndCompileProgram("teal3", "subclass.in"); // FIXME: in teal3/ subdir now
		assertNotNull(m);
		assertTrue(checkResult(m, "Dog"));
	}

	@Test
	public void testStack() {
		if (Program.LAYER == 0) { return; } // FIXME: make this more elegant
		IRProgram m = loadAndCompileProgram("teal3", "stack.in"); // FIXME: in teal3/ subdir now
		assertNotNull(m);
		assertTrue(checkResult(m, 2));
	}

	@Test
	public void testLocalVarQualifier() {
		if (Program.LAYER == 0) { return; } // FIXME: make this more elegant
		IRProgram m = loadAndCompileProgram("teal3", "qualifier.in"); // FIXME: in teal3/ subdir now
		assertNotNull(m);
		assertTrue(checkResult(m, 0, 11, 132));
	}

	// FIXME: this one is important for later layers of Teal but not for Teal-0
	// @Test(expected=InterpreterException.class)
	// public void testLocalVarQualifierFail() throws InterpreterException {
	// 	IRProgram m = loadAndCompileProgram("qualifier.in");
	// 	assertNotNull(m);
	// 	checkResultNoCatch(m, 0, 11, 12);
	// }

	@Test
	public void testArrays() {
		IRProgram m = loadAndCompileProgram("array.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 55, 11));
	}

	@Test
	public void testModules() {
		IRProgram p = loadAndCompileProgram("import.in");
		assertNotNull(p);
		assertTrue(checkResult(p, 123123, 123));
	}

	@Test
	public void testNoExplicitTypes() {
		IRProgram p = loadAndCompileProgram("notypes.teal");
		assertNotNull(p);
		assertTrue(checkResult(p, 128, 123));
	}

	@Test
	public void testModuleDiamond() {
		// verify that if B imports A, C imports A, and P imports B and C,
		// the program generated from P contains a single copy of the declarations of A
		IRProgram p = loadAndCompileProgram("module_diamond.teal");
		assertNotNull(p);
		assertTrue(checkResult(p, 54, 27));
	}

	@Test
	public void testGlobalVarInit() {
		// verify that global variables are initialized to null;
		IRProgram p = loadAndCompileProgram("global-init.teal");
		assertNotNull(p);
		assertTrue(checkResult(p, 6));
	}

        @Test
        public void testStringConcatenation() {
                IRProgram p = loadAndCompileProgram("string-concat.in");
                assertNotNull(p);
                assertTrue(checkResult(p, "Hello World"));
        }

        @Test(expected=InterpreterException.class)
        public void testInvalidPlus() throws InterpreterException {
                IRProgram p = loadAndCompileProgram("invalid-plus.in");
                assertNotNull(p);
                assertTrue(checkResultNoCatch(p, "HELLO"));
        }
}
