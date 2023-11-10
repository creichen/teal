package lang;

import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Queue;
import java.util.Arrays;

import beaver.Parser.Exception;

import lang.ast.ASTNode;
import lang.ast.Program;
import lang.ast.Module;
import lang.ast.Decl;
import lang.ast.TEALParser;
import lang.ast.LangScanner;
import lang.ast.CompilerError;

import lang.ir.*;

import lang.common.SourceLocation;
import lang.common.WithSourceLocation;
import lang.common.Report;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLine;

public class Compiler {
	static boolean CODE_PROBER_MODE = stringToBoolean(System.getenv("TEAL_CODEPROBER_MODE"));

	public static String[] CodeProber_report_styles = new String[] { // CSS styling for Report.Visual
		// "my-red-bg={background-color: #f008}",
		// "my-red-bg#light={background-color: #00f8}",
	};

	public static boolean customASTAction(Program ast) {
		System.out.println("Hello from method 'customASTAction()' in " + Compiler.class + "!");
		// return "false" to finish here, otherwise the program will execute
		return false;
	}

	// "opt" is the command line parameter after '-Z'; useful for reconfiguring
	public static boolean customIRAction(IRProgram ir, String opt) {
		// return "false" to finish here, otherwise the program will execute
		return false;
	}

	// Interpret program with the given parameters, measure execution time in nanoseconds
	public static long measureRunTime(IRProgram p, List<IRValue> args) {
		try {
			long start = System.nanoTime();
			IRResult result = p.eval(args);
			// result.getGlobal(IRVarRef) allows us to look up global variables, if we ever need that
			long stop = System.nanoTime();
			long exec_time = stop - start;
			return exec_time;
		} catch (InterpreterException e) {
			System.err.println("Error while interpreting program: " + e.toString());
			throw new RuntimeException();
		}
	}

	// Interpret program with the give parameters, return the global variable identified by `varref`
	public static IRValue runAndGetGlobal(IRProgram p, List<IRValue> args, IRVarRef varref) {
		try {
			return p.eval(args).getGlobal(varref);
		} catch (InterpreterException e) {
			System.err.println("Error while interpreting program: " + e.toString());
			throw new RuntimeException();
		}
	}

	// Interpret program with the give parameters
	public static ArrayList<IRValue> parseArgs(List<String> strings) {
		ArrayList<IRValue> args = new ArrayList<>();
		if (strings != null) {
			for (String str : strings) {
				try {
					args.add(new IRIntegerValue(Long.parseLong(str)));
				} catch (NumberFormatException ignored) {
					args.add(new IRStringValue(str));
				}
			}
		}
		return args;
	}

	// Interpret program with the give parameters
	public static void interpret(IRProgram p, List<String> strings) {
		ArrayList<IRValue> args = parseArgs(strings);
		try {
			IRValue ret = p.eval(args).getReturnValue();
			System.out.println("" + ret);
		} catch (InterpreterException e) {
			System.err.println("Error while interpreting program: " + e.toString());
		}
	}

	public static Module createModuleFromFile(File f,
						  TEALParser parser,
						  List<? super CompilerError> errors) {
		LangScanner scanner;
		Module m;
		try {
			scanner = new LangScanner(new FileReader(f));
		} catch (FileNotFoundException e) {
			errors.add(new CompilerError("missing-file", "Missing input file '" + f + "'"){});
			return null;
		}

		try {
			m = (Module) parser.parse(scanner);
			m.setSourceFile(f.getPath());
		} catch (IOException | Exception e) {
			errors.add(new CompilerError("parser", "Parsing error in file '" + f + "': " + e){});
			return null;
		} catch (lang.ast.TEALParser.SyntaxError e) {
			errors.add(e.getCompilerError());
			return null;
		}

		return m;
	}

	public static Program createProgramFromFiles(List<String> files, List<String> importPaths,
						     List<? super CompilerError> errors) {
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

			program.moduleMap().put(f, m);
			program.addModule(m);

			unresolvedImports.addAll(m.importedFiles());
		}

		return program;
	}

	static class CmdLineOpts {
		enum Action {
			// Compiler actions
			PARSE,
			CHECK,
			PRINT_AST,
			CUSTOM_AST,
			CUSTOM_IR,
			CODEPROBER,
			IRGEN,
			INTERP
		}

		Action action = CODE_PROBER_MODE ? Action.CODEPROBER : Action.INTERP;
		String outputFile;
		String inputFile;
		String customOption;
		List<String> importPaths;
		List<String> progArgs; // arguments for the interpreted program

		PrintStream outStream = null;

		public void
		setProgArgs(String[] args) {
			if (args == null) {
				this.progArgs = new ArrayList<>();
			} else {
				this.progArgs = Arrays.asList(args);
			}
		}

		/**
		 * Get an output stream, defaulting to System.out (unless overridden)
		 */
		public PrintStream out() {
			if (this.outStream != null) {
				return this.outStream;
			}
			this.outStream = System.out;
			if (this.outputFile != null) {
				try {
					this.outStream = new PrintStream(this.outputFile);
				} catch (FileNotFoundException e) {
					System.err.println("ERROR Can't open output file '" + this.outputFile + "'.");
				}
			}
			return this.outStream;
		}
	}

	private static void printHelp(Options options) {
		new HelpFormatter().printHelp("teal MODULE",
					      "Compile and run a TEAL module.\n\n",
					      options,
					      "", true);
	}

	private static void printVersion() {
		System.out.println("Teal layer " + Program.LAYER + ", version " + Program.VERSION);
	}

	public static CmdLineOpts parseCmdLineArgs(String[] args) {
		DefaultParser parser = new DefaultParser();
		CmdLineOpts ret = new CmdLineOpts();

		Option parse = Option.builder("p").longOpt("parse").hasArg(false)
			.desc("Parse the program and build the AST.").build();
		Option check = Option.builder("c").longOpt("check").hasArg(false)
			.desc("Perform semantic and type checks.").build();
		Option printast = Option.builder("a").longOpt("check").hasArg(false)
			.desc("Perform semantic and type checks and print out the AST.").build();
		Option codegen = Option.builder("g").longOpt("codegen").hasArg(false)
			.desc("Generate IR code and print it out.").build();
		Option run = Option.builder("r").longOpt("run").hasArg(false)
			.desc("Interpret the IR code.").build();
		Option codeprober = Option.builder("D").longOpt("codeprober").hasArg(false)
			.desc("Computer information used by CodeProber (to be used when calling from CodeProber only)").build();
		Option custom1 = Option.builder("Y").longOpt("custom-ast").hasArg(false)
			.desc("Custom analysis on the AST").build();
		Option custom2 = Option.builder("Z").longOpt("custom-ir").hasArgs().optionalArg(true)
			.desc("Custom analysis on the IR").build();
		Option help = Option.builder("h").longOpt("help")
			.desc("Display this help.").build();
		Option version = Option.builder("V").longOpt("version")
			.desc("Print out version information.").build();

		OptionGroup action = new OptionGroup()
			.addOption(run)
			.addOption(check)
			.addOption(codeprober)
			.addOption(parse)
			.addOption(printast)
			.addOption(codegen)
			.addOption(custom1)
			.addOption(custom2)
			.addOption(help)
			.addOption(version);

		Option outputFile = Option.builder("o").longOpt("output").hasArg()
			.desc("Write the compiler's output to FILE.").argName("FILE").build();

		Option importPaths = Option.builder("i").longOpt("path").hasArg()
			.desc("Directories where to search for imported modules.").argName("DIR1:DIR2:...").build();

		Options options = new Options().addOptionGroup(action)
			.addOption(outputFile)
			.addOption(importPaths)
			.addOption(Option.builder("0").longOpt("columns-start-at-zero").hasArg(false)
				   .desc("The leftmost column has the number 0 (default: 1).").build())
			.addOption(Option.builder("s").longOpt("source-locations").hasArg(false)
				   .desc("When printing out ASTs or IR code, include the source location.").build())
			.addOption(Option.builder("A").longOpt("reports-ast").hasArg(false)
				   .desc("Print out all reports on the source AST.").build())
			.addOption(Option.builder("I").longOpt("reports-ir").hasArg(false)
				   .desc("Print out all reports on the IR.").build())
			;

		try {
			CommandLine cmd = parser.parse(options, args);

			// Informative actions (don't use the compiler)
			if (cmd.hasOption("h")) {
				printVersion();
				printHelp(options);
				System.exit(0);
			} else if (cmd.hasOption("V")) {
				printVersion();
				System.exit(0);
			}

			// Assume that the user wants us to run the compiler
			if (cmd.getArgs().length != 1) {
				if (cmd.getArgs().length > 1) {
					System.err.println("Please specify only one MODULE argument.");
				} else {
					System.err.println("Missing MODULE argument.");
				}
				printHelp(options);
				System.exit(1);
			} else {
				ret.inputFile = cmd.getArgs()[0];
			}

			if (cmd.hasOption("p")) {
				ret.action = CmdLineOpts.Action.PARSE;
			} else if (cmd.hasOption("c")) {
				ret.action = CmdLineOpts.Action.CHECK;
			} else if (cmd.hasOption("a")) {
				ret.action = CmdLineOpts.Action.PRINT_AST;
			} else if (cmd.hasOption("D")) {
				ret.action = CmdLineOpts.Action.CODEPROBER;
			} else if (cmd.hasOption("g")) {
				CODE_PROBER_MODE = true;
				ret.action = CmdLineOpts.Action.IRGEN;
			} else if (cmd.hasOption("Y")) {
				ret.action = CmdLineOpts.Action.CUSTOM_AST;
			} else if (cmd.hasOption("Z")) {
				ret.action = CmdLineOpts.Action.CUSTOM_IR;
				ret.customOption = cmd.getOptionValue("Z");
			} else if (cmd.hasOption("r")) {
				ret.action = CmdLineOpts.Action.INTERP;
				ret.setProgArgs(cmd.getOptionValues("r"));
			}

			if (cmd.hasOption("o")) {
				ret.outputFile = cmd.getOptionValue("o");
			}
			if (cmd.hasOption("s")) {
				IRProgram.printSourceLocations = true;
				Program.printSourceLocation = true;
			}

			if (cmd.hasOption("i")) {
				ret.importPaths = Arrays.asList(cmd.getOptionValue("i").split(":"));
			} else {
				ret.importPaths = new ArrayList<>();
				ret.importPaths.add(".");
			}

			if (cmd.hasOption("0")) {
				ASTNode.SOURCE_LEFTMOST_COLUMN_OFFSET = 0;
			} else {
				ASTNode.SOURCE_LEFTMOST_COLUMN_OFFSET = 1;
			}

		} catch (ParseException e) {
			printHelp(options);
			throw new RuntimeException(e);
		}

		return ret;
	}

	static boolean stringToBoolean(String s) {
		if (s != null) {
			switch (s.toLowerCase()) {
			case "true":
			case "t":
			case "yes":
			case "enable":
			case "on":
			case "1":
				return true;
			}
		}
		return false;
	}

	public static <R extends Report> void
	printReports(List<R> reports) {
		for (R r : reports) {
			if (CODE_PROBER_MODE) {
				System.out.println(r.toCodeProberString());
			} else {
				System.err.println(r);
			}
		}
	}

	public static Program tryCompiling(CmdLineOpts opts, List<? super CompilerError> errors) {
		// open the output file / stdout
		PrintStream out = opts.out();

		// parse the program and all its imported modules
		return createProgramFromFiles(Collections.singletonList(opts.inputFile),
					      opts.importPaths,
					      errors);
	}

	public static boolean run(CmdLineOpts opts) {
		List<CompilerError> compilerErrors = new ArrayList<>();

		// open the output file / stdout
		PrintStream out = opts.out();

		Program program = tryCompiling(opts, compilerErrors);

		// print any errors and other reports on the AST so far
		printReports(compilerErrors);

		// fail if there are compiler erorrs
		if (!compilerErrors.isEmpty()) {
			return false;
		}

		// if this is all what's requested, return
		if (opts.action == CmdLineOpts.Action.PARSE) {
			out.print(program.dumpTree());
			return true;
		}

		// run the semantic checks
		List<CompilerError> semaErrors = program.semanticErrors();
		List<CompilerError> nameErrors = program.nameErrors();

		if (!CODE_PROBER_MODE) {
			// Codeprober will extract attributes directly, so only print for command-line use

			printReports(nameErrors);
			printReports(semaErrors);

			// Other reports
			printReports(program.reports());
		}

		// fail if there are any errors
		if (!nameErrors.isEmpty() || !semaErrors.isEmpty()) {
			return false;
		}

		switch (opts.action) {

		case PRINT_AST:
		    out.print(program.dumpTree());
		    // fall through
		case CHECK:
		    return true;

		case CUSTOM_AST:
		    if (!customASTAction(program)) {
			return true;
		    }
		    break;

		case CODEPROBER:
		    return true;

		default:
		    // pass
		}

		// Generate the IR program
		IRProgram irProg = program.genIR();

		if (opts.action == CmdLineOpts.Action.IRGEN) {
			irProg.print(out);
			return true;
		}

		if (opts.action == CmdLineOpts.Action.CUSTOM_IR) {
			if (!customIRAction(irProg, opts.customOption)) {
				return true;
			}
		}

		// Interpret the program
		interpret(irProg, opts.progArgs);

		return true;
	}

	public static Object CodeProber_parse(String[] args) throws Throwable {
		CODE_PROBER_MODE = true;
		CmdLineOpts opts = parseCmdLineArgs(args);

		return tryCompiling(opts, new ArrayList<>());
	}

	public static void main(String[] args) {
		CmdLineOpts opts = parseCmdLineArgs(args);
		boolean success = run(opts);
		if (opts.action == CmdLineOpts.Action.CODEPROBER) {
			return;
		}
		if (success) {
			System.exit(0);
		} else {
			System.exit(1);
		}
	}
}
