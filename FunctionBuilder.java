import java.util.List;
import java.util.ArrayList;

public class FunctionBuilder {
	private String name;
	private Type type;
	private List<Instruction> instructionList;

	public FunctionBuilder(String name, Type type) {
		this.name = name;
		this.type = type;
		this.instructionList = new ArrayList<Instruction>();
	}

	public void addInstruction(Instruction inst) {
		instructionList.add(inst);
	}

	public Function toFunction() {
		return new Function(name, type, instructionList);
	}
}
