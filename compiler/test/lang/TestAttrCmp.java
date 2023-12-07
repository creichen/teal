package lang;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;
import static org.junit.Assert.*;

import lang.ast.TEALParser;
import lang.ast.LangScanner;
import lang.ast.Program;
import lang.ast.Module;
import lang.ast.FunDecl;
import lang.ast.ASTNode;

import lang.attrcmp.*;

public class TestAttrCmp {
    static Program parse(String program_src) {
	Program program = new Program();
	try {
	    LangScanner scanner = new LangScanner(new StringReader(program_src));
	    TEALParser parser = new TEALParser();
	    program.addModule((Module) parser.parse(scanner));
	} catch (Exception exn) {
	    throw new RuntimeException(exn);
	}
	return program;
    }

    @Test
    public void testNodeIDIndexEncoding() {
	int[] offset = new int[] {0};
	for (int i = 0; i < 36*36+10; ++i) {
	    offset[0] = 0;
	    char[] encoding = NodeIDMap.indexFromInt(i).toCharArray();
	    assertEquals(i, NodeIDMap.indexToInt(encoding, offset));
	    assertEquals(offset[0], encoding.length);
	}
    }

    static void testPath(int... path) {
	String encoding = NodeIDMap.pathStringFromPath(path, path.length);
	assertArrayEquals(path,
			  NodeIDMap.pathFromPathString(encoding));
    }

    @Test
    public void testNodePathEncoding() {
	int i, j, k;
	testPath();
	for (i = 0; i < 40; ++i) {
	    testPath(i);
	}
	for (i = 0; i < 40; ++i) {
	    testPath(i, i);
	}
	for (i = 0; i < 40; ++i) {
	    testPath(i, i, i);
	}
	for (int a : new int[] {0, 1, 9, 10, 11, 35, 36, 37, 38}) {
	    for (int b : new int[] {0, 1, 9, 10, 11, 35, 36, 37, 38}) {
		for (int c : new int[] {0, 1, 9, 10, 11, 35, 36, 37, 38}) {
		    testPath(a, b, c);
		}
	    }
	}
    }

    @Test
    public void testNodeIDMap() {
	Program p = parse("var x : int := 1; fun f(y) = y + (x * (y + x));");
	NodeIDMap nidmap = NodeIDMap.from(p);
	assertTrue(nidmap.keys().size() > 10);
	for (String node_id : nidmap.keys()) {
	    assertEquals(node_id,
			 nidmap.getNodeID(nidmap.getNode(node_id)));
	}
	assertTrue(nidmap.getNode("@") instanceof Program);
	assertTrue(nidmap.getNode("@00") instanceof Module);
	assertTrue(nidmap.getNode("@0011") instanceof FunDecl);
    }


    public static final boolean getBooleanTrue() {
	return true;
    }

    public static final int getInt7() {
	return 7;
    }

    public static final long getLong7() {
	return 7;
    }

    public static final char getChar7() {
	return 7;
    }

    public static final short getShort7() {
	return 7;
    }

    public static final byte getByte7() {
	return 7;
    }

    public static final float getFloat7() {
	return 7.0f;
    }

    public static final double getDouble7() {
	return 7.0;
    }

    private static Object call(String method_name) {
	try {
	    Method method = TestAttrCmp.class.getMethod(method_name);
	    return method.invoke(null);
	} catch (Exception exn) {
	    throw new RuntimeException(exn);
	}
    }

    @Test
    public void testAutobox() {
	assertEquals(true, AttributeSummary.autobox(call("getBooleanTrue")));
	assertTrue(AttributeSummary.autobox(call("getBooleanTrue")) instanceof Boolean);

	assertEquals(7, AttributeSummary.autobox(call("getInt7")));
	assertTrue(AttributeSummary.autobox(call("getInt7")) instanceof Integer);

	assertEquals((short)7, AttributeSummary.autobox(call("getShort7")));
	assertTrue(AttributeSummary.autobox(call("getShort7")) instanceof Short);

	assertEquals(7l, AttributeSummary.autobox(call("getLong7")));
	assertTrue(AttributeSummary.autobox(call("getLong7")) instanceof Long);

	assertEquals('\007', AttributeSummary.autobox(call("getChar7")));
	assertTrue(AttributeSummary.autobox(call("getChar7")) instanceof Character);

	assertEquals((byte)7, AttributeSummary.autobox(call("getByte7")));
	assertTrue(AttributeSummary.autobox(call("getByte7")) instanceof Byte);

	assertEquals(7.0f, AttributeSummary.autobox(call("getFloat7")));
	assertTrue(AttributeSummary.autobox(call("getFloat7")) instanceof Float);

	assertEquals(7.0, AttributeSummary.autobox(call("getDouble7")));
	assertTrue(AttributeSummary.autobox(call("getDouble7")) instanceof Double);
    }

    @Test
    public void testEscaping() {
	for (String s : new String[] {
		"",
		"1",
		"foobar",
		"the quick brown fox jumped over the lazy dog"
	    }) {
	    assertTrue(s.equals(AttributeSummary.escape(s)));
	    assertEquals(s, AttributeSummary.unescape(AttributeSummary.escape(s)));
	}

	for (String s : new String[] {
		"yes, and no",
		"!",
		"=42",
		"foo:bar",
		"and now\nfor something completely different.",
		"\t1\t2\t3\n\n\t5?",
		"?",
		"##",
		"\"yes\"",
		"\\1\\2\r\n\t",
	    }) {
	    assertFalse(s.equals(AttributeSummary.escape(s)));
	    assertEquals(s, AttributeSummary.unescape(AttributeSummary.escape(s)));
	}
    }

    @Test
    public void testSplit() {
	assertArrayEquals(new String[] { "int" },
			  AttributeSummary.splitTyArgs("int"));

	assertArrayEquals(new String[] { "int", "float" },
			  AttributeSummary.splitTyArgs("int,float"));

	assertArrayEquals(new String[] { "A<>" },
			  AttributeSummary.splitTyArgs("A<>"));

	assertArrayEquals(new String[] { "A<>", "B", "C<>" },
			  AttributeSummary.splitTyArgs("A<>,B,C<>"));

	assertArrayEquals(new String[] { "A<>", "B", "C<>" },
			  AttributeSummary.splitTyArgs("A<> ,B  ,  C<>"));

	assertArrayEquals(new String[] { "A<>", "B", "C<>" },
			  AttributeSummary.splitTyArgs(" A<>, B,C<>  "));

	assertArrayEquals(new String[] { "A<1>", "B", "C<2>" },
			  AttributeSummary.splitTyArgs(" A<1>, B,C<2>  "));

	assertArrayEquals(new String[] { "A<1,2>" },
			  AttributeSummary.splitTyArgs(" A<1,2> "));

	assertArrayEquals(new String[] { "A<1, 2>" },
			  AttributeSummary.splitTyArgs(" A<1, 2> "));

	assertArrayEquals(new String[] { "A<float<2,2>>", "B", "C<D<A,1,1>,3>" },
			  AttributeSummary.splitTyArgs(" A<float<2,2>>, B,C<D<A,1,1>,3>  "));

	assertNull(AttributeSummary.splitTyArgs(" A<1, 2 "));
	assertNull(AttributeSummary.splitTyArgs(" A<<1, 2> "));
	assertNull(AttributeSummary.splitTyArgs(" A<1, 2>> "));
	assertNull(AttributeSummary.splitTyArgs(" ,A<1, 2> "));
	assertNull(AttributeSummary.splitTyArgs(" A,,B "));

    }

    @Test
    public void testEntriesName() {
	Program p = parse("var x : int := 1; fun f(y) = y + (x * (y + x));");
	AttributeSummary.WithAST asummary = AttributeSummary.withAST(p).empty();
	asummary.withAttribute("name", "String");
	assertEquals(Arrays.asList(
				   "@0010 VarDecl name:String =x",
				   "@001010 IntType name:String =int",
				   "@0011 FunDecl name:String =f",
				   "@001120 VarDecl name:String =y",
				   "@00113000 Access name:String =y",
				   "@001130010 Access name:String =x",
				   "@0011300110 Access name:String =y",
				   "@0011300111 Access name:String =x"
		), asummary.entries(false));
    }

    // @Test
    // public void testEntriesDirectDependencies() {
    // 	Program p = parse("fun f(x) = x; fun g(y) = f(y); fun h(z) = f(g(z));");
    // 	AttributeSummary.WithAST asummary = AttributeSummary.withAST(p).empty();
    // 	asummary.withAttribute("directDependencies", "Set<ASTNode>");
    // 	assertEquals(Arrays.asList("@00 Module directDependencies:Set<ASTNode> ={}",
    // 				   "@0010 FunDecl directDependencies:Set<ASTNode> ={}",
    // 				   "@001020 VarDecl directDependencies:Set<ASTNode> ={}",
    // 				   "@0011 FunDecl directDependencies:Set<ASTNode> ={=@0010}",
    // 				   "@001120 VarDecl directDependencies:Set<ASTNode> ={}",
    // 				   "@0012 FunDecl directDependencies:Set<ASTNode> ={=@0010,=@0011}",
    // 				   "@001220 VarDecl directDependencies:Set<ASTNode> ={}"
    // 		), asummary.entries(false));
    // }

    // @Test
    // public void testEntriesDirectDependencies2() {
    // 	Program p = parse("fun f(x) = print((x + 1) - 2 * x);");
    // 	AttributeSummary.WithAST asummary = AttributeSummary.withAST(p).empty();
    // 	asummary.withAttribute("directDependencies", "Set<ASTNode>");
    // 	assertEquals(Arrays.asList("@00 Module directDependencies:Set<ASTNode> ={}",
    // 				   "@0010 FunDecl directDependencies:Set<ASTNode> ={=@}",
    // 				   "@001020 VarDecl directDependencies:Set<ASTNode> ={}"
    // 		), asummary.entries(false));
    // }

    @Test
    public void testUnparseParseWithAST() {
	Program p = parse("fun f(x) = print((x + 1) - 2 * x);");
	AttributeSummary.WithAST asummary_base = AttributeSummary.withAST(p).empty();
	asummary_base.withAttribute("succ", "SmallSet<ASTNode>");
	String stringified = asummary_base.writeToString();
	AttributeSummary asummary = AttributeSummary.withAST(p).from(stringified);
	assertEquals(Arrays.asList(
				   "@001020 VarDecl succ:SmallSet<ASTNode> ={}",
				   "@0010300 CallExpr succ:SmallSet<ASTNode> ={=@00106}",
				   "@001030010 SubExpr succ:SmallSet<ASTNode> ={=@0010300}",
				   "@0010300100 AddExpr succ:SmallSet<ASTNode> ={=@00103001010}",
				   "@00103001000 Access succ:SmallSet<ASTNode> ={=@00103001001}",
				   "@00103001001 IntConstant succ:SmallSet<ASTNode> ={=@0010300100}",
				   "@0010300101 MulExpr succ:SmallSet<ASTNode> ={=@001030010}",
				   "@00103001010 IntConstant succ:SmallSet<ASTNode> ={=@00103001011}",
				   "@00103001011 Access succ:SmallSet<ASTNode> ={=@0010300101}",
				   "@00105 Entry succ:SmallSet<ASTNode> ={=@00103001000}",
				   "@00106 Exit succ:SmallSet<ASTNode> ={}"
		), asummary.entries(false));
    }

    @Test
    public void testUnparseParseWithoutAST() {
	Program p = parse("fun f(x) = print((x + 1) - 2 * x);");
	AttributeSummary.WithAST asummary_base = AttributeSummary.withAST(p).empty();
	asummary_base.withAttribute("succ", "SmallSet<ASTNode>");
	String stringified = asummary_base.writeToString();
	AttributeSummary asummary = AttributeSummary.withoutAST().from(stringified);
	assertEquals(Arrays.asList(
				   "@001020 VarDecl succ:SmallSet<ASTNode> ={}",
				   "@0010300 CallExpr succ:SmallSet<ASTNode> ={=@00106}",
				   "@001030010 SubExpr succ:SmallSet<ASTNode> ={=@0010300}",
				   "@0010300100 AddExpr succ:SmallSet<ASTNode> ={=@00103001010}",
				   "@00103001000 Access succ:SmallSet<ASTNode> ={=@00103001001}",
				   "@00103001001 IntConstant succ:SmallSet<ASTNode> ={=@0010300100}",
				   "@0010300101 MulExpr succ:SmallSet<ASTNode> ={=@001030010}",
				   "@00103001010 IntConstant succ:SmallSet<ASTNode> ={=@00103001011}",
				   "@00103001011 Access succ:SmallSet<ASTNode> ={=@0010300101}",
				   "@00105 Entry succ:SmallSet<ASTNode> ={=@00103001000}",
				   "@00106 Exit succ:SmallSet<ASTNode> ={}"
		), asummary.entries(false));
    }

    @Test
    public void testUnparseParseWithoutASTEmptyDiffShort() {
	Program p = parse("fun f(x) = print((x + 1) - 2 * x);");
	AttributeSummary.WithAST asummary_base = AttributeSummary.withAST(p).empty();
	asummary_base.withAttribute("succ", "SmallSet<ASTNode>");
	String stringified = asummary_base.writeToString();
	AttributeSummary asummary1 = AttributeSummary.withoutAST().from(stringified);
	AttributeSummary asummary2 = AttributeSummary.withoutAST().from(stringified);
	assertTrue(asummary1.diff(asummary2).isEmpty());
    }

    @Test
    public void testUnparseParseWithoutASTEmptyDiffLong() {
	Program p = parse("var x : int := 1; fun f(y) = y + (x * (y + x));");
	AttributeSummary.WithAST asummary_base = AttributeSummary.withAST(p).empty();
	asummary_base.withAttribute("name", "String");
	asummary_base.withAttribute("decl", "ASTNode");
	asummary_base.withAttribute("succ", "SmallSet<ASTNode>");
	String stringified = asummary_base.writeToString();
	AttributeSummary asummary1 = AttributeSummary.withoutAST().from(stringified);
	AttributeSummary asummary2 = AttributeSummary.withoutAST().from(stringified);
	assertTrue(asummary1.diff(asummary2).isEmpty());
    }

    @Test
    public void testEmptyDiff() {
	Program p = parse("fun f(x) = print((x + 1) - 2 * x);");
	AttributeSummary.WithAST asummary = AttributeSummary.withAST(p).empty();
	assertTrue(asummary.diff(asummary).isEmpty());
	asummary.withAttribute("succ", "SmallSet<ASTNode>");
	assertTrue(asummary.diff(asummary).isEmpty());
	asummary.withAttribute("name", "String");
	assertTrue(asummary.diff(asummary).isEmpty());
	asummary.withAttribute("decl", "ASTNode");
	assertTrue(asummary.diff(asummary).isEmpty());
    }

    @Test
    public void testEmptyDiffSeparateParse() {
	Program p1 = parse("fun f(x) = print((x + 1) - 2 * x);");
	Program p2 = parse("fun f(x) = print((x + 1) - 2 * x);");
	AttributeSummary.WithAST asummary1_with_ast = AttributeSummary.withAST(p1).empty();
	AttributeSummary.WithAST asummary2_with_ast = AttributeSummary.withAST(p2).empty();

	// empty
	AttributeSummary asummary1, asummary2;
	asummary1 = AttributeSummary.withoutAST().from(asummary1_with_ast.writeToString());
	asummary2 = AttributeSummary.withoutAST().from(asummary2_with_ast.writeToString());

	// +succ
	asummary1_with_ast.withAttribute("succ", "SmallSet<ASTNode>");
	asummary2_with_ast.withAttribute("succ", "SmallSet<ASTNode>");
	asummary1 = AttributeSummary.withoutAST().from(asummary1_with_ast.writeToString());
	asummary2 = AttributeSummary.withoutAST().from(asummary2_with_ast.writeToString());
	assertTrue(asummary1.diff(asummary2).isEmpty());

	// +name
	asummary1_with_ast.withAttribute("name", "String");
	asummary2_with_ast.withAttribute("name", "String");
	asummary1 = AttributeSummary.withoutAST().from(asummary1_with_ast.writeToString());
	asummary2 = AttributeSummary.withoutAST().from(asummary2_with_ast.writeToString());
	assertTrue(asummary1.diff(asummary2).isEmpty());

	// +decl
	asummary1_with_ast.withAttribute("decl", "ASTNode");
	asummary2_with_ast.withAttribute("decl", "ASTNode");
	asummary1 = AttributeSummary.withoutAST().from(asummary1_with_ast.writeToString());
	asummary2 = AttributeSummary.withoutAST().from(asummary2_with_ast.writeToString());
	assertTrue(asummary1.diff(asummary2).isEmpty());
    }

    @Test
    public void testSimpleDiffSeparateParse() {
	Program p1 = parse("fun f(x) = print((x + 1) - 2 * x);");
	Program p2 = parse("fun g(x) = print((x + 1) - 2 * x);");
	AttributeSummary.WithAST asummary1_with_ast = AttributeSummary.withAST(p1).empty();
	AttributeSummary.WithAST asummary2_with_ast = AttributeSummary.withAST(p2).empty();

	// empty
	AttributeSummary asummary1, asummary2;
	asummary1 = AttributeSummary.withoutAST().from(asummary1_with_ast.writeToString());
	asummary2 = AttributeSummary.withoutAST().from(asummary2_with_ast.writeToString());

	// +succ
	asummary1_with_ast.withAttribute("succ", "SmallSet<ASTNode>");
	asummary2_with_ast.withAttribute("succ", "SmallSet<ASTNode>");
	asummary1 = AttributeSummary.withoutAST().from(asummary1_with_ast.writeToString());
	asummary2 = AttributeSummary.withoutAST().from(asummary2_with_ast.writeToString());
	assertTrue(asummary1.diff(asummary2).isEmpty());

	// +name
	asummary1_with_ast.withAttribute("name", "String");
	asummary2_with_ast.withAttribute("name", "String");
	asummary1 = AttributeSummary.withoutAST().from(asummary1_with_ast.writeToString());
	asummary2 = AttributeSummary.withoutAST().from(asummary2_with_ast.writeToString());

	Diff diff = asummary1.diff(asummary2);
	assertFalse(diff.isEmpty());
	assertEquals(1, diff.size());
	assertEquals(1, diff.stream()
		     .filter(e -> e.hasNodeType("FunDecl"))
		     .collect(Collectors.toList())
		     .size());
	Diff.Individual match = diff.all().get(0);
	assertEquals("name", match.getAttribute().getName());
	assertEquals("@0010", match.getNodeID());
    }
}
