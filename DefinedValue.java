public class DefinedValue extends Identifier {
	private long value;
	private int width;
	private boolean signed;

	public DefinedValue(String name, boolean global, long value, int width, boolean signed) {
		super(name, new IntegerType(width, signed), global);
		this.value = value;
		this.width = width;
		this.signed = signed;
		if (this.signed && this.value < 0) {
			this.value |= ~((((1L << (8 * width - 1)) - 1) << 1) + 1);
		} else {
			this.value &= (((1L << (8 * width - 1)) - 1) << 1) + 1;
		}
	}

	public long getValue() {
		return value;
	}

	public int getWidth() {
		return width;
	}

	public boolean isSigned() {
		return signed;
	}
}
