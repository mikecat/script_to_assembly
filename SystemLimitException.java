public class SystemLimitException extends RuntimeException {
	public SystemLimitException() {
		super();
	}

	public SystemLimitException(String description) {
		super(description);
	}
}
