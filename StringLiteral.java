public class StringLiteral extends Expression {
	private String string;
	private byte[] bytes;
	private DataType dataType;

	public StringLiteral(String string) {
		this.string = string;
		this.bytes = string.getBytes();
		this.dataType = new ArrayType(new IntegerType(1, false), bytes.length + 1);
	}

	public String getString() {
		return string;
	}
	public byte[] getBytes() {
		return bytes;
	}
	public DataType getDataType() {
		return dataType;
	}

	public Expression evaluate() {
		return this;
	}
}
