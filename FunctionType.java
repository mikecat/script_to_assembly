public class FunctionType extends Type {
	private Type returnType;

	public FunctionType(Type returnType) {
		this.returnType = returnType;
	}

	public Type getReturnType() {
		return returnType;
	}

	public int getWidth() {
		return 4; // ターゲットに依存するので後でなんとかする
	}
}
