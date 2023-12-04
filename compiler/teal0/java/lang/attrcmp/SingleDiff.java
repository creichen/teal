package lang.attrcmp;

public class SingleDiff {
	public final DiffType type;
	public final Object[] explanation;
	public SingleDiff(DiffType type, Object... explanation) {
		this.type = type;
		this.explanation = explanation;
	}
}
