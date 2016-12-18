public class VariableAccess extends Expression {
	private Identifier var;

	public VariableAccess(Identifier var) {
		this.var = var;
	}

	public Identifier getIdentifier() {
		return var;
	}

	public DataType getDataType() {
		return var.getDataType();
	}

	public Expression evaluate() {
		if (var instanceof DefinedValue) {
			DefinedValue dvar = (DefinedValue)var;
			return new IntegerLiteral(dvar.getValue(), dvar.getWidth(), dvar.isSigned());
		} else {
			return this;
		}
	}
}
