package lang;

import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import beaver.Parser.Exception;

import lang.ast.Program;
import lang.ast.Module;
import lang.ast.TEALParser;
import lang.ast.LangScanner;
import lang.ast.CompilerError;

import lang.ir.IRModule;
import lang.ir.IRValue;
import lang.ir.InterpreterException;
import lang.ir.IRIntegerValue;

/**
 * Dumps the parsed Abstract Syntax Tree of a Calc program.
 */
public class Compiler {
	/**
	 * Entry point
	 * @param args
	 */

    public static Object DrAST_root_node; //Enable debugging with DrAST

	public static void interpret(IRModule m, String[] strings) {
		ArrayList<IRValue> args = new ArrayList<>();
		for (int i = 2; i < strings.length; ++i) {
			args.add(new IRIntegerValue(Long.parseLong(strings[i])));
		}
		try {
			IRValue ret = m.eval(args);
			System.out.println("Program returned " + ret);
		} catch (InterpreterException e) {
			System.err.println("Error while intepreting program: " + e.toString());
		}
	}

	public static void main(String[] args) {
		try {
			if (args.length < 1) {
				System.err.println(
						"You must specify a source file on the command line!");
				printUsage();
				System.exit(1);
				return;
			}

			String filename = args[0];

			LangScanner scanner = new LangScanner(new FileReader(filename));
			TEALParser parser = new TEALParser();
			Module module = (Module) parser.parse(scanner);
			Program program = new Program();
			program.addModule(module);
            DrAST_root_node = program; //Enable debugging with DrAST

			// Report errors
			List<CompilerError> semaErrors = program.semanticErrors();
			List<CompilerError> nameErrors = program.nameErrors();

			for (CompilerError e : nameErrors) {
				System.err.println("ERROR " + e.report());
			}

			for (CompilerError e : semaErrors) {
				System.err.println("ERROR " + e.report());
			}

			if (nameErrors.size() != 0 ||
				semaErrors.size() != 0) {
				System.exit(1);
			}

			// Do IR generation
			IRModule m = module.genIR();

			// Dump the IR
			m.print(System.out);

			if (args.length > 1 && args[1].equals("--run"))
				// this all the compilation pipeline for now, interpret
				interpret(m, args);

		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void printUsage() {
		System.err.println("Usage: DumpTree FILE");
		System.err.println("  where FILE is the file to be parsed");
	}
}
