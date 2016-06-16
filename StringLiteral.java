public class StringLiteral extends Expression {
	private String string;
	private byte[] bytes;
	private Type type;

	public StringLiteral(String string) {
		this.string = string;
		this.bytes = string.getBytes();
		this.type = new ArrayType(new PrimitiveType(1, false), bytes.length + 1);
	}

	public String getString() {
		return string;
	}
	public byte[] getBytes() {
		return bytes;
	}
	public Type getType() {
		return type;
	}

	public Expression evaluate() {
		return this;
	}
}
