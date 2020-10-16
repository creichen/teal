package lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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

public class TestInterpreter {
	private static final String TEST_DIRECTORY = "testfiles/interpreter";

	private static IRProgram loadAndCompileProgram(String name) {
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

	@Test
	public void testAdd() {
		IRProgram m = loadAndCompileProgram("add.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 0, -100, 100));
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
		IRProgram m = loadAndCompileProgram("single-class.in");
		assertNotNull(m);

		int x = 5;
		int y = 10;
		int r = (x + y) * 10 * 100;

		assertTrue(checkResult(m, r, x, y));
	}

	@Test
	public void testClassWithMemberInit() {
		IRProgram m = loadAndCompileProgram("class-with-init.in");
		assertNotNull(m);

		assertTrue(checkResult(m, 1115));
	}

	@Test
	public void testMethodCall() {
		IRProgram m = loadAndCompileProgram("method-call.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 1010, 10, 1000));
	}

	@Test
	public void testGenericClass() {
		IRProgram m = loadAndCompileProgram("generic-class.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 50, 10, 15));
	}

	@Test
	public void testConstructor() {
		IRProgram m = loadAndCompileProgram("constructor.in");
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
		// the test contains a circular type reference
		IRProgram m = loadAndCompileProgram("pair.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 100, 5, 10));
		assertTrue(checkResult(m, 100, 10, 5));
	}

	@Test
	public void testSubclass() {
		IRProgram m = loadAndCompileProgram("subclass.in");
		assertNotNull(m);
		assertTrue(checkResult(m, "Dog"));
	}

	@Test
	public void testStack() {
		IRProgram m = loadAndCompileProgram("stack.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 2));
	}

	@Test
	public void testLocalVarQualifier() {
		IRProgram m = loadAndCompileProgram("qualifier.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 0, 11, 132));
	}

	@Test(expected=InterpreterException.class)
	public void testLocalVarQualifierFail() throws InterpreterException {
		IRProgram m = loadAndCompileProgram("qualifier.in");
		assertNotNull(m);
		checkResultNoCatch(m, 0, 11, 12);
	}

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
}
