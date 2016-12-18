public class AutomaticVariable extends Identifier {
	private int id;
	private boolean argument;

	public AutomaticVariable(String name, DataType dataType, int id, boolean argument) {
		super(name, dataType, false);
		this.id = id;
		this.argument = argument;
	}

	public int getId() {
		return id;
	}

	public boolean isArgument() {
		return argument;
	}
}
