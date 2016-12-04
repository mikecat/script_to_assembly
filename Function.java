import java.util.List;

public class Function {
	private String name;
	private DataType dataType;
	private Identifier[] variableList;
	private Instruction[] instructionList;

	public Function(String name, DataType dataType, List<Identifier> variableList, List<Instruction> instructionList) {
		this.name = name;
		this.dataType = dataType;
		this.variableList = variableList.toArray(new Identifier[variableList.size()]);
		this.instructionList = instructionList.toArray(new Instruction[instructionList.size()]);
	}

	public String getName() {
		return name;
	}

	public DataType getDataType() {
		return dataType;
	}

	public int getVariableNumber() {
		return variableList.length;
	}

	public Identifier getVariable(int index) {
		return variableList[index];
	}

	public int getInstructionNumber() {
		return instructionList.length;
	}

	public Instruction getInstruction(int index) {
		return instructionList[index];
	}
}
