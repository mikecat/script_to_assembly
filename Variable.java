public class Variable {
	public static enum VariableKind {
		GLOBAL_VARIABLE,
		LOCAL_VARIABLE,
		ARGUMENT
	}

	private String name;
	private DataType dataType;
	private VariableKind kind;

	public Variable(String name, DataType dataType, VariableKind kind) {
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

	public VariableKind getVariableKind() {
		return kind;
	}
}
