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
}
