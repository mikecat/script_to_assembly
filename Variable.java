public class Variable extends Expression {
	private Identifier var;

	public Variable(Identifier var) {
		this.var = var;
	}

	public Identifier getIdentifier() {
		return var;
	}

	public DataType getDataType() {
		return var.getDataType();
	}

	public Expression evaluate() {
		return this;
	}
}
