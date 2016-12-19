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
			BinaryOperator op = (BinaryOperator)expr;
			// オペランドを評価する
			generateExpressionEvaluation(op.getLeft(), op.getKind() == BinaryOperator.Kind.OP_ASSIGN);
			if (op.getKind() != BinaryOperator.Kind.OP_LOGICAL_AND &&
			op.getKind() != BinaryOperator.Kind.OP_LOGICAL_OR) {
				generateExpressionEvaluation(op.getRight(), false);
				out.println("\tpop %ecx");
			}
			out.println("\tpop %eax");
			// 計算を行う
			switch(op.getKind()) {
			case OP_ARRAY:
				throw new SystemLimitException("OP_ARRAY not implemented yet");
			case OP_MUL:
				throw new SystemLimitException("OP_MUL not implemented yet");
			case OP_DIV:
				throw new SystemLimitException("OP_DIV not implemented yet");
			case OP_MOD:
				throw new SystemLimitException("OP_MOD not implemented yet");
			case OP_ADD:
				throw new SystemLimitException("OP_ADD not implemented yet");
			case OP_SUB:
				throw new SystemLimitException("OP_SUB not implemented yet");
			case OP_LEFT_SHIFT:
				throw new SystemLimitException("OP_LEFT_SHIFT not implemented yet");
			case OP_RIGHT_SHIFT_ARITIMETIC:
				throw new SystemLimitException("OP_RIGHT_SHIFT_ARITIMETIC not implemented yet");
			case OP_RIGHT_SHIFT_LOGICAL:
				throw new SystemLimitException("OP_RIGHT_SHIFT_LOGICAL not implemented yet");
			case OP_LEFT_ROTATE:
				throw new SystemLimitException("OP_LEFT_ROTATE not implemented yet");
			case OP_RIGHT_ROTATE:
				throw new SystemLimitException("OP_RIGHT_ROTATE not implemented yet");
			case OP_BIT_AND:
				throw new SystemLimitException("OP_BIT_AND not implemented yet");
			case OP_BIT_OR:
				throw new SystemLimitException("OP_BIT_OR not implemented yet");
			case OP_BIT_XOR:
				throw new SystemLimitException("OP_BIT_XOR not implemented yet");
			case OP_ASSIGN:
				throw new SystemLimitException("OP_ASSIGN not implemented yet");
			case OP_GT:
				throw new SystemLimitException("OP_GT not implemented yet");
			case OP_GTE:
				throw new SystemLimitException("OP_GTE not implemented yet");
			case OP_LT:
				throw new SystemLimitException("OP_LT not implemented yet");
			case OP_LTE:
				throw new SystemLimitException("OP_LTE not implemented yet");
			case OP_EQUAL:
				throw new SystemLimitException("OP_EQUAL not implemented yet");
			case OP_NOT_EQUAL:
				throw new SystemLimitException("OP_NOT_EQUAL not implemented yet");
			case OP_LOGICAL_AND:
				throw new SystemLimitException("OP_LOGICAL_AND not implemented yet");
			case OP_LOGICAL_OR:
				throw new SystemLimitException("OP_LOGICAL_OR not implemented yet");
			default:
				throw new SystemLimitException("unexpected kind of BinaryOperator: " + op.getKind());
			}
		} else if (expr instanceof UnaryOperator) {
			throw new SystemLimitException("UnaryOperator not implemented yet");
		} else if (expr instanceof FunctionCallOperator) {
			throw new SystemLimitException("FunctionCallOperator not implemented yet");
		} else if (expr instanceof CastOperator) {
			throw new SystemLimitException("CastOperator not implemented yet");
		} else if (expr instanceof VariableAccess) {
			throw new SystemLimitException("VariableAccess not implemented yet");
		} else if (expr instanceof IntegerLiteral) {
			out.println("\tpushl $" + ((IntegerLiteral)expr).getValue());
		} else if (expr instanceof StringLiteral) {
			throw new SystemLimitException("StringLiteral not implemented yet");
		}
	}
}
