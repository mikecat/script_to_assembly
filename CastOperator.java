public class CastOperator extends Expression {
	private Expression operand;
	private DataType destType;

	public CastOperator(DataType destType, Expression operand) {
		this.destType = destType;
		this.operand = operand;
	}

	public DataType getDataType() {
		return destType;
	}

	public Expression evaluate() {
		if (!(destType instanceof IntegerType)) {
			// 整数型以外への変換はできない
			return this;
		}
		Expression operand = this.operand.evaluate();
		if (!(operand instanceof IntegerLiteral)) {
			// 値が決まっていなければ評価できない
			return this;
		}
		IntegerLiteral op = (IntegerLiteral)operand;
		return new IntegerLiteral(op.getValue(), destType.getWidth(), ((IntegerType)destType).isSigned());
	}
}
