package lang.common;

import java.io.PrintStream;

public class Debug {
	private static boolean debugInterpreter = false;
	private static boolean debugIRGen = false;
	private static Debug logger = new Debug();
	private PrintStream out = System.out;

	private Debug() {
		String debugOpts = System.getenv("TEAL_DEBUG");
		if (debugOpts != null) {
			if (debugOpts.contains("interp"))
				debugInterpreter = true;
			if (debugOpts.contains("irgen"))
				debugIRGen = true;
		}
	}

	public static void dbgi(String str) {
		if (debugInterpreter) {
			logger.out.println("[INTERP] " + str);
		}
	}

	public static void dbgt(String str) {
		if (debugInterpreter) {
			logger.out.println("[IRGEN] " + str);
		}
	}
}
