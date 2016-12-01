public class BreakInstruction extends Instruction {
	private int level;

	public BreakInstruction() {
		this(1);
	}

	public BreakInstruction(int level) {
		if (level <= 0) {
			throw new IllegalArgumentException("level must be positive");
		}
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
}
