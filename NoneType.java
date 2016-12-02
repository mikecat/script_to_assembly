public class NoneType extends DataType {
	public int getWidth() {
		return 0;
	}

	public boolean equals(Object o) {
		return (o instanceof NoneType);
	}
}
