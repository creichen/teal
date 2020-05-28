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

import lang.ast.AttoLParser;
import lang.ast.CompilerError;
import lang.ast.LangScanner;
import lang.ast.Program;
import lang.ir.IRModule;
import lang.ir.IRValue;
import lang.ir.InterpreterException;
import lang.ir.IRType;

public class TestInterpreter {
	private static final String TEST_DIRECTORY = "testfiles/interpreter";

	private static IRModule loadAndCompileProgram(String name) {
		Path file = Paths.get(TEST_DIRECTORY, name);
		try {
			LangScanner scanner = new LangScanner(new FileReader(file.toString()));
			AttoLParser parser = new AttoLParser();
			Program program = (Program) parser.parse(scanner);

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

			IRModule irm = program.genIR();

			return irm;
		} catch (FileNotFoundException e) {
			System.err.println("Could not load source file: " + name);
			return null;
		} catch (Exception e) {
			System.err.println("Could not parse file: " + name);
			return null;
		}
	}

	private static boolean checkResult(IRModule m, Object expectedReturn, Object ... testInput) {
		ArrayList<IRValue> args = new ArrayList<>();
		for (Object input : testInput) {

			IRType type;
			if (input instanceof String) {
				type = m.StringType;
			} else if (input instanceof Integer) {
				type = m.IntegerType;
			} else {
				System.err.println("Unsupported type for argument.");
				return false;
			}

			IRValue arg = new IRValue(type, input);
			args.add(arg);
		}

		try {
			IRValue ret = m.eval(args);
			return ret.getValue().equals(expectedReturn);
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
		IRModule m =loadAndCompileProgram("arith.in");
		assertNotNull(m);
		assertTrue(checkResult(m, 0, 11, 8));
	}

	@Test
	public void testWhile() {
		IRModule m =loadAndCompileProgram("sum.in");
		assertNotNull(m);

		int n = 10000;
		int r = (n - 1) * n / 2;
		assertTrue(checkResult(m, r, n));
	}

	@Test
	public void testRecursiveCall() {
		IRModule m =loadAndCompileProgram("sum_rec.in");
		assertNotNull(m);

		int n = 20;
		int r = (n + 1) * n / 2;
		assertTrue(checkResult(m, r, n));
	}
}
