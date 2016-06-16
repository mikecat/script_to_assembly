public class BinaryOperator extends Expression {
	static enum Kind {
		OP_FUNCTION_CALL,
		OP_FUNCTION_ARGS_SEPARATOR,
		OP_ARRAY,
		OP_MUL,
		OP_DIV,
		OP_MOD,
		OP_ADD,
		OP_SUB,
		OP_LEFT_SHIFT,
		OP_RIGHT_SHIFT_ARITIMETIC,
		OP_RIGHT_SHIFT_LOGICAL,
		OP_LEFT_ROTATE,
		OP_RIGHT_ROTATE,
		OP_BIT_AND,
		OP_BIT_OR,
		OP_BIT_XOR,
		OP_ASSIGN,
		OP_GT,
		OP_GTE,
		OP_LT,
		OP_LTE,
		OP_EQUAL,
		OP_NOT_EQUAL,
		OP_LOGICAL_AND,
		OP_LOGICAL_OR
	}

	private Kind kind;
	private Expression left, right;
	private Type type;

	public BinaryOperator(Kind kind, Expression left, Expression right) {
		this.kind = kind;
		this.left = left;
		this.right = right;
		this.type = null;
	}

	public Kind getKind() {
		return kind;
	}
	public Expression getLeft() {
		return left;
	}
	public Expression getRight() {
		return right;
	}
	public Type getType() {
		return type;
	}
}
