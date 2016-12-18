public class AutomaticVariable extends Identifier {
	private int id;

	public AutomaticVariable(String name, DataType dataType, boolean global, int id) {
		super(name, dataType, global);
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
