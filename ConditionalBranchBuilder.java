import java.util.List;
import java.util.ArrayList;

public class ConditionalBranchBuilder extends InstructionBuilder {
	private ConditionalBranchBuilder elseifChain;
	private Expression condition;
	private List<Instruction> thenInstructionList;
	private List<Instruction> elseInstructionList;
	private boolean elseMode;

	public ConditionalBranchBuilder(Expression condition) {
		this(condition, null);
	}

	public ConditionalBranchBuilder(Expression condition, ConditionalBranchBuilder elseifChain) {
		this.elseifChain = elseifChain;
		this.condition = condition;
		this.thenInstructionList = new ArrayList<Instruction>();
		this.elseMode = false;
	}

	public String getInstructionName() {
		return "conditional branch";
	}

	public ConditionalBranchBuilder getElseifChain() {
		return elseifChain;
	}

	public boolean isElseMode() {
		return elseMode;
	}

	public void enterElseMode() {
		if (elseMode) {
			throw new IllegalStateException("already in else mode");
		}
		elseMode = true;
		elseInstructionList = new ArrayList<Instruction>();
	}

	public void addInstruction(Instruction inst) {
		if (elseMode) {
			elseInstructionList.add(inst);
		} else {
			thenInstructionList.add(inst);
		}
	}

	public ConditionalBranch toConditionalBranch() {
		ConditionalBranch cb;
		ConditionalBranchBuilder nextBuilder = elseifChain;
		if (elseMode) {
			cb = new ConditionalBranch(condition, thenInstructionList, elseInstructionList);
		} else {
			cb = new ConditionalBranch(condition, thenInstructionList);
		}
		while (nextBuilder != null) {
			nextBuilder.addInstruction(cb);
			cb = nextBuilder.toConditionalBranch();
			nextBuilder = nextBuilder.getElseifChain();
		}
		return cb;
	}
}
