import java.util.List;

public class Function {
	private String name;
	private DataType returnType;
	private AutomaticVariable[] variableList;
	private Instruction[] instructionList;

	public Function(String name, DataType returnType,
	List<AutomaticVariable> variableList, List<Instruction> instructionList) {
		this.name = name;
		this.returnType = returnType;
		this.variableList = variableList.toArray(new AutomaticVariable[variableList.size()]);
		this.instructionList = instructionList.toArray(new Instruction[instructionList.size()]);
	}

	public String getName() {
		return name;
	}

	public DataType getReturnType() {
		return returnType;
	}

	public int getVariableNumber() {
		return variableList.length;
	}

	public AutomaticVariable getVariable(int index) {
		return variableList[index];
	}

	public int getInstructionNumber() {
		return instructionList.length;
	}

	public Instruction getInstruction(int index) {
		return instructionList[index];
	}
}
