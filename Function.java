import java.util.List;

public class Function {
	private String name;
	private Type type;
	private Instruction[] instructionList;

	public Function(String name, Type type, List<Instruction> instructionList) {
		this.name = name;
		this.type = type;
		this.instructionList = instructionList.toArray(new Instruction[instructionList.size()]);
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public int getInstructionNumber() {
		return instructionList.length;
	}

	public Instruction getInstruction(int index) {
		return instructionList[index];
	}
}
