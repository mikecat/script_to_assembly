import java.util.List;

public class ConditionalBranch extends Instruction {
	private Expression condition;
	private Instruction[] thenInstructionList;
	private Instruction[] elseInstructionList;

	public ConditionalBranch(Expression condition, List<Instruction> thenInstructionList) {
		this(condition, thenInstructionList, null);
	}

	public ConditionalBranch(Expression condition, List<Instruction> thenInstructionList, List<Instruction> elseInstructionList) {
		this.condition = condition;
		this.thenInstructionList = thenInstructionList.toArray(new Instruction[thenInstructionList.size()]);
		if (elseInstructionList == null) {
			this.elseInstructionList = null;
		} else {
			this.elseInstructionList = elseInstructionList.toArray(new Instruction[elseInstructionList.size()]);
		}
	}

	public Expression getCondition() {
		return condition;
	}

	public int getThenInstructionNumber() {
		return thenInstructionList.length;
	}

	public Instruction getThenInstruction(int index) {
		return thenInstructionList[index];
	}

	public boolean hasElse() {
		return elseInstructionList != null;
	}

	public int getElseInstructionNumber() {
		return elseInstructionList.length;
	}

	public Instruction getElseInstruction(int index) {
		return elseInstructionList[index];
	}
}
