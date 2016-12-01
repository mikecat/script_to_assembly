public class ArrayType extends Type {
	private Type elementsType;
	private int elementsNum;

	public ArrayType(Type elementsType, int elementsNum) {
		this.elementsType = elementsType;
		this.elementsNum = elementsNum;
	}

	public Type getElementsType() {
		return elementsType;
	}
	public int getElementsNum() {
		return elementsNum;
	}

	public int getWidth() {
		int elementWidth = elementsType.getWidth();
		if (elementsNum > Integer.MAX_VALUE / elementWidth) {
			throw new SystemLimitException("array size too big");
		}
		return elementWidth * elementsNum;
	}
}
