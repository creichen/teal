package lang.ir;

import lang.common.BuiltinNames;
import java.util.HashMap;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;


/**
 * Implementations of predefined operations for the interpreter.  These must match the ones in "BuiltinNames".
 */
public final class Builtins {

    private static HashMap<String, Type<?>> typeTranslator = new HashMap<>();
    private static HashMap<String, Operation> opTranslator = new HashMap<>();

    // Predefined types used by builtin operations
    public static final Type<IRIntegerValue> INT = new Type<>(BuiltinNames.INT, IRIntegerValue.class);
    public static final Type<IRStringValue> STRING = new Type<>(BuiltinNames.STRING, IRStringValue.class);
    public static final Type<IRValue> ANY = new Type<>(BuiltinNames.ANY, IRValue.class);
    public static final Type<IRArray> ARRAY = new Type<>(BuiltinNames.ARRAY, IRArray.class);

    // Declare method implementations
    static {
	// If you want to add a new builtin operation, declare it below.
	INT_OP(BuiltinNames.INT_ADD, ctx -> ctx.getInt(0) + ctx.getInt(1));
	INT_OP(BuiltinNames.INT_SUB, ctx -> ctx.getInt(0) - ctx.getInt(1));
	INT_OP(BuiltinNames.INT_MUL, ctx -> ctx.getInt(0) * ctx.getInt(1));
	INT_OP(BuiltinNames.INT_DIV, ctx -> ctx.getInt(0) / ctx.getNonzeroInt(1, "Division by 0"));
	INT_OP(BuiltinNames.INT_MOD, ctx -> ctx.getInt(0) % ctx.getNonzeroInt(1, "Division by 0"));

	BOOL_OP(BuiltinNames.ANY_EQ, ctx -> ctx.get(0).equalsIR(ctx.get(1)));
	BOOL_OP(BuiltinNames.ANY_NEQ, ctx -> !ctx.get(0).equalsIR(ctx.get(1)));

	BOOL_OP(BuiltinNames.INT_LEQ, ctx -> ctx.getInt(0) <= ctx.getInt(1));
	BOOL_OP(BuiltinNames.INT_GEQ, ctx -> ctx.getInt(0) >= ctx.getInt(1));
	BOOL_OP(BuiltinNames.INT_LT, ctx -> ctx.getInt(0) < ctx.getInt(1));
	BOOL_OP(BuiltinNames.INT_GT, ctx -> ctx.getInt(0) > ctx.getInt(1));
	BOOL_OP(BuiltinNames.INT_AND, ctx -> (ctx.getInt(0) != 0 && ctx.getInt(1) != 0));
	BOOL_OP(BuiltinNames.INT_OR, ctx -> (ctx.getInt(0) != 0 || ctx.getInt(1) != 0));
	VOID_OP(BuiltinNames.PRINT, ctx -> System.out.println(ctx.get(0).toShortString()));
	OP(BuiltinNames.READ, ctx -> {
		try {
		    // How many objects does one need to read a line from stdin in Java? ;)
		    String line = (new BufferedReader(new InputStreamReader(System.in))).readLine();
		    return new IRStringValue(line);
		} catch (IOException e) {
		    throw new InterpreterException("Exception while executing read().");
		}
	    });
	OP(BuiltinNames.STRING_TO_INT, ctx -> new IRIntegerValue(Integer.parseInt(ctx.getString(0))));
	OP(BuiltinNames.INT_TO_STRING, ctx -> new IRStringValue(Long.toString(ctx.getInt(0))));
	OP(BuiltinNames.CAN_CONVERT_TO_INT, ctx -> {
			// Regex copied from the scanner specification
			Boolean can_convert = ctx.getString(0).matches("-?(0|[1-9][0-9]*)");
			if (can_convert) {
				return new IRIntegerValue(1);
			} else {
				return new IRIntegerValue(0);
			}
		});
	OP(BuiltinNames.CONCAT, ctx -> new IRStringValue(ctx.getString(0).concat(ctx.getString(1))));
	OP(BuiltinNames.ARRAY_LENGTH, ctx -> new IRIntegerValue(ctx.getArray(0).getSize()));
	INT_OP(BuiltinNames.TIME, ctx -> {
			Instant t = Instant.now();
			return t.getEpochSecond() * 1_000_000_000 + t.getNano();
		});
    }

    private static Type<?> translateType(String typename) {
	Type<?> ty = typeTranslator.get(typename);
	if (ty == null) {
	    throw new RuntimeException("Interpreter initalisation error: unknown type '" + typename + "' or type is not yet supported for builtin operations");
	}
	return ty;
    }

    /**
     * Map a Teal built-in operation name to an executable operation object
     */
    public static Operation translateOperation(String opname) {
	Operation op = opTranslator.get(opname);
	if (op == null) {
	    throw new RuntimeException("Interpreter linkage error: unknown or unimplemented built-in operation '" + opname + "'");
	}
	return op;
    }

    /**
     * Dynamic type checking for a Teal type, for built-in operations
     */
    public static class Type<T> {
	private Class<T> classobj;
	private String typename;

	public Type(String name, Class<T> classobj) {
	    this.classobj = classobj;
	    this.typename = name;
	    Builtins.typeTranslator.put(name, this);
	}

	public void checkArg(IRValue v, String op, int index) throws InterpreterException {
	    if (!this.classobj.isInstance(v)) {
		throw new InterpreterException("IR error: while calling builtin operation " + op + ", parameter #" + index
					       + " expects " + this.typename
					       + " but received " + v.getClass());
	    }
	}

	public void checkReturn(IRValue v, String op) throws InterpreterException {
	    if (!this.classobj.isInstance(v)) {
		throw new InterpreterException("IR internal error: builtin operation " + op + " promised " + this.typename
					       + " but returned " + v);
	    }
	}
    }

    /**
     * Represents a Java implementation of a built-in Teal operation, with linkage information
     */
    public static final class Operation {
	private BuiltinImplementation<IRValue> implementation;
	private String name;
	private Type<?>[] arg_types;
	private Type<?> ret_type;

	Operation(BuiltinNames.Operation op, BuiltinImplementation<IRValue> impl) {
	    this.implementation = impl;
	    String[] arg_type_names = op.getArgumentTypes();
	    this.arg_types = new Type<?>[arg_type_names.length];
	    for (int i = 0; i < arg_type_names.length; i++) {
		this.arg_types[i] = Builtins.translateType(arg_type_names[i]);
	    }
	    this.ret_type = Builtins.translateType(op.getReturnType());
	    this.name = op.getName();

	    opTranslator.put(op.getName(), this);
	}

	/**
	 * Run the function
	 *
	 * CHecks arguments for type correctness
	 */
	public IRValue eval(IRFunctionEvalCtx ctx) throws InterpreterException {
	    if (ctx.getArgsNr() != this.arg_types.length) {
		throw new InterpreterException("Wrong number of arguments to built-in operation " + this.name
					       + ": expected " + this.arg_types.length + ", got " + ctx.getArgsNr());
	    }
	    for (int i = 0; i < ctx.getArgsNr(); i++) {
		this.arg_types[i].checkArg(ctx.getArg(i), this.name, i);
	    }
	    IRValue v = this.implementation.apply(new Args(ctx));
	    this.ret_type.checkReturn(v, this.name);
	    return v;
	}

	/**
	 * Represents actual parameters passed to evaluate the builtin operation
	 */
	public final class Args {
	    IRFunctionEvalCtx context;
	    public Args(IRFunctionEvalCtx ctx) {
		this.context = ctx;
	    }

	    /**
	     * Returns one of the arguments
	     */
	    public IRValue get(int offset) {
		return this.context.getArg(offset);
	    }

	    /**
	     * Returns one of the arguments and translates it to long
	     */
	    public long getInt(int offset) {
		return ((IRIntegerValue) this.get(offset)).asLong();
	    }

            /**
             * Returns an array argument, with dynamic checking
             */
            public IRArray getArray(int offset) {
                return (IRArray) this.get(offset);
            }

	    /**
	     * Returns one of the arguments and translates it to long
	     */
	    public long getNonzeroInt(int offset, String msg) throws InterpreterException {
		long i = this.getInt(offset);
		if (i == 0) {
		    throw new InterpreterException(msg);
		}
		return i;
	    }

	    /**
	     * Returns one of the arguments and translates it to String
	     */
	    public String getString(int offset) {
		return ((IRStringValue) this.get(offset)).asString();
	    }
	}
    }

    /**
     * Helper operation for declaring and linking a builtin implementation.
     *
     * This is the general-purpose version.
     */
    private static void OP(BuiltinNames.Operation op, BuiltinImplementation<IRValue> impl) {
	new Operation(op, impl);
    }

    /**
     * Helper operation for declaring and linking a builtin implementation that returns a Long.
     *
     * Automatically converts the Long into the IR format.
     */
    private static void INT_OP(BuiltinNames.Operation op, BuiltinImplementation<Long> impl) {
	new Operation(op, ctx -> new IRIntegerValue(impl.apply(ctx)));
    }

    /**
     * Helper operation for declaring and linking a builtin implementation that returns a Boolean.
     *
     * Automatically converts the Boolean into the IR format.
     */
    private static void BOOL_OP(BuiltinNames.Operation op, BuiltinImplementation<Boolean> impl) {
	new Operation(op, ctx -> new IRIntegerValue(impl.apply(ctx) ? 1 : 0));
    }

    /**
     * Helper operation for declaring and linking a builtin that returns nothing.
     *
     * Automatically returns an IR null value.
     */
    private static void VOID_OP(BuiltinNames.Operation op, BuiltinConsumerImplementation impl) {
	new Operation(op, ctx -> { impl.apply(ctx); return new IRNullValue(null); });
    }

    static interface BuiltinImplementation<T> {
	public T apply(Operation.Args args) throws InterpreterException;
    }

    static interface BuiltinConsumerImplementation {
	public void apply(Operation.Args args) throws InterpreterException;
    }

    static {
	// Check that we have implemented all operations declared in BuiltinNames
	// Must come last!
	int failures = 0;

	for (BuiltinNames.Operation op : BuiltinNames.getOperations()) {
	    String opname = op.getName();
	    if (!Builtins.opTranslator.containsKey(opname)) {
		System.err.println(Builtins.class + " lacks implementation of builtin operation `" + opname + "'");
		failures += 1;
	    }
	}
	if (failures > 0) {
	    throw new RuntimeException("IR interpreter is missing " + failures + " built-in operation implementations");
	}
    }
}
