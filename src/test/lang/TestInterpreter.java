package lang;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import lang.ir.IRModule;
import lang.ir.IRValue;
import lang.ir.IRIntegerValue;
import lang.ir.IRStringValue;
import lang.ir.InterpreterException;
import lang.ir.IRTypeRef;

public class TestInterpreter {
	private static final String TEST_DIRECTORY = "testfiles/interpreter";

	private static IRModule loadAndCompileProgram(String name) {
		Path file = Paths.get(TEST_DIRECTORY, name);
		try {
			LangScanner scanner = new LangScanner(new FileReader(file.toString()));
			TEALParser parser = new TEALParser();
			Module module = (Module) parser.parse(scanner);
			Program program = new Program();
			program.addModule(module);

			List<CompilerError> semaErrors = program.semanticErrors();
			List<CompilerError> nameErrors = program.nameErrors();

			for (CompilerError e : nameErrors) {
				System.err.println("ERROR " + e.report());
			}

			for (CompilerError e : semaErrors) {
				System.err.println("ERROR " + e.report());
			}

			if (semaErrors.size() != 0)
				return null;

			if (nameErrors.size() != 0)
				return null;

			IRModule irm = module.genIR();

			return irm;
		} catch (FileNotFoundException e) {
			System.err.println("Could not load source file: " + name);
			return null;
		} catch (Exception e) {
			System.err.println("Could not parse file: " + name);
			return null;
		}
	}

	private static boolean checkResultNoCatch(IRModule m, Object expectedReturn, Object ... testInput) throws InterpreterException {
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


		IRValue ret = m.eval(args);
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

	private static boolean checkResult(IRModule m, Object expectedReturn, Object ... testInput) {
		try {
			return checkResultNoCatch(m, expectedReturn, testInput);
		} catch (InterpreterException e) {
			System.err.println("Error while intepreting program: " + e.toString());
		}
		return false;
	}

	@Test
	public void testAdd() {
		IRModule m = loadAndCompileProgram("add.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 0, -100, 100));
	}

	@Test
	public void testEq() {
		IRModule m = loadAndCompileProgram("eq.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 0, 10, 13));
		assertTrue(checkResult(m, 1, 1001, 1001));
	}

	@Test
	public void testArith() {
		IRModule m = loadAndCompileProgram("arith.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 0, 11, 8));
	}

	@Test
	public void testWhile() {
		IRModule m = loadAndCompileProgram("sum.in");
		assertNotNull(m);

		int n = 10000;
		int r = (n - 1) * n / 2;
		assertTrue(checkResult(m, r, n));
	}

	@Test
	public void testRecursiveCall() {
		IRModule m = loadAndCompileProgram("sum_rec.in");
		assertNotNull(m);

		int n = 20;
		int r = (n + 1) * n / 2;
		assertTrue(checkResult(m, r, n));
	}

	@Test
	public void testSingleClass() {
		IRModule m = loadAndCompileProgram("single-class.in");
		assertNotNull(m);

		int x = 5;
		int y = 10;
		int r = (x + y) * 10 * 100;

		assertTrue(checkResult(m, r, x, y));
	}

	@Test
	public void testClassWithMemberInit() {
		IRModule m = loadAndCompileProgram("class-with-init.in");
		assertNotNull(m);

		assertTrue(checkResult(m, 1115));
	}

	@Test
	public void testMethodCall() {
		IRModule m = loadAndCompileProgram("method-call.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 1010, 10, 1000));
	}

	@Test
	public void testGenericClass() {
		IRModule m = loadAndCompileProgram("generic-class.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 50, 10, 15));
	}

	@Test
	public void testConstructor() {
		IRModule m = loadAndCompileProgram("constructor.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 11, 10, 5));
		assertTrue(checkResult(m, 5, 5, 10));
	}

	@Test
	public void testFactorial() {
		// the test contains a circular function reference
		IRModule m = loadAndCompileProgram("fact.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 1, 0));
		assertTrue(checkResult(m, 720, 6));
	}

	@Test
	public void testPair() {
		// the test contains a circular type reference
		IRModule m = loadAndCompileProgram("pair.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 100, 5, 10));
		assertTrue(checkResult(m, 100, 10, 5));
	}

	@Test
	public void testSubclass() {
		IRModule m = loadAndCompileProgram("subclass.in");
		assertNotNull(m);
		assertTrue(checkResult(m, "Dog"));
	}

	@Test
	public void testStack() {
		IRModule m = loadAndCompileProgram("stack.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 2));
	}

	@Test
	public void testLocalVarQualifier() {
		IRModule m = loadAndCompileProgram("qualifier.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 0, 11, 132));
	}

	@Test(expected=InterpreterException.class)
	public void testLocalVarQualifierFail() throws InterpreterException {
		IRModule m = loadAndCompileProgram("qualifier.in");
		assertNotNull(m);
		checkResultNoCatch(m, 0, 11, 12);
	}
}
