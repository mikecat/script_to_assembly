import java.util.List;

public class Function {
	private String name;
	private Type type;
	private Variable[] variableList;
	private Instruction[] instructionList;

	public Function(String name, Type type, List<Variable> variableList, List<Instruction> instructionList) {
		this.name = name;
		this.type = type;
		this.variableList = variableList.toArray(new Variable[variableList.size()]);
		this.instructionList = instructionList.toArray(new Instruction[instructionList.size()]);
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public int getVariableNumber() {
		return variableList.length;
	}

	public Variable getVariable(int index) {
		return variableList[index];
	}

	public int getInstructionNumber() {
		return instructionList.length;
	}

	public Instruction getInstruction(int index) {
		return instructionList[index];
	}
}
