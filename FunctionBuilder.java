import java.util.List;
import java.util.ArrayList;

public class FunctionBuilder extends InstructionBuilder {
	private String name;
	private DataType dataType;
	private List<Variable> variableList;
	private List<Instruction> instructionList;
	private int variableCount, argumentCount;

	public FunctionBuilder(String name, DataType dataType) {
		this.name = name;
		this.dataType = dataType;
		this.variableList = new ArrayList<Variable>();
		this.instructionList = new ArrayList<Instruction>();
		this.variableCount = 0;
		this.argumentCount = 0;
	}

	public String getInstructionName() {
		return "function";
	}

	public void addVariable(String name, DataType dataType) {
		variableList.add(new Variable(name, dataType, Variable.Kind.LOCAL_VARIABLE, variableCount++));
	}

	public void addArgument(String name, DataType dataType) {
		variableList.add(new Variable(name, dataType, Variable.Kind.ARGUMENT, argumentCount++));
	}

	public void addInstruction(Instruction inst) {
		instructionList.add(inst);
	}

	public Function toFunction() {
		return new Function(name, dataType, variableList, instructionList);
	}
}
