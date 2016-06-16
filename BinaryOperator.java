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
		if (this.left.getType() instanceof ArrayType) {
			this.left = new UnaryOperator(UnaryOperator.Kind.UNARY_AUTO_TO_POINTER, this.left);
		}
		if (this.right.getType() instanceof ArrayType) {
			this.right = new UnaryOperator(UnaryOperator.Kind.UNARY_AUTO_TO_POINTER, this.right);
		}

		PrimitiveType pt1, pt2;
		switch(this.kind) {
		case OP_FUNCTION_CALL:
			throw new RuntimeException("function not implemented yet");
			//break;
		case OP_FUNCTION_ARGS_SEPARATOR:
			this.type = null;
			break;
		case OP_ARRAY:
			if (!(this.left.getType() instanceof PointerType) || !(this.right.getType() instanceof PrimitiveType)) {
				throw new RuntimeException("invalid operand for array indexing");
			}
			this.type = ((PointerType)this.left.getType()).getPointsAt();
			break;
		case OP_MUL:
		case OP_DIV:
		case OP_MOD:
		case OP_BIT_AND:
		case OP_BIT_OR:
		case OP_BIT_XOR:
			if (!(this.left.getType() instanceof PrimitiveType) || !(this.right.getType() instanceof PrimitiveType)) {
				throw new RuntimeException("invalid operand");
			}
			pt1 = (PrimitiveType)this.left.getType();
			pt2 = (PrimitiveType)this.right.getType();
			// 大きい方の型に合わせる
			if (pt1.getWidth() > pt2.getWidth()) {
				this.type = pt1;
			} else if (pt1.getWidth() < pt2.getWidth()) {
				this.type = pt2;
			} else {
				// サイズが同じ場合、少なくとも1方が符号なしなら符号なし
				this.type = new PrimitiveType(pt1.getWidth(), pt1.isSigned() && pt2.isSigned());
			}
			break;
		case OP_ADD:
			if (this.left.getType() instanceof PrimitiveType && this.right.getType() instanceof PrimitiveType) {
				pt1 = (PrimitiveType)this.left.getType();
				pt2 = (PrimitiveType)this.right.getType();
				// 大きい方の型に合わせる
				if (pt1.getWidth() > pt2.getWidth()) {
					this.type = pt1;
				} else if (pt1.getWidth() < pt2.getWidth()) {
					this.type = pt2;
				} else {
					// サイズが同じ場合、少なくとも1方が符号なしなら符号なし
					this.type = new PrimitiveType(pt1.getWidth(), pt1.isSigned() && pt2.isSigned());
				}
			} else if (this.left.getType() instanceof PointerType && this.right.getType() instanceof PrimitiveType) {
				this.type = this.left.getType();
			} else if (this.left.getType() instanceof PrimitiveType && this.right.getType() instanceof PointerType) {
				this.type = this.right.getType();
			} else {
				throw new RuntimeException("invalid operand for addition");
			}
			break;
		case OP_SUB:
			if (this.left.getType() instanceof PrimitiveType && this.right.getType() instanceof PrimitiveType) {
				pt1 = (PrimitiveType)this.left.getType();
				pt2 = (PrimitiveType)this.right.getType();
				// 大きい方の型に合わせる
				if (pt1.getWidth() > pt2.getWidth()) {
					this.type = pt1;
				} else if (pt1.getWidth() < pt2.getWidth()) {
					this.type = pt2;
				} else {
					// サイズが同じ場合、少なくとも1方が符号なしなら符号なし
					this.type = new PrimitiveType(pt1.getWidth(), pt1.isSigned() && pt2.isSigned());
				}
			} else if (this.left.getType() instanceof PointerType && this.right.getType() instanceof PointerType) {
				this.type = new PrimitiveType(4, true);
			} else {
				throw new RuntimeException("invalid operand for addition");
			}
			break;
		case OP_LEFT_SHIFT:
		case OP_RIGHT_SHIFT_ARITIMETIC:
		case OP_RIGHT_SHIFT_LOGICAL:
		case OP_LEFT_ROTATE:
		case OP_RIGHT_ROTATE:
			this.type = this.left.getType();
			break;
		case OP_ASSIGN:
			this.type = this.left.getType();
			break;
		case OP_GT:
		case OP_GTE:
		case OP_LT:
		case OP_LTE:
			this.type = new PrimitiveType(4, true);
			break;
		case OP_EQUAL:
		case OP_NOT_EQUAL:
			this.type = new PrimitiveType(4, true);
			break;
		case OP_LOGICAL_AND:
		case OP_LOGICAL_OR:
			this.type = new PrimitiveType(4, true);
			break;
		}
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
