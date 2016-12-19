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
		this.out.println(".section .text");
		for (int i = 0; i < functionDefinitionList.length; i++) {
			generateFunction(functionDefinitionList[i]);
		}
		this.out.flush();
	}

	private void generateFunction(Function func) {
		out.println(".globl " + func.getName());
		out.println(func.getName() + ":");
	}
}
