public class Identifier {
	private String name;
	private DataType dataType;
	private boolean global;

	public Identifier(String name, DataType dataType, boolean global) {
		this.name = name;
		this.dataType = dataType;
		this.global = global;
	}

	public String getName() {
		return name;
	}

	public DataType getDataType() {
		return dataType;
	}

	public boolean isGlobal() {
		return global;
	}
}
