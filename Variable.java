public class Variable extends Identifier {
	private int id;

	public Variable(String name, DataType dataType, boolean global, int id) {
		super(name, dataType, global);
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
