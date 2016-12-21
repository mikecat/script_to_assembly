import java.util.List;
import java.util.ArrayList;

public class FunctionBuilder extends InstructionBuilder {
	private String name;
	private DataType returnType;
	private List<AutomaticVariable> variableList;
	private List<Instruction> instructionList;
	private int variableCount, argumentCount;

	public FunctionBuilder(String name, DataType returnType) {
		this.name = name;
		this.returnType = returnType;
		this.variableList = new ArrayList<AutomaticVariable>();
		this.instructionList = new ArrayList<Instruction>();
		this.variableCount = 0;
	}

	public String getInstructionName() {
		return "function";
	}

	public Identifier addVariable(String name, DataType dataType, boolean isArgument) {
		AutomaticVariable newVariable = new AutomaticVariable(name, dataType, variableCount++, isArgument);
		variableList.add(newVariable);
		return newVariable;
	}

	public void addInstruction(Instruction inst) {
		instructionList.add(inst);
	}

	public Function toFunction() {
		return new Function(name, returnType, variableList, instructionList);
	}
}
