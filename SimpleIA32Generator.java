import java.io.Writer;
import java.io.PrintWriter;

public class SimpleIA32Generator extends AssemblyGenerator {
	public int getSystemIntSize() {
		return 4;
	}

	public int getPointerSize() {
		return 4;
	}

	public int getFunctionSize() {
		return 4;
	}

	PrintWriter out;
	private String currentFunctionName;

	public void generateAssembly(Writer out,
	StaticVariable[] staticVariableDefinitionList,
	Function[] functionDefinitionList) {
		this.out = new PrintWriter(out);

		// プログラムを出力する
		this.out.println(".section .text");
		for (int i = 0; i < functionDefinitionList.length; i++) {
			generateFunction(functionDefinitionList[i]);
		}

		// データの領域を出力する
		this.out.println(".section .bss");
		for (int i = 0; i < staticVariableDefinitionList.length; i++) {
			StaticVariable sv = staticVariableDefinitionList[i];
			this.out.println(".comm " + sv.getName() + ", " + sv.getDataType().getWidth());
		}

		this.out.flush();
	}

	private void generateFunction(Function func) {
		// 関数の開始
		currentFunctionName = func.getName();
		out.println(".globl " + currentFunctionName);
		out.println(currentFunctionName + ":");
		// 関数内の命令をそれぞれ出力する
		int instNum = func.getInstructionNumber();
		for (int i = 0; i < instNum; i++) {
			generateInstruction(func.getInstruction(i));
		}
		// 関数の終わり
		out.println("stoa.funcend." + currentFunctionName + ":");
		out.println("\tret");
	}

	private void generateInstruction(Instruction inst) {
		if (inst instanceof NormalExpression) {
			Expression expr = ((NormalExpression)inst).getExpression();
			generateExpressionEvaluation(expr);
			out.println("\tpop %eax");
		} else if (inst instanceof ReturnInstruction) {
			ReturnInstruction ret = (ReturnInstruction)inst;
			if (ret.hasExpression()) {
				generateExpressionEvaluation(ret.getExpression());
				out.println("\tpop %eax");
			}
			out.println("\tjmp stoa.funcend." + currentFunctionName);
		} else if (inst instanceof ConditionalBranch) {
			throw new SystemLimitException("ConditionalBranch not implemented yet");
		} else if (inst instanceof InfiniteLoop) {
			throw new SystemLimitException("InfiniteLoop not implemented yet");
		} else if (inst instanceof WhileLoop) {
			throw new SystemLimitException("WhileLoop not implemented yet");
		} else if (inst instanceof BreakInstruction) {
			throw new SystemLimitException("BreakInstruction not implemented yet");
		} else if (inst instanceof ContinueInstruction) {
			throw new SystemLimitException("ContinueInstruction not implemented yet");
		}
	}

	private void generateExpressionEvaluation(Expression expr) {
		generateExpressionEvaluation(expr, false);
	}

	private void generateExpressionEvaluation(Expression expr, boolean wantAddress) {
		if (expr instanceof BinaryOperator) {
			throw new SystemLimitException("BinaryOperator not implemented yet");
		} else if (expr instanceof UnaryOperator) {
			throw new SystemLimitException("UnaryOperator not implemented yet");
		} else if (expr instanceof FunctionCallOperator) {
			throw new SystemLimitException("FunctionCallOperator not implemented yet");
		} else if (expr instanceof CastOperator) {
			throw new SystemLimitException("CastOperator not implemented yet");
		} else if (expr instanceof VariableAccess) {
			throw new SystemLimitException("VariableAccess not implemented yet");
		} else if (expr instanceof IntegerLiteral) {
			throw new SystemLimitException("IntegerLiteral not implemented yet");
		} else if (expr instanceof StringLiteral) {
			throw new SystemLimitException("StringLiteral not implemented yet");
		}
	}
}
