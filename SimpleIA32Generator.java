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
	private List<Integer> currentFunctionVariableOffset;
	private List<String> continueLabel;
	private List<String> breakLabel;
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
		continueLabel = new ArrayList<String>();
		breakLabel = new ArrayList<String>();

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
		// 自動変数のオフセットを計算する
		currentFunctionVariableOffset = new ArrayList<Integer>();
		int variableNum = func.getVariableNumber();
		int variableOffset = 0;
		int argumentOffset = 8;
		for (int i = 0; i < variableNum; i++) {
			AutomaticVariable var = func.getVariable(i);
			if (var.getId() != currentFunctionVariableOffset.size()) {
				throw new SystemLimitException("automatic variable ID mismatch");
			}
			int size = var.getDataType().getWidth();
			long alignedSizeLong = (((long)size + 3) / 4) * 4;
			if (alignedSizeLong > Integer.MAX_VALUE) {
				throw new SystemLimitException("variable size too big after alignment");
			}
			int alignedSize = (int)alignedSizeLong;
			if (var.isArgument()) {
				if ((long)argumentOffset + alignedSize > Integer.MAX_VALUE) {
					throw new SystemLimitException("arugments too big");
				}
				// 追加した後で次の引数に備えて更新
				currentFunctionVariableOffset.add(argumentOffset);
				argumentOffset += alignedSize;
			} else {
				if ((long)variableOffset - alignedSize < Integer.MIN_VALUE) {
					throw new SystemLimitException("local variables too big");
				}
				// 今の変数のために更新した後、追加
				variableOffset -= alignedSize;
				currentFunctionVariableOffset.add(variableOffset);
			}
		}
		// 関数の開始
		currentFunctionName = func.getName();
		currentFunctionReturnType = func.getReturnType();
		out.println(".globl " + currentFunctionName);
		out.println(currentFunctionName + ":");
		out.println("\tpush %ebp");
		out.println("\tmov %esp, %ebp");
		if (variableOffset != 0) out.println("\tsub $" + (-(long)variableOffset) + ", %esp");
		// 関数内の命令をそれぞれ出力する
		int instNum = func.getInstructionNumber();
		for (int i = 0; i < instNum; i++) {
			generateInstruction(func.getInstruction(i));
		}
		// 関数の終わり
		out.println("stoa.funcend." + currentFunctionName + ":");
		out.println("\tleave");
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
			String label1 = getNextLabel();
			String label2 = getNextLabel();
			continueLabel.add(label1);
			breakLabel.add(label2);
			out.println(label1 + ":");
			for (int i = 0; i < insNum; i++) {
				generateInstruction(infLoop.getInstruction(i));
			}
			out.println("\tjmp " + label1);
			out.println(label2 + ":");
			continueLabel.remove(continueLabel.size() - 1);
			breakLabel.remove(breakLabel.size() - 1);
		} else if (inst instanceof WhileLoop) {
			WhileLoop wLoop = (WhileLoop)inst;
			int insNum = wLoop.getInstructionNumber();
			String label1 = getNextLabel();
			String label2 = getNextLabel();
			String label3 = getNextLabel();
			continueLabel.add(label1);
			breakLabel.add(label2);
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
			continueLabel.remove(continueLabel.size() - 1);
			breakLabel.remove(breakLabel.size() - 1);
		} else if (inst instanceof BreakInstruction) {
			BreakInstruction bi = (BreakInstruction)inst;
			int level = bi.getLevel();
			if (level > breakLabel.size()) {
				throw new SyntaxException("break level too high");
			}
			out.println("\tjmp " + breakLabel.get(breakLabel.size() - level));
		} else if (inst instanceof ContinueInstruction) {
			ContinueInstruction ci = (ContinueInstruction)inst;
			int level = ci.getLevel();
			if (level > continueLabel.size()) {
				throw new SyntaxException("coneinue level too high");
			}
			out.println("\tjmp " + continueLabel.get(continueLabel.size() - level));
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
			CastOperator co = (CastOperator)expr;
			DataType coType = co.getDataType();
			generateExpressionEvaluation(co.getOperand(), coType.getWidth(), false);
			int srcWidth = coType.getWidth();
			if (srcWidth < requestedSize) {
				boolean isSigned = (coType instanceof IntegerType) ? ((IntegerType)coType).isSigned() : false;
				out.println("\tpop %eax");
				if (srcWidth == 1 && requestedSize == 2) {
					out.println((isSigned ? "\tmovsbw" : "\tmovzbw") + " %al, %ax");
				} else if (srcWidth == 1 && requestedSize == 4) {
					out.println((isSigned ? "\tmovsbl" : "\tmovzbl") + " %al, %eax");
				} else if (srcWidth == 2 && requestedSize == 4) {
					out.println((isSigned ? "\tmovswl" : "\tmovzwl") + " %ax, %eax");
				}
				out.println("\tpush %eax");
			}
		} else if (expr instanceof VariableAccess) {
			boolean pushLater = false;
			int dataWidth = expr.getDataType().getWidth();
			Identifier ident = ((VariableAccess)expr).getIdentifier();
			if (ident instanceof StaticVariable) {
				if (wantAddress || expr.getDataType() instanceof FunctionType) {
					out.println("\tpushl $" + ident.getName());
				} else {
					pushLater = true;
					switch (dataWidth) {
					case 1: out.println("\tmovb (" + ident.getName() + "), %al"); break;
					case 2: out.println("\tmovw (" + ident.getName() + "), %ax"); break;
					case 4: out.println("\tmovl (" + ident.getName() + "), %eax"); break;
					default: throw new SystemLimitException(dataWidth + "-byte variable not implemented");
					}
				}
			} else if (ident instanceof AutomaticVariable) {
				String memoryAccess = currentFunctionVariableOffset.get(((AutomaticVariable)ident).getId()) + "(%ebp)";
				if (wantAddress) {
					out.println("\tlea " + memoryAccess + ", %eax");
					out.println("\tpushl %eax");
				} else {
					pushLater = true;
					switch (dataWidth) {
					case 1: out.println("\tmovb " + memoryAccess + ", %al"); break;
					case 2: out.println("\tmovw " + memoryAccess + ", %ax"); break;
					case 4: out.println("\tmovl " + memoryAccess + ", %eax"); break;
					default: throw new SystemLimitException(dataWidth + "-byte variable not implemented");
					}
				}
			} else if (ident instanceof DefinedValue) {
				out.println("\tpushl $" + ((DefinedValue)ident).getValue());
			} else if (ident instanceof AddressVariable) {
				if (wantAddress) {
					out.println("\tpushl $" + ((AddressVariable)ident).getAddress());
				} else {
					pushLater = true;
					switch (dataWidth) {
					case 1: out.println("\tmovb (" + ((AddressVariable)ident).getAddress() + "), %al"); break;
					case 2: out.println("\tmovw (" + ((AddressVariable)ident).getAddress() + "), %ax"); break;
					case 4: out.println("\tmovl (" + ((AddressVariable)ident).getAddress() + "), %eax"); break;
					default: throw new SystemLimitException(dataWidth + "-byte variable not implemented");
					}
				}
			}
			if (pushLater) {
				boolean isSigned = expr.getDataType() instanceof IntegerType && ((IntegerType)expr.getDataType()).isSigned();
				if (dataWidth == 1 && requestedSize == 2) {
					out.println((isSigned ? "\tmovsbw" : "\tmovzbw") + " %al, %ax");
				} else if (dataWidth == 1 && requestedSize == 4) {
					out.println((isSigned ? "\tmovsbl" : "\tmovzbl") + " %al, %eax");
				} else if (dataWidth == 2 && requestedSize == 4) {
					out.println((isSigned ? "\tmovswl" : "\tmovzwl") + " %ax, %eax");
				}
				out.println("\tpushl %eax");
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
		if (op.getKind() != BinaryOperator.Kind.OP_LOGICAL_AND &&
		op.getKind() != BinaryOperator.Kind.OP_LOGICAL_OR) {
			generateExpressionEvaluation(op.getLeft(), op.getDataType().getWidth(),
				op.getKind() == BinaryOperator.Kind.OP_ASSIGN);
			generateExpressionEvaluation(op.getRight(), op.getDataType().getWidth(), false);
			out.println("\tpop %ecx");
			out.println("\tpop %eax");
		}
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
		int dataSize = op.getDataType().getWidth();
		String regSrc = null, regDest = null;
		switch (op.getDataType().getWidth()) {
		case 1: regSrc = "%cl"; regDest = "%al"; break;
		case 2: regSrc = "%cx"; regDest = "%ax"; break;
		case 4: regSrc = "%ecx"; regDest = "%eax"; break;
		default:
			if (!wantAddress) {
				throw new SystemLimitException(op.getDataType().getWidth() + "-byte calculation not supported");
			}
			break;
		}
		String comparisionOperatorInstruction = null;
		switch(op.getKind()) {
		case OP_ARRAY:
			out.println("\txchg %ecx, %eax");
			out.println("\tmov $" + ((PointerType)op.getLeft().getDataType()).getPointsAt().getWidth() + ", %edx");
			out.println((op.getRight().getDataType() instanceof IntegerType &&
				((IntegerType)op.getRight().getDataType()).isSigned() ? "\timul" : "\tmul") + " %edx");
			out.println("\tadd %ecx, %eax");
			if (!wantAddress) {
				switch (dataSize) {
				case 1: out.println("\tmovb (%eax), %al"); break;
				case 2: out.println("\tmovw (%eax), %ex"); break;
				case 4: out.println("\tmovl (%eax), %eax"); break;
				}
			}
			break;
		case OP_MUL:
			if (((IntegerType)op.getDataType()).isSigned()) {
				out.println("\timul " + regSrc);
			} else {
				out.println("\tmul " + regSrc);
			}
			break;
		case OP_DIV:
		case OP_MOD:
			if (((IntegerType)op.getDataType()).isSigned()) {
				switch (dataSize) {
				case 1:
					out.println("\tcbw");
					out.println("\tidiv %cl");
					break;
				case 2:
					out.println("\tcwd");
					out.println("\tidiv %cx");
					break;
				case 4:
					out.println("\tcdq");
					out.println("\tidiv %ecx");
					break;
				}
			} else {
				switch (dataSize) {
				case 1:
					out.println("xor %ah, %ah");
					out.println("\tdiv %cl");
					break;
				case 2:
					out.println("xor %dx, %dx");
					out.println("\tdiv %cx");
					break;
				case 4:
					out.println("xor %edx, %edx");
					out.println("\tdiv %ecx");
					break;
				}
			}
			if (op.getKind() == BinaryOperator.Kind.OP_MOD) {
				switch (dataSize) {
				case 1: out.println("\tmov %ah, %al"); break;
				case 2: out.println("\tmov %dx, %ex"); break;
				case 4: out.println("\tmov %edx, %eax"); break;
				}
			}
			break;
		case OP_ADD:
			if (op.getLeft().getDataType() instanceof PointerType) {
				out.println("\txchg %ecx, %eax");
				out.println("\tmov $" + ((PointerType)op.getLeft().getDataType()).getPointsAt().getWidth() + ", %edx");
				out.println((op.getRight().getDataType() instanceof IntegerType &&
					((IntegerType)op.getRight().getDataType()).isSigned() ? "\timul" : "\tmul") + " %edx");
				out.println("\txchg %ecx, %eax");
			} else if (op.getRight().getDataType() instanceof PointerType) {
				out.println("\tmov $" + ((PointerType)op.getRight().getDataType()).getPointsAt().getWidth() + ", %edx");
				out.println((op.getRight().getDataType() instanceof IntegerType &&
					((IntegerType)op.getRight().getDataType()).isSigned() ? "\timul" : "\tmul") + " %edx");
			}
			out.println("\tadd " + regSrc + ", " + regDest);
			break;
		case OP_SUB:
			out.println("\tsub " + regSrc + ", " + regDest);
			if (op.getLeft().getDataType() instanceof PointerType && op.getRight().getDataType() instanceof PointerType) {
				if (op.getLeft().getDataType().getWidth() != 4 || op.getRight().getDataType().getWidth() != 4) {
					throw new SystemLimitException("subtraction of non 4-byte pointers isn't supported");
				}
				out.println("\tmov $" + ((PointerType)op.getLeft().getDataType()).getPointsAt().getWidth() + ", %ecx");
				out.println("\tcdq");
				out.println("\tidiv %ecx");
			}
			break;
		case OP_LEFT_SHIFT:
			out.println("\tshl %cl, " + regDest);
			break;
		case OP_RIGHT_SHIFT_ARITIMETIC:
			out.println("\tsar %cl, " + regDest);
			break;
		case OP_RIGHT_SHIFT_LOGICAL:
			out.println("\tshr %cl, " + regDest);
			break;
		case OP_LEFT_ROTATE:
			out.println("\trol %cl, " + regDest);
			break;
		case OP_RIGHT_ROTATE:
			out.println("\tror %cl, " + regDest);
			break;
		case OP_BIT_AND:
			out.println("\tand " + regSrc + ", " + regDest);
			break;
		case OP_BIT_OR:
			out.println("\tor " + regSrc + ", " + regDest);
			break;
		case OP_BIT_XOR:
			out.println("\txor " + regSrc + ", " + regDest);
			break;
		case OP_ASSIGN:
			out.println("\txchg %ecx, %eax");
			switch (dataSize) {
			case 1: out.println("\tmovb %al, (%ecx)"); break;
			case 2: out.println("\tmovw %ax, (%ecx)"); break;
			case 4: out.println("\tmovl %eax, (%ecx)"); break;
			}
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
			{
				// 左辺を評価する
				generateExpressionEvaluation(op.getLeft(), 4, false);
				String label = getNextLabel();
				out.println("\txor %ecx, %ecx");
				out.println("\tpop %eax");
				out.println("\ttest %eax, %eax");
				out.println("\tjz " + label);
				// 左辺が0でなければ、右辺を評価する
				generateExpressionEvaluation(op.getRight(), 4, false);
				out.println("\txor %ecx, %ecx");
				out.println("\tpop %eax");
				out.println("\ttest %eax, %eax");
				out.println("\tjz " + label);
				out.println("\tinc %ecx");
				out.println(label + ":");
				out.println("\tmov %ecx, %eax");
			}
			break;
		case OP_LOGICAL_OR:
			{
				// 左辺を評価する
				generateExpressionEvaluation(op.getLeft(), 4, false);
				String label = getNextLabel();
				out.println("\tmov $1, %ecx");
				out.println("\tpop %eax");
				out.println("\ttest %eax, %eax");
				out.println("\tjnz " + label);
				// 左辺が0ならば、右辺を評価する
				generateExpressionEvaluation(op.getRight(), 4, false);
				out.println("\txor %ecx, %ecx");
				out.println("\tpop %eax");
				out.println("\ttest %eax, %eax");
				out.println("\tjz " + label);
				out.println("\tinc %ecx");
				out.println(label + ":");
				out.println("\tmov %ecx, %eax");
			}
			break;
		default:
			throw new SystemLimitException("unexpected kind of BinaryOperator: " + op.getKind());
		}
		if (comparisionOperatorInstruction != null) {
			String label = getNextLabel();
			out.println("\tcmp " + regSrc + ", " + regDest);
			out.println("\tmov $0, %eax");
			out.println("\t" + comparisionOperatorInstruction + " " + label);
			out.println("\tinc %eax");
			out.println(label + ":");
		}
		if (!wantAddress) {
			boolean isSigned = op.getDataType() instanceof IntegerType && ((IntegerType)op.getDataType()).isSigned();
			if (dataSize == 1 && requestedSize == 2) {
				out.println((isSigned ? "\tmovsbw" : "\tmovzbw") + " %al, %ax");
			} else if (dataSize == 1 && requestedSize == 4) {
				out.println((isSigned ? "\tmovsbl" : "\tmovzbl") + " %al, %eax");
			} else if (dataSize == 2 && requestedSize == 4) {
				out.println((isSigned ? "\tmovswl" : "\tmovzwl") + " %ax, %eax");
			}
		}
		out.println("\tpush %eax");
	}

	private void generateUnaryOperatorEvaluation(UnaryOperator op, int requestedSize, boolean wantAddress) {
		// オペランドを評価する
		boolean requestAddress = op.getKind() == UnaryOperator.Kind.UNARY_DEREFERENCE ||
			op.getKind() == UnaryOperator.Kind.UNARY_ADDRESS ||
			op.getKind() == UnaryOperator.Kind.UNARY_AUTO_TO_POINTER;
		int requestSize = requestAddress ? 4 : // アドレス
			op.getKind() == UnaryOperator.Kind.UNARY_LOGICAL_NOT ? 4 : // 整数として評価する
			op.getDataType().getWidth(); // データを要求する
		int dataSize = requestSize;
		if (op.getKind() != UnaryOperator.Kind.UNARY_SIZE) {
			generateExpressionEvaluation(op.getOperand(), requestSize, requestAddress);
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
			{
				String label = getNextLabel();
				out.println("\txor %ecx, %ecx");
				out.println("\ttest %eax, %eax");
				out.println("\tjnz " + label);
				out.println("\tinc %ecx");
				out.println(label + ":");
				out.println("\tmov %ecx, %eax");
			}
			dataSize = 4;
			break;
		case UNARY_BIT_NOT:
			out.println("\tnot %eax");
			break;
		case UNARY_DEREFERENCE:
			if (!wantAddress) {
				switch (op.getDataType().getWidth()) {
				case 1: out.println("\tmovb (%eax), %al"); break;
				case 2: out.println("\tmovw (%eax), %ax"); break;
				case 4: out.println("\tmovl (%eax), %eax"); break;
				default: throw new SystemLimitException("dereferencing " +
					op.getDataType().getWidth() + "-byte data is not supported");
				}
			}
			break;
		case UNARY_ADDRESS:
			// 下からアドレスが来るので、そのまま渡す
			// すなわち、何もしない
			break;
		case UNARY_SIZE:
			// 式を評価せず、結果のサイズを積む
			out.println("\tmov $" + op.getDataType().getWidth() + ", %eax");
			dataSize = 4;
			break;
		case UNARY_AUTO_TO_POINTER:
			// 下からポインタの値が来るので、そのまま渡す
			// すなわち、何もしない
			break;
		default:
			throw new SystemLimitException("unexpected kind of UnaryOperator: " + op.getKind());
		}
		if (!wantAddress) {
			boolean isSigned = op.getDataType() instanceof IntegerType && ((IntegerType)op.getDataType()).isSigned();
			if (dataSize == 1 && requestedSize == 2) {
				out.println((isSigned ? "\tmovsbw" : "\tmovzbw") + " %al, %ax");
			} else if (dataSize == 1 && requestedSize == 4) {
				out.println((isSigned ? "\tmovsbl" : "\tmovzbl") + " %al, %eax");
			} else if (dataSize == 2 && requestedSize == 4) {
				out.println((isSigned ? "\tmovswl" : "\tmovzwl") + " %ax, %eax");
			}
		}
		out.println("\tpush %eax");
	}
}
