public class IntegerType extends DataType {
	private int width; // バイト数
	private boolean signed;

	public IntegerType(int width, boolean signed) {
		this.width = width;
		this.signed = signed;
	}

	public int getWidth() {
		return width;
	}
	public boolean isSigned() {
		return signed;
	}

	public boolean equals(Object o) {
		if (!(o instanceof IntegerType)) {
			return false;
		}
		IntegerType target = (IntegerType)o;
		return width == target.width && signed == target.signed;
	}
}
