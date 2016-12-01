public class ContinueInstruction extends Instruction {
	private int level;

	public ContinueInstruction() {
		this(1);
	}

	public ContinueInstruction(int level) {
		if (level <= 0) {
			throw new IllegalArgumentException("level must be positive");
		}
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
}
