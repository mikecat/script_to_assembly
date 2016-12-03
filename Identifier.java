public class Identifier extends Expression {
	private Variable var;

	public Identifier(Variable var) {
		this.var = var;
	}

	public Variable getVariable() {
		return var;
	}

	public DataType getDataType() {
		return var.getDataType();
	}

	public Expression evaluate() {
		return this;
	}
}
