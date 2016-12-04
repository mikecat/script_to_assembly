public class PointerType extends DataType {
	private DataType pointsAt;

	public PointerType(DataType pointsAt) {
		this.pointsAt = pointsAt;
	}

	public DataType getPointsAt() {
		return pointsAt;
	}

	public int getWidth() {
		return DataType.getPointerSize();
	}

	public boolean equals(Object o) {
		if (!(o instanceof PointerType)) {
			return false;
		}
		PointerType target = (PointerType)o;
		return pointsAt.equals(target.pointsAt);
	}
}
