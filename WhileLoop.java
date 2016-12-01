import java.util.List;

public class WhileLoop extends Instruction {
	private Expression condition;
	private Instruction[] instructionList;

	public WhileLoop(Expression condition, List<Instruction> instructionList) {
		this.condition = condition;
		this.instructionList = instructionList.toArray(new Instruction[instructionList.size()]);
	}

	public Expression getCondition() {
		return condition;
	}

	public int getInstructionNumber() {
		return instructionList.length;
	}

	public Instruction getInstruction(int index) {
		return instructionList[index];
	}
}
