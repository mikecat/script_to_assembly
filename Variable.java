public class Variable {
	public static enum VariableKind {
		GLOBAL_VARIABLE,
		LOCAL_VARIABLE,
		ARGUMENT
	}

	private String name;
	private Type type;
	private VariableKind kind;

	public Variable(String name, Type type, VariableKind kind) {
		this.name = name;
		this.type = type;
		this.kind = kind;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public VariableKind getVariableKind() {
		return kind;
	}
}
