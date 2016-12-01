public class Identifier extends Expression {
	public DataType getDataType() {
		return null;
	}

	public Expression evaluate() {
		return this;
	}
}
