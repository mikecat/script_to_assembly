import java.io.Writer;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

public class LPC1114FN28Generator extends AssemblyGenerator {
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
	private List<Long> integerLiteralList;

	private String getNextLabel() {
		return "stoa.label." + (nextLabelId++);
	}

	public void generateAssembly(Writer out,
	StaticVariable[] staticVariableDefinitionList,
	Function[] functionDefinitionList) {
		this.out = new PrintWriter(out);
		nextLabelId = 0;
		stringLiteralList = new ArrayList<String>();
		integerLiteralList = new ArrayList<Long>();
		continueLabel = new ArrayList<String>();
		breakLabel = new ArrayList<String>();

		// プログラムを出力する
		for (int i = 0; i < functionDefinitionList.length; i++) {
			generateFunction(functionDefinitionList[i]);
		}

		// 文字列リテラルを出力する
		for (int i = 0; i < stringLiteralList.size(); i++) {
			this.out.println("stoa.string" + i + ":");
			this.out.print("\tdb ");
			byte[] b = stringLiteralList.get(i).getBytes();
			for (int j = 0; j < b.length; j++) {
				int num = b[j];
				if (num < 0) num += 256;
				this.out.print(num + ", ");
			}
			this.out.println("0");
		}

		// 数値リテラルを出力する
		this.out.println("\talign 4");
		for (int i = 0; i < integerLiteralList.size(); i++) {
			this.out.println("stoa.integer" + i + ":");
			this.out.println("\tdd " + integerLiteralList.get(i));
		}

		// データの領域を出力する
		this.out.println("absolute 0x10000000");
		for (int i = 0; i < staticVariableDefinitionList.length; i++) {
			StaticVariable sv = staticVariableDefinitionList[i];
			this.out.println("\talign 4");
			this.out.println(sv.getName() + ": resb " + sv.getDataType().getWidth());
		}

		this.out.flush();
	}

	private void generateFunction(Function func) {
		// 自動変数のオフセットを計算する
		currentFunctionVariableOffset = new ArrayList<Integer>();
		int variableNum = func.getVariableNumber();
		int variableOffset = 0;
		int argumentOffset = 24;
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
		if (variableOffset > 508) {
			throw new SystemLimitException("local variables too big to support in this version");
		}
		// 関数の開始
		currentFunctionName = func.getName();
		currentFunctionReturnType = func.getReturnType();
		out.println(currentFunctionName + ":");
		out.println("\tPUSH {R4-R7,LR}");
		out.println("\tMOV R1, R12");
		out.println("\tPUSH {R1}");
		out.println("\tMOV R12, SP");
		if (variableOffset != 0) out.println("\tSUB SP, #" + (-(long)variableOffset));
		// 関数内の命令をそれぞれ出力する
		int instNum = func.getInstructionNumber();
		for (int i = 0; i < instNum; i++) {
			generateInstruction(func.getInstruction(i));
		}
		// 関数の終わり
		out.println("stoa.funcend." + currentFunctionName + ":");
		out.println("\tMOV SP, R12");
		out.println("\tPOP {R1}");
		out.println("\tMOV R12, R1");
		out.println("\tPOV {R4-R7,PC}");
	}

	private void generateInstruction(Instruction inst) {
		if (inst instanceof NormalExpression) {
			Expression expr = ((NormalExpression)inst).getExpression();
			generateExpressionEvaluation(expr, 0, false,7, 0x3f);
		} else if (inst instanceof ReturnInstruction) {
			ReturnInstruction ret = (ReturnInstruction)inst;
			if (ret.hasExpression()) {
				generateExpressionEvaluation(ret.getExpression(),
					currentFunctionReturnType.getWidth(), false, 0, 0xfe);
			}
			out.println("\tBL stoa.funcend." + currentFunctionName);
		} else if (inst instanceof ConditionalBranch) {
			ConditionalBranch cb = (ConditionalBranch)inst;
			int insNum;
			generateExpressionEvaluation(cb.getCondition(), 4, false, 7, 0x3f);
			out.println("\tTST R7, R7");
			String label1 = getNextLabel();
			String label2 = getNextLabel();
			String label3 = null;
			out.println("\tBNE " + label1);
			out.println("\tBL " + label2);
			out.println(label1 + ":");
			insNum = cb.getThenInstructionNumber();
			for (int i = 0; i < insNum; i++) {
				generateInstruction(cb.getThenInstruction(i));
			}
			if (cb.hasElse()) {
				label3 = getNextLabel();
				out.println("\tBL " + label3);
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
			out.println("\tBL " + label1);
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
			generateExpressionEvaluation(wLoop.getCondition(), 4, false, 7, 0x3f);
			out.println("\tTST R7, R7");
			out.println("\tBNE " + label3);
			out.println("\tBL " + label2);
			out.println(label3 + ":");
			for (int i = 0; i < insNum; i++) {
				generateInstruction(wLoop.getInstruction(i));
			}
			out.println("\tBL " + label1);
			out.println(label2 + ":");
			continueLabel.remove(continueLabel.size() - 1);
			breakLabel.remove(breakLabel.size() - 1);
		} else if (inst instanceof BreakInstruction) {
			BreakInstruction bi = (BreakInstruction)inst;
			int level = bi.getLevel();
			if (level > breakLabel.size()) {
				throw new SyntaxException("break level too high");
			}
			out.println("\tBL " + breakLabel.get(breakLabel.size() - level));
		} else if (inst instanceof ContinueInstruction) {
			ContinueInstruction ci = (ContinueInstruction)inst;
			int level = ci.getLevel();
			if (level > continueLabel.size()) {
				throw new SyntaxException("coneinue level too high");
			}
			out.println("\tBL " + continueLabel.get(continueLabel.size() - level));
		}
	}

	private void generateExpressionEvaluation(Expression expr, int requestedSize, boolean wantAddress,
	int outRegister, int availableRegisters) {
	}
}
