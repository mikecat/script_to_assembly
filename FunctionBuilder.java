import java.util.List;
import java.util.ArrayList;

public class FunctionBuilder {
	private String name;
	private DataType dataType;
	private List<Variable> variableList;
	private List<Instruction> instructionList;

	public FunctionBuilder(String name, DataType dataType) {
		this.name = name;
		this.dataType = dataType;
		this.variableList = new ArrayList<Variable>();
		this.instructionList = new ArrayList<Instruction>();
	}

	public void addVariable(Variable var) {
		variableList.add(var);
	}

	public void addInstruction(Instruction inst) {
		instructionList.add(inst);
	}

	public Function toFunction() {
		return new Function(name, dataType, variableList, instructionList);
	}
}
