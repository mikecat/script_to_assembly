public class PointerType extends Type {
	private Type pointsAt;

	public PointerType(Type pointsAt) {
		this.pointsAt = pointsAt;
	}

	public Type getPointsAt() {
		return pointsAt;
	}

	public int getWidth() {
		return 4; // ターゲットに依存するので、後でなんとかする
	}
}
