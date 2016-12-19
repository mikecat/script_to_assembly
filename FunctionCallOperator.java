import java.util.List;
import java.util.ArrayList;

public class FunctionCallOperator extends Expression {
	private Expression function;
	private List<Expression> arguments;
	private DataType dataType;

	public FunctionCallOperator(Expression function, Expression arguments) {
		if (!(function.getDataType() instanceof FunctionType)) {
			throw new SyntaxException("tried to call something not a function");
		}
		this.function = function;
		this.dataType = ((FunctionType)function.getDataType()).getReturnType();
		this.arguments = new ArrayList<Expression>();
		if (arguments != null) {
			// 引数のリストを取得する
			while (arguments instanceof BinaryOperator &&
			((BinaryOperator)arguments).getKind() == BinaryOperator.Kind.OP_FUNCTION_ARGS_SEPARATOR) {
				this.arguments.add(((BinaryOperator)arguments).getLeft());
				arguments = ((BinaryOperator)arguments).getRight();
			}
			this.arguments.add(arguments);
		}
	}

	public Expression getFunction() {
		return function;
	}

	public int getArgumentsNum() {
		return arguments.size();
	}

	public Expression getArgument(int index) {
		return arguments.get(index);
	}

	public DataType getDataType() {
		return dataType;
	}

	public Expression evaluate() {
		return this;
	}
}
