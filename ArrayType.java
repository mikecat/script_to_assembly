public class ArrayType extends DataType {
	private DataType elementDataType;
	private int elementsNum;

	public ArrayType(DataType elementDataType, int elementsNum) {
		this.elementDataType = elementDataType;
		this.elementsNum = elementsNum;
	}

	public DataType getElementDataType() {
		return elementDataType;
	}
	public int getElementsNum() {
		return elementsNum;
	}

	public int getWidth() {
		int elementWidth = elementDataType.getWidth();
		if (elementsNum > Integer.MAX_VALUE / elementWidth) {
			throw new SystemLimitException("array size too big");
		}
		return elementWidth * elementsNum;
	}
}
