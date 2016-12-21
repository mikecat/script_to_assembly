import java.io.Writer;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

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

	private PrintWriter out;
	private String currentFunctionName;
	private DataType currentFunctionReturnType;
	private int nextLabelId;
	private List<String> stringLiteralList;

	private String getNextLabel() {
		return "stoa.label." + (nextLabelId++);
	}

	public void generateAssembly(Writer out,
	StaticVariable[] staticVariableDefinitionList,
	Function[] functionDefinitionList) {
		this.out = new PrintWriter(out);
		nextLabelId = 0;
		stringLiteralList = new ArrayList<String>();

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

		// 文字列リテラルを出力する
		this.out.println(".section .rodata");
		for (int i = 0; i < stringLiteralList.size(); i++) {
			this.out.println("stoa.string" + i + ":");
			this.out.print("\t.byte ");
			byte[] b = stringLiteralList.get(i).getBytes();
			for (int j = 0; j < b.length; j++) {
				int num = b[j];
				if (num < 0) num += 256;
				this.out.print(num + ", ");
			}
			this.out.println("0");
		}

		this.out.flush();
	}

	private void generateFunction(Function func) {
		// 関数の開始
		currentFunctionName = func.getName();
		currentFunctionReturnType = func.getReturnType();
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
			generateExpressionEvaluation(expr, 0, false);
			out.println("\tpop %eax");
		} else if (inst instanceof ReturnInstruction) {
			ReturnInstruction ret = (ReturnInstruction)inst;
			if (ret.hasExpression()) {
				generateExpressionEvaluation(ret.getExpression(),
					currentFunctionReturnType.getWidth(), false);
				out.println("\tpop %eax");
			}
			out.println("\tjmp stoa.funcend." + currentFunctionName);
		} else if (inst instanceof ConditionalBranch) {
			ConditionalBranch cb = (ConditionalBranch)inst;
			int insNum;
			generateExpressionEvaluation(cb.getCondition(), 4, false);
			out.println("\tpop %eax");
			out.println("\ttest %eax, %eax");
			String label1 = getNextLabel();
			String label2 = getNextLabel();
			String label3 = null;
			out.println("\tjnz " + label1);
			out.println("\tjmp " + label2);
			out.println(label1 + ":");
			insNum = cb.getThenInstructionNumber();
			for (int i = 0; i < insNum; i++) {
				generateInstruction(cb.getThenInstruction(i));
			}
			if (cb.hasElse()) {
				label3 = getNextLabel();
				out.println("\tjmp " + label3);
			}
			out.println(label2 + ":");
			if (cb.hasElse()) {
				insNum = cb.getElseInstructionNumber();
				for (int i = 0; i < insNum; i++) {
					generateInstruction(cb.getElseInstruction(i));
				}
				out.println(label3 + ":");
			}
		} else if (inst instanceof InfiniteLoop) {
			InfiniteLoop infLoop = (InfiniteLoop)inst;
			int insNum = infLoop.getInstructionNumber();
			String label = getNextLabel();
			out.println(label + ":");
			for (int i = 0; i < insNum; i++) {
				generateInstruction(infLoop.getInstruction(i));
			}
			out.println("\tjmp " + label);
		} else if (inst instanceof WhileLoop) {
			WhileLoop wLoop = (WhileLoop)inst;
			int insNum = wLoop.getInstructionNumber();
			String label1 = getNextLabel();
			String label2 = getNextLabel();
			String label3 = getNextLabel();
			out.println(label1 + ":");
			generateExpressionEvaluation(wLoop.getCondition(), 4, false);
			out.println("\tpop %eax");
			out.println("\ttest %eax, %eax");
			out.println("\tjnz " + label3);
			out.println("\tjmp " + label2);
			out.println(label3 + ":");
			for (int i = 0; i < insNum; i++) {
				generateInstruction(wLoop.getInstruction(i));
			}
			out.println("\tjmp " + label1);
			out.println(label2 + ":");
		} else if (inst instanceof BreakInstruction) {
			throw new SystemLimitException("BreakInstruction not implemented yet");
		} else if (inst instanceof ContinueInstruction) {
			throw new SystemLimitException("ContinueInstruction not implemented yet");
		}
	}

	private void generateExpressionEvaluation(Expression expr, int requestedSize, boolean wantAddress) {
		if (expr instanceof BinaryOperator) {
			generateBinaryOperatorEvaluation((BinaryOperator)expr, requestedSize, wantAddress);
		} else if (expr instanceof UnaryOperator) {
			generateUnaryOperatorEvaluation((UnaryOperator)expr, requestedSize, wantAddress);
		} else if (expr instanceof FunctionCallOperator) {
			FunctionCallOperator funcCall = (FunctionCallOperator)expr;
			int argumentNum = funcCall.getArgumentsNum();
			for (int i = argumentNum - 1; i >= 0; i--) {
				generateExpressionEvaluation(funcCall.getArgument(i), 4, false);
			}
			generateExpressionEvaluation(funcCall.getFunction(), 4, false);
			out.println("\tpop %eax");
			out.println("\tcall *%eax");
			out.println("\tadd $" + (4 * argumentNum) + ", %esp");
			out.println("\tpush %eax");
		} else if (expr instanceof CastOperator) {
			throw new SystemLimitException("CastOperator not implemented yet");
		} else if (expr instanceof VariableAccess) {
			if (expr.getDataType().getWidth() != 4) {
				throw new SystemLimitException("currently only 4-byte variable is supported");
			}
			Identifier ident = ((VariableAccess)expr).getIdentifier();
			if (ident instanceof StaticVariable) {
				if (wantAddress || expr.getDataType() instanceof FunctionType) {
					out.println("\tpushl $" + ident.getName());
				} else {
					out.println("\tpushl (" + ident.getName() + ")");
				}
			} else if (ident instanceof AutomaticVariable) {
				throw new SystemLimitException("AutomaticVariable not implemented yet");
			} else if (ident instanceof DefinedValue) {
				out.println("\tpushl $" + ((DefinedValue)ident).getValue());
			} else if (ident instanceof AddressVariable) {
				if (wantAddress) {
					out.println("\tpushl $" + ((AddressVariable)ident).getAddress());
				} else {
					out.println("\tpushl (" + ((AddressVariable)ident).getAddress() + ")");
				}
			}
		} else if (expr instanceof IntegerLiteral) {
			out.println("\tpushl $" + ((IntegerLiteral)expr).getValue());
		} else if (expr instanceof StringLiteral) {
			StringLiteral sl = (StringLiteral)expr;
			String str = sl.getString();
			int idx = stringLiteralList.indexOf(str);
			if (idx < 0) {
				idx = stringLiteralList.size();
				stringLiteralList.add(str);
			}
			out.println("\tpushl $stoa.string" + idx);
		}
	}

	private void generateBinaryOperatorEvaluation(BinaryOperator op, int requestedSize, boolean wantAddress) {
		// オペランドを評価する
		generateExpressionEvaluation(op.getLeft(), op.getDataType().getWidth(),
			op.getKind() == BinaryOperator.Kind.OP_ASSIGN);
		if (op.getKind() != BinaryOperator.Kind.OP_LOGICAL_AND &&
		op.getKind() != BinaryOperator.Kind.OP_LOGICAL_OR) {
			generateExpressionEvaluation(op.getRight(), op.getDataType().getWidth(), false);
			out.println("\tpop %ecx");
		}
		out.println("\tpop %eax");
		boolean isComparisionSigned = false;
		if (op.getLeft().getDataType() instanceof IntegerType && op.getRight().getDataType() instanceof IntegerType) {
			IntegerType leftType = (IntegerType)op.getLeft().getDataType();
			IntegerType rightType = (IntegerType)op.getRight().getDataType();
			if (leftType.getWidth() >= rightType.getWidth()) {
				isComparisionSigned = leftType.isSigned();
			} else {
				isComparisionSigned = rightType.isSigned();
			}
		}
		// 計算を行う
		String comparisionOperatorInstruction = null;
		switch(op.getKind()) {
		case OP_ARRAY:
			throw new SystemLimitException("OP_ARRAY not implemented yet");
		case OP_MUL:
			if (((IntegerType)op.getDataType()).isSigned()) {
				out.println("\timul %ecx");
			} else {
				out.println("\tmul %ecx");
			}
			break;
		case OP_DIV:
			if (((IntegerType)op.getDataType()).isSigned()) {
				out.println("\tcdq");
				out.println("\tidiv %ecx");
			} else {
				out.println("xor %edx, %edx");
				out.println("\tdiv %ecx");
			}
			break;
		case OP_MOD:
			if (((IntegerType)op.getDataType()).isSigned()) {
				out.println("\tcdq");
				out.println("\tidiv %ecx");
			} else {
				out.println("xor %edx, %edx");
				out.println("\tdiv %ecx");
			}
			out.println("\tmov %edx, %eax");
			break;
		case OP_ADD:
			out.println("\tadd %ecx, %eax");
			break;
		case OP_SUB:
			out.println("\tsub %ecx, %eax");
			break;
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
			out.println("\txchg %ecx, %eax");
			out.println("\tmov %eax, (%ecx)");
			break;
		case OP_GT:
			comparisionOperatorInstruction = isComparisionSigned ? "jng" : "jna";
			break;
		case OP_GTE:
			comparisionOperatorInstruction = isComparisionSigned ? "jnge" : "jnae";
			break;
		case OP_LT:
			comparisionOperatorInstruction = isComparisionSigned ? "jnl" : "jnb";
			break;
		case OP_LTE:
			comparisionOperatorInstruction = isComparisionSigned ? "jnle" : "jnbe";
			break;
		case OP_EQUAL:
			comparisionOperatorInstruction = "jne";
			break;
		case OP_NOT_EQUAL:
			comparisionOperatorInstruction = "je";
			break;
		case OP_LOGICAL_AND:
			throw new SystemLimitException("OP_LOGICAL_AND not implemented yet");
		case OP_LOGICAL_OR:
			throw new SystemLimitException("OP_LOGICAL_OR not implemented yet");
		default:
			throw new SystemLimitException("unexpected kind of BinaryOperator: " + op.getKind());
		}
		if (comparisionOperatorInstruction != null) {
			String label = getNextLabel();
			out.println("\tcmp %ecx, %eax");
			out.println("\tmov $0, %eax");
			out.println("\t" + comparisionOperatorInstruction + " " + label);
			out.println("\tinc %eax");
			out.println(label + ":");
		}
		out.println("\tpush %eax");
	}

	private void generateUnaryOperatorEvaluation(UnaryOperator op, int requestedSize, boolean wantAddress) {
		// オペランドを評価する
		if (op.getKind() != UnaryOperator.Kind.UNARY_SIZE) {
			generateExpressionEvaluation(op.getOperand(), op.getDataType().getWidth(),
				op.getKind() == UnaryOperator.Kind.UNARY_DEREFERENCE || op.getKind() == UnaryOperator.Kind.UNARY_ADDRESS);
			out.println("\tpop %eax");
		}
		switch(op.getKind()) {
		case UNARY_MINUS:
			out.println("\tneg %eax");
			break;
		case UNARY_PLUS:
			// 何もしない
			break;
		case UNARY_LOGICAL_NOT:
			break;
		case UNARY_BIT_NOT:
			out.println("\tnot %eax");
			break;
		case UNARY_DEREFERENCE:
			if (!wantAddress) {
				out.println("\tmov (%eax), %eax");
			}
			break;
		case UNARY_ADDRESS:
			// 下からアドレスが来るので、そのまま渡す
			// すなわち、何もしない
			break;
		case UNARY_SIZE:
			// 式を評価せず、結果のサイズを積む
			out.println("\tmov $" + op.getDataType().getWidth() + ", %eax");
			break;
		case UNARY_AUTO_TO_POINTER:
			// 下からポインタの値が来るので、そのまま渡す
			// すなわち、何もしない
			break;
		default:
			throw new SystemLimitException("unexpected kind of UnaryOperator: " + op.getKind());
		}
		out.println("\tpush %eax");
	}
}
