import java.io.Writer;

public abstract class AssemblyGenerator {
	public abstract int getSystemIntSize();
	public abstract int getPointerSize();
	public abstract int getFunctionSize();
	public abstract void generateAssembly(Writer out,
		StaticVariable[] staticVariableDefinitionList,
		Function[] functionDefinitionList);
}
