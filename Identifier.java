public class Identifier {
	public static enum Kind {
		GLOBAL_VARIABLE,
		LOCAL_VARIABLE,
		ARGUMENT,
		ADDRESS_VARIABLE
	}

	private String name;
	private DataType dataType;
	private Kind kind;
	private long value;

	public Identifier(String name, DataType dataType, Kind kind, long value) {
		this.name = name;
		this.dataType = dataType;
		this.kind = kind;
		this.value = value;
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

	public long getValue() {
		return value;
	}
}
