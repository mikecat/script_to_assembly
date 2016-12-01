import java.util.List;

public class InfiniteLoop extends Instruction {
	private Instruction[] instructionList;

	public InfiniteLoop(List<Instruction> instructionList) {
		this.instructionList = instructionList.toArray(new Instruction[instructionList.size()]);
	}

	public int getInstructionNumber() {
		return instructionList.length;
	}

	public Instruction getInstruction(int index) {
		return instructionList[index];
	}
}
