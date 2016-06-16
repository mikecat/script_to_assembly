public class UnaryOperator extends Expression {
	static enum Kind {
		UNARY_MINUS,
		UNARY_PLUS,
		UNARY_LOGICAL_NOT,
		UNARY_BIT_NOT,
		UNARY_DEREFERENCE,
		UNARY_ADDRESS,
		UNARY_SIZE,
		UNARY_AUTO_TO_POINTER
	}

	private Kind kind;
	private Expression operand;
	private Type type;

	public UnaryOperator(Kind kind, Expression operand) {
		this.kind = kind;
		this.operand = operand;
		if (this.operand.getType() instanceof ArrayType &&
		this.kind != Kind.UNARY_ADDRESS && this.kind != Kind.UNARY_SIZE && this.kind != Kind.UNARY_AUTO_TO_POINTER) {
			this.operand = new UnaryOperator(Kind.UNARY_AUTO_TO_POINTER, this.operand);
		}

		switch(kind) {
		case UNARY_MINUS:
		case UNARY_PLUS:
		case UNARY_BIT_NOT:
			this.type = this.operand.getType();
			break;
		case UNARY_LOGICAL_NOT:
			this.type = new PrimitiveType(4, true); // TBD
			break;
		case UNARY_DEREFERENCE:
			if (this.operand.getType() instanceof PointerType) {
				this.type = ((PointerType)this.operand.getType()).getPointsAt();
			} else {
				throw new RuntimeException("cannot dereference what is not a pointer");
			}
			break;
		case UNARY_ADDRESS:
			this.type = new PointerType(this.operand.getType());
			break;
		case UNARY_SIZE:
			this.type = new PrimitiveType(4, false); // ターゲットに依存するので後でなんとかする
			break;
		case UNARY_AUTO_TO_POINTER:
			if (this.operand.getType() instanceof ArrayType) {
				// 配列をその先頭要素を指すポインタに変換する
				this.type = new PointerType(((ArrayType)this.operand.getType()).getElementsType());
			} else {
				throw new RuntimeException("internal error: strange operand for auto convert to pointer");
			}
			break;
		default:
			throw new RuntimeException("internal error: strange unary operator type");
		}
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

	public Expression evaluate() {
		Expression operand = this.operand.evaluate();
		if (kind == Kind.UNARY_SIZE) {
			// サイズは型のみを見るので評価できる
			return new IntegerLiteral(operand.getType().getWidth(), 4, false);
		}
		if (!(operand instanceof IntegerLiteral)) {
			// 値が決まっていなければ評価できない
			return this;
		}
		IntegerLiteral op = (IntegerLiteral)operand;
		switch (kind) {
		case UNARY_MINUS:
			return new IntegerLiteral(-op.getValue(), op.getWidth(), op.isSigned());
		case UNARY_PLUS:
			return op;
		case UNARY_LOGICAL_NOT:
			return new IntegerLiteral(op.getValue() == 0 ? 1 : 0, 4, true);
		case UNARY_BIT_NOT:
			return new IntegerLiteral(~op.getValue(), op.getWidth(), op.isSigned());
		case UNARY_SIZE:
			return new IntegerLiteral(op.getType().getWidth(), 4, false);
		case UNARY_DEREFERENCE:
		case UNARY_ADDRESS:
		case UNARY_AUTO_TO_POINTER:
		default:
			return this;
		}
	}
}
