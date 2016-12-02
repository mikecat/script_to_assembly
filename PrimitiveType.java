public class PrimitiveType extends DataType {
	private int width; // バイト数
	private boolean signed;

	public PrimitiveType(int width, boolean signed) {
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
		if (!(o instanceof PrimitiveType)) {
			return false;
		}
		PrimitiveType target = (PrimitiveType)o;
		return width == target.width && signed == target.signed;
	}
}
