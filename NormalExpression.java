public class NormalExpression extends Instruction {
	private Expression expression;

	public NormalExpression(Expression expression) {
		this.expression = expression;
	}

	public Expression getExpression() {
		return expression;
	}
}
