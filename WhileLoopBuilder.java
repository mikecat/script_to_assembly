import java.util.List;
import java.util.ArrayList;

public class WhileLoopBuilder extends InstructionBuilder {
	private Expression condition;
	private List<Instruction> instructionList;

	public WhileLoopBuilder(Expression condition) {
		this.condition = condition;
		this.instructionList = new ArrayList<Instruction>();
	}

	public String getInstructionName() {
		return "while loop";
	}

	public void addInstruction(Instruction inst) {
		instructionList.add(inst);
	}

	public WhileLoop toWhileLoop() {
		return new WhileLoop(condition, instructionList);
	}
}
