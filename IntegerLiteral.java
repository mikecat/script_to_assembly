public class IntegerLiteral extends Expression {
	private long value;
	private int width;
	private boolean signed;
	private DataType dataType;

	public IntegerLiteral(long value, int width, boolean signed) {
		this.value = value;
		this.width = width;
		this.signed = signed;
		this.dataType = new PrimitiveType(width, signed);
		if (this.signed && this.value < 0) {
			this.value |= ~((((1L << (8 * width - 1)) - 1) << 1) + 1);
		} else {
			this.value &= (((1L << (8 * width - 1)) - 1) << 1) + 1;
		}
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
	public DataType getDataType() {
		return dataType;
	}

	public Expression evaluate() {
		return this;
	}
}
