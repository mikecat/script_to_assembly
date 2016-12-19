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
		out.println(".globl " + func.getName());
		out.println(func.getName() + ":");
		// 関数内の命令をそれぞれ出力する
		int instNum = func.getInstructionNumber();
		for (int i = 0; i < instNum; i++) {
			generateInstruction(func.getInstruction(i));
		}
		// 関数の終わり
		out.println("stoa.funcend." + func.getName() + ":");
		out.println("\tret");
	}

	private void generateInstruction(Instruction inst) {
		if (inst instanceof NormalExpression) {
			throw new SystemLimitException("NormalExpression not implemented yet");
		} else if (inst instanceof ReturnInstruction) {
			throw new SystemLimitException("ReturnInstruction not implemented yet");
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
}
