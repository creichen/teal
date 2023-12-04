package lang.attrcmp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lang.ast.ASTNode;
import lang.ast.Program;

/**
 *
 */
public abstract class AttributeSummary {
	protected List<? extends String> nodes;
	protected Map<String, String> node_type = new HashMap<>();
	protected List<Attribute> attributes = new ArrayList<>();
	protected Map<String, EntryMap> entries = new HashMap<>();

	protected AttributeSummary() {
	}

	/**
	 * A collection of all attributes collected for this tree
	 *
	 * @param include_empty Include empty AST nodes
	 */
	public Collection<String> entries(boolean include_empty) {
		List<String> results = new ArrayList<>();
		for (String node : nodes) {
			String prefix = node + " " + this.node_type.get(node);
			EntryMap emap = this.entries.get(node);
			if (emap.isEmpty()) {
				if (include_empty) {
					results.add(prefix);
				}
			}
			for (String element : emap.encodeElements()) {
				results.add(prefix + " " + element);
			}
		}
		return results;
	}

	/**
	 * The standard serialised form
	 */
	public Collection<String> serializedForm() {
		List<String> results = new ArrayList<>();
		for (Attribute attr : this.attributes) {
			results.add("." + attr.signature());
		}
		results.addAll(this.entries(true));
		return results;
	}

	public void writeTo(File f) throws IOException {
		try (OutputStream ostream = new FileOutputStream(f)) {
			this.writeTo(ostream);
		}
	}

	public void writeTo(OutputStream ostream) throws IOException {
		try (PrintStream ps = new PrintStream(ostream)) {
			this.writeTo(ps);
		}
	}

	public void writeTo(Writer writer) throws IOException {
		try ( PrintWriter pw = new PrintWriter(writer)) {
			this.writeTo(pw);
		}
	}

	public void writeTo(PrintStream pstream) throws IOException {
		try ( PrintWriter pw = new PrintWriter(pstream)) {
			this.writeTo(pw);
		}
	}

	public void writeTo(PrintWriter pw) throws IOException {
		for (String s : this.serializedForm()) {
			pw.println(s);
		}
		pw.flush();
	}

	public String writeToString() {
		StringWriter sw = new StringWriter();
		try {
			this.writeTo(sw);
		} catch (IOException exn) {
			throw new RuntimeException(exn);
		}
		return sw.toString();
	}

	void setAttributes(List<Attribute> new_attributes) {
		this.attributes = new_attributes;
	}

	void addAttribute(Attribute attr) {
		this.attributes.add(attr);
	}

	public List<? extends Attribute> getAttributes()  {
		return this.attributes;
	}

	public AttributeSummary withAttributes(List<? extends Attribute> attributes) {
		for (Attribute attr : attributes) {
			if (!this.attributes.contains(attr)) {
				this.addAttribute(attr);
				this.updateAttribute(attr);
			}
		}
		return this;
	}

	public AttributeSummary withAttributesFrom(AttributeSummary asumm) {
		return this.withAttributes(asumm.getAttributes());
	}

	public static BuilderWithoutAST withoutAST() {
		return new BuilderWithoutAST(new WithoutAST());
	}

	public static BuilderWithAST withAST(Program program) {
		return withNodeIDMap(NodeIDMap.from(program));
	}

	public static BuilderWithAST withNodeIDMap(NodeIDMap nidmap) {
		return new BuilderWithAST(new WithAST(nidmap));
	}

	EntryMap entryMap(String node_id) {
		if (this.entries.containsKey(node_id)) {
			return this.entries.get(node_id);
		}
		EntryMap entrymap = new EntryMap();
		this.entries.put(node_id, entrymap);
		return entrymap;
	}

	public Diff diff(AttributeSummary other) {
		Diff diff = new Diff();
		if (!this.nodes.equals(other.nodes)) {
			throw new ASTMismatchException();
		}
		for (String node_id : this.nodes) {
			String node_type = this.node_type.get(node_id);
			EntryMap self_em = this.entryMap(node_id);
			EntryMap other_em = other.entryMap(node_id);
			diff.add(node_id, node_type, self_em.diff(other_em));
		}
		return diff;
	}

	protected abstract void updateAttribute(Attribute attr);

	Attribute
	createAttribute(String attribute_name, String ty_name) {
		return new Attribute(attribute_name,
				     this.attributeTypeFromString(ty_name));
	}

	AttributeSummary
	withAttribute(Attribute attr) {
		if (this.attributes.contains(attr)) {
			return this;
		}
		this.attributes.add(attr);
		this.updateAttribute(attr);
		return this;
	}

	public AttributeSummary
	withAttribute(String attribute_name, String ty_name) {
		return this.withAttribute(this.createAttribute(attribute_name, ty_name));
	}

	public static abstract class Value<T> {
		public static final String ENCODE_NULL = "null";
		public static final String ENCODE_ANY = "?";
		public static final String ENCODE_VALUE_PREFIX = "=";

		public boolean isNull() {
			return false;
		}

		public boolean isIgnore() {
			return false;
		}

		public boolean isValue() {
			return false;
		}

		public T getValue() {
			throw new RuntimeException("Not a value: " + this);
		}

		public static final <S> Value<S> getNull() {
			return new NullValue<S>();
		}

		public static final <S> Value<S> getIgnore() {
			return new IgnoreValue<S>();
		}

		public static final <S> Value<S> from(S value) {
			if (value == null) {
				return getNull();
			}
			return new ActualValue<S>(value);
		}

		@Override
		public boolean equals(Object other) {
			if (other == null) {
				return false;
			}
			return other.getClass().equals(this.getClass());
		}
	}

	public static class NullValue<T> extends Value<T> {
		@Override
		public boolean isNull() {
			return true;
		}
		@Override
		public String toString() {
			return Value.ENCODE_NULL;
		}
	}

	public static class IgnoreValue<T> extends Value<T> {
		@Override
		public boolean isIgnore() {
			return true;
		}
		@Override
		public String toString() {
			return Value.ENCODE_ANY;
		}
	}

	public static class ActualValue<T> extends Value<T> {
		private T val;
		public ActualValue(T obj) {
			this.val = obj;
		}

		@Override
		public boolean isValue() {
			return true;
		}

		@Override
		public T getValue() {
			return this.val;
		}

		@Override
		public int hashCode() {
			return this.val.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (super.equals(other)) {
				ActualValue other_v = (ActualValue) other;
				return other_v.getValue().equals(this.getValue());
			}
			return false;
		}
		@Override
		public String toString() {
			return Value.ENCODE_VALUE_PREFIX + this.val;
		}
	}

	/**
	 * Summarises nodes of a particular type.
	 */
	public abstract class AttributeType<T> {
		public abstract Class<T> getEncodedType();

		/**
		 * Encodes a value as a string.
		 * The result must not contain any newline characters.
		 *
		 * For recursive encoding, use <tt>encodeQuoted()</tt> on inner elements.
		 */
		public String encode(T obj) {
			return obj.toString();
		}

		/**
		 * Encodes a value as a string.
		 * The result must not contain any newline characters.
		 *
		 * For recursive decoding, use <tt>decodeQuoted()</tt> on inner elements.
		 */
		public abstract Object decode(String str);

		public Object[] diff(T expected, T actual) {
			if (expected != actual) {
				return new Object[] {
					"expected:",
					expected,
					"actual:",
					actual
				};
			}
			return null;
		}

		public final String encodeQuoted(T obj) {
			return this.encodeValue(Value.from((T) obj), true);
		}

		public final Value<T> decodeQuoted(String str) {
			return (Value<T>) this.decodeValue(str, true);
		}

		public final String encodeValue(Value<T> obj, boolean escaped) {
			if (obj.isNull()) {
				return Value.ENCODE_NULL;
			} else if (obj.isIgnore()) {
				return Value.ENCODE_ANY;
			}
			String v = this.encode(obj.getValue());
			if (escaped) {
				v = escape(v);
			}
			return "=" + v;
		}

		public final Value<Object> decodeValue(String str, boolean escaped) {
			if (str.startsWith("=")) {
				String s = str.substring(1);
				if (escaped) {
					s = unescape(s);
				}
				return Value.from(decode(s));
			}
			else if (str.equals(Value.ENCODE_NULL)) {
				return Value.getNull();
			}
			else if (str.equals(Value.ENCODE_ANY)) {
				return Value.getIgnore();
			} else throw new RuntimeException("Cannot decode: '" + str + "'");
		}

		public abstract String typeName();

		public String signature() {
			return this.typeName();
		}

		public AttributeType<?> build(AttributeType<?>[] type_args) {
			if (type_args == null || type_args.length == 0) {
				return this;
			}
			throw new RuntimeException("No type parameters supported:" + this.typeName());
		}

		@Override
		public boolean equals(Object other) {
			if (other == null) {
				return false;
			}
			if (this.getClass().isInstance(other)
			    && other.getClass().isInstance(this)) {
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.getEncodedType().hashCode();
		}

		@Override
		public String toString() {
			return this.signature();
		}
	}

	public abstract class ParametricAttributeType<T> extends AttributeType<T> {
		protected final int num_type_args;
		protected AttributeType<?>[] type_args = null;
		protected ParametricAttributeType(int num_args) {
			this.num_type_args = num_args;
		}

		protected abstract AttributeType<?> buildWithParameters(AttributeType<?>[] type_args);

		public final AttributeType<?> build(AttributeType<?>[] type_args) {
			if (type_args.length == num_type_args) {
				ParametricAttributeType<?> result =
					(ParametricAttributeType<?>)
					this.buildWithParameters(type_args);
				result.type_args = type_args;
				return result;
			}
			throw new RuntimeException("Wrong number of type parameters for " + this.typeName());
		}

		@Override
		public boolean equals(Object other) {
			if (super.equals(other)) {
				ParametricAttributeType o = (ParametricAttributeType<?>) other;
				return Arrays.deepEquals(this.type_args, o.type_args);
			}
			return false;
		}

		@Override
		public String signature() {
			StringBuffer sb = new StringBuffer();
			sb.append(this.typeName());
			if (this.type_args != null) {
				sb.append("<");
				for (int i = 0; i < this.type_args.length; ++i) {
					if (i > 0) {
						sb.append(",");
					}
					sb.append(this.type_args[i].signature());
				}
				sb.append(">");
			}
			return sb.toString();
		}

		@Override
		public int hashCode() {
			int hash = super.hashCode();
			if (this.type_args != null) {
				for (Object obj : this.type_args) {
					hash = (hash << 2) ^ obj.hashCode();
				}
			}
			return hash;
		}
	}

	public abstract class AttributeTypeSemanticEq<T> extends AttributeType<T> {
		public Object[] diff(T expected, T actual) {
			if (expected != null
			    && expected.equals(actual)) {
					return null;
			}
			return super.diff(expected, actual);
		}
	}


	/**
	 * Represent arbitrary objects by their toString() value
	 */
	public class ATAny extends AttributeType<Object> {
		@Override
		public Class<Object> getEncodedType() {
			return Object.class;
		}

		@Override
		public Object decode(String str) {
			return str;
		}

		@Override
		public String typeName() {
			return "any";
		}
	}

	public class ATString extends AttributeTypeSemanticEq<String> {
		@Override
		public Class<String> getEncodedType() {
			return String.class;
		}

		@Override
		public Object decode(String str) {
			return str;
		}

		@Override
		public String typeName() {
			return "String";
		}
	}

	public class ATBoolean extends AttributeTypeSemanticEq<Boolean> {
		@Override
		public Class<Boolean> getEncodedType() {
			return Boolean.class;
		}

		@Override
		public Boolean decode(String str) {
			return Boolean.parseBoolean(str);
		}

		@Override
		public String typeName() {
			return "boolean";
		}
	}

	public class ATInteger extends AttributeTypeSemanticEq<Integer> {
		@Override
		public Class<Integer> getEncodedType() {
			return Integer.class;
		}

		@Override
		public Integer decode(String str) {
			return Integer.parseInt(str);
		}

		@Override
		public String typeName() {
			return "int";
		}
	}

	public class ATSet<T> extends ParametricAttributeType<Set> {
		ATSet() {
			this((AttributeType<T>) new ATAny());
		}

		AttributeType<T> elt;
		ATSet(AttributeType<T> elt) {
			super(1);
			this.elt = elt;
		}

		@Override
		protected AttributeType<?> buildWithParameters(AttributeType<?>[] type_args) {
			return new ATSet(type_args[0]);
		}

		@Override
		public Class<Set> getEncodedType() {
			return Set.class;
		}

		@Override
		public String encode(Set set) {
			List<String> elts = new ArrayList<String>();
			for (Object obj : set) {
				elts.add(this.elt.encodeQuoted((T) obj));
			}
			Collections.sort(elts);
			return "{" +  elts.stream()
				.collect(Collectors.joining(",")) + "}";
		}

		@Override
		public Object decode(String elts_str) {
			if (elts_str.charAt(0) != '{'
			    || elts_str.charAt(elts_str.length() - 1) != '}') {
				throw new RuntimeException("Ill-formed set: " + elts_str);
			}
			elts_str = elts_str.substring(1, elts_str.length() - 1);

			Set<Object> results = new HashSet<>();
			if (elts_str.length() != 0) {
				String[] elts = elts_str.split(",");
				for (String elt : elts) {
					Value<T> v = this.elt.decodeQuoted(elt);
					if (v.isValue()) {
						results.add(v.getValue());
					}
					if (v.isNull()) {
						results.add(null);
					}
				}
			}
			return results;
		}

		@Override
		public String typeName() {
			return "Set";
		}

		@Override
		public Object[] diff(Set expected, Set actual) {
			if (expected.equals(actual)) {
				return null;
			}
			ArrayList<Object> missing = new ArrayList<>();
			ArrayList<Object> unexpected = new ArrayList<>();

			for (Object obj : expected) {
				if (!actual.contains(expected)) {
					missing.add(obj);
				}
			}
			for (Object obj : actual) {
				if (!expected.contains(expected)) {
					unexpected.add(obj);
				}
			}
			if (missing.isEmpty() && unexpected.isEmpty()) {
				throw new RuntimeException("Inconsistent set difference");
			}

			sort(missing);
			sort(unexpected);

			return new Object[] {
				"missing:",
				missing.isEmpty()? "none" : missing.toArray(),
				"unexpected:",
				unexpected.isEmpty()? "none" : unexpected.toArray(),
			};
		}
	}

	protected abstract AttributeType<?> getASTNodeEncoder();

	protected AttributeType<?>[] attribute_type_table = new AttributeType<?>[] {
		new ATAny(),
		new ATBoolean(),
		new ATInteger(),
		new ATString(),
		getASTNodeEncoder(),
		new ATSet(),
	};

	AttributeType<?>
	attributeTypeFromString(String type_name) {
		AttributeType<?> result = attributeTypeFromStringRec(type_name);
		if (result == null) {
			throw new RuntimeException("Unknown/unsupported attribute type '"
						   + type_name
						   +"'; known types are: " + attribute_type_table);
		}
		return result;
	}

	private AttributeType<?>
	attributeTypeFromStringRec(String type_name) {
		int args_start = type_name.indexOf('<');
		if (args_start == -1) {
			return this.attributeTypeFromName(type_name);
		}
		int args_end = type_name.lastIndexOf('>');
		if (args_end == -1 || args_end <= args_start) {
			return null;
		}
		AttributeType<?> ty_con = this.attributeTypeFromName(type_name.substring(0, args_start));
		if (ty_con == null) {
			return null;
		}
		List<AttributeType<?>> ty_args = new ArrayList<>();
		String args_str = type_name.substring(args_start + 1, args_end);
		String[] split_args = AttributeSummary.splitTyArgs(args_str);
		if (split_args == null) {
			return null;
		}
		for (String arg_str : split_args) {
			AttributeType<?> arg = attributeTypeFromStringRec(arg_str);
			if (arg == null) {
				return null;
			}
			ty_args.add(arg);
		}
		return ty_con.build(ty_args.toArray(new AttributeType<?>[ty_args.size()]));
	}

	/**
	 * Splits "foo<bar<A, B>>, quux, quuux<C, d>" into "foo<bar<A, B>>", "quux", "quuux<C, d>"
	 *
	 * Returns null elements if trailing error
	 */
	public static String[]
	splitTyArgs(String arglist) {
		ArrayList<String> results = new ArrayList<>();
		int depth = 0;

		int start = -1;
		int stop = -1;

		for (int i = 0; i <= arglist.length(); ++i) {
			char c = '\0';
			if (i < arglist.length()) {
				c = arglist.charAt(i);
			} else {
				if (start == -1 && stop == -1) {
					// No need to finish up processing
					break;
				}
			}
			switch (c) {
			case '\t':
			case ' ': // ignore whitespace
				break;

			case '\0': // fall through
			case ',': if (depth == 0) {
					// split
					if (start > -1
					    && stop > -1) {
						results.add(arglist.substring(start, stop));
					} else {
						return null; // Error
					}
					start = stop = -1;
					break;
				} else {
					stop = i + 1;
				}
				break;

			case '<': depth += 1;
				break;

			case '>': depth -= 1;
				if (depth < 0) {
					return null;
				}
				// fall through
			default:
				if (start == -1) {
					start = i;
				}
				stop = i + 1;
			}
		}
		if (depth != 0) {
			return null;
		}
		return results.toArray(new String[results.size()]);
	}

	private AttributeType<?>
	attributeTypeFromName(String type_name) {
		for (AttributeType<?> ty : attribute_type_table) {
			if (ty.typeName().equals(type_name)) {
				return ty;
			}
		}
		return null;
	}

	public class Attribute<T> {
		AttributeType<T> ty;
		String attribute;

		public Attribute(String name, AttributeType<T> ty) {
			if (name == null || name.length() == 0) {
				throw new RuntimeException("Empty/null string for attribute name");
			}
			this.attribute = name;
			this.ty = ty;
		}

		public String signature() {
			return attribute + ":" + this.ty.signature();
		}

		public Value<?> getFrom(ASTNode node) {
			try {
				Method method = node.getClass().getMethod(this.attribute);
				Object obj = autobox(method.invoke(node));
				return Value.from(obj);
			} catch (Exception __) {
				return null;
			}
		}

		public String getName() {
			return this.attribute;
		}

		public String encode(Value<T> value) {
			String suffix = ty.encodeValue(value, false);
			return this.signature() + " " + suffix;
		}

		/**
		 * Treat unexpected occurrences as deltas
		 */
		public boolean trackUnexpected() {
			return false;
		}

		/**
		 * Treat missing occurrences as deltas
		 */
		public boolean trackMissing() {
			return true;
		}

		public Value<?> decode(String str) {
			// if (!str.startsWith(this.signature() + " ")) {
			// 	throw new RuntimeException("Tried to decode '" + str + "' with " + this);
			// }
			String substr = str;//.substring(this.signature().length() + 1);
			if (substr.length() < 2) {
				throw new RuntimeException("Ill-formed: " + str);
			}
			return ty.decodeValue(substr, false);
		}

		public NodeDiff
		diff(Value<T> self, Value<T> other) {
			if (self == null) {
				if (other == null || other.isIgnore()) {
					return NodeDiff.EMPTY;
				}
				return NodeDiff.unexpected(this, self);
			}
			if (self.isIgnore()) {
				return NodeDiff.EMPTY;
			}
			if (other == null) {
				return NodeDiff.missing(this, self);
			}
			if (other.isIgnore()) {
				return NodeDiff.EMPTY;
			}
			if (self.isNull() && other.isNull()) {
				return NodeDiff.EMPTY;
			}
			if (self.isNull() && !other.isNull()) {
				return NodeDiff.make(this, new Object[] {
						"expected null",
						"actual:",
						other.getValue()
					});
			}
			if (!self.isNull() && other.isNull()) {
				return NodeDiff.make(this, new Object[] {
						"expected:",
						self.getValue(),
						"actual is null"
					});
			}
			Object[] delta = ty.diff(self.getValue(), other.getValue());
			if (delta == null) {
				return NodeDiff.EMPTY;
			}
			return NodeDiff.make(this, delta);
		}

		@Override
		public String toString() {
			return this.signature();
		}

		@Override
		public int hashCode() {
			return this.attribute.hashCode() ^ (this.ty.hashCode() << 3);
		}

		@Override
		public boolean equals(Object other) {
			if (other == null) {
				return false;
			}
			if (other instanceof Attribute) {
				Attribute o = (Attribute) other;
				return (o.attribute.equals(this.attribute)
					&& o.ty.equals(this.ty));
			}
			return false;
		}
	}

	public static final String escape(String str) {
		return str.replace("\\", "\\\\")
			.replace("!", "\\x21")
			.replace("#", "\\x23")
			.replace(",", "\\x2c")
			.replace(":", "\\x3a")
			.replace("=", "\\x3d")
			.replace("?", "\\x3f")
			.replace("\t", "\\t")
			.replace("\b", "\\b")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\f", "\\f")
			.replace("\"", "\\\"");
	}

	public static final String unescape(String str) {
		return str.replace("\\x21", "!")
			.replace("\\x23", "#")
			.replace("\\x2c", ",")
			.replace("\\x3a", ":")
			.replace("\\x3d", "=")
			.replace("\\x3f", "?")
			.replace("\\t", "\t")
			.replace("\\b", "\b")
			.replace("\\n", "\n")
			.replace("\\r", "\r")
			.replace("\\f", "\f")
			.replace("\\\"", "\"")
			.replace("\\\\", "\\");
	}

	public static Object autobox(Object obj) {
		if (obj == null) {
			return obj;
		}
		if (boolean.class.isInstance(obj)) {
			return (boolean)obj;
		}
		if (int.class.isInstance(obj)) {
			return (int)obj;
		}
		if (long.class.isInstance(obj)) {
			return (long)obj;
		}
		if (short.class.isInstance(obj)) {
			return (short)obj;
		}
		if (byte.class.isInstance(obj)) {
			return (byte)obj;
		}
		if (char.class.isInstance(obj)) {
			return (char)obj;
		}
		if (float.class.isInstance(obj)) {
			return (float)obj;
		}
		if (double.class.isInstance(obj)) {
			return (double)obj;
		}
		return obj;
	}

	public static class Builder<T extends AttributeSummary> {
		protected T summary;
		public Builder(T asummary) {
			this.summary = asummary;
		}

		public AttributeSummary
		from(File f) throws IOException {
			try (FileInputStream istream = new FileInputStream(f)) {
				return this.from(istream);
			}
		}

		public AttributeSummary
		from(InputStream r) throws IOException {
			try (Reader reader = new InputStreamReader(r)) {
				return this.from(reader);
			}
		}

		public AttributeSummary
		from(Reader reader) throws IOException {
			try (BufferedReader buf_reader = new BufferedReader(reader)) {
				return this.from(buf_reader);
			}
		}

		public AttributeSummary
		from(BufferedReader buf) throws IOException {
			String line;
			int line_nr = 0;
			HashMap<String, Attribute> attrs = new HashMap<>();
			do {
				line = buf.readLine();
				++line_nr;
				if (line.startsWith("#")) {
					// comment
					continue;
				}
				if (line != null && line.startsWith(".")) {
					// attr spec
					String spec = line.substring(1);
					String[] elts = spec.split(":");
					if (elts.length != 2) {
						throw new RuntimeException("Ill-formed attr spec in line " + line_nr);
					}
					Attribute attr = this.summary.createAttribute(elts[0],
										      elts[1]);
					this.summary.withAttribute(attr);
					attrs.put(spec, attr);
				}
			} while (line != null && line.startsWith("."));
			List<String> node_ids = new ArrayList<>();
			this.summary.nodes = node_ids;
			String last_node_id = "";
			do {
				String[] elts = line.split(" ", 4);
				String node_id;
				if (elts.length < 2) {
					throw new RuntimeException("Ill-formed node spec in line " + line_nr);
				}
				node_id = elts[0];
				if (!node_id.equals(last_node_id)) {
					node_ids.add(node_id);
					this.summary.node_type.put(node_id, elts[1]);
				}
				EntryMap emap = this.summary.entryMap(node_id);
				if (elts.length == 4) {
					// attribute spec
					String attr_spec = elts[2];
					String value = elts[3];
					Attribute attr = attrs.get(attr_spec);
					if (attr == null) {
						throw new RuntimeException("Attribute "+attr_spec+" undeclared in node spec in line " + line_nr);
					}
					emap.put(attr, attr.decode(value));
				}
				line = buf.readLine();
				++line_nr;
			} while (line != null);
			return this.summary;
		}

		public AttributeSummary
		from(String s) {
			try {
				return this.from(new StringReader(s));
			} catch (IOException exn) {
				// Should be impossible
				throw new RuntimeException(exn);
			}
		}
	}

	public static class BuilderWithAST extends Builder<WithAST> {
		public BuilderWithAST(WithAST asummary) {
			super(asummary);
		}

		public WithAST
		empty() {
			this.summary.buildFromNodeIDMap();
			return this.summary;
		}
	}

	public static class BuilderWithoutAST extends Builder<WithoutAST> {
		public BuilderWithoutAST(WithoutAST asummary) {
			super(asummary);
		}
	}

	public static class WithAST extends AttributeSummary {
		protected NodeIDMap nidmap;
		WithAST(NodeIDMap nidmap) {
			this.nidmap = nidmap;
			this.nodes = nidmap.keys();
		}

		@Override
		protected void updateAttribute(Attribute attr) {
			for (String node_id : this.nidmap.keys()) {
				EntryMap entrymap = this.entryMap(node_id);
				ASTNode node = nidmap.getNode(node_id);
				entrymap.put(attr, attr.getFrom(node));
			}
		}


		void buildFromNodeIDMap() {
			for (String node_id : this.nidmap.keys()) {
				ASTNode node = nidmap.getNode(node_id);
				node_type.put(node_id, node.getClass().getSimpleName());
				this.entryMap(node_id);
			}
		}


		@Override
		protected AttributeType<?> getASTNodeEncoder() {
			return new ATASTNode();
		}

		public class ATASTNode extends AttributeType<ASTNode> {
			@Override
			public Class<ASTNode> getEncodedType() {
				return ASTNode.class;
			}

			@Override
			public String encode(ASTNode node) {
				return nidmap.getNodeID(node);
			}

			@Override
			public Object decode(String node_id) {
				return nidmap.getNode(node_id);
			}

			@Override
			public String typeName() {
				return "ASTNode";
			}
		}

	}

	public static class WithoutAST extends AttributeSummary {
		@Override
		protected void updateAttribute(Attribute _attr) {
		}

		@Override
		protected AttributeType<?> getASTNodeEncoder() {
			return new ATFakeASTNode();
		}

		public class ATFakeASTNode extends AttributeType<String> {
			@Override
			public Class<String> getEncodedType() {
				return String.class;
			}

			@Override
			public String encode(String node) {
				return node;
			}

			@Override
			public Object decode(String node_id) {
				return node_id;
			}

			@Override
			public String typeName() {
				return "ASTNode";
			}
		}

	}

	final class EntryMap {
		private HashMap<Attribute, Value<?>> data;
		public void put(Attribute attr, Value<?> value) {
			if (value == null) {
				return;
			}
			if (this.data == null) {
				this.data = new HashMap<>();
			}
			this.data.put(attr, value);
		}
		public boolean has(Attribute attr) {
			if (this.data == null) {
				return false;
			}
			return this.data.containsKey(attr);
		}
		public Value<?> get(Attribute attr) {
			if (this.data == null) {
				return null;
			}
			if (this.has(attr)) {
				return this.data.get(attr);
			}
			return null;
		}
		public boolean isEmpty() {
			return this.data == null || this.data.isEmpty();
		}
		public List<? extends String> encodeElements() {
			ArrayList<String> result = new ArrayList<>();
			if (this.data == null) {
				return result;
			}
			for (Attribute attr : attributes) {
				if (this.has(attr)) {
					result.add(attr.encode(this.get(attr)));
				}
			}
			return result;
		}

		public NodeDiff diffFor(EntryMap other, Attribute attr) {
			return attr.diff(this.get(attr), other.get(attr));
		}

		public NodeDiff diff(EntryMap other) {
			NodeDiff diff = new NodeDiff();
			for (Attribute attr : attributes) {
				diff.add(this.diffFor(other, attr));
			}
			return diff;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof EntryMap) {
				EntryMap o_map = (EntryMap) other;
				return this.diff(o_map).isEmpty();
			}
			return false;
		}
	}

	public static void sort(List<Object> objects) {
		if (objects.isEmpty()) {
			return;
		}
		// First check if they are comparable

		List<Comparable> comparable_objs = new ArrayList<>();
		boolean all_comparable = true;
		for (Object obj : objects) {
			if (obj instanceof Comparable) {
				comparable_objs.add((Comparable) obj);
			} else {
				comparable_objs = null;
				break;
			}
		}
		if (comparable_objs != null) {
			// Comparable, so let's use the built-in comparator

			objects.clear();
			Collections.sort(comparable_objs);
			for (Object obj : comparable_objs) {
				objects.add(obj);
			}
			return;
		}

		Collections.sort(objects,
				 new Comparator<Object>() {
					 @Override
					 public int compare(Object l, Object r) {
						 if (l == r) {
							 return 0;
						 }
						 if (l == null) {
							 return -1;
						 }
						 if (r == null) {
							 return 1;
						 }
						 return l.toString().compareTo(r.toString());
					 }
				 });

	}
}
