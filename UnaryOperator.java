public class UnaryOperator extends Expression {
	static enum Kind {
		UNARY_MINUS,
		UNARY_PLUS,
		UNARY_LOGICAL_NOT,
		UNARY_BIT_NOT,
		UNARY_DEREFERENCE,
		UNARY_ADDRESS
	}

	private Kind kind;
	private Expression operand;
	private Type type;

	public UnaryOperator(Kind kind, Expression operand) {
		this.kind = kind;
		this.operand = operand;
		this.type = null;
	}

	public Kind getKind() {
		return kind;
	}
	public Expression getOperand() {
		return operand;
	}
	public Type getType() {
		return type;
	}
}
