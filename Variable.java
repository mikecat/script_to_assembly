public class Variable {
	public static enum Kind {
		GLOBAL_VARIABLE,
		LOCAL_VARIABLE,
		ARGUMENT
	}

	private String name;
	private DataType dataType;
	private Kind kind;

	public Variable(String name, DataType dataType, Kind kind) {
		this.name = name;
		this.dataType = dataType;
		this.kind = kind;
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
}
