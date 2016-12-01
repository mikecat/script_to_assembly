public class FunctionType extends DataType {
	private DataType returnType;

	public FunctionType(DataType returnType) {
		this.returnType = returnType;
	}

	public DataType getReturnType() {
		return returnType;
	}

	public int getWidth() {
		return 4; // ターゲットに依存するので後でなんとかする
	}
}
