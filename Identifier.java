public class Identifier {
	private String name;
	private DataType dataType;
	private boolean globalDeclaration;

	public Identifier(String name, DataType dataType, boolean globalDeclaration) {
		this.name = name;
		this.dataType = dataType;
		this.globalDeclaration = globalDeclaration;
	}

	public String getName() {
		return name;
	}

	public DataType getDataType() {
		return dataType;
	}

	public boolean isGlobalDeclaration() {
		return globalDeclaration;
	}
}
