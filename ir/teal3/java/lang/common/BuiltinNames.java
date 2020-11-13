package lang.common;

import java.util.ArrayList;

/**
 * Names of predefined types and operations.  See "Builtins" for implementations of builtin operations.
 */
public final class BuiltinNames {
    // Predefined types
    public static final String INT = "int";
    public static final String STRING = "string";
    public static final String ANY = "any";
    public static final String ARRAY = "array";
    // Reserved type Qualifier (for general-purpose type qualifiers)
    public static final String QUALIFIER = "Qualifier";


    // Built-in operations
    // Plus can be used for strings and integer alike.
    public static final Operation INT_ADD = ARITHMETIC_BINOP("__builtin_int_add");
    public static final Operation INT_SUB = ARITHMETIC_BINOP("__builtin_int_sub");
    public static final Operation INT_MUL = ARITHMETIC_BINOP("__builtin_int_mul");
    public static final Operation INT_DIV = ARITHMETIC_BINOP("__builtin_int_div");
    public static final Operation INT_MOD = ARITHMETIC_BINOP("__builtin_int_mod");

    public static final Operation ANY_EQ = new Operation("__builtin_any_eq", INT, ANY, ANY);
    public static final Operation ANY_NEQ = new Operation("__builtin_any_neq", INT, ANY, ANY);

    public static final Operation INT_LEQ = ARITHMETIC_BINOP("__builtin_int_leq");
    public static final Operation INT_GEQ = ARITHMETIC_BINOP("__builtin_int_geq");
    public static final Operation INT_LT = ARITHMETIC_BINOP("__builtin_int_lt");
    public static final Operation INT_GT = ARITHMETIC_BINOP("__builtin_int_gt");

    public static final Operation INT_AND = ARITHMETIC_BINOP("__builtin_int_logical_and");
    public static final Operation INT_OR = ARITHMETIC_BINOP("__builtin_int_logical_or");

    public static final Operation CONCAT = new Operation("concat", STRING, STRING, STRING);
    public static final Operation PRINT = new Operation("print", ANY, ANY);
    public static final Operation READ = new Operation("read", STRING);

    // Additional builtins
    public static final Operation STRING_TO_INT = new Operation("string_to_int", INT, STRING);
    public static final Operation INT_TO_STRING = new Operation("int_to_string", STRING, INT);
    public static final Operation CAN_CONVERT_TO_INT = new Operation("can_convert_to_int", INT, STRING);
    public static final Operation ARRAY_LENGTH = new Operation("array_length", INT, ARRAY);


    private static ArrayList<Operation> operations;
    static void addOperation(Operation op) {
	if (BuiltinNames.operations == null) {
	    BuiltinNames.operations = new ArrayList<>();
	}
	BuiltinNames.operations.add(op);
    }
    public static ArrayList<? extends Operation> getOperations() {
	return BuiltinNames.operations;
    }

    /**
     * Encodes a built-in operation along with its types
     */
    public static final class Operation {
	private String name;
	private String ret_type;
	private String[] arg_types;

	public Operation(String name, String ret_type, String ... arg_types) {
	    this.name = name;
	    this.ret_type = ret_type;
	    this.arg_types = arg_types;
	    BuiltinNames.addOperation(this);
	}

	public String getName() {
	    return this.name;
	}

	public String getReturnType() {
	    return this.ret_type;
	}

	public String[] getArgumentTypes() {
	    return this.arg_types;
	}
    }

    public static Operation BINOP(String name, String ret_type, String lhs_type, String rhs_type) {
	return new Operation(name, ret_type, lhs_type, rhs_type);
    }

    public static Operation ARITHMETIC_BINOP(String name) {
	return new Operation(name, INT, INT, INT);
    }

}
