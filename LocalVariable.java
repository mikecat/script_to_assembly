public class LocalVariable extends Identifier {
	private int id;

	public LocalVariable(String name, DataType dataType, boolean global, int id) {
		super(name, dataType, global);
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
