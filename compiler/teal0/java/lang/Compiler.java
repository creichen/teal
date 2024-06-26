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
import java.lang.reflect.Field;

import beaver.Parser.Exception;

import lang.ast.ASTNode;
import lang.ast.Program;
import lang.ast.Module;
import lang.ast.Decl;
import lang.ast.TEALParser;
import lang.ast.LangScanner;
import lang.ast.CompilerError;
import lang.attrcmp.Diff;
import lang.attrcmp.AttributeSummary;

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
			ATTR_EXTRACT,
			ATTR_DIFF,
			INTERP
		}

		Action action = CODE_PROBER_MODE ? Action.CODEPROBER : Action.INTERP;
		String outputFile;
		String inputFile;
		String customOption;
		String attrSpecFile;
		String[] attributesToExtract;
		List<String> importPaths;
		List<String> progArgs; // arguments for the interpreted program

		PrintStream outStream = null;

		public void
		setProgArgs(List<String> args) {
			if (args == null) {
				this.progArgs = new ArrayList<>();
			} else {
				this.progArgs = args;
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

	private static boolean attrDiff(Program program, CmdLineOpts opts) {
		try {
			AttributeSummary asummary = AttributeSummary.withAST(program).empty();
			AttributeSummary reference = AttributeSummary.withAST(program).from(new File(opts.attrSpecFile));
			asummary = asummary.withAttributesFrom(reference);
			Diff diff = reference.diff(asummary);
			if (diff.isEmpty()) {
				System.exit(0);
			}
			opts.out().print(diff);
			System.exit(1);
		} catch (IOException exn) {
			if (exn instanceof java.io.FileNotFoundException) {
				System.err.println("File not found: " + opts.inputFile);
				System.exit(2);
			}
			throw new RuntimeException(exn);
		} catch (lang.attrcmp.ASTMismatchException __) {
			System.err.println("AST MISMATCH");
			System.exit(3);
		}
		return true;
	}

	private static boolean attrExtract(Program program, CmdLineOpts opts) {
		AttributeSummary asummary = AttributeSummary.withAST(program).empty();
		for (String val : opts.attributesToExtract) {
			String[] vals = val.split(":");
			if (vals.length != 2) {
				System.err.println("Invalid NAME:TYPE attribute specification: '"+val+"");
				System.exit(1);
			}
			asummary.withAttribute(vals[0], vals[1]);
		}
		try {
			asummary.writeTo(opts.out());
		} catch (IOException exn) {
			throw new RuntimeException(exn);
		}
		return true;
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
		Option attr_extract = Option.builder("e").longOpt("attr-extract").hasArg(true)
			.argName("NAME:TYPE").valueSeparator('/')
			.desc("Extract an AST dump with all of the specified attributes.  Format: '<NAME>:<TYPE>', e.g., 'name:String' or 'refs:Set<ASTNode>'.  For multiple attributes, separate with '/'.").build();
		Option attr_delta = Option.builder("d").longOpt("attr-diff").hasArg(true).argName("ATTRFILE")
			.desc("Loads the result of an earlier --attr-extract and prints a diff.  Exit status 0 if both are the same.").build();
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
			.addOption(attr_extract)
			.addOption(attr_delta)
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
			if (cmd.getArgs().length < 1) {
				System.err.println("Missing MODULE argument.");
				System.exit(1);
			} else {
				String[] cmd_args = cmd.getArgs();

				// Arguments to the intepreted program
				ArrayList<String> teal_code_args = new ArrayList<>();
				for (int i = 1; i < cmd_args.length; ++i) {
					teal_code_args.add(cmd_args[i]);
				}
				ret.setProgArgs(teal_code_args);
			}
			ret.inputFile = cmd.getArgs()[0];

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
			} else if (cmd.hasOption("e")) {
				ret.action = CmdLineOpts.Action.ATTR_EXTRACT;
			} else if (cmd.hasOption("d")) {
				ret.action = CmdLineOpts.Action.ATTR_DIFF;
			}

			if (cmd.hasOption("o")) {
				ret.outputFile = cmd.getOptionValue("o");
			}
			if (cmd.hasOption("d")) {
				ret.attrSpecFile = cmd.getOptionValue("d");
			}
			if (cmd.hasOption("e")) {
				ret.attributesToExtract = cmd.getOptionValue("e").split("/");
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

	/**
	 * Try compiling with the specified input
	 *
	 * @param opts All command-line options
	 * @param errors Output-only: Errors from lexing and parsing
	 * @return The program (prior to semantic analysis).  Never <tt>null</tt>.
	 */
	public static Program tryParsing(CmdLineOpts opts, List<? super CompilerError> errors) {
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

		Program program = tryParsing(opts, compilerErrors);

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

			try {
				printReports(nameErrors);
			} catch (RuntimeException exn) {
				exn.printStackTrace();
			}
			try {
				printReports(semaErrors);
			} catch (RuntimeException exn) {
				exn.printStackTrace();
			}

			// Other reports
			try {
				printReports(program.reports());
			} catch (RuntimeException exn) {
				exn.printStackTrace();
			}

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

		case ATTR_DIFF:
			attrDiff(program, opts);
			return true;

		case ATTR_EXTRACT:
			return Compiler.attrExtract(program, opts);

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

		ArrayList<Report> reports = new ArrayList<>();
		Program result = tryParsing(opts, reports);
		try {
			// Make sure to report parser errors
			result.reports().addAll(reports);
		} catch (RuntimeException exn) {
			// internal error during reports()?
			exn.printStackTrace();

			// Nasty hackery: Forcefully set reports()
			reports.add(new InternalErrorReport(result, "INTERNAL ERROR: Could not evaluate Program.reports()", exn));
			try {
				Field field = Program.class.getDeclaredField("Program_reports_computed");
				field.setAccessible(true);
				field.set(result, true);

				field = Program.class.getDeclaredField("Program_reports_value");
				field.setAccessible(true);
				field.set(result, reports);
			} catch (Throwable exn2) {
				exn2.printStackTrace();
			}
		}
		return result;
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

	static final class InternalErrorReport extends Report {
		private String[] lines;
		public InternalErrorReport(Program program,
					   String comment,
					   Throwable exn) {
			super("internal", Report.CodeProber.ERR,
			      program);
			java.io.StringWriter swriter = new java.io.StringWriter();
			java.io.PrintWriter pwriter = new java.io.PrintWriter(swriter);
			exn.printStackTrace(pwriter);
			this.withExplanation(comment);
			this.lines = swriter.toString().split("\n");
		}

		@Override
		public Object cpr_getOutput() {
			return this.lines;
		}
	}
}
