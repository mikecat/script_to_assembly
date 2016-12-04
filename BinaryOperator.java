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
	private DataType dataType;

	private DataType getAritimeticDataType(DataType left, DataType right) {
		if (!(left instanceof IntegerType) || !(right instanceof IntegerType)) {
			throw new SyntaxException("invalid operand for aritimetic");
		}
		IntegerType pt1 = (IntegerType)this.left.getDataType();
		IntegerType pt2 = (IntegerType)this.right.getDataType();
		// 大きい方の型に合わせる
		if (pt1.getWidth() > pt2.getWidth()) {
			return pt1;
		} else if (pt1.getWidth() < pt2.getWidth()) {
			return pt2;
		} else {
			// サイズが同じ場合、少なくとも1方が符号なしなら符号なし
			return new IntegerType(pt1.getWidth(), pt1.isSigned() && pt2.isSigned());
		}
	}

	public BinaryOperator(Kind kind, Expression left, Expression right) {
		this.kind = kind;
		this.left = left;
		this.right = right;
		if (this.left.getDataType() instanceof ArrayType) {
			this.left = new UnaryOperator(UnaryOperator.Kind.UNARY_AUTO_TO_POINTER, this.left);
		}
		if (this.right.getDataType() instanceof ArrayType) {
			this.right = new UnaryOperator(UnaryOperator.Kind.UNARY_AUTO_TO_POINTER, this.right);
		}

		switch(this.kind) {
		case OP_FUNCTION_CALL:
			if (!(this.left.getDataType() instanceof FunctionType)) {
				throw new SyntaxException("tried to call something not a function");
			}
			this.dataType = ((FunctionType)this.left.getDataType()).getReturnType();
			//break;
		case OP_FUNCTION_ARGS_SEPARATOR:
			this.dataType = null;
			break;
		case OP_ARRAY:
			if (!(this.left.getDataType() instanceof PointerType) || !(this.right.getDataType() instanceof IntegerType)) {
				throw new SyntaxException("invalid operand for array indexing");
			}
			this.dataType = ((PointerType)this.left.getDataType()).getPointsAt();
			break;
		case OP_MUL:
		case OP_DIV:
		case OP_MOD:
		case OP_BIT_AND:
		case OP_BIT_OR:
		case OP_BIT_XOR:
			this.dataType = getAritimeticDataType(this.left.getDataType(), this.right.getDataType());
			break;
		case OP_ADD:
			if (this.left.getDataType() instanceof IntegerType && this.right.getDataType() instanceof IntegerType) {
				this.dataType = getAritimeticDataType(this.left.getDataType(), this.right.getDataType());
			} else if (this.left.getDataType() instanceof PointerType && this.right.getDataType() instanceof IntegerType) {
				this.dataType = this.left.getDataType();
			} else if (this.left.getDataType() instanceof IntegerType && this.right.getDataType() instanceof PointerType) {
				this.dataType = this.right.getDataType();
			} else {
				throw new SyntaxException("invalid operand for addition");
			}
			break;
		case OP_SUB:
			if (this.left.getDataType() instanceof IntegerType && this.right.getDataType() instanceof IntegerType) {
				this.dataType = getAritimeticDataType(this.left.getDataType(), this.right.getDataType());
			} else if (this.left.getDataType() instanceof PointerType && this.right.getDataType() instanceof PointerType) {
				this.dataType = new IntegerType(4, true);
			} else {
				throw new SyntaxException("invalid operand for subtraction");
			}
			break;
		case OP_LEFT_SHIFT:
		case OP_RIGHT_SHIFT_ARITIMETIC:
		case OP_RIGHT_SHIFT_LOGICAL:
		case OP_LEFT_ROTATE:
		case OP_RIGHT_ROTATE:
			this.dataType = this.left.getDataType();
			break;
		case OP_ASSIGN:
			this.dataType = this.left.getDataType();
			break;
		case OP_GT:
		case OP_GTE:
		case OP_LT:
		case OP_LTE:
			if (this.left.getDataType() instanceof IntegerType && this.right.getDataType() instanceof IntegerType) {
				// 整数同士の比較はできる
				IntegerType pt1 = (IntegerType)this.left.getDataType();
				IntegerType pt2 = (IntegerType)this.right.getDataType();
				// ただし、同じサイズで符号の有無が違う場合はめんどいからダメ
				if (pt1.getWidth() == pt2.getWidth() && pt1.isSigned() != pt2.isSigned()) {
					throw new SyntaxException("comparing primitives with same width and different signedness isn't allowed");
				}
			} else if (!(this.left.getDataType() instanceof PointerType && this.left.getDataType() instanceof PointerType)) {
				// ポインタ同士の比較はできる、それ以外はエラー
				throw new SyntaxException("invaild operands for comparision");
			}
			this.dataType = new IntegerType(4, true);
			break;
		case OP_EQUAL:
		case OP_NOT_EQUAL:
			if (this.left.getDataType() instanceof IntegerType && this.right.getDataType() instanceof IntegerType) {
				// 整数同士の等価かの判断はできる
				IntegerType pt1 = (IntegerType)this.left.getDataType();
				IntegerType pt2 = (IntegerType)this.right.getDataType();
				// ただし、同じサイズで符号の有無が違う場合はめんどいからダメ
				if (pt1.getWidth() == pt2.getWidth() && pt1.isSigned() != pt2.isSigned()) {
					throw new SyntaxException("checking equality of primitives with same width and different signedness isn't allowed");
				}
			} else if (!(this.left.getDataType() instanceof PointerType && this.left.getDataType() instanceof PointerType) &&
			!(this.left.getDataType() instanceof FunctionType && this.left.getDataType() instanceof FunctionType)) {
				// ポインタや関数同士の等価かの判断はできる、それ以外はエラー
				throw new SyntaxException("invaild operands for equality check");
			}
			this.dataType = new IntegerType(4, true);
			break;
		case OP_LOGICAL_AND:
		case OP_LOGICAL_OR:
			this.dataType = new IntegerType(4, true);
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
	public DataType getDataType() {
		return dataType;
	}

	public Expression evaluate() {
		Expression left = this.left.evaluate();
		Expression right = this.right.evaluate();
		if ((!(left instanceof IntegerLiteral) || !(right instanceof IntegerLiteral)) &&
		kind != Kind.OP_LOGICAL_AND && kind != Kind.OP_LOGICAL_OR) {
			// 値が決まっていなければ評価できない
			return this;
		}
		IntegerLiteral lop = left instanceof IntegerLiteral ? (IntegerLiteral)left : null;
		IntegerLiteral rop = right instanceof IntegerLiteral ? (IntegerLiteral)right : null;
		int width = 0;
		boolean isSigned = true;
		if (lop != null && rop != null) {
			if (lop.getWidth() > rop.getWidth()) {
				width = lop.getWidth();
				isSigned = lop.isSigned();
			} else if (lop.getWidth() < rop.getWidth()) {
				width = rop.getWidth();
				isSigned = rop.isSigned();
			} else {
				width = lop.getWidth();
				isSigned = lop.isSigned() && rop.isSigned();
			}
		}
		switch (kind) {
		case OP_MUL:
			return new IntegerLiteral(lop.getValue() * rop.getValue(), width, isSigned);
		case OP_DIV:
			return new IntegerLiteral(lop.getValue() / rop.getValue(), width, isSigned);
		case OP_MOD:
			return new IntegerLiteral(lop.getValue() % rop.getValue(), width, isSigned);
		case OP_ADD:
			return new IntegerLiteral(lop.getValue() + rop.getValue(), width, isSigned);
		case OP_SUB:
			return new IntegerLiteral(lop.getValue() - rop.getValue(), width, isSigned);
		case OP_LEFT_SHIFT:
			return new IntegerLiteral(lop.getValue() << rop.getValue(), width, isSigned);
		case OP_RIGHT_SHIFT_ARITIMETIC:
			return this; // not implemented yet
		case OP_RIGHT_SHIFT_LOGICAL:
			return this; // not implemented yet
		case OP_LEFT_ROTATE:
			return this; // not implemented yet
		case OP_RIGHT_ROTATE:
			return this; // not implemented yet
		case OP_BIT_AND:
			return new IntegerLiteral(lop.getValue() & rop.getValue(), width, isSigned);
		case OP_BIT_OR:
			return new IntegerLiteral(lop.getValue() | rop.getValue(), width, isSigned);
		case OP_BIT_XOR:
			return new IntegerLiteral(lop.getValue() ^ rop.getValue(), width, isSigned);
		case OP_GT:
			return new IntegerLiteral(lop.getValue() > rop.getValue() ? 1 : 0, 4, true);
		case OP_GTE:
			return new IntegerLiteral(lop.getValue() >= rop.getValue() ? 1 : 0, 4, true);
		case OP_LT:
			return new IntegerLiteral(lop.getValue() < rop.getValue() ? 1 : 0, 4, true);
		case OP_LTE:
			return new IntegerLiteral(lop.getValue() <= rop.getValue() ? 1 : 0, 4, true);
		case OP_EQUAL:
			return new IntegerLiteral(lop.getValue() == rop.getValue() ? 1 : 0, 4, true);
		case OP_NOT_EQUAL:
			return new IntegerLiteral(lop.getValue() != rop.getValue() ? 1 : 0, 4, true);
		case OP_LOGICAL_AND:
			if (!(left instanceof IntegerLiteral)) return this;
			if (((IntegerLiteral)left).getValue() == 0) {
				return new IntegerLiteral(0, 4, true);
			} else {
				if (!(right instanceof IntegerLiteral)) return this;
				return new IntegerLiteral(((IntegerLiteral)right).getValue() == 0 ? 0 : 1, 4, true);
			}
		case OP_LOGICAL_OR:
			if (!(left instanceof IntegerLiteral)) return this;
			if (((IntegerLiteral)left).getValue() != 0) {
				return new IntegerLiteral(1, 4, true);
			} else {
				if (!(right instanceof IntegerLiteral)) return this;
				return new IntegerLiteral(((IntegerLiteral)right).getValue() == 0 ? 0 : 1, 4, true);
			}
		case OP_FUNCTION_CALL:
		case OP_FUNCTION_ARGS_SEPARATOR:
		case OP_ARRAY:
		case OP_ASSIGN:
		default:
			return this;
		}
	}
}
