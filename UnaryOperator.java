public class UnaryOperator extends Expression {
	static enum Kind {
		UNARY_FUNCTION_CALL,
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
	private DataType dataType;

	public UnaryOperator(Kind kind, Expression operand) {
		this.kind = kind;
		this.operand = operand;
		if (this.operand.getDataType() instanceof ArrayType &&
		this.kind != Kind.UNARY_ADDRESS && this.kind != Kind.UNARY_SIZE && this.kind != Kind.UNARY_AUTO_TO_POINTER) {
			this.operand = new UnaryOperator(Kind.UNARY_AUTO_TO_POINTER, this.operand);
		}

		switch(kind) {
		case UNARY_FUNCTION_CALL:
			if (!(this.operand.getDataType() instanceof FunctionType)) {
				throw new SyntaxException("tried to call something not a function");
			}
			this.dataType = ((FunctionType)this.operand.getDataType()).getReturnType();
			break;
		case UNARY_MINUS:
		case UNARY_PLUS:
		case UNARY_BIT_NOT:
			this.dataType = this.operand.getDataType();
			break;
		case UNARY_LOGICAL_NOT:
			this.dataType = new PrimitiveType(4, true); // TBD
			break;
		case UNARY_DEREFERENCE:
			if (this.operand.getDataType() instanceof PointerType) {
				this.dataType = ((PointerType)this.operand.getDataType()).getPointsAt();
			} else {
				throw new SyntaxException("cannot dereference what is not a pointer");
			}
			break;
		case UNARY_ADDRESS:
			this.dataType = new PointerType(this.operand.getDataType());
			break;
		case UNARY_SIZE:
			this.dataType = new PrimitiveType(4, false); // ターゲットに依存するので後でなんとかする
			break;
		case UNARY_AUTO_TO_POINTER:
			if (this.operand.getDataType() instanceof ArrayType) {
				// 配列をその先頭要素を指すポインタに変換する
				this.dataType = new PointerType(((ArrayType)this.operand.getDataType()).getElementDataType());
			} else {
				throw new SystemLimitException("internal error: strange operand for auto convert to pointer");
			}
			break;
		default:
			throw new SystemLimitException("internal error: strange unary operator type");
		}
	}

	public Kind getKind() {
		return kind;
	}
	public Expression getOperand() {
		return operand;
	}
	public DataType getDataType() {
		return dataType;
	}

	public Expression evaluate() {
		Expression operand = this.operand.evaluate();
		if (kind == Kind.UNARY_SIZE) {
			// サイズは型のみを見るので評価できる
			return new IntegerLiteral(operand.getDataType().getWidth(), 4, false);
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
			return new IntegerLiteral(op.getDataType().getWidth(), 4, false);
		case UNARY_FUNCTION_CALL:
		case UNARY_DEREFERENCE:
		case UNARY_ADDRESS:
		case UNARY_AUTO_TO_POINTER:
		default:
			return this;
		}
	}
}
