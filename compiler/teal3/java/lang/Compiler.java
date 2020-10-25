package lang;

import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Queue;

import beaver.Parser.Exception;

import lang.ast.Program;
import lang.ast.Module;
import lang.ast.TEALParser;
import lang.ast.LangScanner;
import lang.ast.CompilerError;

import lang.ir.IRModule;
import lang.ir.IRValue;
import lang.ir.IRProgram;
import lang.ir.InterpreterException;
import lang.ir.IRIntegerValue;

public class Compiler {
    public static Object DrAST_root_node; //Enable debugging with DrAST

	public static void interpret(IRProgram p, String[] strings) {
		ArrayList<IRValue> args = new ArrayList<>();
		for (int i = 2; i < strings.length; ++i) {
			args.add(new IRIntegerValue(Long.parseLong(strings[i])));
		}
		try {
			IRValue ret = p.eval(args);
			System.out.println("Program returned " + ret);
		} catch (InterpreterException e) {
			System.err.println("Error while intepreting program: " + e.toString());
		}
	}

	public static Module createModuleFromFile(File f,
											  TEALParser parser,
											  List<CompilerError> errors) {
		LangScanner scanner;
		Module m;
		try {
			scanner = new LangScanner(new FileReader(f));
		} catch (FileNotFoundException e) {
			errors.add(new CompilerError() {
					@Override
					public String report() {
						return "Missing input file '" + f + "'";
					}

					@Override
					public int getStartLoc() {
						return 0;
					}
				});
			return null;
		}

		try {
			m = (Module) parser.parse(scanner);
			m.setSourceFile(f.getPath());
		} catch (IOException | Exception e) {
			errors.add(new CompilerError() {
					@Override
					public String report() {
						return "Parsing error in file '" + f + "': " + e;
					}

					@Override
					public int getStartLoc() {
						return 0;
					}
				});
			return null;
		}

		return m;
	}

	public static Program createProgramFromFiles(List<String> files, List<String> importPaths,
												 List<CompilerError> errors) {
		Queue<File> unresolvedImports = new LinkedList<>();

		Program program = new Program();
		TEALParser parser = new TEALParser();

		// seed the unresolved imports with the source files
		files.stream().map(f -> new File(f)).collect(Collectors.toCollection(() -> unresolvedImports));

		// now transitively import all the modules
		while (!unresolvedImports.isEmpty()) {
			File f = unresolvedImports.remove();
			if (program.moduleMap().containsKey(f)) {
				continue;
			}
			Module m = createModuleFromFile(f, parser, errors);
			if (m == null) {
			    System.err.println("Skipping file due to errors.");
			    continue;
			}
			m.setNameFromFile(f);
			// add the module's current directory as import path
			if (f.getParentFile() == null)
				m.addImportPaths(Collections.singleton("."));
			else
				m.addImportPaths(Collections.singleton(f.getParentFile().getPath()));
			m.addImportPaths(importPaths);
			System.out.println(f);

			program.moduleMap().put(f, m);
			program.addModule(m);

			unresolvedImports.addAll(m.importedFiles());
		}

		return program;
	}

	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println(
							   "You must specify a source file on the command line!");
			printUsage();
			System.exit(1);
			return;
		}

		String filename = args[0];

		List<CompilerError> compilerErrors = new ArrayList<>();

		Program program = createProgramFromFiles(Collections.singletonList(filename),
												 Collections.singletonList("."),
												 compilerErrors);

		DrAST_root_node = program; //Enable debugging with DrAST

		// Report errors
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

		if (nameErrors.size() != 0 ||
			semaErrors.size() != 0 ||
			compilerErrors.size() != 0) {
			System.exit(1);
		}

		// Generate the IR
		IRProgram p = program.genIR();

		// Dump the IR
		p.print(System.out);

		if (args.length > 1 && args[1].equals("--run"))
			// this all the compilation pipeline for now, interpret
			interpret(p, args);
	}

	private static void printUsage() {
		System.err.println("Usage: DumpTree FILE");
		System.err.println("  where FILE is the file to be parsed");
	}
}
