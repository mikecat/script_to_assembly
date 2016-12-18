public class DefinedValue extends Identifier {
	private long value;

	public DefinedValue(String name, DataType dataType, boolean global, long value) {
		super(name, dataType, global);
		this.value = value;
	}

	public long getValue() {
		return value;
	}
}
