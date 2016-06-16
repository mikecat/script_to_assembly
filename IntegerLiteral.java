public class IntegerLiteral extends Expression {
	private long value;
	private int width;
	private boolean signed;
	private Type type;

	public IntegerLiteral(long value, int width, boolean signed) {
		this.value = value;
		this.width = width;
		this.signed = signed;
		this.type = new PrimitiveType(width, signed);
	}

	public long getValue() {
		return value;
	}
	public int getWidth() {
		return width;
	}
	public boolean isSigned() {
		return signed;
	}
	public Type getType() {
		return type;
	}

	public Expression evaluate() {
		return this;
	}
}
