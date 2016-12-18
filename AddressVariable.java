public class AddressVariable extends Identifier {
	private long address;

	public AddressVariable(String name, DataType dataType, boolean global, long address) {
		super(name, dataType, global);
		this.address = address;
	}

	public long getAddress() {
		return address;
	}
}
