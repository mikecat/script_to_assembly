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
	}
}
