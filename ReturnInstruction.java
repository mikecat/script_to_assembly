public class ReturnInstruction extends Instruction {
	private Expression expression;

	public ReturnInstruction() {
		this(null);
	}

	public ReturnInstruction(Expression expression) {
		this.expression = expression;
	}

	public boolean hasExpression() {
		return expression != null;
	}

	public Expression getExpression() {
		return expression;
	}
}
