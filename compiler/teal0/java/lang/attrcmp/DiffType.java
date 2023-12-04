package lang.attrcmp;

public enum DiffType {
	MISSING('-'),
	UNEXPECTED('+'),
	DELTA('!');
	private char prefix;
	public char getPrefix() {
		return this.prefix;
	}
	DiffType(char prefix) {
		this.prefix = prefix;
	}
}
