import java.util.List;
import java.util.ArrayList;

public class InfiniteLoopBuilder extends InstructionBuilder {
	private List<Instruction> instructionList;

	public InfiniteLoopBuilder() {
		this.instructionList = new ArrayList<Instruction>();
	}

	public String getInstructionName() {
		return "infinite loop";
	}

	public void addInstruction(Instruction inst) {
		instructionList.add(inst);
	}

	public InfiniteLoop toInfiniteLoop() {
		return new InfiniteLoop(instructionList);
	}
}
