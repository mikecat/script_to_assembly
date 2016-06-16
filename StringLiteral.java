public class StringLiteral extends Expression {
	private String string;

	public StringLiteral(String string) {
		this.string = string;
	}

	public String getString() {
		return string;
	}
}
