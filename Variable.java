public class Variable {
	public static enum Kind {
		GLOBAL_VARIABLE,
		LOCAL_VARIABLE,
		ARGUMENT
	}

	private String name;
	private DataType dataType;
	private Kind kind;
	private int id;

	public Variable(String name, DataType dataType, Kind kind, int id) {
		this.name = name;
		this.dataType = dataType;
		this.kind = kind;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public DataType getDataType() {
		return dataType;
	}

	public Kind getKind() {
		return kind;
	}

	public int getId() {
		return id;
	}
}
